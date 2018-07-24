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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import portal.api.impl.PortalJpaController;
import portal.api.model.Category;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.ExperimentMetadata;
import portal.api.model.ExperimentOnBoardDescriptor;
import portal.api.model.VFImage;
import portal.api.model.Infrastructure;
import portal.api.model.MANOplatform;
import portal.api.model.MANOprovider;
import portal.api.model.PortalProperty;
import portal.api.model.PortalUser;
import portal.api.model.Product;
import portal.api.model.SubscribedResource;
import portal.api.model.VxFMetadata;
import portal.api.model.VxFOnBoardedDescriptor;

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
	
//	public VxFMetadata addVxFMetadataToVxFs(VxFMetadata bm){
//		portalJpaController.saveVxFMetadata(bm);
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
	
	public PortalUser updateUserInfo( PortalUser user) {
		PortalUser bm = portalJpaController.updatePortalUser(user);
		return bm;
	}
	
//	public VxFMetadata updateVxFInfo(long l, VxFMetadata bm) {
//		VxFMetadata bmr = portalJpaController.updateVxFMetadata(bm);
//		return bmr;
//	}
	
	public Product updateProductInfo(Product bm) {
		Product bmr = portalJpaController.updateProduct(bm);
		return bmr;
	}


	public void deleteUser(int userid) {
		portalJpaController.deleteUser(userid);
	}

	public List<VxFMetadata> getVxFs(Long categoryid, boolean isPublished) {
		List<VxFMetadata> ls = portalJpaController.readVxFsMetadata(categoryid,0, 100000, isPublished);
		
		return ls;
	}
	

	/**
	 * returns first 100000 apps only :-)
	 * @param categoryid 
	 * @return list of apps
	 */
	public List<ExperimentMetadata> getExperiments(Long categoryid, boolean isPublished) {
		List<ExperimentMetadata> ls = portalJpaController.readExperimentsMetadata(categoryid, 0, 100000, isPublished);		
		return ls;
	}

	
	public void deleteProduct(int vxfid) {
		portalJpaController.deleteProduct(vxfid);
		
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
	
	public PortalUser getUserByAPIKEY(String apikey) {
		return portalJpaController.readPortalUserByAPIKEY( apikey );
	}

	

	public Product getProductByID(long vxfid) {
		return (Product) portalJpaController.readProductByID(vxfid);
	}
	
	public Product getProductByUUID(String uuid) {
		return (Product) portalJpaController.readProductByUUID(uuid);
	}
	


	public Product getProductByName(String name) {
		return (Product) portalJpaController.readProductByName(name);
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



//	public ExperimentMetadata getExperimentMetadataByID(int appid) {
//		return (ExperimentMetadata) portalJpaController.readProductByID(appid);
//	}
//
//
//	public ExperimentMetadata getExperimentMetadataByUUID(String uuid) {
//		return (ExperimentMetadata) portalJpaController.readProductByUUID(uuid);
//	}


//	public ExperimentMetadata updateApplicationInfo(int appid, ExperimentMetadata sm) {
//		ExperimentMetadata bmr = portalJpaController.updateExperimentMetadata(sm);
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

	public List<PortalProperty> getProperties() {

		List<PortalProperty> ls = portalJpaController.readProperties(0, 100000);
		return ls;	
	}
	
	public static PortalProperty getPropertyByName(String name) {
		return portalJpaController.readPropertyByName(name);
	}


	public PortalProperty getPropertyByID(int propid) {
		return portalJpaController.readPropertyByID(propid);
	}


	public List<VxFMetadata> getVxFsByUserID(Long ownerid) {

		List<VxFMetadata> ls = portalJpaController.readVxFsMetadataForOwnerID( ownerid, 0, 100000);	
		return ls;
		
	}


	public List<ExperimentMetadata> getAppsByUserID(Long ownerid) {
		List<ExperimentMetadata> ls = portalJpaController.readAppsMetadataForOwnerID( ownerid, 0, 100000);	
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


	public Object getMANOplatforms() {

		List<MANOplatform> ls = portalJpaController.readMANOplatforms(0, 100000);
		return ls;	
	}


	public MANOplatform addMANOplatform(MANOplatform c) {
		portalJpaController.saveMANOplatform(c);
		return c;
	}


	public MANOplatform getMANOplatformByID(int catid) {
		return portalJpaController.readMANOplatformById(catid);
	}


	public MANOplatform updateMANOplatformInfo(MANOplatform c) {
		MANOplatform bmr = portalJpaController.updateMANOplatform(c);
		return bmr;
	}


	public void deleteMANOplatform(int mpid) {
		portalJpaController.deleteMANOplatform(mpid);
		
	}

	


	public Object getMANOproviders() {

		List<MANOprovider> ls = portalJpaController.readMANOproviders(0, 100000);
		return ls;	
	}


	public MANOprovider addMANOprovider(MANOprovider c) {
		portalJpaController.saveMANOprovider(c);
		return c;
	}


	public MANOprovider getMANOproviderByID(int catid) {
		return portalJpaController.readMANOproviderById(catid);
	}


	public MANOprovider updateMANOproviderInfo(MANOprovider c) {
		MANOprovider bmr = portalJpaController.updateMANOprovider(c);
		return bmr;
	}


	public void deleteMANOprovider(int mpid) {
		portalJpaController.deleteMANOprovider(mpid);
		
	}

	

	public Object getVxFOnBoardedDescriptors() {

		List<VxFOnBoardedDescriptor> ls = portalJpaController.readVxFOnBoardedDescriptors(0, 100000);
		return ls;	
	}


	public VxFOnBoardedDescriptor addVxFOnBoardedDescriptor(VxFOnBoardedDescriptor c) {
		portalJpaController.saveVxFOnBoardedDescriptor(c);
		return c;
	}


	public VxFOnBoardedDescriptor updateVxFOnBoardedDescriptor(VxFOnBoardedDescriptor c) {
		VxFOnBoardedDescriptor bmr = portalJpaController.updateVxFOnBoardedDescriptor(c);
		return bmr;
	}


	public void deleteVxFOnBoardedDescriptor(int mpid) {
		portalJpaController.deleteVxFOnBoardedDescriptor(mpid);
		
	}


	public VxFOnBoardedDescriptor getVxFOnBoardedDescriptorByID(int mpid) {
		return portalJpaController.readVxFOnBoardedDescriptorById( mpid );
	}


	
	public Object getExperimentOnBoardDescriptors() {

		List<ExperimentOnBoardDescriptor> ls = portalJpaController.readExperimentOnBoardDescriptors(0, 100000);
		return ls;	
	}


	public ExperimentOnBoardDescriptor addExperimentOnBoardDescriptor(ExperimentOnBoardDescriptor c) {
		portalJpaController.saveExperimentOnBoardDescriptor(c);
		return c;
	}


	public ExperimentOnBoardDescriptor updateExperimentOnBoardDescriptor(ExperimentOnBoardDescriptor c) {
		ExperimentOnBoardDescriptor bmr = portalJpaController.updateExperimentOnBoardDescriptor(c);
		return bmr;
	}


	public void deleteExperimentOnBoardDescriptor(int mpid) {
		portalJpaController.deleteExperimentOnBoardDescriptor(mpid);
		
	}


	public ExperimentOnBoardDescriptor getExperimentOnBoardDescriptorByID(int mpid) {
		return portalJpaController.readExperimentOnBoardDescriptorById( mpid );
	}

	/**
	 * 
	 * Infrastructure objects
	 */

	public List<Infrastructure> getInfrastructures() {
		List<Infrastructure> ls = portalJpaController.readInfrastructures(0, 100000);
		return ls;	
	}


	public Infrastructure addInfrastructure(Infrastructure c) {
		portalJpaController.saveInfrastructure(c);
		return c;
	}


	public Infrastructure getInfrastructureByID(int infraid) {
		return portalJpaController.readInfrastructureById( infraid );
	}


	public Infrastructure updateInfrastructureInfo(Infrastructure c) {
		Infrastructure bmr = portalJpaController.updateInfrastructure(c);
		return bmr;
	}


	public void deleteInfrastructure(int infraid) {
		portalJpaController.deletInfrastructure( infraid );
		
	}


	public DeploymentDescriptor getDeploymentByUUID(String uuid) {
		return (DeploymentDescriptor) portalJpaController.readDeploymentByUUID( uuid );
	}
	
	
	
	/**
	 * 
	 * VFImage objects
	 */

	public List<VFImage> getVFImages() {
		List< VFImage> ls = portalJpaController.readVFImages(0, 100000);
		return ls;	
	}



	public VFImage getVFImageByID(int infraid) {
		return portalJpaController.readVFImageById( infraid );
	}


	/**
	 * @param vfimg
	 * @return
	 */
	public VFImage updateVFImageInfo(VFImage vfimg) 
	{

		VFImage bm = portalJpaController.updateVFImage( vfimg );
		return bm;
		
	}


	public void deleteVFImage(int infraid) {
		portalJpaController.deleteVFImage( infraid );
		
	}


	public VFImage getVFImageByUUID(String uuid) {
		return portalJpaController.readVFImageByUUID( uuid );
	}


	public VFImage getVFImageByName(String imagename) {
		return portalJpaController.readVFImageByName( imagename );
	}


	public VFImage saveVFImage(VFImage vfimg) {
		// Save now vxf for User
		PortalUser vxfOwner = this.getUserByID( vfimg.getOwner().getId() );
		vxfOwner.addVFImage( vfimg );
		vfimg.setOwner(vxfOwner); // replace given owner with the one from our DB

		PortalUser owner = this.updateUserInfo(  vxfOwner );
		VFImage registeredvfimg = this.getVFImageByUUID( vfimg.getUuid() );
		return registeredvfimg;
	}


	public List<VFImage> getVFImagesByUserID(long ownerid) {

		List<VFImage> ls = portalJpaController.readVFImagesForOwnerID( ownerid, 0, 100000);	
		return ls;
	}


	

	





	
}
