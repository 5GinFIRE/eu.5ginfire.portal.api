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
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * @author ctranoris
 *
 */
@Entity(name = "Image")
public class Image {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id = 0;
	
	@Basic()
	private String name = null;
	
	@Basic()
	private String uuid = null;
	
	@Basic()
	private String shortDescription = null;
	
	@Basic()
	private String packageLocation = null;
	
	@Basic()
	private Date dateCreated;
	
	@OneToMany(cascade = { CascadeType.ALL })
	@JoinTable()
	private List<VxFMetadata> usedByVxFs = new ArrayList<>();
	
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
	@JoinColumns({ @JoinColumn() })
	private PortalUser owner = null;
	
	@Basic()
	private boolean published;
	
	@Lob
	@Column(name = "TERMS", columnDefinition = "LONGTEXT")
	private String termsOfUse;	
	
	@OneToMany(cascade = { CascadeType.ALL })
	@JoinTable()
	private List<Infrastructure> deployedInfrastructures = new ArrayList<>();

	public Image(){
		
	}
	
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the shortDescription
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * @param shortDescription the shortDescription to set
	 */
	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * @return the packageLocation
	 */
	public String getPackageLocation() {
		return packageLocation;
	}

	/**
	 * @param packageLocation the packageLocation to set
	 */
	public void setPackageLocation(String packageLocation) {
		this.packageLocation = packageLocation;
	}

	/**
	 * @return the dateCreated
	 */
	public Date getDateCreated() {
		return dateCreated;
	}

	/**
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	/**
	 * @return the usedByVxFs
	 */
	public List<VxFMetadata> getUsedByVxFs() {
		return usedByVxFs;
	}

	/**
	 * @param usedByVxFs the usedByVxFs to set
	 */
	public void setUsedByVxFs(List<VxFMetadata> usedByVxFs) {
		this.usedByVxFs = usedByVxFs;
	}

	/**
	 * @return the owner
	 */
	public PortalUser getOwner() {
		return owner;
	}

	/**
	 * @param owner the owner to set
	 */
	public void setOwner(PortalUser owner) {
		this.owner = owner;
	}

	/**
	 * @return the published
	 */
	public boolean isPublished() {
		return published;
	}

	/**
	 * @param published the published to set
	 */
	public void setPublished(boolean published) {
		this.published = published;
	}

	/**
	 * @return the termsOfUse
	 */
	public String getTermsOfUse() {
		return termsOfUse;
	}

	/**
	 * @param termsOfUse the termsOfUse to set
	 */
	public void setTermsOfUse(String termsOfUse) {
		this.termsOfUse = termsOfUse;
	}

	/**
	 * @return the deployedInfrastructures
	 */
	public List<Infrastructure> getDeployedInfrastructures() {
		return deployedInfrastructures;
	}

	/**
	 * @param deployedInfrastructures the deployedInfrastructures to set
	 */
	public void setDeployedInfrastructures(List<Infrastructure> deployedInfrastructures) {
		this.deployedInfrastructures = deployedInfrastructures;
	}
	
	
	
	
	
}
