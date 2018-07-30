/**
 * Copyright 2017 University of Patras 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and limitations under the License.
 */

package portal.api.validation.ci;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author ctranoris
 *
 */
public class ValidationJobResult {

	/** */
	@JsonProperty("vnfd_id")
	private int vxfid;
	/** */
	@JsonProperty("validation_status")
	private Boolean validationStatus;
	/** */
	@JsonProperty("jenkins_output_log")
	private String outputLog;

	/** */
	@JsonProperty("build_id")
	private int buildId;
		
	
	/**
	 * @return the vxfid
	 */
	public int getVxfid() {
		return vxfid;
	}
	/**
	 * @param vxfid the vxfid to set
	 */
	public void setVxfid(int vxfid) {
		this.vxfid = vxfid;
	}


	/**
	 * @return the validationStatus
	 */
	public Boolean getValidationStatus() {
		return validationStatus;
	}
	/**
	 * @param validationStatus the validationStatus to set
	 */
	public void setValidationStatus(Boolean validationStatus) {
		this.validationStatus = validationStatus;
	}
	/**
	 * @return the outputLog
	 */
	public String getOutputLog() {
		return outputLog;
	}
	/**
	 * @param outputLog the outputLog to set
	 */
	public void setOutputLog(String outputLog) {
		this.outputLog = outputLog;
	}
	/**
	 * @return the buildId
	 */
	public int getBuildId() {
		return buildId;
	}
	/**
	 * @param buildId the buildId to set
	 */
	public void setBuildId(int buildId) {
		this.buildId = buildId;
	}

	
}
