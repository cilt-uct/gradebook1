/**********************************************************************************
*
* $Id$
*
***********************************************************************************
*
* Copyright (c) 2007 The Regents of the University of California
*
* Licensed under the Educational Community License, Version 1.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.opensource.org/licenses/ecl1.php
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
**********************************************************************************/
package org.sakaiproject.component.gradebook;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.sakaiproject.service.gradebook.shared.AssessmentNotFoundException;
import org.sakaiproject.service.gradebook.shared.AssignmentHasIllegalPointsException;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.ConflictingExternalIdException;
import org.sakaiproject.service.gradebook.shared.GradebookExternalAssessmentService;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.tool.gradebook.Assignment;
import org.sakaiproject.tool.gradebook.AssignmentGradeRecord;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class GradebookExternalAssessmentServiceImpl extends BaseHibernateManager implements GradebookExternalAssessmentService {
	private static final Log log = LogFactory.getLog(GradebookExternalAssessmentServiceImpl.class);
    // Special logger for data contention analysis.
    private static final Log logData = LogFactory.getLog(GradebookExternalAssessmentServiceImpl.class.getName() + ".GB_DATA");

	public void addExternalAssessment(final String gradebookUid, final String externalId, final String externalUrl,
			final String title, final double points, final Date dueDate, final String externalServiceDescription)
            throws ConflictingAssignmentNameException, ConflictingExternalIdException, GradebookNotFoundException {

        // Ensure that the required strings are not empty
        if(StringUtils.trimToNull(externalServiceDescription) == null ||
                StringUtils.trimToNull(externalId) == null ||
                StringUtils.trimToNull(title) == null) {
            throw new RuntimeException("External service description, externalId, and title must not be empty");
        }

        // Ensure that points is > zero
        if(points <= 0) {
            throw new AssignmentHasIllegalPointsException("Points must be > 0");
        }

        // Ensure that the assessment name is unique within this gradebook
		if (isAssignmentDefined(gradebookUid, title)) {
            throw new ConflictingAssignmentNameException("An assignment with that name already exists in gradebook uid=" + gradebookUid);
        }

		getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				// Ensure that the externalId is unique within this gradebook
				Integer externalIdConflicts = (Integer)session.createQuery(
					"select count(asn) from Assignment as asn where asn.externalId=? and asn.gradebook.uid=?").
					setString(0, externalId).
					setString(1, gradebookUid).
					uniqueResult();
				if (externalIdConflicts.intValue() > 0) {
					throw new ConflictingExternalIdException("An external assessment with that ID already exists in gradebook uid=" + gradebookUid);
				}

				// Get the gradebook
				Gradebook gradebook = getGradebook(gradebookUid);

				// Create the external assignment
				Assignment asn = new Assignment(gradebook, title, new Double(points), dueDate);
				asn.setExternallyMaintained(true);
				asn.setExternalId(externalId);
				asn.setExternalInstructorLink(externalUrl);
				asn.setExternalStudentLink(externalUrl);
				asn.setExternalAppName(externalServiceDescription);
                //set released to be true to support selective release
                asn.setReleased(true);

                session.save(asn);
				return null;
			}
		});
        if (log.isInfoEnabled()) log.info("External assessment added to gradebookUid=" + gradebookUid + ", externalId=" + externalId + " by userUid=" + getUserUid() + " from externalApp=" + externalServiceDescription);
	}

	/**
	 * @see org.sakaiproject.service.gradebook.shared.GradebookService#updateExternalAssessment(java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, java.util.Date)
     */
    public void updateExternalAssessment(final String gradebookUid, final String externalId, final String externalUrl,
                                         final String title, final double points, final Date dueDate) throws GradebookNotFoundException, AssessmentNotFoundException,AssignmentHasIllegalPointsException {
        final Assignment asn = getExternalAssignment(gradebookUid, externalId);

        if(asn == null) {
            throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
        }

        // Ensure that points is > zero
        if(points <= 0) {
            throw new AssignmentHasIllegalPointsException("Points must be > 0");
        }

        // Ensure that the required strings are not empty
        if( StringUtils.trimToNull(externalId) == null ||
                StringUtils.trimToNull(title) == null) {
            throw new RuntimeException("ExternalId, and title must not be empty");
        }

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                asn.setExternalInstructorLink(externalUrl);
                asn.setExternalStudentLink(externalUrl);
                asn.setName(title);
                asn.setDueDate(dueDate);
                //support selective release
                asn.setReleased(true);
                asn.setPointsPossible(new Double(points));
                session.update(asn);
                if (log.isInfoEnabled()) log.info("External assessment updated in gradebookUid=" + gradebookUid + ", externalId=" + externalId + " by userUid=" + getUserUid());
                return null;

            }
        };
        getHibernateTemplate().execute(hc);
	}

	/**
	 * @see org.sakaiproject.service.gradebook.shared.GradebookService#removeExternalAssessment(java.lang.String, java.lang.String)
	 */
	public void removeExternalAssessment(final String gradebookUid,
            final String externalId) throws GradebookNotFoundException, AssessmentNotFoundException {
        // Get the external assignment
        final Assignment asn = getExternalAssignment(gradebookUid, externalId);
        if(asn == null) {
            throw new AssessmentNotFoundException("There is no external assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
        }

        // We need to go through Spring's HibernateTemplate to do
        // any deletions at present. See the comments to deleteGradebook
        // for the details.
        HibernateTemplate hibTempl = getHibernateTemplate();

        List toBeDeleted = hibTempl.find("from AssignmentGradeRecord as agr where agr.gradableObject=?", asn);
        int numberDeleted = toBeDeleted.size();
        hibTempl.deleteAll(toBeDeleted);
        if (log.isInfoEnabled()) log.info("Deleted " + numberDeleted + " externally defined scores");

        // Delete the assessment.
		hibTempl.flush();
		hibTempl.clear();
		hibTempl.delete(asn);

        if (log.isInfoEnabled()) log.info("External assessment removed from gradebookUid=" + gradebookUid + ", externalId=" + externalId + " by userUid=" + getUserUid());
	}

    private Assignment getExternalAssignment(final String gradebookUid, final String externalId) throws GradebookNotFoundException {
        final Gradebook gradebook = getGradebook(gradebookUid);

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
				return session.createQuery(
					"from Assignment as asn where asn.gradebook=? and asn.externalId=?").
					setEntity(0, gradebook).
					setString(1, externalId).
					uniqueResult();
            }
        };
        return (Assignment)getHibernateTemplate().execute(hc);
    }

	/**
	 * @see org.sakaiproject.service.gradebook.shared.GradebookService#updateExternalAssessmentScore(java.lang.String, java.lang.String, java.lang.String, Double)
	 */
	public void updateExternalAssessmentScore(final String gradebookUid, final String externalId,
			final String studentUid, final Double points) throws GradebookNotFoundException, AssessmentNotFoundException {

        final Assignment asn = getExternalAssignment(gradebookUid, externalId);

        if(asn == null) {
            throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
        }

        if (logData.isDebugEnabled()) logData.debug("BEGIN: Update 1 score for gradebookUid=" + gradebookUid + ", external assessment=" + externalId + " from " + asn.getExternalAppName());

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Date now = new Date();

                AssignmentGradeRecord agr = getAssignmentGradeRecord(asn, studentUid, session);

                // Try to reduce data contention by only updating when the
                // score has actually changed.
                Double oldPointsEarned = (agr == null) ? null : agr.getPointsEarned();
                if ( ((points != null) && (!points.equals(oldPointsEarned))) ||
					((points == null) && (oldPointsEarned != null)) ) {
					if (agr == null) {
						agr = new AssignmentGradeRecord(asn, studentUid, points);
					} else {
						agr.setPointsEarned(points);
					}

					agr.setDateRecorded(now);
					agr.setGraderId(getUserUid());
					if (log.isDebugEnabled()) log.debug("About to save AssignmentGradeRecord id=" + agr.getId() + ", version=" + agr.getVersion() + ", studenttId=" + agr.getStudentId() + ", pointsEarned=" + agr.getPointsEarned());
					session.saveOrUpdate(agr);

					// Sync database.
					session.flush();
					session.clear();
				} else {
					if(log.isDebugEnabled()) log.debug("Ignoring updateExternalAssessmentScore, since the new points value is the same as the old");
				}
                return null;
            }
        };
        getHibernateTemplate().execute(hc);
        if (logData.isDebugEnabled()) logData.debug("END: Update 1 score for gradebookUid=" + gradebookUid + ", external assessment=" + externalId + " from " + asn.getExternalAppName());
		if (log.isDebugEnabled()) log.debug("External assessment score updated in gradebookUid=" + gradebookUid + ", externalId=" + externalId + " by userUid=" + getUserUid() + ", new score=" + points);
	}

	public void updateExternalAssessmentScores(final String gradebookUid, final String externalId, final Map studentUidsToScores)
		throws GradebookNotFoundException, AssessmentNotFoundException {

        final Assignment assignment = getExternalAssignment(gradebookUid, externalId);
        if (assignment == null) {
            throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
        }
		final Set studentIds = studentUidsToScores.keySet();
		if (studentIds.isEmpty()) {
			return;
		}
		final Date now = new Date();
		final String graderId = getUserUid();

		getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				List existingScores;
				if (studentIds.size() <= MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST) {
					Query q = session.createQuery("from AssignmentGradeRecord as gr where gr.gradableObject=:go and gr.studentId in (:studentIds)");
					q.setParameter("go", assignment);
					q.setParameterList("studentIds", studentIds);
					existingScores = q.list();
				} else {
					Query q = session.createQuery("from AssignmentGradeRecord as gr where gr.gradableObject=:go");
					q.setParameter("go", assignment);
					existingScores = filterGradeRecordsByStudents(q.list(), studentIds);
				}

				Set previouslyUnscoredStudents = new HashSet(studentIds);
				Set changedStudents = new HashSet();
				for (Iterator iter = existingScores.iterator(); iter.hasNext(); ) {
					AssignmentGradeRecord agr = (AssignmentGradeRecord)iter.next();
					String studentUid = agr.getStudentId();
					previouslyUnscoredStudents.remove(studentUid);

					// Try to reduce data contention by only updating when a score
					// has changed.
					Double oldPointsEarned = agr.getPointsEarned();
					Double newPointsEarned = (Double)studentUidsToScores.get(studentUid);
					if ( ((newPointsEarned != null) && (!newPointsEarned.equals(oldPointsEarned))) || ((newPointsEarned == null) && (oldPointsEarned != null)) ) {
						agr.setDateRecorded(now);
						agr.setGraderId(graderId);
						agr.setPointsEarned(newPointsEarned);
						session.update(agr);
						changedStudents.add(studentUid);
					}
				}
				for (Iterator iter = previouslyUnscoredStudents.iterator(); iter.hasNext(); ) {
					String studentUid = (String)iter.next();

					// Don't save unnecessary null scores.
					Double newPointsEarned = (Double)studentUidsToScores.get(studentUid);
					if (newPointsEarned != null) {
						AssignmentGradeRecord agr = new AssignmentGradeRecord(assignment, studentUid, newPointsEarned);
						agr.setDateRecorded(now);
						agr.setGraderId(graderId);
						session.save(agr);
						changedStudents.add(studentUid);
					}
				}

				if (log.isDebugEnabled()) log.debug("updateExternalAssessmentScores sent " + studentIds.size() + " records, actually changed " + changedStudents.size());

				// Sync database.
				session.flush();
				session.clear();
                return null;
            }
        });
	}

	public boolean isAssignmentDefined(final String gradebookUid, final String assignmentName)
        throws GradebookNotFoundException {
        Assignment assignment = (Assignment)getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				return getAssignmentWithoutStats(gradebookUid, assignmentName, session);
			}
		});
        return (assignment != null);
    }

	public boolean isExternalAssignmentDefined(String gradebookUid, String externalId) throws GradebookNotFoundException {
        Assignment assignment = getExternalAssignment(gradebookUid, externalId);
        return (assignment != null);
	}

}