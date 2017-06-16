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

package portal.api.impl;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import portal.api.model.IPortalClientAPI;
import portal.api.model.InstalledVxF;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

@Path("/client")
public class PortalClientAPIImpl implements IPortalClientAPI {
	private static final transient Log logger = LogFactory.getLog(PortalClientAPIImpl.class.getName());

	
	
//	Add everywhere a Header showing a version
//	X-portal-API-Version : 1.0.0
	
	
	@Context
	UriInfo uri;
	//see more about COntext at example http://www.blackpepper.co.uk/custom-context-providers-for-cxf-with-the-context-annotation/

	private PortalInstallationMgmt portalInstallationMgmtRef;

	public PortalInstallationMgmt getportalInstallationMgmtRef() {
		return portalInstallationMgmtRef;
	}

	public void setportalInstallationMgmtRef(PortalInstallationMgmt portalServiceRef) {
		this.portalInstallationMgmtRef = portalServiceRef;
	}

	// just to get an example json!
	@GET
	@Path("/ivxfs/example")
	@Produces("application/json")
	public Response getJsonInstalledVxFExample(@Context HttpHeaders headers, @Context  HttpServletRequest request) {
		
		
		String userAgent = headers.getRequestHeader("user-agent").get(0);
		logger.info("Received GET for Example. user-agent= " + userAgent);
		
		if ( headers.getRequestHeaders().get("X-portal-API-Version") != null ){
			String XportalAPIVersion = headers.getRequestHeader("X-portal-API-Version").get(0);
			logger.info("Received GET for Example. X-portal-API-Version= " + XportalAPIVersion);
		}
		
		Map<String, Cookie> cookies = headers.getCookies();		
		logger.info("cookies for Example = " + cookies.toString() );
		HttpSession  session = request.getSession(true);
		logger.info("session = " + session.getId());

		URI endpointUrl = uri.getBaseUri();

		InstalledVxF installedVxF = new InstalledVxF(("12cab8b8-668b-4c75-99a9-39b24ed3d8be"), endpointUrl
				+ "repo/ivxfs/12cab8b8-668b-4c75-99a9-39b24ed3d8be");
		installedVxF.setName("ServiceName");

		ResponseBuilder response = Response.ok(installedVxF);
		
		CacheControl cacheControl = new CacheControl();
		cacheControl.setNoCache(true);
		response.cacheControl(cacheControl);

		return response.build();
	}

	@GET
	@Path("/ivxfs/{uuid}")
	@Produces("application/json")
	public Response getInstalledVxFInfoByUUID(@PathParam("uuid") String uuid) {

		logger.info("Received GET for uuid: " + uuid);

		InstalledVxF installedVxF = portalInstallationMgmtRef.getVxF(uuid);

		if (installedVxF != null) {
			return Response.ok().entity(installedVxF).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Installed vxf with uuid=" + uuid + " not found in portal client registry");
			throw new WebApplicationException(builder.build());
		}

	}

	@GET
	@Path("/ivxfs/")
	@Produces("application/json")
	public Response getInstalledVxFs() {

		// for (int i = 0; i < 20; i++) { //add 20 more random
		// portalServiceRef.installService( UUID.randomUUID() ,
		// "www.repoexample.comRANDOM", "1.1.1RANDOM"+i);
		// }
		return Response.ok().entity(portalInstallationMgmtRef.getManagedInstalledVxFs().values()).build();

	}

	@POST
	@Path("/ivxfs/")
	@Produces("application/json")
	public Response installVxF(InstalledVxF reqInstallVxF) {

		logger.info("Received POST for uuid: " + reqInstallVxF.getUuid());

		InstalledVxF installedVxF = portalInstallationMgmtRef.installVxFAndStart(reqInstallVxF.getUuid(), reqInstallVxF.getRepoUrl());

		if (installedVxF != null) {
			return Response.ok().entity(installedVxF).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested VxF with uuid=" + reqInstallVxF.getUuid() + " cannot be installed");
			throw new WebApplicationException(builder.build());
		}

	}
	
	

