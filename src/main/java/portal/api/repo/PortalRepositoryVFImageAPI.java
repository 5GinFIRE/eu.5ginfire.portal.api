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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import portal.api.bugzilla.model.ErrorMsg;
import portal.api.bus.BusController;
import portal.api.model.Infrastructure;
import portal.api.model.PortalUser;
import portal.api.model.Product;
import portal.api.model.UserRoleType;
import portal.api.model.VFImage;
import portal.api.model.VxFMetadata;
import portal.api.util.AttachmentUtil;


/**
 * @author ctranoris
 *
 */
@Path("/repo")
public class PortalRepositoryVFImageAPI {
	

	@Context
	UriInfo uri;

	@Context
	MessageContext ws;
	

	@Context
	protected SecurityContext sc;

	@Context
	private PortalRepository portalRepositoryRef;
	
	private static final String VFIMAGESDIR = System.getProperty("user.home") + File.separator + ".vfimages"
			+ File.separator ;

	private static final transient Log logger = LogFactory.getLog( PortalRepositoryVFImageAPI.class.getName());
	
	
	public void setPortalRepositoryRef(PortalRepository portalRepositoryRef) {
		this.portalRepositoryRef = portalRepositoryRef;
	}

	
	/**
	 * 
	 * Image object API
	 */

