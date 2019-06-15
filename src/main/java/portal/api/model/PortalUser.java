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
import java.util.Iterator;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import portal.api.util.EncryptionUtil;

@Entity(name = "PortalUser")
@JsonIgnoreProperties(value = { "products", "vfimages", "deployments", "subscribedResources" })
public class PortalUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id = 0;

	@Basic()
	private String organization = null;
	@Basic()
	private String name = null;
	@Basic()
	private String email = null;
	@Basic()
	private String username = null;
	@Basic()
	private String password = null;
//	@Basic()
//	private String role = null;
	@Basic()	
	private Boolean active = false;
	@Basic()
	private String currentSessionID = null;
	@Basic()
	private String apikey = null;
	
	/**
	 * 
	 */
	@ElementCollection(targetClass=UserRoleType.class, fetch = FetchType.EAGER)
	@Enumerated(EnumType.ORDINAL)
	private List<UserRoleType> roles = new ArrayList<UserRoleType>();
	
	/**
	 * 
	 */
	@OneToMany(cascade = {  CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@JoinTable()
	private List<Product> products = new ArrayList<Product>();
	

	/**
	 * 
	 */
	@OneToMany(cascade = {  CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH  }, fetch = FetchType.EAGER)
	@JoinTable()
	private List<VFImage> vfimages = new ArrayList<VFImage>();
	

	public List<UserRoleType> getRoles() {
		return roles;
	}

	@OneToMany(cascade = {  CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH  }, fetch = FetchType.EAGER)
	@JoinTable()
	private List<DeploymentDescriptor> deployments = new ArrayList<DeploymentDescriptor>();
	
	@OneToMany(cascade = {  CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH  }, fetch = FetchType.EAGER)
	@JoinTable()
	private List<SubscribedResource> subscribedResources = new ArrayList<SubscribedResource>();
	
	
	

	public List<SubscribedResource> getSubscribedResources() {
		return subscribedResources;
	}

	public void setSubscribedResources(List<SubscribedResource> subscribedResources) {
		this.subscribedResources = subscribedResources;
	}

	public List<DeploymentDescriptor> getDeployments() {
		return deployments;
	}

	public void setDeployments(List<DeploymentDescriptor> deployments) {
		this.deployments = deployments;
	}

	public List<Product> getProducts() {
		return products;
	}

	public void setProducts(List<Product> products) {
		this.products = products;
	}
	
	public void addProduct(Product prod) {
		if (!this.products.contains(prod)) {
			this.products.add(prod);
			prod.setOwner(this);
		}
	}
	
	public void removeFromProducts(Product prod) {
		if (this.products.contains(prod)) {
			this.products.remove(prod);
		}
	}
	
	public void clearProducts() {
		while (!this.products.isEmpty()) {
			removeFromProducts(this.products.iterator().next());
		}
	}
	
	public Product getProductById(long l) {

		for (Iterator iterator = this.products.iterator(); iterator.hasNext();) {
			Product prod = (Product) iterator.next();
			if (prod.getId() == l)
				return prod;
		}
		return null;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String newOrganization) {
		organization = newOrganization;
	}

//	public List<VxFMetadata> getVxFs() {
//		return vxfs;
//	}

//	public void addVxF(VxFMetadata vxfsValue) {
//		if (!vxfs.contains(vxfsValue)) {
//			vxfs.add(vxfsValue);
//			vxfsValue.setOwner(this);
//		}
//	}
//
//	public void removeFromVxFs(VxFMetadata vxfsValue) {
//		if (vxfs.contains(vxfsValue)) {
//			vxfs.remove(vxfsValue);
//		}
//	}
//
//	public void clearVxFs() {
//		while (!vxfs.isEmpty()) {
//			removeFromVxFs(vxfs.iterator().next());
//		}
//	}
//
//	public void setVxFs(List<VxFMetadata> newVxFs) {
//		vxfs = newVxFs;
//	}

	public int getId() {
		return id;
	}

	public void setId(int newId) {
		id = newId;
	}

	@Override
	public String toString() {
		return "PortalUser " + " [organization: " + getOrganization() + "]" + " [id: " + getId() + "]";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@JsonIgnore
	public String getPassword() {
		return password;
	}

	@JsonProperty
	public void setPassword(String password) {

		if ( (password!=null) && (!password.equals("")))//else will not change it
			this.password = EncryptionUtil.hash(password);
		
		//this.password = password;
	}
	
	public void setPasswordUnencrypted(String password) {
		this.password = password;
	}

//	public VxFMetadata getVxFById(int vxfid) {
//
//		for (Iterator iterator = vxfs.iterator(); iterator.hasNext();) {
//			VxFMetadata vxfMetadata = (VxFMetadata) iterator.next();
//			if (vxfMetadata.getId() == vxfid)
//				return vxfMetadata;
//		}
//		return null;
//	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getCurrentSessionID() {
		return currentSessionID;
	}

	public void setCurrentSessionID(String currentSessionID) {
		this.currentSessionID = currentSessionID;
	}

	public void addRole(UserRoleType role ) {
		this.roles.add(role);		
	}
	
	public void removeRole(UserRoleType role ) {
		this.roles.remove(role);		
	}
	

	
	public List<VFImage> getVFImages() {
		return vfimages;
	}

	public void setVFImages(List<VFImage> products) {
		this.vfimages = products;
	}
	
	public void addVFImage( VFImage prod ) {
		if (!this.vfimages.contains(prod)) {
			this.vfimages.add(prod);
			prod.setOwner(this);
		}
	}
	
	public void removeFromVFImages( VFImage prod) {
		if (this.vfimages.contains(prod)) {
			this.vfimages.remove(prod);
		}
	}

	/**
	 * @return the apikey
	 */
	public String getApikey() {
		return apikey;
	}

	/**
	 * @param apikey the apikey to set
	 */
	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	@JsonIgnore
	public PortalUser getSnippedDetails() {
		@JsonIgnoreProperties(value = { "products", "vfimages", "deployments", "subscribedResources", "apikey", "roles", "currentSessionID", "active"})
		class SnipPortalUser extends PortalUser{			
		}		
		
		SnipPortalUser p = new SnipPortalUser();
		p.setId( this.getId() );
		p.setName( this.getName() );
		p.setUsername(  this.getUsername() );
		p.setOrganization( this.getOrganization() );
		p.setEmail(this.getEmail());
		return p;
	}
}