	@DELETE
	@Path("/ivxfs/{uuid}")
	@Produces("application/json")
	public Response uninstallVxF(@PathParam("uuid") String uuid) {

		logger.info("Received @DELETE for uuid: " + uuid);

		InstalledVxF installedVxF = portalInstallationMgmtRef.getVxF(uuid);

		if (installedVxF != null) {
			portalInstallationMgmtRef.uninstallVxF(uuid);
			return Response.ok().entity(installedVxF).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Installed vxf with uuid=" + uuid + " not found in portal client registry");
			throw new WebApplicationException(builder.build());
		}

	}

	@PUT
	@Path("/ivxfs/{uuid}/stop")
	@Produces("application/json")
	public Response stopVxF(@PathParam("uuid") String uuid) {

		logger.info("Received @PUT (stop) for uuid: " + uuid);

		InstalledVxF installedVxF = portalInstallationMgmtRef.getVxF(uuid);

		if (installedVxF != null) {
			portalInstallationMgmtRef.stopVxF(uuid);
			return Response.ok().entity(installedVxF).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Installed vxf with uuid=" + uuid + " not found in portal client registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@PUT
	@Path("/ivxfs/{uuid}/start")
	@Produces("application/json")
	public Response startVxF(@PathParam("uuid") String uuid) {

		logger.info("Received  @PUT (start) for uuid: " + uuid);

		InstalledVxF installedVxF = portalInstallationMgmtRef.getVxF(uuid);

		if (installedVxF != null) {
			portalInstallationMgmtRef.startVxF(uuid);
			return Response.ok().entity(installedVxF).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Installed vxf with uuid=" + uuid + " not found in portal client registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@PUT
	@Path("/ivxfs/{uuid}/reconfigure")
	@Produces("application/json")
	public Response reConfigureVxF(String uuid) {
		logger.info("Received  @PUT (reconfigure) for uuid: " + uuid);

		InstalledVxF installedVxF = portalInstallationMgmtRef.getVxF(uuid);

		if (installedVxF != null) {
			portalInstallationMgmtRef.configureVxF(uuid);
			return Response.ok().entity(installedVxF).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Installed vxf with uuid=" + uuid + " not found in portal client registry");
			throw new WebApplicationException(builder.build());
		}
	}
	
	
	
	/****************FIREAdapters configure*/
	
	@GET
	@Path("/fireadapters/")
	@Produces("application/json")
	public Response getInstalledFIREAdapters() {

		return getInstalledVxFs();

	}
	
	@GET
	@Path("/fireadapters/{uuid}")
	@Produces("application/json")
	public Response getInstalledFIREAdapterInfoByUUID(@PathParam("uuid") String uuid) {
		return getInstalledVxFInfoByUUID(uuid);
	}

	

	@POST
	@Path("/fireadapters/")
	@Produces("application/json")
	public Response installfireadapter(InstalledVxF reqInstallVxF) {
		return installVxF(reqInstallVxF);
	}
	
	

	@DELETE
	@Path("/fireadapters/{uuid}")
	@Produces("application/json")
	public Response uninstallFireadapter(@PathParam("uuid") String uuid) {
		
		return uninstallVxF(uuid);
	}

	@PUT
	@Path("/fireadapters/{uuid}/stop")
	@Produces("application/json")
	public Response stopfireadapter(@PathParam("uuid") String uuid) {
		return stopVxF(uuid);
	}

	@PUT
	@Path("/fireadapters/{uuid}/start")
	@Produces("application/json")
	public Response startFireadapter(@PathParam("uuid") String uuid) {
		return startVxF(uuid);
	
	}

	@PUT
	@Path("/fireadapters/{uuid}/reconfigure")
	@Produces("application/json")
	public Response reConfigureFireadapter(String uuid) {
		return reConfigureVxF(uuid);
	}


}
