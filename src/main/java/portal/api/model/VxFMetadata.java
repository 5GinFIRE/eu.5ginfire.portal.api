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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;


@Entity(name = "VxFMetadata")
public class VxFMetadata extends Product{


	private boolean published;	

	private boolean certified;
	
	private String certifiedBy;	

	private PackagingFormat packagingFormat = PackagingFormat.OSMvTWO;
	
	
	@OneToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinTable()
	private List<MANOplatform> supportedMANOPlatforms = new ArrayList<MANOplatform>();
	
	

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public boolean isCertified() {
		return certified;
	}

	public void setCertified(boolean certified) {
		this.certified = certified;
	}

	public String getCertifiedBy() {
		return certifiedBy;
	}

	public void setCertifiedBy(String certifiedBy) {
		this.certifiedBy = certifiedBy;
	}

	public PackagingFormat getPackagingFormat() {
		return packagingFormat;
	}

	public void setPackagingFormat(PackagingFormat packagingFormat) {
		this.packagingFormat = packagingFormat;
	}

	public List<MANOplatform> getSupportedMANOPlatforms() {
		return supportedMANOPlatforms;
	}

	public void setSupportedMANOPlatforms(List<MANOplatform> supportedMANOPlatforms) {
		this.supportedMANOPlatforms = supportedMANOPlatforms;
	}

	
	
}
