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
import portal.api.model.PortalUser;
import portal.api.model.Product;
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
		
		
		return Response.ok().entity(portalRepositoryRef.getVFImages()).build();
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
		
		String uuid = UUID.randomUUID().toString();

		logger.info("image name = " + vfimg.getName());
		logger.info("shortDescription = " +vfimg.getShortDescription());

		vfimg.setUuid(uuid);
		vfimg.setDateCreated(new Date());
		

		URI endpointUrl = uri.getBaseUri();
		String tempDir = VFIMAGESDIR + uuid + File.separator;
		Files.createDirectories(Paths.get(tempDir));
	
		if (vfimagefile != null) {
			String imageFileNamePosted = AttachmentUtil.getFileName(vfimagefile.getHeaders());
			logger.info("vfimagefile = " + imageFileNamePosted);
			if (!imageFileNamePosted.equals("")) {
				String imgfile = AttachmentUtil.saveFile( vfimagefile, tempDir + imageFileNamePosted);
				logger.info("vfimagefile saved to = " + imgfile);
				
				vfimg.setPackageLocation(endpointUrl.toString().replace("http:", "") + "repo/vfimages/image/" + uuid + "/"
						+ imageFileNamePosted);
			}
		}
		
		
		// Save now vxf for User
		PortalUser vxfOwner = portalRepositoryRef.getUserByID( vfimg.getOwner().getId() );
		vxfOwner.addVFImage( vfimg );
		vfimg.setOwner(vxfOwner); // replace given owner with the one from our DB

		PortalUser owner = portalRepositoryRef.updateUserInfo( vfimg.getOwner().getId(), vxfOwner);
		VFImage registeredvfimg = portalRepositoryRef.getVFImageByUUID(uuid);
				
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
	@Path("/admin/vfimages/{infraid}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateVFImage(@PathParam("infraid") int infraid, VFImage c) {
		VFImage previousCategory = portalRepositoryRef.getVFImageByID(infraid);

		VFImage u = portalRepositoryRef.updateVFImageInfo(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested Image with name=" + c.getName() + " cannot be updated");
			throw new WebApplicationException(builder.build());
		}

	}

	@DELETE
	@Path("/admin/vfimages/{infraid}")
	public Response deleteVFImage(@PathParam("infraid") int infraid) {
		portalRepositoryRef.deleteVFImage(infraid);
		return Response.ok().build();

	}

	@GET
	@Path("/admin/vfimages/{infraid}")
	@Produces("application/json")
	public Response getVFImageById(@PathParam("infraid") int infraid) {
		VFImage sm = portalRepositoryRef.getVFImageByID(infraid);

		if (sm != null) {
			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Image " + infraid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

}
