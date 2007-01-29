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

import java.io.Externalizable;
import java.util.Collection;
import java.util.Map;

import org.sakaiproject.service.gradebook.shared.Assignment;

public class GradebookDefinition extends VersionedExternalizable implements Externalizable {	
	private static final long serialVersionUID = 1L;
	public static final String EXTERNALIZABLE_VERSION = "1";

	private String selectedGradingScaleUid;
	private Map<String, Double> selectedGradingScaleBottomPercents;
	private Collection<Assignment> assignments;
	
    public GradebookDefinition() {
	}
	
	public String getExternalizableVersion() {
		return EXTERNALIZABLE_VERSION;
	}

	public Collection<Assignment> getAssignments() {
		return assignments;
	}
	public void setAssignments(Collection<Assignment> assignments) {
		this.assignments = assignments;
	}
	public Map<String, Double> getSelectedGradingScaleBottomPercents() {
		return selectedGradingScaleBottomPercents;
	}
	public void setSelectedGradingScaleBottomPercents(Map<String, Double> selectedGradingScaleBottomPercents) {
		this.selectedGradingScaleBottomPercents = selectedGradingScaleBottomPercents;
	}
	public String getSelectedGradingScaleUid() {
		return selectedGradingScaleUid;
	}
	public void setSelectedGradingScaleUid(String selectedGradingScaleUid) {
		this.selectedGradingScaleUid = selectedGradingScaleUid;
	}

}
