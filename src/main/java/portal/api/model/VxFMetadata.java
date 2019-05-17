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
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;


@Entity(name = "VxFMetadata")
public class VxFMetadata extends Product{


	/**
	 * 
	 */
	private boolean certified;
	
	/**
	 * 
	 */
	private String certifiedBy;	

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
	 * 
	 */
	@OneToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@JoinTable()
	private List<MANOplatform> supportedMANOPlatforms = new ArrayList<MANOplatform>();

	/**
	 * 
	 */
	@OneToMany(cascade = {  CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH  }, fetch = FetchType.EAGER)
	@JoinTable()
	private List<VxFOnBoardedDescriptor> vxfOnBoardedDescriptors = new ArrayList<VxFOnBoardedDescriptor>();
	
	@OneToMany(cascade = {  CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH  }, fetch = FetchType.EAGER)
	@JoinTable()
	private List<VFImage> vfimagesVDU = new ArrayList<>();
	
	
	/**
	 * @return
	 */
	public List<VxFOnBoardedDescriptor> getVxfOnBoardedDescriptors() {
		return vxfOnBoardedDescriptors;
	}

	/**
	 * @param vxfOnBoardedDescriptors
	 */
	public void setVxfOnBoardedDescriptors(List<VxFOnBoardedDescriptor> vxfOnBoardedDescriptors) {
		this.vxfOnBoardedDescriptors = vxfOnBoardedDescriptors;
	}



	/**
	 * @return
	 */
	public boolean isCertified() {
		return certified;
	}

	/**
	 * @param certified
	 */
	public void setCertified(boolean certified) {
		this.certified = certified;
	}

	/**
	 * @return
	 */
	public String getCertifiedBy() {
		return certifiedBy;
	}

	/**
	 * @param certifiedBy
	 */
	public void setCertifiedBy(String certifiedBy) {
		this.certifiedBy = certifiedBy;
	}

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
	 * @return
	 */
	public List<MANOplatform> getSupportedMANOPlatforms() {
		return supportedMANOPlatforms;
	}

	/**
	 * @param supportedMANOPlatforms
	 */
	public void setSupportedMANOPlatforms(List<MANOplatform> supportedMANOPlatforms) {
		this.supportedMANOPlatforms = supportedMANOPlatforms;
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

	/**
	 * @return the vfimagesVDU
	 */
	public List<VFImage> getVfimagesVDU() {
		return vfimagesVDU;
	}

	/**
	 * @param vfimagesVDU the vfimagesVDU to set
	 */
	public void setVfimagesVDU(List<VFImage> vfimagesVDU) {
		this.vfimagesVDU = vfimagesVDU;
	}

	
}
