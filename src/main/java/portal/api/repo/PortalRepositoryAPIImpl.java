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

import portal.api.model.ExperimentMetadata;
import portal.api.model.PortalProperty;
import portal.api.model.PortalUser;
import portal.api.model.VxFMetadata;
import portal.api.osm.client.OSMClient;
import portal.api.cloudOAuth.KeystoneCloudAccess;
import portal.api.cloudOAuth.OAuthClientManager;
import portal.api.cloudOAuth.OAuthUser;
import portal.api.cloudOAuth.OAuthUtils;
import portal.api.model.Category;
import portal.api.model.DeployArtifact;
import portal.api.model.DeployContainer;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.DeploymentDescriptorStatus;
import portal.api.model.IPortalRepositoryAPI;
import portal.api.model.InstalledVxF;
import portal.api.model.InstalledVxFStatus;
import portal.api.model.MANOplatform;
import portal.api.model.Product;
import portal.api.model.ProductExtensionItem;
import portal.api.model.SubscribedResource;
import portal.api.model.UserSession;
import portal.api.util.EmailUtil;
import urn.ietf.params.xml.ns.yang.nfvo.vnfd.rev150910.vnfd.catalog.Vnfd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;
import org.apache.cxf.rs.security.cors.CorsHeaderConstants;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.apache.cxf.rs.security.cors.LocalPreflight;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrant;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

import com.woorea.openstack.keystone.model.Access;
import com.woorea.openstack.keystone.model.Access.Service.Endpoint;
import com.woorea.openstack.keystone.model.Tenant;
import com.woorea.openstack.keystone.model.Access.Service;
import com.woorea.openstack.nova.model.Server;
import com.woorea.openstack.nova.model.Servers;

//CORS support
//@CrossOriginResourceSharing(
//        allowOrigins = {
//           "http://83.212.106.218"
//        },
//        allowCredentials = true
//        
//)
@Path("/repo")
public class PortalRepositoryAPIImpl implements IPortalRepositoryAPI {

	@Context
	UriInfo uri;

	@Context
	HttpHeaders headers;

	@Context
	MessageContext ws;

	@Context
	protected SecurityContext securityContext;

	private static final transient Log logger = LogFactory.getLog(PortalRepositoryAPIImpl.class.getName());

	private static final String METADATADIR = System.getProperty("user.home") + File.separator + ".portal/metadata/";

	private PortalRepository portalRepositoryRef;
	private OAuthClientManager oAuthClientManagerRef;

	public static final String KEYSTONE_AUTH_URL = "http://cloud.lab.fi-ware.org:4730/v2.0";

	// PortalUser related API

	/*************** Users API *************************/

	@GET
	@Path("/admin/users/")
	@Produces("application/json")
	// @RolesAllowed("admin") //see this for this annotation
	// http://pic.dhe.ibm.com/infocenter/radhelp/v9/index.jsp?topic=%2Fcom.ibm.javaee.doc%2Ftopics%2Ftsecuringejee.html
	public Response getUsers() {

		if (securityContext != null) {
			if (securityContext.getUserPrincipal() != null)
				logger.info(" securityContext.getUserPrincipal().toString() >" + securityContext.getUserPrincipal().getName() + "<");

		}

		return Response.ok().entity(portalRepositoryRef.getUserValues()).build();
	}

	@GET
	@Path("/admin/users/{userid}")
	@Produces("application/json")
	public Response getUserById(@PathParam("userid") int userid) {

		PortalUser u = portalRepositoryRef.getUserByID(userid);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User with id=" + userid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@POST
	@Path("/admin/users/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addUser(PortalUser user) {

		logger.info("Received POST for usergetUsername: " + user.getUsername());
		// logger.info("Received POST for usergetPassword: " + user.getPassword());
		// logger.info("Received POST for usergetOrganization: " + user.getOrganization());

		if ((user.getUsername() == null) || (user.getUsername().equals("") || (user.getEmail() == null) || (user.getEmail().equals("")))) {
			ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
			builder.entity("New user with username=" + user.getUsername() + " cannot be registered");
			logger.info("New user with username=" + user.getUsername() + " cannot be registered BAD_REQUEST.");
			throw new WebApplicationException(builder.build());
		}

		PortalUser u = portalRepositoryRef.getUserByUsername(user.getUsername());
		if (u != null) {
			return Response.status(Status.BAD_REQUEST).entity("Username exists").build();
		}

		u = portalRepositoryRef.getUserByEmail(user.getEmail());
		if (u != null) {
			return Response.status(Status.BAD_REQUEST).entity("Email exists").build();
		}

		u = portalRepositoryRef.addPortalUserToUsers(user);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested user with username=" + user.getUsername() + " cannot be installed");
			return builder.build();
		}
	}

	@POST
	@Path("/register/")
	@Produces("application/json")
	@Consumes("multipart/form-data")
	public Response addNewRegisterUser(List<Attachment> ats) {

		PortalUser user = new PortalUser();
		user.setName(getAttachmentStringValue("name", ats));
		user.setUsername(getAttachmentStringValue("username", ats));
		user.setPassword(getAttachmentStringValue("userpassword", ats));
		user.setOrganization(getAttachmentStringValue("userorganization", ats) + "^^" + getAttachmentStringValue("randomregid", ats));
		user.setEmail(getAttachmentStringValue("useremail", ats));
		user.setActive(false);// in any case the user should be not active
		user.setRole("ROLE_EXPERIMENTER"); // otherwise in post he can choose ROLE_PORTALADMIN, and the immediately register :-)

		String msg = getAttachmentStringValue("emailmessage", ats);
		logger.info("Received register for usergetUsername: " + user.getUsername());

		Response r = addUser(user);

		if (r.getStatusInfo().getStatusCode() == Status.OK.getStatusCode()) {
			logger.info("Email message: " + msg);
			EmailUtil.SendRegistrationActivationEmail(user.getEmail(), msg);
		}

		return r;
	}

