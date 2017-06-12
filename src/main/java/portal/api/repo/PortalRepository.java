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

package portal.api.repo;

import portal.api.impl.PortalJpaController;
import portal.api.model.ApplicationMetadata;
import portal.api.model.PortalProperty;
import portal.api.model.PortalUser;
import portal.api.model.BunMetadata;
import portal.api.model.Category;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.InstalledBun;
import portal.api.model.Product;
import portal.api.model.SubscribedResource;
import portal.api.model.UserSession;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author ctranoris
 *
 */
public class PortalRepository {

	private static final transient Log logger = LogFactory.getLog(PortalRepository.class.getName());
	private static PortalJpaController portalJpaController;
	
	
	
	public PortalRepository(){
	}
	
	
	/**
	 * Add new portal user
	 * 
	 * @param s
	 *            PortalUser to add
	 * @return the PortalUser
	 */
	public PortalUser addPortalUserToUsers(PortalUser s) {
		portalJpaController.saveUser(s);
		return s;
	}
	
//	public BunMetadata addBunMetadataToBuns(BunMetadata bm){
//		portalJpaController.saveBunMetadata(bm);
//		return bm;
//	}
//	
	public Collection<PortalUser> getUserValues() {

		List<PortalUser> ls = portalJpaController.readUsers(0, 100000);
//		HashMap<Integer, PortalUser> cb = new HashMap<>();
//		
//		for (PortalUser buser : ls) {
//			cb.put(buser.getId() , buser);
//		}
		
		return ls;
	}
	
	public PortalUser updateUserInfo(int userid, PortalUser user) {
		PortalUser bm = portalJpaController.updatePortalUser(user);
		return bm;
	}
	
//	public BunMetadata updateBunInfo(long l, BunMetadata bm) {
//		BunMetadata bmr = portalJpaController.updateBunMetadata(bm);
//		return bmr;
//	}
	
	public Product updateProductInfo(Product bm) {
		Product bmr = portalJpaController.updateProduct(bm);
		return bmr;
	}


	public void deleteUser(int userid) {
		portalJpaController.deleteUser(userid);
	}

	public List<BunMetadata> getBuns(Long categoryid) {
		List<BunMetadata> ls = portalJpaController.readBunsMetadata(categoryid,0, 100000);
		
		return ls;
	}
	
	public void deleteProduct(int bunid) {
		portalJpaController.deleteProduct(bunid);
		
	}


	public PortalUser getUserByID(int userid) {
		return portalJpaController.readPortalUserById(userid);
	}

	public PortalUser getUserByUsername(String un) {
		return portalJpaController.readPortalUserByUsername(un);
	}
	
	public PortalUser getUserBySessionID(String id) {
		return portalJpaController.readPortalUserBySessionID(id);
	}
	

	public PortalUser getUserByEmail(String email) {
		return portalJpaController.readPortalUserByEmail(email);
		}

	

	public Product getProductByID(long bunid) {
		return (Product) portalJpaController.readProductByID(bunid);
	}
	
	public Product getProductByUUID(String uuid) {
		return (Product) portalJpaController.readProductByUUID(uuid);
	}


	
	
	public PortalJpaController getPortalJpaController() {
		return portalJpaController;
	}

	public void setPortalJpaController(PortalJpaController portalJpaController) {
		this.portalJpaController = portalJpaController;
		logger.info("======================== SETing setPortalJpaController ========================");
		this.portalJpaController.initData();
	}

	public Collection<SubscribedResource> getSubscribedResourcesAsCollection() {

		List<SubscribedResource> ls = portalJpaController.readSubscribedResources(0, 100000);
		
		return ls;
	}


	public SubscribedResource getSubscribedResourceByID(int smId) {
		return portalJpaController.readSubscribedResourceById(smId);
	}


	public SubscribedResource addSubscribedResource(SubscribedResource sm) {
		portalJpaController.saveSubscribedResource(sm);
		return sm;
	}


