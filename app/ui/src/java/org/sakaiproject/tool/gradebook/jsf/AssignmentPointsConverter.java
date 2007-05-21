/**********************************************************************************
 *
 * $Id$
 *
 ***********************************************************************************
 *
 * Copyright (c) 2005 The Regents of the University of California, The MIT Corporation
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

package org.sakaiproject.tool.gradebook.jsf;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.tool.gradebook.AbstractGradeRecord;
import org.sakaiproject.tool.gradebook.Assignment;
import org.sakaiproject.tool.gradebook.AssignmentGradeRecord;
import org.sakaiproject.tool.gradebook.GradableObject;

/**
 * This formatting-only converver consolidates the rather complex formatting
 * logic for assignment and assignment grade points. If the points are null,
 * they should be displayed in a special way. If the points belong to an
 * assignment which doesn't count toward the final grade, they should be
 * displayed in a special way with a tooltip "title" attribute.
 */
public class AssignmentPointsConverter extends PointsConverter {
	private static final Log log = LogFactory.getLog(AssignmentPointsConverter.class);

	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (log.isDebugEnabled()) log.debug("getAsString(" + context + ", " + component + ", " + value + ")");

		String formattedScore;
		boolean notCounted = false;
		Object workingValue = value;
		boolean percentage = false;
		
		if (value != null) {
			if (value instanceof Assignment) {
				Assignment assignment = (Assignment)value;
				workingValue = assignment.getPointsPossible();
				notCounted = assignment.isNotCounted();
				// if weighting enabled, item is not counted if not assigned
				// a category
				if (!notCounted && assignment.getGradebook().getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY) {
					notCounted = assignment.getCategory() == null;
				}
			} else if (value instanceof AbstractGradeRecord) {
				 if(
					value instanceof AssignmentGradeRecord
						&&
			       ((GradableObject)((AbstractGradeRecord)value).getGradableObject()).getGradebook().getGrade_type() 
						== GradebookService.GRADE_TYPE_POINTS 
//						&&
//				   ((GradableObject)((AbstractGradeRecord)value).getGradableObject()).getGradebook().getCategory_type() 
//				   		!= GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY
				   			){
					//if grade by points and no category weighting
					workingValue = ((AbstractGradeRecord)value).getPointsEarned();
				} else {
					//display percentage
					percentage = true;
					workingValue = ((AbstractGradeRecord)value).getGradeAsPercentage();

					if (value instanceof AssignmentGradeRecord) {
						Assignment assignment = ((AssignmentGradeRecord)value).getAssignment();
						notCounted = assignment.isNotCounted();
						// if weighting enabled, item is only counted if assigned
						// a category
						if (!notCounted && assignment.getGradebook().getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY) {
							notCounted = assignment.getCategory() == null;
						}
					}
				}
			}
		}
		formattedScore = super.getAsString(context, component, workingValue);
		if (notCounted) {
			formattedScore = FacesUtil.getLocalizedString("score_not_counted",
					new String[] {formattedScore, FacesUtil.getLocalizedString("score_not_counted_tooltip")});
		}
		if(percentage && workingValue != null){
			formattedScore += "%";
		}
		return formattedScore;
	}
}