	@PUT
	@Path("/admin/users/{userid}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateUserInfo(@PathParam("userid") int userid, PortalUser user) {
		logger.info("Received PUT for user: " + user.getUsername());

		PortalUser previousUser = portalRepositoryRef.getUserByID(userid);

		List<Product> previousProducts = previousUser.getProducts();

		if (user.getProducts().size() == 0) {
			user.getProducts().addAll(previousProducts);
		}

		PortalUser u = portalRepositoryRef.updateUserInfo(userid, user);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested user with username=" + user.getUsername() + " cannot be updated");
			throw new WebApplicationException(builder.build());
		}
	}

	@DELETE
	@Path("/admin/users/{userid}")
	@Produces("application/json")
	public Response deleteUser(@PathParam("userid") int userid) {
		logger.info("Received DELETE for userid: " + userid);

		portalRepositoryRef.deleteUser(userid);

		return Response.ok().build();
	}

	@GET
	@Path("/users/{userid}/vxfs")
	@Produces("application/json")
	public Response getAllVxFsofUser(@PathParam("userid") int userid) {
		logger.info("getAllVxFsofUser for userid: " + userid);
		PortalUser u = portalRepositoryRef.getUserByID(userid);

		if (u != null) {
			List<Product> prods = u.getProducts();
			List<VxFMetadata> vxfs = new ArrayList<VxFMetadata>();
			for (Product p : prods) {
				if (p instanceof VxFMetadata)
					vxfs.add((VxFMetadata) p);
			}

			return Response.ok().entity(vxfs).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User with id=" + userid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/users/{userid}/experiments")
	@Produces("application/json")
	public Response getAllAppsofUser(@PathParam("userid") int userid) {
		logger.info("getAllAppsofUser for userid: " + userid);
		PortalUser u = portalRepositoryRef.getUserByID(userid);

		if (u != null) {
			List<Product> prods = u.getProducts();
			List<ExperimentMetadata> apps = new ArrayList<ExperimentMetadata>();
			for (Product p : prods) {
				if (p instanceof ExperimentMetadata)
					apps.add((ExperimentMetadata) p);
			}

			return Response.ok().entity(apps).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User with id=" + userid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/users/{userid}/vxfs/{vxfid}")
	@Produces("application/json")
	public Response getVxFofUser(@PathParam("userid") int userid, @PathParam("vxfid") int vxfid) {
		logger.info("getVxFofUser for userid: " + userid + ", vxfid=" + vxfid);
		PortalUser u = portalRepositoryRef.getUserByID(userid);

		if (u != null) {
			VxFMetadata vxf = (VxFMetadata) u.getProductById(vxfid);
			return Response.ok().entity(vxf).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User with id=" + userid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/users/{userid}/experiments/{appid}")
	@Produces("application/json")
	public Response getAppofUser(@PathParam("userid") int userid, @PathParam("appid") int appid) {
		logger.info("getAppofUser for userid: " + userid + ", appid=" + appid);
		PortalUser u = portalRepositoryRef.getUserByID(userid);

		if (u != null) {
			ExperimentMetadata appmeta = (ExperimentMetadata) u.getProductById(appid);
			return Response.ok().entity(appmeta).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User with id=" + userid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	// VxFS API

	private Product addNewProductData(Product prod, Attachment image, Attachment vxfFile, List<Attachment> screenshots) {

		String uuid = UUID.randomUUID().toString();

		logger.info("prodname = " + prod.getName());
		logger.info("version = " + prod.getVersion());
		logger.info("shortDescription = " + prod.getShortDescription());
		logger.info("longDescription = " + prod.getLongDescription());

		prod.setUuid(uuid);
		prod.setDateCreated(new Date());
		prod.setDateUpdated(new Date());

		// String[] catIDs = categories.split(",");
		// for (String catid : catIDs) {
		// Category category = portalRepositoryRef.getCategoryByID( Integer.valueOf(catid) );
		// prod.addCategory(category);
		// }

		// for (ProductExtensionItem e : extensions) {
		//
		// }
		//
		// String[] exts = extensions.split(",");
		// for (String extparmval : exts) {
		// String[] i = extparmval.split("=");
		// prod.addExtensionItem(i[0], i[1]);
		// }

		URI endpointUrl = uri.getBaseUri();

		String tempDir = METADATADIR + uuid + File.separator;
		try {
			Files.createDirectories(Paths.get(tempDir));

			if (image != null) {
				String imageFileNamePosted = getFileName(image.getHeaders());
				logger.info("image = " + imageFileNamePosted);
				if (!imageFileNamePosted.equals("")) {
					String imgfile = saveFile(image, tempDir + imageFileNamePosted);
					logger.info("imgfile saved to = " + imgfile);
					prod.setIconsrc(endpointUrl + "repo/images/" + uuid + File.separator + imageFileNamePosted);
				}
			}

			if (vxfFile != null) {
				String vxfFileNamePosted = getFileName(vxfFile.getHeaders());
				logger.info("vxfFile = " + vxfFileNamePosted);
				if (!vxfFileNamePosted.equals("")) {
					String vxffilepath = saveFile(vxfFile, tempDir + vxfFileNamePosted);
					logger.info("vxffilepath saved to = " + vxffilepath);
					prod.setPackageLocation(endpointUrl + "repo/packages/" + uuid + File.separator + vxfFileNamePosted);
				}
			}

			List<Attachment> ss = screenshots;
			String screenshotsFilenames = "";
			int i = 1;
			for (Attachment shot : ss) {
				String shotFileNamePosted = getFileName(shot.getHeaders());
				logger.info("Found screenshot image shotFileNamePosted = " + shotFileNamePosted);
				logger.info("shotFileNamePosted = " + shotFileNamePosted);
				if (!shotFileNamePosted.equals("")) {
					shotFileNamePosted = "shot" + i + "_" + shotFileNamePosted;
					String shotfilepath = saveFile(shot, tempDir + shotFileNamePosted);
					logger.info("shotfilepath saved to = " + shotfilepath);
					shotfilepath = endpointUrl + "repo/images/" + uuid + File.separator + shotFileNamePosted;
					screenshotsFilenames += shotfilepath + ",";
					i++;
				}
			}
			if (screenshotsFilenames.length() > 0)
				screenshotsFilenames = screenshotsFilenames.substring(0, screenshotsFilenames.length() - 1);

			prod.setScreenshots(screenshotsFilenames);

		} catch (IOException e) {
			e.printStackTrace();
		}

		// we must replace given product categories with the ones from our DB
		for (Category c : prod.getCategories()) {
			Category catToUpdate = portalRepositoryRef.getCategoryByID(c.getId());
			// logger.info("BEFORE PROD SAVE, category "+catToUpdate.getName()+"  contains Products: "+ catToUpdate.getProducts().size() );
			prod.getCategories().set(prod.getCategories().indexOf(c), catToUpdate);

		}

		// Save now vxf for User
		PortalUser vxfOwner = portalRepositoryRef.getUserByID(prod.getOwner().getId());
		vxfOwner.addProduct(prod);
		prod.setOwner(vxfOwner); // replace given owner with the one from our DB

		PortalUser owner = portalRepositoryRef.updateUserInfo(prod.getOwner().getId(), vxfOwner);
		Product registeredProd = portalRepositoryRef.getProductByUUID(uuid);

		// now fix category references
		for (Category c : registeredProd.getCategories()) {
			Category catToUpdate = portalRepositoryRef.getCategoryByID(c.getId());
			catToUpdate.addProduct(registeredProd);
			portalRepositoryRef.updateCategoryInfo(catToUpdate);
		}

		return registeredProd;
	}

	/******************* VxFs API ***********************/

	@GET
	@Path("/vxfs")
	@Produces("application/json")
	public Response getAllVxFs(@QueryParam("categoryid") Long categoryid) {
		logger.info("getVxFs categoryid=" + categoryid);

		List<VxFMetadata> vxfs = portalRepositoryRef.getVxFs(categoryid);
		return Response.ok().entity(vxfs).build();

	}

	@GET
	@Path("/admin/vxfs")
	@Produces("application/json")
	public Response getVxFs(@QueryParam("categoryid") Long categoryid) {
		logger.info("getVxFs categoryid=" + categoryid);

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u != null) {
			List<VxFMetadata> vxfs;

			if (u.getRole().equals("ROLE_PORTALADMIN")) {
				vxfs = portalRepositoryRef.getVxFs(categoryid);
			} else {
				vxfs = portalRepositoryRef.getVxFsByUserID((long) u.getId());
			}

			return Response.ok().entity(vxfs).build();

		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User not found in portal registry or not logged in");
			throw new WebApplicationException(builder.build());
		}

	}

	@POST
	@Path("/admin/vxfs/")
	@Consumes("multipart/form-data")
	public Response addVxFMetadata(List<Attachment> ats) {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u == null) {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User not found in portal registry or not logged in ");
			throw new WebApplicationException(builder.build());
		}

		VxFMetadata vxf = new VxFMetadata();

		try {
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser(getAttachmentStringValue("vxf", ats));
			vxf = parser.readValueAs(VxFMetadata.class);

			logger.info("Received @POST for vxf : " + vxf.getName());
			logger.info("Received @POST for vxf.extensions : " + vxf.getExtensions());

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		vxf = (VxFMetadata) addNewProductData(vxf,

		getAttachmentByName("prodIcon", ats), getAttachmentByName("prodFile", ats), getListOfAttachmentsByName("screenshots", ats));

		return Response.ok().entity(vxf).build();

	}

	@PUT
	@Path("/admin/vxfs/{bid}")
	@Consumes("multipart/form-data")
	public Response updateVxFMetadata(@PathParam("bid") int bid, List<Attachment> ats) {

		VxFMetadata vxf = new VxFMetadata();

		try {
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser(getAttachmentStringValue("vxf", ats));
			vxf = parser.readValueAs(VxFMetadata.class);

			logger.info("Received @POST for vxf : " + vxf.getName());
			logger.info("Received @POST for vxf.extensions : " + vxf.getExtensions());

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// VxFMetadata sm = (VxFMetadata) portalRepositoryRef.getProductByID(bid);
		vxf = (VxFMetadata) updateProductMetadata(vxf, getAttachmentByName("prodIcon", ats), getAttachmentByName("prodFile", ats),
				getListOfAttachmentsByName("screenshots", ats));

		return Response.ok().entity(vxf).build();

	}

	// VxFs related API

	private Product updateProductMetadata(Product prod, Attachment image, Attachment prodFile, List<Attachment> screenshots) {

		logger.info("userid = " + prod.getOwner().getId());
		logger.info("vxfname = " + prod.getName());
		logger.info("vxfid = " + prod.getId());

		logger.info("vxfuuid = " + prod.getUuid());
		logger.info("version = " + prod.getVersion());
		logger.info("shortDescription = " + prod.getShortDescription());
		logger.info("longDescription = " + prod.getLongDescription());

		// get User
		PortalUser vxfOwner = portalRepositoryRef.getUserByID(prod.getOwner().getId());
		prod.setOwner(vxfOwner); // replace given owner with the one from our DB

		prod.setDateUpdated(new Date());

		// first remove all references of the product from the previous categories
		Product prodPreUpdate = (Product) portalRepositoryRef.getProductByID(prod.getId());
		for (Category c : prodPreUpdate.getCategories()) {
			// logger.info("Will remove product "+prodPreUpdate.getName()+ ", from Previous Category "+c.getName() );
			c.removeProduct(prodPreUpdate);
			portalRepositoryRef.updateCategoryInfo(c);
		}

		// we must replace API given product categories with the ones from our DB
		for (Category c : prod.getCategories()) {
			Category catToUpdate = portalRepositoryRef.getCategoryByID(c.getId());
			// logger.info("BEFORE PROD SAVE, category "+catToUpdate.getName()+"  contains Products: "+ catToUpdate.getProducts().size() );
			prod.getCategories().set(prod.getCategories().indexOf(c), catToUpdate);
		}

		URI endpointUrl = uri.getBaseUri();

		String tempDir = METADATADIR + prod.getUuid() + File.separator;
		try {
			Files.createDirectories(Paths.get(tempDir));

			if (image != null) {
				String imageFileNamePosted = getFileName(image.getHeaders());
				logger.info("image = " + imageFileNamePosted);
				if (!imageFileNamePosted.equals("unknown")) {
					String imgfile = saveFile(image, tempDir + imageFileNamePosted);
					logger.info("imgfile saved to = " + imgfile);
					prod.setIconsrc(endpointUrl + "repo/images/" + prod.getUuid() + File.separator + imageFileNamePosted);
				}
			}

			if (prodFile != null) {
				String vxfFileNamePosted = getFileName(prodFile.getHeaders());
				logger.info("vxfFile = " + vxfFileNamePosted);
				if (!vxfFileNamePosted.equals("unknown")) {
					String vxffilepath = saveFile(prodFile, tempDir + vxfFileNamePosted);
					logger.info("vxffilepath saved to = " + vxffilepath);
					prod.setPackageLocation(endpointUrl + "repo/packages/" + prod.getUuid() + File.separator + vxfFileNamePosted);
				}
			}

			List<Attachment> ss = screenshots;
			String screenshotsFilenames = "";
			int i = 1;
			for (Attachment shot : ss) {
				String shotFileNamePosted = getFileName(shot.getHeaders());
				logger.info("Found screenshot image shotFileNamePosted = " + shotFileNamePosted);
				logger.info("shotFileNamePosted = " + shotFileNamePosted);
				if (!shotFileNamePosted.equals("")) {
					shotFileNamePosted = "shot" + i + "_" + shotFileNamePosted;
					String shotfilepath = saveFile(shot, tempDir + shotFileNamePosted);
					logger.info("shotfilepath saved to = " + shotfilepath);
					shotfilepath = endpointUrl + "repo/images/" + prod.getUuid() + File.separator + shotFileNamePosted;
					screenshotsFilenames += shotfilepath + ",";
					i++;
				}
			}
			if (screenshotsFilenames.length() > 0)
				screenshotsFilenames = screenshotsFilenames.substring(0, screenshotsFilenames.length() - 1);

			prod.setScreenshots(screenshotsFilenames);

		} catch (IOException e) {

			e.printStackTrace();
		}

		// save product
		prod = portalRepositoryRef.updateProductInfo(prod);

		// now fix category product references
		for (Category catToUpdate : prod.getCategories()) {
			Product p = portalRepositoryRef.getProductByID(prod.getId());
			catToUpdate.addProduct(p);
			portalRepositoryRef.updateCategoryInfo(catToUpdate);
		}

		if (vxfOwner.getProductById(prod.getId()) == null)
			vxfOwner.addProduct(prod);
		portalRepositoryRef.updateUserInfo(prod.getOwner().getId(), vxfOwner);
		return prod;
	}

	@GET
	@Path("/images/{uuid}/{imgfile}")
	@Produces("image/*")
	public Response getEntityImage(@PathParam("uuid") String uuid, @PathParam("imgfile") String imgfile) {
		logger.info("getEntityImage of uuid: " + uuid);
		String imgAbsfile = METADATADIR + uuid + File.separator + imgfile;
		logger.info("Image RESOURCE FILE: " + imgAbsfile);
		File file = new File(imgAbsfile);
		ResponseBuilder response = Response.ok((Object) file);
		response.header("Content-Disposition", "attachment; filename=" + file.getName());
		return response.build();

	}

	@GET
	@Path("/packages/{uuid}/{vxffile}")
	@Produces("application/gzip")
	public Response downloadVxFPackage(@PathParam("uuid") String uuid, @PathParam("vxffile") String vxffile) {

		logger.info("vxffile: " + vxffile);
		logger.info("uuid: " + uuid);

		String vxfAbsfile = METADATADIR + uuid + File.separator + vxffile;
		logger.info("VxF RESOURCE FILE: " + vxfAbsfile);
		File file = new File(vxfAbsfile);

		if ((uuid.equals("77777777-668b-4c75-99a9-39b24ed3d8be")) || (uuid.equals("22cab8b8-668b-4c75-99a9-39b24ed3d8be"))) {
			URL res = getClass().getResource("/files/" + vxffile);
			logger.info("TEST LOCAL RESOURCE FILE: " + res);
			file = new File(res.getFile());
		}

		ResponseBuilder response = Response.ok((Object) file);
		response.header("Content-Disposition", "attachment; filename=" + file.getName());
		return response.build();
	}

	@DELETE
	@Path("/admin/vxfs/{vxfid}")
	public void deleteVxF(@PathParam("vxfid") int vxfid) {
		portalRepositoryRef.deleteProduct(vxfid);
	}

	@GET
	@Path("/vxfs/{vxfid}")
	@Produces("application/json")
	public Response getVxFMetadataByID(@PathParam("vxfid") int vxfid) {
		logger.info("getVxFMetadataByID  vxfid=" + vxfid);
		VxFMetadata vxf = (VxFMetadata) portalRepositoryRef.getProductByID(vxfid);

		if (vxf != null) {
			return Response.ok().entity(vxf).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("vxf with id=" + vxfid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}
	
	
	@GET
	@Path("/osmvnfds/{vxfid}")
	@Produces("application/json")
	public Response getOSMVNFMetadataByID(@PathParam("vxfid") String vxfid) {
		logger.info("getOSMVNFMetadataByID  vxfid=" + vxfid);
		
		Vnfd vnfd = OSMClient.getVNFDbyID( vxfid ) ; 

		if (vnfd != null) {
			return Response.ok().entity(vnfd).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("vxf with id=" + vxfid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/admin/vxfs/{vxfid}")
	@Produces("application/json")
	public Response getAdminVxFMetadataByID(@PathParam("vxfid") int vxfid) {

		return getVxFMetadataByID(vxfid);
	}

	@GET
	@Path("/vxfs/uuid/{uuid}")
	@Produces("application/json")
	public Response getVxFMetadataByUUID(@PathParam("uuid") String uuid) {

		logger.info("Received GET for vxf uuid: " + uuid);
		VxFMetadata vxf = null;

		URI endpointUrl = uri.getBaseUri();
		if (uuid.equals("77777777-668b-4c75-99a9-39b24ed3d8be")) {
			vxf = new VxFMetadata();
			vxf.setUuid(uuid);
			vxf.setName("IntegrTestLocal example service");
			vxf.setShortDescription("An example local service");
			vxf.setVersion("1.0.0");
			vxf.setIconsrc("");
			vxf.setLongDescription("");

			vxf.setPackageLocation(endpointUrl + "repo/packages/77777777-668b-4c75-99a9-39b24ed3d8be/examplevxf.tar.gz");
			// }else if (uuid.equals("12cab8b8-668b-4c75-99a9-39b24ed3d8be")) {
			// vxf = new VxFMetadata(uuid, "AN example service");
			// vxf.setShortDescription("An example local service");
			// vxf.setVersion("1.0.0rc1");
			// vxf.setIconsrc("");
			// vxf.setLongDescription("");
			// //URI endpointUrl = uri.getBaseUri();
			//
			// vxf.setPackageLocation( endpointUrl +"repo/packages/12cab8b8-668b-4c75-99a9-39b24ed3d8be/examplevxf.tar.gz");
		} else if (uuid.equals("22cab8b8-668b-4c75-99a9-39b24ed3d8be")) {
			vxf = new VxFMetadata();
			vxf.setUuid(uuid);
			vxf.setName("IntegrTestLocal example ErrInstall service");
			vxf.setShortDescription("An example ErrInstall local service");
			vxf.setVersion("1.0.0");
			vxf.setIconsrc("");
			vxf.setLongDescription("");
			// URI endpointUrl = uri.getBaseUri();

			vxf.setPackageLocation(endpointUrl + "repo/packages/22cab8b8-668b-4c75-99a9-39b24ed3d8be/examplevxfErrInstall.tar.gz");
		} else {
			vxf = (VxFMetadata) portalRepositoryRef.getProductByUUID(uuid);
		}

		if (vxf != null) {
			return Response.ok().entity(vxf).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Installed vxf with uuid=" + uuid + " not found in local registry");
			throw new WebApplicationException(builder.build());
		}

	}

	public PortalRepository getPortalRepositoryRef() {
		return portalRepositoryRef;
	}

	public void setPortalRepositoryRef(PortalRepository portalRepositoryRef) {
		this.portalRepositoryRef = portalRepositoryRef;
	}

	public void setoAuthClientManagerRef(OAuthClientManager oAuthClientManagerRef) {
		this.oAuthClientManagerRef = oAuthClientManagerRef;
	}

	// Sessions related API

	// @OPTIONS
	// @Path("/sessions/")
	// @Produces("application/json")
	// @Consumes("application/json")
	// @LocalPreflight
	// public Response addUserSessionOption(){
	//
	//
	// logger.info("Received OPTIONS  addUserSessionOption ");
	// String origin = headers.getRequestHeader("Origin").get(0);
	// if (origin != null) {
	// return Response.ok()
	// .header(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS, "GET POST DELETE PUT HEAD OPTIONS")
	// .header(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS, "true")
	// .header(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS, "Origin, X-Requested-With, Content-Type, Accept")
	// .header(CorsHeaderConstants.HEADER_AC_ALLOW_ORIGIN, origin)
	// .build();
	// } else {
	// return Response.ok().build();
	// }
	// }

	@POST
	@Path("/sessions/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addUserSession(UserSession userSession) {

		logger.info("Received POST addUserSession usergetUsername: " + userSession.getUsername());
		// logger.info("DANGER, REMOVE Received POST addUserSession password: " + userSession.getPassword());

		if (securityContext != null) {
			if (securityContext.getUserPrincipal() != null)
				logger.info(" securityContext.getUserPrincipal().toString() >" + securityContext.getUserPrincipal().toString() + "<");

		}

		Subject currentUser = SecurityUtils.getSubject();
		if (currentUser != null) {
			AuthenticationToken token = new UsernamePasswordToken(userSession.getUsername(), userSession.getPassword());
			try {
				currentUser.login(token);
				PortalUser portalUser = portalRepositoryRef.getUserByUsername(userSession.getUsername());
				portalUser.setCurrentSessionID(ws.getHttpServletRequest().getSession().getId());
				userSession.setPortalUser(portalUser);
				userSession.setPassword("");
				;// so not tosend in response

				logger.info(" currentUser = " + currentUser.toString());
				logger.info("User [" + currentUser.getPrincipal() + "] logged in successfully.");

				portalRepositoryRef.updateUserInfo(portalUser.getId(), portalUser);

				return Response.ok().entity(userSession).build();
			} catch (AuthenticationException ae) {

				return Response.status(Status.UNAUTHORIZED).build();
			}
		}

		return Response.status(Status.UNAUTHORIZED).build();
	}

	@GET
	@Path("/sessions/logout")
	@Produces("application/json")
	public Response logoutUser() {

		logger.info("Received logoutUser ");

		if (securityContext != null) {
			if (securityContext.getUserPrincipal() != null)
				logger.info(" securityContext.getUserPrincipal().toString() >" + securityContext.getUserPrincipal().toString() + "<");

			SecurityUtils.getSubject().logout();
		}

		return Response.ok().build();
	}

	// THIS IS NOT USED
	@GET
	@Path("/sessions/")
	@Produces("application/json")
	public Response getUserSessions() {

		logger.info("Received GET addUserSession usergetUsername: ");
		logger.info("Received GET addUserSession password: ");

		if (securityContext != null) {
			if (securityContext.getUserPrincipal() != null)
				logger.info(" securityContext.getUserPrincipal().toString() >" + securityContext.getUserPrincipal().toString() + "<");

		}

		Subject currentUser = SecurityUtils.getSubject();
		if ((currentUser != null) && (currentUser.getPrincipal() != null)) {

			// logger.info(" currentUser = " + currentUser.toString() );
			// logger.info( "User [" + currentUser.getPrincipal() + "] logged in successfully." );
			// logger.info(" currentUser  employee  = " + currentUser.hasRole("employee") );
			// logger.info(" currentUser  boss  = " + currentUser.hasRole("boss") );

			return Response.ok().build();
		}

		return Response.status(Status.UNAUTHORIZED).build();
	}

	// Subscribed resources related API

	@GET
	@Path("/admin/subscribedresources/")
	@Produces("application/json")
	public Response getSubscribedResources() {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u != null) {

			if (u.getRole().equals("ROLE_PORTALADMIN")) {
				return Response.ok().entity(portalRepositoryRef.getSubscribedResourcesAsCollection()).build(); // return all
			} else
				return Response.ok().entity(u.getSubscribedResources()).build();

		}

		ResponseBuilder builder = Response.status(Status.NOT_FOUND);
		builder.entity("User not found in portal registry or not logged in");
		throw new WebApplicationException(builder.build());

	}

	@GET
	@Path("/admin/subscribedresources/{smId}")
	@Produces("application/json")
	public Response getSubscribedResourceById(@PathParam("smId") int smId) {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		SubscribedResource sm = portalRepositoryRef.getSubscribedResourceByID(smId);

		if ((sm != null) && (u != null)) {

			if ((u.getRole().equals("ROLE_PORTALADMIN")) || (sm.getOwner().getId() == u.getId()))
				return Response.ok().entity(sm).build();

		}

		ResponseBuilder builder = Response.status(Status.NOT_FOUND);
		builder.entity("SubscribedResource" + smId + " not found in portal registry");
		throw new WebApplicationException(builder.build());

	}

	@POST
	@Path("/admin/subscribedresources/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addSubscribedResource(SubscribedResource sm) {

		PortalUser u = sm.getOwner();
		u = portalRepositoryRef.getUserByID(sm.getOwner().getId());

		if (u != null) {
			sm.setOwner(u);

			u.getSubscribedResources().add(sm);
			u = portalRepositoryRef.updateUserInfo(u.getId(), u);

			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested SubscribedResource with rls=" + sm.getURL() + " cannot be registered under not found user");
			throw new WebApplicationException(builder.build());
		}
	}

	@PUT
	@Path("/admin/subscribedresources/{smId}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateSubscribedResource(@PathParam("smId") int smId, SubscribedResource sm) {
		logger.info("Received SubscribedResource for user: " + sm.getURL());

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		PortalUser reattachedUser = portalRepositoryRef.getUserByID(sm.getOwner().getId());
		sm.setOwner(reattachedUser);

		if (u != null) {

			if ((u.getRole().equals("ROLE_PORTALADMIN")) || (sm.getOwner().getId() == u.getId())) {

				SubscribedResource sr = portalRepositoryRef.updateSubscribedResourceInfo(smId, sm);
				return Response.ok().entity(u).build();
			}

		}

		ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
		builder.entity("Requested SubscribedResource with url=" + sm.getURL() + " cannot be updated");
		throw new WebApplicationException(builder.build());

	}

	@DELETE
	@Path("/admin/subscribedresources/{smId}")
	@Produces("application/json")
	public Response deleteSubscribedResource(@PathParam("smId") int smId) {
		logger.info("Received SubscribedResource for userid: " + smId);

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		SubscribedResource sm = portalRepositoryRef.getSubscribedResourceByID(smId);
		if (u != null) {

			if ((u.getRole().equals("ROLE_PORTALADMIN")) || (sm.getOwner().getId() == u.getId())) {
				portalRepositoryRef.deleteSubscribedResource(smId);
				return Response.ok().build();

			}
		}

		ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
		builder.entity("Requested SubscribedResource with id=" + smId + " cannot be deleted");
		throw new WebApplicationException(builder.build());
	}

	// Applications related API

	@GET
	@Path("/admin/experiments")
	@Produces("application/json")
	public Response getApps(@QueryParam("categoryid") Long categoryid) {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u != null) {
			List<ExperimentMetadata> apps;

			if (u.getRole().equals("ROLE_PORTALADMIN")) {
				apps = portalRepositoryRef.getApps(categoryid);
			} else {
				apps = portalRepositoryRef.getAppsByUserID((long) u.getId());
			}

			return Response.ok().entity(apps).build();

		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User not found in portal registry or not logged in");
			throw new WebApplicationException(builder.build());
		}

	}

	@GET
	@Path("/experiments")
	@Produces("application/json")
	public Response getAllApps(@QueryParam("categoryid") Long categoryid) {
		logger.info("getApps categoryid=" + categoryid);
		List<ExperimentMetadata> vxfs = portalRepositoryRef.getApps(categoryid);
		return Response.ok().entity(vxfs).build();
	}

	@GET
	@Path("/experiments/{appid}")
	@Produces("application/json")
	public Response getAppMetadataByID(@PathParam("appid") int appid) {
		logger.info("getAppMetadataByID  appid=" + appid);
		ExperimentMetadata app = (ExperimentMetadata) portalRepositoryRef.getProductByID(appid);

		if (app != null) {
			return Response.ok().entity(app).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("App with id=" + appid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/admin/experiments/{appid}")
	@Produces("application/json")
	public Response getAdminAppMetadataByID(@PathParam("appid") int appid) {
		return getAppMetadataByID(appid);
	}

	@GET
	@Path("/experiments/uuid/{uuid}")
	@Produces("application/json")
	public Response getAppMetadataByUUID(@PathParam("uuid") String uuid) {
		logger.info("Received GET for app uuid: " + uuid);
		ExperimentMetadata app = null;

		URI endpointUrl = uri.getBaseUri();
		app = (ExperimentMetadata) portalRepositoryRef.getProductByUUID(uuid);

		if (app != null) {
			return Response.ok().entity(app).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Installed app with uuid=" + uuid + " not found in local registry");
			throw new WebApplicationException(builder.build());
		}

	}

	@POST
	@Path("/admin/experiments/")
	@Consumes("multipart/form-data")
	public Response addAppMetadata(List<Attachment> ats) {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u == null) {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User not found in portal registry or not logged in ");
			throw new WebApplicationException(builder.build());
		}

		ExperimentMetadata app = new ExperimentMetadata();

		try {
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser(getAttachmentStringValue("application", ats));
			app = parser.readValueAs(ExperimentMetadata.class);

			logger.info("Received @POST for app : " + app.getName());
			logger.info("Received @POST for app.containers : " + app.getContainers().size());
			logger.info("Received @POST for app.containers(0).name : " + app.getContainers().get(0).getName());

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ExperimentMetadata sm = new ExperimentMetadata();
		app = (ExperimentMetadata) addNewProductData(app, getAttachmentByName("prodIcon", ats), getAttachmentByName("prodFile", ats),
				getListOfAttachmentsByName("screenshots", ats));

		return Response.ok().entity(app).build();

	}

	@PUT
	@Path("/admin/experiments/{aid}")
	@Consumes("multipart/form-data")
	public Response updateAppMetadata(@PathParam("aid") int aid, List<Attachment> ats) {

		ExperimentMetadata appmeta = new ExperimentMetadata();

		try {
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser(getAttachmentStringValue("application", ats));
			appmeta = parser.readValueAs(ExperimentMetadata.class);

			logger.info("Received @POST for app : " + appmeta.getName());
			logger.info("Received @POST for app.containers : " + appmeta.getContainers().size());

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ExperimentMetadata appmeta = (ExperimentMetadata) portalRepositoryRef.getProductByID(aid);

		appmeta = (ExperimentMetadata) updateProductMetadata(appmeta, getAttachmentByName("prodIcon", ats), getAttachmentByName("prodFile", ats),
				getListOfAttachmentsByName("screenshots", ats));

		return Response.ok().entity(appmeta).build();
	}

	@DELETE
	@Path("/admin/experiments/{appid}")
	public void deleteApp(@PathParam("appid") int appid) {
		portalRepositoryRef.deleteProduct(appid);

	}

	// categories API
	@GET
	@Path("/categories/")
	@Produces("application/json")
	public Response getCategories() {
		return Response.ok().entity(portalRepositoryRef.getCategories()).build();
	}

	@GET
	@Path("/admin/categories/")
	@Produces("application/json")
	public Response getAdminCategories() {
		return Response.ok().entity(portalRepositoryRef.getCategories()).build();
	}

	@POST
	@Path("/admin/categories/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addCategory(Category c) {
		Category u = portalRepositoryRef.addCategory(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested Category with name=" + c.getName() + " cannot be installed");
			throw new WebApplicationException(builder.build());
		}
	}

	@PUT
	@Path("/admin/categories/{catid}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateCategory(@PathParam("catid") int catid, Category c) {
		Category previousCategory = portalRepositoryRef.getCategoryByID(catid);

		Category u = portalRepositoryRef.updateCategoryInfo(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested Category with name=" + c.getName() + " cannot be updated");
			throw new WebApplicationException(builder.build());
		}

	}

	@DELETE
	@Path("/admin/categories/{catid}")
	public Response deleteCategory(@PathParam("catid") int catid) {
		Category category = portalRepositoryRef.getCategoryByID(catid);
		if ((category.getProducts().size() > 0)) {
			ResponseBuilder builder = Response.status(Status.METHOD_NOT_ALLOWED);
			builder.entity("The category has assigned elements. You cannot delete it!");
			throw new WebApplicationException(builder.build());
		} else {
			portalRepositoryRef.deleteCategory(catid);
			return Response.ok().build();
		}
	}

	@GET
	@Path("/categories/{catid}")
	@Produces("application/json")
	public Response getCategoryById(@PathParam("catid") int catid) {
		Category sm = portalRepositoryRef.getCategoryByID(catid);

		if (sm != null) {
			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Category " + catid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/admin/categories/{catid}")
	@Produces("application/json")
	public Response getAdminCategoryById(@PathParam("catid") int catid) {
		return getCategoryById(catid);
	}

	// Attachment utils ///////////////////////
	private String saveFile(Attachment att, String filePath) {
		DataHandler handler = att.getDataHandler();
		try {
			InputStream stream = handler.getInputStream();
			MultivaluedMap map = att.getHeaders();
			File f = new File(filePath);
			OutputStream out = new FileOutputStream(f);

			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = stream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			stream.close();
			out.flush();
			out.close();
			return f.getAbsolutePath();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String getFileName(MultivaluedMap<String, String> header) {
		String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
		for (String filename : contentDisposition) {
			if ((filename.trim().startsWith("filename"))) {
				String[] name = filename.split("=");
				String exactFileName = name[1].trim().replaceAll("\"", "");
				return exactFileName;
			}
		}
		return "unknown";
	}

	public String getAttachmentStringValue(String name, List<Attachment> attachments) {

		Attachment att = getAttachmentByName(name, attachments);
		if (att != null) {
			return att.getObject(String.class);
		}
		return null;
	}

	public Attachment getAttachmentByName(String name, List<Attachment> attachments) {

		for (Attachment attachment : attachments) {
			String s = getAttachmentName(attachment.getHeaders());
			if ((s != null) && (s.equals(name)))
				return attachment;
		}

		return null;
	}

	private List<Attachment> getListOfAttachmentsByName(String name, List<Attachment> attachments) {

		List<Attachment> la = new ArrayList<Attachment>();
		for (Attachment attachment : attachments) {
			if (getAttachmentName(attachment.getHeaders()).equals(name))
				la.add(attachment);
		}
		return la;
	}

	private String getAttachmentName(MultivaluedMap<String, String> header) {

		if (header.getFirst("Content-Disposition") != null) {
			String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
			for (String filename : contentDisposition) {
				if ((filename.trim().startsWith("name"))) {
					String[] name = filename.split("=");
					String exactFileName = name[1].trim().replaceAll("\"", "");
					return exactFileName;
				}
			}
		}
		return null;
	}

	/***************************************** OAUTH2 cloud Related API *********************************************/

	@GET
	@Path("/oauth2/")
	@Produces("application/json")
	public Response oauth2Sessions(@QueryParam("oath2serverurl") String oath2serverurl, @QueryParam("oath2requestkey") String oath2requestkey) {

		// the params
		logger.info("Received GET oath2serverurl: " + oath2serverurl);
		logger.info("Received GET oath2requestkey: " + oath2requestkey);

		return Response.seeOther(oAuthClientManagerRef.getAuthorizationServiceURI(getCallbackURI(), oath2requestkey)).build();
	}

	@GET
	@Path("/oauth2/login")
	@Produces("text/html")
	// @Produces("application/json")
	public Response oauth2login(@QueryParam("code") String code) {

		// This one is the callback URL, which is called by the cloud OAUTH2 service
		logger.info("Received authorized request token code: " + code + ". Preparing AuthorizationCodeGrant header.");

		AuthorizationCodeGrant codeGrant = new AuthorizationCodeGrant(code, getCallbackURI());
		logger.info("Requesting OAuth server accessTokenService to replace an authorized request token with an access token");
		ClientAccessToken accessToken = oAuthClientManagerRef.getAccessToken(codeGrant);
		if (accessToken == null) {
			String msg = "NO_OAUTH_ACCESS_TOKEN, Problem replacing your authorization key for OAuth access token,  please report to portal admin";
			logger.info(msg);
			return Response.status(Status.UNAUTHORIZED).entity(msg).build();
		}

		try {
			logger.info("OAUTH2 accessTokenService accessToken = " + accessToken.toString());
			String authHeader = oAuthClientManagerRef.createAuthorizationHeader(accessToken);
			logger.info("OAUTH2 accessTokenService authHeader = " + authHeader);
			logger.info("accessToken getTokenType= " + accessToken.getTokenType());
			logger.info("accessToken getTokenKey= " + accessToken.getTokenKey());
			logger.info("accessToken getRefreshToken= " + accessToken.getRefreshToken());
			logger.info("accessToken getExpiresIn= " + accessToken.getExpiresIn());

			Tenant t = KeystoneCloudAccess.getFirstTenant(accessToken.getTokenKey());
			OAuthUser fu = OAuthUtils.getOAuthUser(authHeader, accessToken); //get user information since we are authorized via oauth
			fu.setxOAuth2Token(accessToken.getTokenKey());
			fu.setTenantName(t.getName());
			fu.setTenantId(t.getId());
			fu.setCloudToken(KeystoneCloudAccess.getAccessModel(t, accessToken.getTokenKey()).getToken().getId());

			// check if user exists in Portal database
			PortalUser u = portalRepositoryRef.getUserByUsername(fu.getNickName());

			String roamPassword = UUID.randomUUID().toString(); // creating a temporary session password, to login
			if (u == null) {
				u = new PortalUser(); // create as new user
				u.setEmail(fu.getEmail());
				u.setUsername(fu.getNickName());
				;
				u.setName(fu.getDisplayName());
				u.setOrganization("FI-WARE");
				u.setRole("TESTBED_PROVIDER");
				u.setPassword(roamPassword);
				u.setCurrentSessionID(ws.getHttpServletRequest().getSession().getId());
				portalRepositoryRef.addPortalUserToUsers(u);
			} else {
				u.setEmail(fu.getEmail());
				u.setName(fu.getDisplayName());
				u.setPassword(roamPassword);
				u.setOrganization("FI-WARE");
				u.setCurrentSessionID(ws.getHttpServletRequest().getSession().getId());
				u = portalRepositoryRef.updateUserInfo(u.getId(), u);
			}

			UserSession userSession = new UserSession();
			userSession.setPortalUser(u);
			userSession.setPassword(roamPassword);
			userSession.setUsername(u.getUsername());
			userSession.setCLOUDUser(fu);

			Subject currentUser = SecurityUtils.getSubject();
			if (currentUser != null) {
				AuthenticationToken token = new UsernamePasswordToken(userSession.getUsername(), userSession.getPassword());
				try {
					currentUser.login(token);

				} catch (AuthenticationException ae) {

					return Response.status(Status.UNAUTHORIZED).build();
				}
			}

			userSession.setPassword("");// trick so not to send in response
			ObjectMapper mapper = new ObjectMapper();

			// see https://developer.mozilla.org/en-US/docs/Web/API/Window.postMessage
			// there are CORS issues so to do this trich the popup window communicates with the parent window ia this script
			String comScript = "<script type='text/javascript'>function receiveMessage(event){" + "event.source.postMessage('"
					+ mapper.writeValueAsString(userSession) + "', event.origin);" + "}" + "window.addEventListener('message', receiveMessage, false);"
					+ "</script>";

			return Response.ok("<html><body><p>Succesful Login</p>" + comScript + "</body></html>"

			).build();

		} catch (RuntimeException ex) {
			ex.printStackTrace();
			return Response.status(Status.UNAUTHORIZED).entity("USER Access problem").build();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Response.ok().build();

	}

	private URI getCallbackURI() {
		return URI.create(uri.getBaseUri() + "repo/oauth2/login");
	}

	@GET
	@Path("/cloud/computeendpoints")
	@Produces("application/json")
	public Response getCLOUDServiceCatalogComputeEndpoints(@QueryParam("xauthtoken") String xauthtoken) {

		List<Endpoint> scatalog = KeystoneCloudAccess.getServiceCatalogEndpointsOnlyCompute(xauthtoken);

		return Response.ok(scatalog).build();
	}

	@GET
	@Path("/cloud/servers")
	@Produces("application/json")
	public Response getCLOUDServiceComputeServers(@QueryParam("endPointPublicURL") String endPointPublicURL,
			@QueryParam("cloudAccessToken") String cloudAccessToken) {

		ArrayList<Server> servers = KeystoneCloudAccess.getServers(endPointPublicURL, cloudAccessToken);

		return Response.ok(servers).build();
	}

	@GET
	@Path("/admin/properties/")
	@Produces("application/json")
	public Response getProperties() {
		return Response.ok().entity(portalRepositoryRef.getProperties()).build();
	}

	@PUT
	@Path("/admin/properties/{propid}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateProperty(@PathParam("catid") int propid, PortalProperty p) {
		PortalProperty previousProperty = portalRepositoryRef.getPropertyByID(propid);

		PortalProperty u = portalRepositoryRef.updateProperty(p);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested PortalProperty with name=" + p.getName() + " cannot be updated");
			throw new WebApplicationException(builder.build());
		}

	}

	@GET
	@Path("/admin/properties/{propid}")
	@Produces("application/json")
	public Response getPropertyById(@PathParam("propid") int propid) {
		PortalProperty sm = portalRepositoryRef.getPropertyByID(propid);

		if (sm != null) {
			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("PortalProperty " + propid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/admin/deployments")
	@Produces("application/json")
	public Response getAllDeploymentsofUser() {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u != null) {
			logger.info("getAllDeploymentsofUser for userid: " + u.getId());
			List<DeploymentDescriptor> deployments;

			if (u.getRole().equals("ROLE_PORTALADMIN")) {
				deployments = portalRepositoryRef.getAllDeploymentDescriptors();
			} else {
				deployments = u.getDeployments();
			}

			return Response.ok().entity(deployments).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User not found in portal registry or not logged in");
			throw new WebApplicationException(builder.build());
		}

	}

	@POST
	@Path("/admin/deployments")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addDeployment(DeploymentDescriptor deployment) {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u != null) {
			logger.info("addDeployment for userid: " + u.getId());

			for (DeploymentDescriptor d : u.getDeployments()) {
				logger.info("deployment already for userid: " + d.getId());
			}

			deployment.setDateCreated(new Date());
			deployment.setStatus(DeploymentDescriptorStatus.PENDING_ADMIN_AUTH);

			u = portalRepositoryRef.getUserByID(u.getId());
			deployment.setOwner(u); // reattach from the DB model
			u.getDeployments().add(deployment);

			ExperimentMetadata baseApplication = (ExperimentMetadata) portalRepositoryRef.getProductByID(deployment.getBaseApplication().getId());
			deployment.setBaseApplication(baseApplication); // reattach from the DB model

			for (DeployContainer dc : deployment.getDeployContainers()) {
				dc.getTargetResource().setOwner(u);// reattach from the DB model, in case missing from the request
			}

			u = portalRepositoryRef.updateUserInfo(u.getId(), u);

			return Response.ok().entity(deployment).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User not found in portal registry or not logged in. DeploymentDescriptor not added.");
			throw new WebApplicationException(builder.build());
		}
	}

	@DELETE
	@Path("/admin/deployments/{id}")
	@Consumes("application/json")
	@Produces("application/json")
	public Response deleteDeployment(@PathParam("id") int id) {
		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		DeploymentDescriptor dep = portalRepositoryRef.getDeploymentByID(id);
		if (u != null) {
			if  (  u.getRole().equals("ROLE_PORTALADMIN") ||  u.getId() == dep.getOwner().getId())    {
				portalRepositoryRef.deleteDeployment(id);
				return Response.ok().build();
			}
		}

		ResponseBuilder builder = Response.status(Status.NOT_FOUND);
		builder.entity("User not found in portal registry or not logged in");
		throw new WebApplicationException(builder.build());
	}

	@GET
	@Path("/admin/deployments/{id}")
	@Produces("application/json")
	public Response getDeploymentById(@PathParam("id") int deploymentId) {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u != null) {
			logger.info("getDeploymentById for id: " + deploymentId);
			DeploymentDescriptor deployment = portalRepositoryRef.getDeploymentByID(deploymentId);

			if ((u.getRole().equals("ROLE_PORTALADMIN")) || (deployment.getOwner().getId() == u.getId())) {
				return Response.ok().entity(deployment).build();
			}

		}

		ResponseBuilder builder = Response.status(Status.NOT_FOUND);
		builder.entity("User not found in portal registry or not logged in");
		throw new WebApplicationException(builder.build());

	}

	@PUT
	@Path("/admin/deployments/{id}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateDeployment(@PathParam("id") int id, DeploymentDescriptor d, @QueryParam("action") String action) {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if ((u != null) ) { 

			if (action.equals("AUTH") && (u.getRole().equals("ROLE_PORTALADMIN")) ) // only admin can alter a deployment
				d.setStatus(DeploymentDescriptorStatus.QUEUED);
			else if (action.equals("UNINSTALL")  &&  (u.getRole().equals("ROLE_PORTALADMIN") ||  u.getId() == d.getOwner().getId())  )
				d.setStatus(DeploymentDescriptorStatus.UNINSTALLING);
			else if (action.equals("DENY") && (u.getRole().equals("ROLE_PORTALADMIN")) )
				d.setStatus(DeploymentDescriptorStatus.DENIED);

			PortalUser deploymentOwner = portalRepositoryRef.getUserByID(d.getOwner().getId() );
			d.setOwner(deploymentOwner); // reattach from the DB model

			ExperimentMetadata baseApplication = (ExperimentMetadata) portalRepositoryRef.getProductByID(d.getBaseApplication().getId());
			d.setBaseApplication(baseApplication); // reattach from the DB model

			for (DeployContainer dc : d.getDeployContainers()) {
				
				dc.getTargetResource().setOwner(deploymentOwner);// reattach from the DB model, in case missing from the request
			}

			DeploymentDescriptor deployment = portalRepositoryRef.updateDeploymentDescriptor(d);

			logger.info("updateDeployment for id: " + d.getId());

			return Response.ok().entity(deployment).build();

		}

		ResponseBuilder builder = Response.status(Status.FORBIDDEN);
		builder.entity("User not found in portal registry or not logged in as admin");
		throw new WebApplicationException(builder.build());

	}

	@POST
	@Path("/registerresource/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addANewAnauthSubscribedResource(SubscribedResource sm) {

		logger.info("Received SubscribedResource for client: " + sm.getUuid() + ", URLs:" + sm.getURL() + ", OwnerID:" + sm.getOwner().getId());

		PortalUser u = sm.getOwner();
		u = portalRepositoryRef.getUserByID(sm.getOwner().getId());

		if ((u != null) && (sm.getUuid() != null)) {

			SubscribedResource checkSM = portalRepositoryRef.getSubscribedResourceByUUID(sm.getUuid());

			if (checkSM == null) {
				sm.setOwner(u);
				sm.setActive(false);
				u.getSubscribedResources().add(sm);
				u = portalRepositoryRef.updateUserInfo(u.getId(), u);
				return Response.ok().entity(sm).build();
			} else {
				checkSM.setURL(sm.getURL());// update URL if changed
				// u = portalRepositoryRef.updateUserInfo( u.getId(), u);
				checkSM = portalRepositoryRef.updateSubscribedResourceInfo(checkSM.getId(), checkSM);
				return Response.ok().entity(checkSM).build();
			}

		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested SubscribedResource with rls=" + sm.getURL() + " cannot be registered under not found user");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/registerresource/deployments/target/uuid/{uuid}")
	@Consumes("application/json")
	@Produces("application/json")
	public Response getDeployContainerByTargetResourceUUID(@PathParam("uuid") String uuid) {

		SubscribedResource res = updateLastSeenResource(uuid);
		if (res!=null)
			logger.info("Received req for Deployent by client: " + res.getUuid() + ", URLs:" + res.getURL() + ", OwnerID:" + res.getOwner().getId());

		List<DeploymentDescriptor> deployments = portalRepositoryRef.getAllDeploymentDescriptors();
		for (DeploymentDescriptor deploymentDescriptor : deployments) 
			if ((deploymentDescriptor.getStatus()!= DeploymentDescriptorStatus.PENDING_ADMIN_AUTH )&&
					(deploymentDescriptor.getStatus()!= DeploymentDescriptorStatus.DENIED )&&
					(deploymentDescriptor.getStatus()!= DeploymentDescriptorStatus.UNINSTALLED )){
				List<DeployContainer> dcs = deploymentDescriptor.getDeployContainers();
				for (DeployContainer dc : dcs) {
					if ((dc.getTargetResource()!=null) && (dc.getTargetResource().getUuid().equals(uuid))) {						
						dc.setMasterDeploymentStatus( deploymentDescriptor.getStatus() );
						return Response.ok().entity(dc).build();
					}
				}
			}

		ResponseBuilder builder = Response.status(Status.NOT_FOUND);
		builder.entity("Deploy Container for TargetResource not found");
		throw new WebApplicationException(builder.build());

	}

	// /registerresource/deployments/target/uuid/"+ clientUUID+"/installedvxfuuid/"+installedVxFUUID+"/status/"+status"

	@PUT
	@Path("/registerresource/deployments/target/uuid/{clientUUID}/installedvxfuuid/{installedVxFUUID}/status/{status}/deployContainerid/{cid}")
	@Consumes("application/json")
	@Produces("application/json")
	public Response updateDeployContainerTargetResourceStatus(@PathParam("clientUUID") String clientUUID,
			@PathParam("installedVxFUUID") String installedVxFUUID, @PathParam("status") String status,@PathParam("cid") Long deployContainerId) {

		SubscribedResource res = updateLastSeenResource(clientUUID);
		if (res!=null)
			logger.info("Received ResourceStatus: " + status + ", for Deployent by client: " + res.getUuid() + ", URLs:" + res.getURL() 
					+ ", OwnerID:"+ res.getOwner().getId()
					+ ", installedVxFUUID:"+ installedVxFUUID);

		List<DeploymentDescriptor> deployments = portalRepositoryRef.getAllDeploymentDescriptors();
		for (DeploymentDescriptor deploymentDescriptor : deployments) {
			List<DeployContainer> dcs = deploymentDescriptor.getDeployContainers();
			for (DeployContainer dc : dcs) {
				if ((deployContainerId == dc.getId()) && (dc.getTargetResource()!=null) && dc.getTargetResource().getUuid().equals(clientUUID)) {
					List<DeployArtifact> artifacts = dc.getDeployArtifacts();
					for (DeployArtifact deployArtifact : artifacts)
						if (deployArtifact.getUuid().equals(installedVxFUUID)) {
							deployArtifact.setStatus(InstalledVxFStatus.valueOf(status));
							
						}

					deploymentDescriptor.setStatus( resolveStatus(deploymentDescriptor) ); // we must write here code to properly find the status!
					portalRepositoryRef.updateDeploymentDescriptor(deploymentDescriptor);
					
					return Response.status(Status.OK).build();
				}else{
					logger.info(" dc.getTargetResource()==null !! PROBLEM");
				}
			}

		}

		logger.info("Deploy Container for TargetResource not found");
		ResponseBuilder builder = Response.status(Status.NOT_FOUND);
		builder.entity("Deploy Container for TargetResource not found");
		throw new WebApplicationException(builder.build());

	}

	private DeploymentDescriptorStatus resolveStatus(DeploymentDescriptor deploymentDescriptor) {

		Boolean allInstalled = true;
		Boolean allUnInstalled = true;
		DeploymentDescriptorStatus status= deploymentDescriptor.getStatus();

		List<DeployContainer> containers = deploymentDescriptor.getDeployContainers();
		for (DeployContainer deployContainer : containers) {
			List<DeployArtifact> artifacts = deployContainer.getDeployArtifacts();
			for (DeployArtifact deployArtifact : artifacts) {
				if (deployArtifact.getStatus()!=InstalledVxFStatus.STARTED)
					allInstalled= false;
				if (deployArtifact.getStatus()!=InstalledVxFStatus.UNINSTALLED)
					allUnInstalled= false;

				if ((deployArtifact.getStatus()==InstalledVxFStatus.FAILED))
					return DeploymentDescriptorStatus.FAILED;
				if ((deployArtifact.getStatus()==InstalledVxFStatus.UNINSTALLING))
					return DeploymentDescriptorStatus.UNINSTALLING;
				if ((deployArtifact.getStatus()==InstalledVxFStatus.CONFIGURING)|| 
						(deployArtifact.getStatus()==InstalledVxFStatus.DOWNLOADING)|| 
						(deployArtifact.getStatus()==InstalledVxFStatus.DOWNLOADED)|| 
						(deployArtifact.getStatus()==InstalledVxFStatus.INSTALLING)|| 
						(deployArtifact.getStatus()==InstalledVxFStatus.INSTALLED)|| 
						(deployArtifact.getStatus()==InstalledVxFStatus.STARTING)
						)
					return DeploymentDescriptorStatus.INSTALLING;
			}
		}
		
		if (allInstalled) 
			return DeploymentDescriptorStatus.INSTALLED;
		else if (allUnInstalled) 
			return DeploymentDescriptorStatus.UNINSTALLED;
		else
			return status;
	}

	private SubscribedResource updateLastSeenResource(String clientUUID) {

		SubscribedResource res = portalRepositoryRef.getSubscribedResourceByUUID(clientUUID);
		if (res != null) {
			res.setLastUpdate(new Date()); // each time Portal Client Polls api, we update this Last seen of client
			PortalUser reattachedUser = portalRepositoryRef.getUserByID(res.getOwner().getId());
			res.setOwner(reattachedUser);
			portalRepositoryRef.updateSubscribedResourceInfo(res.getId(), res);
		}

		return res;
	}
	
	
	//admin MANO platforms
	
	
	// categories API
		@GET
		@Path("/manoplatforms/")
		@Produces("application/json")
		public Response getMANOplatforms() {
			return Response.ok().entity(portalRepositoryRef.getMANOplatforms()).build();
		}

		@GET
		@Path("/admin/manoplatforms/")
		@Produces("application/json")
		public Response getAdminMANOplatforms() {
			return Response.ok().entity(portalRepositoryRef.getMANOplatforms()).build();
		}

		@POST
		@Path("/admin/manoplatforms/")
		@Produces("application/json")
		@Consumes("application/json")
		public Response addMANOplatform(MANOplatform c) {
			MANOplatform u = portalRepositoryRef.addMANOplatform(c);

			if (u != null) {
				return Response.ok().entity(u).build();
			} else {
				ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
				builder.entity("Requested MANOplatform with name=" + c.getName() + " cannot be installed");
				throw new WebApplicationException(builder.build());
			}
		}

		@PUT
		@Path("/admin/manoplatforms/{mpid}")
		@Produces("application/json")
		@Consumes("application/json")
		public Response updateMANOplatform(@PathParam("mpid") int mpid, MANOplatform c) {
			MANOplatform previousMP = portalRepositoryRef.getMANOplatformByID(mpid);

			MANOplatform u = portalRepositoryRef.updateMANOplatformInfo(c);

			if (u != null) {
				return Response.ok().entity(u).build();
			} else {
				ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
				builder.entity("Requested MANOplatform with name=" + c.getName() + " cannot be updated");
				throw new WebApplicationException(builder.build());
			}

		}

		@DELETE
		@Path("/admin/manoplatforms/{mpid}")
		public Response deleteMANOplatform(@PathParam("mpid") int mpid) {
			MANOplatform category = portalRepositoryRef.getMANOplatformByID(mpid);
			
				portalRepositoryRef.deleteMANOplatform(mpid);
				return Response.ok().build();
			
		}

		@GET
		@Path("/manoplatforms/{mpid}")
		@Produces("application/json")
		public Response getMANOplatformById(@PathParam("mpid") int mpid) {
			MANOplatform sm = portalRepositoryRef.getMANOplatformByID(mpid);

			if (sm != null) {
				return Response.ok().entity(sm).build();
			} else {
				ResponseBuilder builder = Response.status(Status.NOT_FOUND);
				builder.entity("MANOplatform " + mpid + " not found in portal registry");
				throw new WebApplicationException(builder.build());
			}
		}

		@GET
		@Path("/admin/manoplatforms/{mpid}")
		@Produces("application/json")
		public Response getAdminMANOplatformById(@PathParam("mpid") int mpid) {
			return getCategoryById(mpid);
		}

}