	@GET
	@Path("/admin/vfimages/")
	@Produces("application/json")
	public Response getAdminVFImages() {
		
		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());
		logger.info( ws.getHttpHeaders().toString() );
		
		
		if (u != null) {
			List<VFImage> vfimagess;

			if (u.getRoles().contains(UserRoleType.PORTALADMIN) || u.getRoles().contains(UserRoleType.TESTBED_PROVIDER)) {
				vfimagess = portalRepositoryRef.getVFImages();
			} else {
				vfimagess = portalRepositoryRef.getVFImagesByUserID((long) u.getId());
			}

			return Response.ok().entity( vfimagess ).build();

		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User not found in portal registry or not logged in");
			throw new WebApplicationException(builder.build());
		}
		
	}

	@POST
	@Path("/admin/vfimages/")
	@Consumes("multipart/form-data")
	@Produces("application/json")
	public Response addVFImage( List<Attachment> ats ) {
		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u == null) {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User not found in portal registry or not logged in ");
			throw new WebApplicationException(builder.build());
		}

		VFImage vfimg = null;
		
		String emsg = "";

		try {
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser( AttachmentUtil.getAttachmentStringValue("vfimage", ats));
			vfimg = parser.readValueAs( VFImage.class );

			logger.info("Received @POST for VFImage : " + vfimg.getName());
			
			String uuid = UUID.randomUUID().toString();
			vfimg.setUuid(uuid);
			vfimg.setDateCreated(new Date());
			
			vfimg = addNewVFImage(vfimg,					
					AttachmentUtil.getAttachmentByName("prodFile", ats));

		} catch (JsonProcessingException e) {
			vfimg = null;
			e.printStackTrace();
			logger.error( e.getMessage() );
			emsg =  e.getMessage();
		} catch (IOException e) {
			vfimg = null;
			e.printStackTrace();
			logger.error( e.getMessage() );
			emsg =  e.getMessage();
		}


		if (vfimg != null) {

			BusController.getInstance().newVFImageAdded( vfimg );		
			return Response.ok().entity(vfimg).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity( new ErrorMsg( "Requested Image cannot be installed. " + emsg )  );						
			throw new WebApplicationException(builder.build());
			
		}
		
	
	}

	/**
	 * @param vfimg
	 * @param vfimagefile
	 * @return
	 * @throws IOException
	 */
	private VFImage addNewVFImage(VFImage vfimg, Attachment vfimagefile) throws IOException {

		logger.info("image name = " + vfimg.getName());
		logger.info("shortDescription = " +vfimg.getShortDescription());

		URI endpointUrl = uri.getBaseUri();
		String tempDir = VFIMAGESDIR + vfimg.getUuid() + File.separator;
	
		if (vfimagefile != null) {
			String imageFileNamePosted = AttachmentUtil.getFileName(vfimagefile.getHeaders());
			logger.info("vfimagefile = " + imageFileNamePosted);
			if (!imageFileNamePosted.equals("") && !imageFileNamePosted.equals("unknown")) {
				Files.createDirectories(Paths.get(tempDir));
				String imgfile = AttachmentUtil.saveFile( vfimagefile, tempDir + imageFileNamePosted);
				logger.info("vfimagefile saved to = " + imgfile);
				
				vfimg.setPackageLocation(endpointUrl.toString().replace("http:", "") + "repo/vfimages/image/" + vfimg.getUuid() + "/"
						+ imageFileNamePosted);
			}
		}
		
		VFImage registeredvfimg =  portalRepositoryRef.saveVFImage( vfimg ); 
		
				
		return registeredvfimg;
	}
	
	
	/**
	 * @param vfimg
	 * @param vfimagefile
	 * @return
	 * @throws IOException
	 */
	private VFImage updateVFImage(VFImage vfimg, Attachment vfimagefile) throws IOException {

		logger.info("image name = " + vfimg.getName());
		logger.info("shortDescription = " +vfimg.getShortDescription());

		URI endpointUrl = uri.getBaseUri();
		String tempDir = VFIMAGESDIR + vfimg.getUuid() + File.separator;
		
		VFImage prevfImage = portalRepositoryRef.getVFImageByID( vfimg.getId() );
		
		//reattach usedVxF to the instance
		for (VxFMetadata vxf : prevfImage.getUsedByVxFs()) {			
			vfimg.getUsedByVxFs().add(vxf);
		}
		
		//reattach getDeployedInfrastructures to the instance
		for (Infrastructure i : prevfImage.getDeployedInfrastructures()) {			
			vfimg.getDeployedInfrastructures().add(i); 
		}
	
		if (vfimagefile != null) {
			String imageFileNamePosted = AttachmentUtil.getFileName(vfimagefile.getHeaders());
			logger.info("vfimagefile = " + imageFileNamePosted);
			if (!imageFileNamePosted.equals("") && !imageFileNamePosted.equals("unknown")) {
				Files.createDirectories(Paths.get(tempDir));
				String imgfile = AttachmentUtil.saveFile( vfimagefile, tempDir + imageFileNamePosted);
				logger.info("vfimagefile saved to = " + imgfile);
				
				vfimg.setPackageLocation(endpointUrl.toString().replace("http:", "") + "repo/vfimages/image/" + vfimg.getUuid() + "/"
						+ imageFileNamePosted);
			}
		}
		
		VFImage registeredvfimg =  portalRepositoryRef.updateVFImageInfo( vfimg ); 
		
				
		return registeredvfimg;
	}

	
	@GET
	@Path("/vfimages/image/{uuid}/{vfimagefile}")
	@Produces("application/gzip")
	public Response downloadVxFPackage(@PathParam("uuid") String uuid, @PathParam("vfimagefile") String vfimagefile) {

		logger.info("vfimagefile: " + vfimagefile);
		logger.info("uuid: " + uuid);

		String vxfAbsfile = VFIMAGESDIR + uuid + File.separator + vfimagefile;
		logger.info("VxF RESOURCE FILE: " + vxfAbsfile);
		File file = new File(vxfAbsfile);

		ResponseBuilder response = Response.ok((Object) file);
		response.header("Content-Disposition", "attachment; filename=" + file.getName());
		return response.build();
	}
	
	
	

	@PUT
	@Path("/admin/vfimages/")	
	@Consumes("multipart/form-data")
	@Produces("application/json")
	public Response updateVFImage(@PathParam("uuid") int infraid, List<Attachment> ats) {
		
		VFImage vfimg = null;
		
		String emsg = "";

		try {
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser( AttachmentUtil.getAttachmentStringValue("vfimage", ats));
			vfimg = parser.readValueAs( VFImage.class );
			
			if ( !checkUserIDorIsAdmin( vfimg.getOwner().getId() ) ){
				 return Response.status(Status.FORBIDDEN ).build();
			}

			logger.info("Received @PUT for VFImage : " + vfimg.getName());
			
			vfimg = updateVFImage(vfimg,					
					AttachmentUtil.getAttachmentByName("prodFile", ats));

		} catch (JsonProcessingException e) {
			vfimg = null;
			e.printStackTrace();
			logger.error( e.getMessage() );
			emsg =  e.getMessage();
		} catch (IOException e) {
			vfimg = null;
			e.printStackTrace();
			logger.error( e.getMessage() );
			emsg =  e.getMessage();
		}


		if (vfimg != null) {

			BusController.getInstance().aVFImageUpdated( vfimg );		
			return Response.ok().entity(vfimg).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity( new ErrorMsg( "Requested Image cannot be installed. " + emsg )  );						
			throw new WebApplicationException(builder.build());
			
		}

	}

	@DELETE
	@Path("/admin/vfimages/{id}")
	public Response deleteVFImage(@PathParam("id") int id) {
		
		VFImage sm = portalRepositoryRef.getVFImageByID(id);
		
		if ( !checkUserIDorIsAdmin( sm.getOwner().getId() ) ){
			 return Response.status(Status.FORBIDDEN ).build();
		}

		portalRepositoryRef.deleteVFImage(id);
		return Response.ok().build();

	}

	@GET
	@Path("/admin/vfimages/{id}")
	@Produces("application/json")
	public Response getVFImageById(@PathParam("id") int id) {
		VFImage sm = portalRepositoryRef.getVFImageByID(id);

		if (sm != null) {
			
			if ( !checkUserIDorIsAdmin( sm.getOwner().getId() ) ){
				 return Response.status(Status.FORBIDDEN ).build();
			}
			
			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity( new ErrorMsg( "Image id = " + id + " not found in portal registry") );
			throw new WebApplicationException(builder.build());
		}
	}
	
	@GET
	@Path("/admin/vfimages/name/{imagename}")
	@Produces("application/json")
	public Response getVFImageByName(@PathParam("imagename") String imagename) {
		VFImage sm = portalRepositoryRef.getVFImageByName( imagename );

		if (sm != null) {
			if ( !checkUserIDorIsAdmin( sm.getOwner().getId() ) ){
				 return Response.status(Status.FORBIDDEN ).build();
			}
			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity( new ErrorMsg( "Image " + imagename + " not found in portal registry") );
			throw new WebApplicationException(builder.build());
		}
	}

	/**
	 * @param userID
	 * @return true if user logged is equal to the requested id of owner, or is PORTALADMIN
	 */
	private boolean checkUserIDorIsAdmin(int userID){
		if ( sc.isUserInRole( UserRoleType.PORTALADMIN.name() ) ){
			 return true;
		}
		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());
		if ( u.getId() == userID ){
			return true;
		}
		return false;
	}
}
