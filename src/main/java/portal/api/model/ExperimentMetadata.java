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

package portal.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;


@Entity(name = "ExperimentMetadata")
public class ExperimentMetadata extends Product{


	/**
	 * 
	 */
	private boolean valid;


	/**
	 * 
	 */
	@Basic()
	private ValidationStatus validationStatus = ValidationStatus.NOT_STARTED;
	
	/**
	 * 
	 */
	private PackagingFormat packagingFormat = PackagingFormat.OSMvTWO;
	
	/**
	 * @return
	 */
	public PackagingFormat getPackagingFormat() {
		return packagingFormat;
	}

	/**
	 * @param packagingFormat
	 */
	public void setPackagingFormat(PackagingFormat packagingFormat) {
		this.packagingFormat = packagingFormat;
	}

	
	/**
	 * 
	 */
	@OneToMany(cascade = { CascadeType.ALL })
	@JoinTable()
	private List<ExperimentOnBoardDescriptor> experimentOnBoardDescriptors = new ArrayList<ExperimentOnBoardDescriptor>();
	
	/**
	 * 
	 */
	@OneToMany(cascade = { CascadeType.ALL })
	@JoinTable()
	private List<ConstituentVxF> constituentVxF = new ArrayList<ConstituentVxF>();
	
	
	
	/**
	 * @return
	 */
	public List<ConstituentVxF> getConstituentVxF() {
		return constituentVxF;
	}

	/**
	 * @param constituentVxF
	 */
	public void setConstituentVxF(List<ConstituentVxF> constituentVxF) {
		this.constituentVxF = constituentVxF;
	}

	/**
	 * @return
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * @param valid
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	/**
	 * @return
	 */
	public List<ExperimentOnBoardDescriptor> getExperimentOnBoardDescriptors() {
		return experimentOnBoardDescriptors;
	}

	/**
	 * @param e
	 */
	public void setExperimentOnBoardDescriptors(List<ExperimentOnBoardDescriptor> e) {
		this.experimentOnBoardDescriptors = e;
	}
	


	/**
	 * @return
	 */
	public ValidationStatus getValidationStatus() {
		return validationStatus;
	}

	/**
	 * @param validationStatus
	 */
	public void setValidationStatus(ValidationStatus validationStatus) {
		this.validationStatus = validationStatus;
	}
	
	

}