	public SubscribedResource updateSubscribedResourceInfo(int smId, SubscribedResource sm) {
		SubscribedResource bm = portalJpaController.updateSubscribedResource(sm);
		return bm;
	}


	public void deleteSubscribedResource(int smId) {
		portalJpaController.deleteSubscribedResource(smId);
		
	}


	/**
	 * returns first 100000 apps only :-)
	 * @param categoryid 
	 * @return list of apps
	 */
	public List<ApplicationMetadata> getApps(Long categoryid) {
		List<ApplicationMetadata> ls = portalJpaController.readAppsMetadata(categoryid, 0, 100000);		
		return ls;
	}


//	public ApplicationMetadata getApplicationMetadataByID(int appid) {
//		return (ApplicationMetadata) portalJpaController.readProductByID(appid);
//	}
//
//
//	public ApplicationMetadata getApplicationMetadataByUUID(String uuid) {
//		return (ApplicationMetadata) portalJpaController.readProductByUUID(uuid);
//	}


//	public ApplicationMetadata updateApplicationInfo(int appid, ApplicationMetadata sm) {
//		ApplicationMetadata bmr = portalJpaController.updateApplicationMetadata(sm);
//		return bmr;
//		
//	}


	public Object getCategories() {

		List<Category> ls = portalJpaController.readCategories(0, 100000);
		return ls;	}


	public Category addCategory(Category c) {
		portalJpaController.saveCategory(c);
		return c;
	}


	public Category getCategoryByID(int catid) {
		return portalJpaController.readCategoryByID(catid);
	}


	public Category updateCategoryInfo(Category c) {
		Category bmr = portalJpaController.updateCategory(c);
		return bmr;
	}


	public void deleteCategory(int catid) {
		portalJpaController.deleteCategory(catid);
		
	}


	
	public PortalProperty addproperty(PortalProperty p) {
		portalJpaController.saveProperty(p);
		return p;
	}

	public void deleteProperty(int propid) {
		portalJpaController.deleteProperty(propid);
		
	}
	

	public PortalProperty updateProperty(PortalProperty p) {
		PortalProperty bp = portalJpaController.updateProperty(p);
		return bp;
	}

	public Object getProperties() {

		List<PortalProperty> ls = portalJpaController.readProperties(0, 100000);
		return ls;	
	}
	
	public static PortalProperty getPropertyByName(String name) {
		return portalJpaController.readPropertyByName(name);
	}


	public PortalProperty getPropertyByID(int propid) {
		return portalJpaController.readPropertyByID(propid);
	}


	public List<BunMetadata> getBunsByUserID(Long ownerid) {

		List<BunMetadata> ls = portalJpaController.readBunsMetadataForOwnerID( ownerid, 0, 100000);	
		return ls;
		
	}


	public List<ApplicationMetadata> getAppsByUserID(Long ownerid) {
		List<ApplicationMetadata> ls = portalJpaController.readAppsMetadataForOwnerID( ownerid, 0, 100000);	
		return ls;
	}


	public List<DeploymentDescriptor> getAllDeploymentDescriptors() {
		List<DeploymentDescriptor> ls = portalJpaController.readDeploymentDescriptors( 0, 100000);	
		return ls;
	}


	public void deleteDeployment(int id) {
		portalJpaController.deleteDeployment(id);
		
	}


	public DeploymentDescriptor getDeploymentByID(int deploymentId) {
		return (DeploymentDescriptor) portalJpaController.readDeploymentByID(deploymentId);
	}


	public DeploymentDescriptor updateDeploymentDescriptor(DeploymentDescriptor d) {
		DeploymentDescriptor bmr = portalJpaController.updateDeploymentDescriptor(d);
		return bmr;
	}


	public SubscribedResource getSubscribedResourceByUUID(String uuid) {
		return portalJpaController.readSubscribedResourceByuuid(uuid);
	}




	
}
