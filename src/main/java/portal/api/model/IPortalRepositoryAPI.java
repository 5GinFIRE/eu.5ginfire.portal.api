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

import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;


public interface IPortalRepositoryAPI {

	//utility

	Response getEntityImage(String uuid, String imgfile);
	
	//USER related methods 
	Response getUsers(); 
	Response getUserById(int userid);
	Response addUser(PortalUser user);
	Response updateUserInfo(int userid, PortalUser user);
	Response deleteUser(int userid);
	Response getAllVxFsofUser(int userid);
	Response getAllAppsofUser(int userid);

	//Sessions
	public Response addUserSession(UserSession userSession);
	public Response getUserSessions();
	
	//categories
	Response getCategories();
	Response getCategoryById(int catid);
	Response addCategory(Category c);
	Response updateCategory(int catId, Category c);
	Response deleteCategory(int catId);
	
	//VxFs related API methods
	Response getVxFs(Long categoryid);
	Response getVxFMetadataByID(int vxfid);
	Response getVxFMetadataByUUID(String uuid);		
	Response getVxFofUser( int userid, int vxfid);
	Response downloadVxFPackage(String uuid, String vxffile);	
	Response updateVxFMetadata(int bid, List<Attachment> attachements);
	Response addVxFMetadata(List<Attachment> attachements);
	void deleteVxF( int vxfid);
	
//	//Subscribed resources
//	Response getSubscribedResources();
//	Response getSubscribedResourceById(int smId);
//	Response addSubscribedResource(SubscribedResource sm);
//	Response updateSubscribedResource(int smId, SubscribedResource sm);
//	Response deleteSubscribedResource(int smId);
	
	//experiments Related API methods
	Response getApps(Long categoryid);
	Response getAppMetadataByID(int appid);
	Response getAppMetadataByUUID(String uuid);		
	Response getAppofUser( int userid, int appid);
	Response updateExperimentMetadata(int aid, List<Attachment> attachements);
	Response addExperimentMetadata( List<Attachment> attachements);
	void deleteExperiment(int appid);
	
	
}
