/**********************************************************************************
*
* $Id$
*
***********************************************************************************
*
* Copyright (c) 2003, 2004, 2005 The Regents of the University of Michigan, Trustees of Indiana University,
*                  Board of Trustees of the Leland Stanford, Jr., University, and The MIT Corporation
* 
* Licensed under the Educational Community License Version 1.0 (the "License");
* By obtaining, using and/or copying this Original Work, you agree that you have read,
* understand, and will comply with the terms and conditions of the Educational Community License.
* You may obtain a copy of the License at:
* 
*      http://cvs.sakaiproject.org/licenses/license_1_0.html
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
* AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
* DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
**********************************************************************************/

package org.sakaiproject.tool.gradebook.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sakaiproject.tool.gradebook.Assignment;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.tool.gradebook.facades.Enrollment;
import org.sakaiproject.tool.gradebook.facades.standalone.dataload.UserLoader;

/**
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman </a>
 *
 */
public class TestGradeLoader extends GradebookLoaderTestBase {

	public void testPopulate() throws Exception {
        List gradebooks = new ArrayList();


        // Fetch the first gradebook.
        Gradebook gb = gradebookManager.getGradebook(TestGradebookLoader.GRADEBOOK_WITH_GRADES);

		Set enrollments = courseManagement.getEnrollments(gb.getUid());
		List assignments = gradableObjectManager.getAssignments(gb.getId());

		for(Iterator asnIter = assignments.iterator(); asnIter.hasNext();) {
			Assignment asn = (Assignment)asnIter.next();

			// Don't add grades for at least one assignment.
			if (asn.getName().equals(TestGradebookLoader.ASN_NO_DUE_DATE_NAME)) {
				continue;
			}

			Map map = new HashMap(); // Stores studentId->grade
			for(Iterator enrIter = enrollments.iterator(); enrIter.hasNext();) {
				Enrollment enr = (Enrollment)enrIter.next();
                // Don't add grades for those no good lazy students
                if(!enr.getUser().getUserUid().equals(UserLoader.AUTHID_WITHOUT_GRADES_1) &&
                        !enr.getUser().getUserUid().equals(UserLoader.AUTHID_WITHOUT_GRADES_2)) {
                    Double grade = new Double(Math.ceil(asn.getPointsPossible().doubleValue() * Math.random()));
                    if(asn.isExternallyMaintained()) {
                        gradebookService.updateExternalAssessmentScore(gb.getUid(), asn.getExternalId(), enr.getUser().getUserUid(), grade);
                    } else {
                        map.put(enr.getUser().getUserUid(), grade);
                    }
                }
			}

            // Save the internal assignment scores
            if(!asn.isExternallyMaintained()) {
            	gradeManager.updateAssignmentGradeRecords(asn.getId(), map);
            }
		}
        // Ensure that this is actually saved to the database
        setComplete();
	}
}
/**************************************************************************************************************************************************************************************************************************************************************
 * $Id$
 *************************************************************************************************************************************************************************************************************************************************************/
