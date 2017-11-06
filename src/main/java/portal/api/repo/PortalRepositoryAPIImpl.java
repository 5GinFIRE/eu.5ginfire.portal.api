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

import java.io.ByteArrayOutputStream;
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
import java.util.UUID;

import javax.activation.DataHandler;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
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
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import portal.api.model.Category;
import portal.api.model.ConstituentVxF;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.DeploymentDescriptorStatus;
import portal.api.model.DeploymentDescriptorVxFPlacement;
import portal.api.model.ExperimentMetadata;
import portal.api.model.ExperimentOnBoardDescriptor;
import portal.api.model.IPortalRepositoryAPI;
import portal.api.model.Infrastructure;
import portal.api.model.MANOplatform;
import portal.api.model.MANOprovider;
import portal.api.model.OnBoardingStatus;
import portal.api.model.PortalProperty;
import portal.api.model.PortalUser;
import portal.api.model.Product;
import portal.api.model.SubscribedResource;
import portal.api.model.UserRoleType;
import portal.api.model.UserSession;
import portal.api.model.VxFMetadata;
import portal.api.model.VxFOnBoardedDescriptor;
import portal.api.osm.client.OSMClient;
import portal.api.util.EmailUtil;
import pt.it.av.atnog.extractors.NSExtractor;
import pt.it.av.atnog.extractors.VNFExtractor;
import pt.it.av.atnog.requirements.NSRequirements;
import pt.it.av.atnog.requirements.VNFRequirements;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.Nsd;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.descriptor.ConstituentVnfd;
import urn.ietf.params.xml.ns.yang.nfvo.vnfd.rev150910.vnfd.catalog.Vnfd;

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

	private static final String METADATADIR = System.getProperty("user.home") + File.separator + ".portal"
			+ File.separator + "metadata" + File.separator;

	private PortalRepository portalRepositoryRef;

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
				logger.info(" securityContext.getUserPrincipal().toString() >"
						+ securityContext.getUserPrincipal().getName() + "<");

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
		// logger.info("Received POST for usergetPassword: " +
		// user.getPassword());
		// logger.info("Received POST for usergetOrganization: " +
		// user.getOrganization());

		if ((user.getUsername() == null)
				|| (user.getUsername().equals("") || (user.getEmail() == null) || (user.getEmail().equals("")))) {
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
		user.setOrganization(getAttachmentStringValue("userorganization", ats) + "^^"
				+ getAttachmentStringValue("randomregid", ats));
		user.setEmail(getAttachmentStringValue("useremail", ats));
		user.setActive(false);// in any case the user should be not active
		user.addRole(UserRoleType.EXPERIMENTER); // otherwise in post he can choose
		user.addRole(UserRoleType.VXF_DEVELOPER); // otherwise in post he can choose
		// PORTALADMIN, and the
		// immediately register :-)

		String msg = getAttachmentStringValue("emailmessage", ats);
		logger.info("Received register for usergetUsername: " + user.getUsername());

		Response r = addUser(user);

		if (r.getStatusInfo().getStatusCode() == Status.OK.getStatusCode()) {
			logger.info("Email message: " + msg);
			String subj = "[5GinFIREPortal] " + PortalRepository.getPropertyByName("activationEmailSubject").getValue();
			EmailUtil.SendRegistrationActivationEmail(user.getEmail(), msg, subj);
		}

		return r;
	}

	@POST
	@Path("/register/verify")
	@Produces("application/json")
	@Consumes("multipart/form-data")
	public Response addNewRegisterUserVerify(List<Attachment> ats) {

		String username = getAttachmentStringValue("username", ats);
		String rid = getAttachmentStringValue("rid", ats);

		PortalUser u = portalRepositoryRef.getUserByUsername(username);
		if (u.getOrganization().contains("^^")) {
			u.setOrganization(u.getOrganization().substring(0, u.getOrganization().indexOf("^^")));
			u.setActive(true);
		}
		u = portalRepositoryRef.updateUserInfo(u.getId(), u);
		// getAttachmentStringValue("username", ats)

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested user with username=" + u.getUsername() + " cannot be updated");
			throw new WebApplicationException(builder.build());
		}
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

	private Product addNewProductData(Product prod, Attachment image, Attachment submittedFile,
			List<Attachment> screenshots) {

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
		// Category category = portalRepositoryRef.getCategoryByID(
		// Integer.valueOf(catid) );
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
					prod.setIconsrc(endpointUrl.toString().replace("http:", "") + "repo/images/" + uuid + "/"
							+ imageFileNamePosted);
				}
			}

			if (submittedFile != null) {
				String aFileNamePosted = getFileName(submittedFile.getHeaders());
				logger.info("vxfFile = " + aFileNamePosted);
				if (!aFileNamePosted.equals("")) {
					String vxffilepath = saveFile(submittedFile, tempDir + aFileNamePosted);
					logger.info("vxffilepath saved to = " + vxffilepath);
					prod.setPackageLocation(endpointUrl.toString().replace("http:", "") + "repo/packages/" + uuid + "/"
							+ aFileNamePosted);
					File f = new File(vxffilepath);
					if (prod instanceof VxFMetadata) {
						VNFExtractor vnfExtract = new VNFExtractor(f);
						Vnfd vnfd = vnfExtract.extractVnfdDescriptor();
						if (vnfd != null) {
							prod.setName(vnfd.getId());
							prod.setVersion(vnfd.getVersion());
							prod.setVendor(vnfd.getVendor());
							prod.setShortDescription(vnfd.getName());
							prod.setLongDescription(vnfd.getDescription());
							VNFRequirements vr = new VNFRequirements(vnfd);
							prod.setDescriptorHTML(vr.toHTML());
							prod.setDescriptor(vnfExtract.getDescriptorYAMLfile());

							if (vnfExtract.getIconfilePath() != null) {

								String imageFileNamePosted = vnfd.getLogo();
								logger.info("image = " + imageFileNamePosted);
								if (!imageFileNamePosted.equals("")) {
									String imgfile = saveFile(vnfExtract.getIconfilePath(),
											tempDir + imageFileNamePosted);
									logger.info("imgfile saved to = " + imgfile);
									prod.setIconsrc(endpointUrl.toString().replace("http:", "") + "repo/images/" + uuid
											+ "/" + imageFileNamePosted);
								}
							}

						} else {
							return null;
						}
					} else if (prod instanceof ExperimentMetadata) {
						NSExtractor nsExtract = new NSExtractor(f);
						Nsd ns = nsExtract.extractNsDescriptor();
						if (ns != null) {
							prod.setName(ns.getId());
							prod.setVersion(ns.getVersion());
							prod.setVendor(ns.getVendor());
							prod.setShortDescription(ns.getName());
							prod.setLongDescription(ns.getDescription());
							NSRequirements vr = new NSRequirements(ns);
							prod.setDescriptorHTML(vr.toHTML());
							prod.setDescriptor(nsExtract.getDescriptorYAMLfile());

							for (ConstituentVnfd v : ns.getConstituentVnfd()) {
								ConstituentVxF cvxf = new ConstituentVxF();
								cvxf.setMembervnfIndex(v.getMemberVnfIndex().intValue()); // ok we will survive with
																							// this
								cvxf.setVnfdidRef(v.getVnfdIdRef());

								VxFMetadata vxf = (VxFMetadata) portalRepositoryRef.getProductByName(v.getVnfdIdRef());

								cvxf.setVxfref(vxf);

								((ExperimentMetadata) prod).getConstituentVxF().add(cvxf);
							}
							if (nsExtract.getIconfilePath() != null) {

								String imageFileNamePosted = ns.getLogo();
								logger.info("image = " + imageFileNamePosted);
								if (!imageFileNamePosted.equals("")) {
									String imgfile = saveFile(nsExtract.getIconfilePath(),
											tempDir + imageFileNamePosted);
									logger.info("imgfile saved to = " + imgfile);
									prod.setIconsrc(endpointUrl.toString().replace("http:", "") + "repo/images/" + uuid
											+ "/" + imageFileNamePosted);
								}
							}
						} else {
							return null;
						}
					}
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
					shotfilepath = endpointUrl.toString().replace("http:", "") + "repo/images/" + uuid + "/"
							+ shotFileNamePosted;
					screenshotsFilenames += shotfilepath + ",";
					i++;
				}
			}
			if (screenshotsFilenames.length() > 0)
				screenshotsFilenames = screenshotsFilenames.substring(0, screenshotsFilenames.length() - 1);

			prod.setScreenshots(screenshotsFilenames);

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		// we must replace given product categories with the ones from our DB
		for (Category c : prod.getCategories()) {
			Category catToUpdate = portalRepositoryRef.getCategoryByID(c.getId());
			// logger.info("BEFORE PROD SAVE, category "+catToUpdate.getName()+"
			// contains Products: "+ catToUpdate.getProducts().size() );
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

		List<VxFMetadata> vxfs = portalRepositoryRef.getVxFs(categoryid, true);
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

			if (u.getRoles().contains(UserRoleType.PORTALADMIN)) {
				vxfs = portalRepositoryRef.getVxFs(categoryid, false);
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

				getAttachmentByName("prodIcon", ats), getAttachmentByName("prodFile", ats),
				getListOfAttachmentsByName("screenshots", ats));

		if (vxf != null) {
			return Response.ok().entity(vxf).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested entity cannot be installed");
			throw new WebApplicationException(builder.build());
		}

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

		// VxFMetadata sm = (VxFMetadata)
		// portalRepositoryRef.getProductByID(bid);

		for (VxFOnBoardedDescriptor vxFOnBoardedDescriptor : vxf.getVxfOnBoardedDescriptors()) {
			vxFOnBoardedDescriptor.setVxf(vxf);
		}

		vxf = (VxFMetadata) updateProductMetadata(vxf, getAttachmentByName("prodIcon", ats),
				getAttachmentByName("prodFile", ats), getListOfAttachmentsByName("screenshots", ats));

		return Response.ok().entity(vxf).build();

	}

	// VxFs related API

	private Product updateProductMetadata(Product prod, Attachment image, Attachment prodFile,
			List<Attachment> screenshots) {

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

		// first remove all references of the product from the previous
		// categories
		Product prodPreUpdate = (Product) portalRepositoryRef.getProductByID(prod.getId());
		for (Category c : prodPreUpdate.getCategories()) {
			// logger.info("Will remove product "+prodPreUpdate.getName()+ ",
			// from Previous Category "+c.getName() );
			c.removeProduct(prodPreUpdate);
			portalRepositoryRef.updateCategoryInfo(c);
		}

		// we must replace API given product categories with the ones from our
		// DB
		for (Category c : prod.getCategories()) {
			Category catToUpdate = portalRepositoryRef.getCategoryByID(c.getId());
			// logger.info("BEFORE PROD SAVE, category "+catToUpdate.getName()+"
			// contains Products: "+ catToUpdate.getProducts().size() );
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
					prod.setIconsrc(endpointUrl.toString().replace("http:", "") + "repo/images/" + prod.getUuid() + "/"
							+ imageFileNamePosted);
				}
			}

			if (prodFile != null) {
				String vxfFileNamePosted = getFileName(prodFile.getHeaders());
				logger.info("vxfFile = " + vxfFileNamePosted);
				if (!vxfFileNamePosted.equals("unknown")) {
					String vxffilepath = saveFile(prodFile, tempDir + vxfFileNamePosted);
					logger.info("vxffilepath saved to = " + vxffilepath);
					prod.setPackageLocation(endpointUrl.toString().replace("http:", "") + "repo/packages/"
							+ prod.getUuid() + "/" + vxfFileNamePosted);

					File f = new File(vxffilepath);
					if (prod instanceof VxFMetadata) {
						VNFExtractor vnfExtract = new VNFExtractor(f);
						Vnfd vnfd = vnfExtract.extractVnfdDescriptor();
						if (vnfd != null) {
							prod.setName(vnfd.getId());
							prod.setVersion(vnfd.getVersion());
							prod.setVendor(vnfd.getVendor());
							prod.setShortDescription(vnfd.getName());
							prod.setLongDescription(vnfd.getDescription());
							VNFRequirements vr = new VNFRequirements(vnfd);
							prod.setDescriptorHTML(vr.toHTML());
							prod.setDescriptor(vnfExtract.getDescriptorYAMLfile());

							if (vnfExtract.getIconfilePath() != null) {

								String imageFileNamePosted = vnfd.getLogo();
								logger.info("image = " + imageFileNamePosted);
								if (!imageFileNamePosted.equals("")) {
									String imgfile = saveFile(vnfExtract.getIconfilePath(),
											tempDir + imageFileNamePosted);
									logger.info("imgfile saved to = " + imgfile);
									prod.setIconsrc(endpointUrl.toString().replace("http:", "") + "repo/images/"
											+ prod.getUuid() + "/" + imageFileNamePosted);
								}
							}
						}
					} else if (prod instanceof ExperimentMetadata) {
						NSExtractor nsExtract = new NSExtractor(f);
						Nsd ns = nsExtract.extractNsDescriptor();
						if (ns != null) {
							prod.setName(ns.getId());
							prod.setVersion(ns.getVersion());
							prod.setVendor(ns.getVendor());
							prod.setShortDescription(ns.getName());
							prod.setLongDescription(ns.getDescription());
							NSRequirements vr = new NSRequirements(ns);
							prod.setDescriptorHTML(vr.toHTML());
							prod.setDescriptor(nsExtract.getDescriptorYAMLfile());
							((ExperimentMetadata) prod).getConstituentVxF().clear();
							for (ConstituentVnfd v : ns.getConstituentVnfd()) {
								ConstituentVxF cvxf = new ConstituentVxF();
								cvxf.setMembervnfIndex(v.getMemberVnfIndex().intValue()); // ok we will survive with
																							// this
								cvxf.setVnfdidRef(v.getVnfdIdRef());

								VxFMetadata vxf = (VxFMetadata) portalRepositoryRef.getProductByName(v.getVnfdIdRef());

								cvxf.setVxfref(vxf);

								((ExperimentMetadata) prod).getConstituentVxF().add(cvxf);
							}

							if (nsExtract.getIconfilePath() != null) {

								String imageFileNamePosted = ns.getLogo();
								logger.info("image = " + imageFileNamePosted);
								if (!imageFileNamePosted.equals("")) {
									String imgfile = saveFile(nsExtract.getIconfilePath(),
											tempDir + imageFileNamePosted);
									logger.info("imgfile saved to = " + imgfile);
									prod.setIconsrc(endpointUrl.toString().replace("http:", "") + "repo/images/"
											+ prod.getUuid() + "/" + imageFileNamePosted);
								}
							}
						}
					}

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
					shotfilepath = endpointUrl.toString().replace("http:", "") + "repo/images/" + prod.getUuid() + "/"
							+ shotFileNamePosted;
					screenshotsFilenames += shotfilepath + ",";
					i++;
				}
			}
			if (screenshotsFilenames.length() > 0)
				screenshotsFilenames = screenshotsFilenames.substring(0, screenshotsFilenames.length() - 1);

			prod.setScreenshots(screenshotsFilenames);

		} catch (IOException e) {

			e.printStackTrace();
			return null;
		}

		// save product
		prod = portalRepositoryRef.updateProductInfo(prod);

		// now fix category product references
		for (Category catToUpdate : prod.getCategories()) {
			Product p = portalRepositoryRef.getProductByID(prod.getId());
			Category c = portalRepositoryRef.getCategoryByID( catToUpdate.getId() );
			c.addProduct(p);
			portalRepositoryRef.updateCategoryInfo(c);
		}

		if (vxfOwner.getProductById(prod.getId()) == null)
			vxfOwner.addProduct(prod);
		portalRepositoryRef.updateUserInfo(prod.getOwner().getId(), vxfOwner);
		return prod;
	}

	@GET
	@Path("/images/{uuid}/{imgfile}")
	@Produces({ "image/jpeg,image/png" })
	public Response getEntityImage(@PathParam("uuid") String uuid, @PathParam("imgfile") String imgfile) {
		logger.info("getEntityImage of uuid: " + uuid);
		String imgAbsfile = METADATADIR + uuid + File.separator + imgfile;
		logger.info("Image RESOURCE FILE: " + imgAbsfile);
		File file = new File(imgAbsfile);

		// ResponseBuilder response = Response.ok((Object) file );
		// logger.info( "attachment; filename=" + file.getName() );
		// response.header("Content-Disposition", "attachment; filename=" +
		// file.getName());
		// return response.build();
		// String mediaType = SomeContentTypeMapHere(file)
		return Response.ok(file).build();
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

		if ((uuid.equals("77777777-668b-4c75-99a9-39b24ed3d8be"))
				|| (uuid.equals("22cab8b8-668b-4c75-99a9-39b24ed3d8be"))) {
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

			vxf.setPackageLocation(endpointUrl.toString().replace("http:", "")
					+ "repo/packages/77777777-668b-4c75-99a9-39b24ed3d8be/examplevxf.tar.gz");
			// }else if (uuid.equals("12cab8b8-668b-4c75-99a9-39b24ed3d8be")) {
			// vxf = new VxFMetadata(uuid, "AN example service");
			// vxf.setShortDescription("An example local service");
			// vxf.setVersion("1.0.0rc1");
			// vxf.setIconsrc("");
			// vxf.setLongDescription("");
			// //URI endpointUrl = uri.getBaseUri();
			//
			// vxf.setPackageLocation( endpointUrl
			// +"repo/packages/12cab8b8-668b-4c75-99a9-39b24ed3d8be/examplevxf.tar.gz");
		} else if (uuid.equals("22cab8b8-668b-4c75-99a9-39b24ed3d8be")) {
			vxf = new VxFMetadata();
			vxf.setUuid(uuid);
			vxf.setName("IntegrTestLocal example ErrInstall service");
			vxf.setShortDescription("An example ErrInstall local service");
			vxf.setVersion("1.0.0");
			vxf.setIconsrc("");
			vxf.setLongDescription("");
			// URI endpointUrl = uri.getBaseUri();

			vxf.setPackageLocation(endpointUrl.toString().replace("http:", "")
					+ "repo/packages/22cab8b8-668b-4c75-99a9-39b24ed3d8be/examplevxfErrInstall.tar.gz");
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

	// Sessions related API

	// @OPTIONS
	// @Path("/sessions/")
	// @Produces("application/json")
	// @Consumes("application/json")
	// @LocalPreflight
	// public Response addUserSessionOption(){
	//
	//
	// logger.info("Received OPTIONS addUserSessionOption ");
	// String origin = headers.getRequestHeader("Origin").get(0);
	// if (origin != null) {
	// return Response.ok()
	// .header(CorsHeaderConstants.HEADER_AC_ALLOW_METHODS, "GET POST DELETE PUT
	// HEAD OPTIONS")
	// .header(CorsHeaderConstants.HEADER_AC_ALLOW_CREDENTIALS, "true")
	// .header(CorsHeaderConstants.HEADER_AC_ALLOW_HEADERS, "Origin,
	// X-Requested-With, Content-Type, Accept")
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
		// logger.info("DANGER, REMOVE Received POST addUserSession password: "
		// + userSession.getPassword());

		if (securityContext != null) {
			if (securityContext.getUserPrincipal() != null)
				logger.info(" securityContext.getUserPrincipal().toString() >"
						+ securityContext.getUserPrincipal().toString() + "<");

		}

		Subject currentUser = SecurityUtils.getSubject();
		if (currentUser != null) {
			AuthenticationToken token = new UsernamePasswordToken(userSession.getUsername(), userSession.getPassword());
			try {
				currentUser.login(token);
				PortalUser portalUser = portalRepositoryRef.getUserByUsername(userSession.getUsername());

				if (!portalUser.getActive()) {
					logger.info("User [" + currentUser.getPrincipal() + "] is not Active");
					return Response.status(Status.UNAUTHORIZED).build();
				}

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
				logger.info(" securityContext.getUserPrincipal().toString() >"
						+ securityContext.getUserPrincipal().toString() + "<");

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
				logger.info(" securityContext.getUserPrincipal().toString() >"
						+ securityContext.getUserPrincipal().toString() + "<");

		}

		Subject currentUser = SecurityUtils.getSubject();
		if ((currentUser != null) && (currentUser.getPrincipal() != null)) {

			// logger.info(" currentUser = " + currentUser.toString() );
			// logger.info( "User [" + currentUser.getPrincipal() + "] logged in
			// successfully." );
			// logger.info(" currentUser employee = " +
			// currentUser.hasRole("employee") );
			// logger.info(" currentUser boss = " + currentUser.hasRole("boss")
			// );

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

			if (u.getRoles().contains(UserRoleType.PORTALADMIN)) {
				return Response.ok().entity(portalRepositoryRef.getSubscribedResourcesAsCollection()).build(); // return
																												// all
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

			if ((u.getRoles().contains(UserRoleType.PORTALADMIN)) || (sm.getOwner().getId() == u.getId()))
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
			builder.entity("Requested SubscribedResource with rls=" + sm.getURL()
					+ " cannot be registered under not found user");
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

			if ((u.getRoles().contains(UserRoleType.PORTALADMIN)) || (sm.getOwner().getId() == u.getId())) {

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

			if ((u.getRoles().contains(UserRoleType.PORTALADMIN)) || (sm.getOwner().getId() == u.getId())) {
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

			if (u.getRoles().contains(UserRoleType.PORTALADMIN)) {
				apps = portalRepositoryRef.getExperiments(categoryid, false);
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
		List<ExperimentMetadata> vxfs = portalRepositoryRef.getExperiments(categoryid, true);
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
	public Response addExperimentMetadata(List<Attachment> ats) {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if (u == null) {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("User not found in portal registry or not logged in ");
			throw new WebApplicationException(builder.build());
		}

		ExperimentMetadata experiment = new ExperimentMetadata();

		try {
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser(getAttachmentStringValue("exprm", ats));
			experiment = parser.readValueAs(ExperimentMetadata.class);

			logger.info("Received @POST for experiment : " + experiment.getName());

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ExperimentMetadata sm = new ExperimentMetadata();
		experiment = (ExperimentMetadata) addNewProductData(experiment, getAttachmentByName("prodIcon", ats),
				getAttachmentByName("prodFile", ats), getListOfAttachmentsByName("screenshots", ats));
		if (experiment != null) {
			return Response.ok().entity(experiment).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested entity cannot be installed");
			throw new WebApplicationException(builder.build());
		}

	}

	@PUT
	@Path("/admin/experiments/{aid}")
	@Consumes("multipart/form-data")
	public Response updateAppMetadata(@PathParam("aid") int aid, List<Attachment> ats) {

		ExperimentMetadata appmeta = new ExperimentMetadata();

		try {
			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser(getAttachmentStringValue("exprm", ats));
			appmeta = parser.readValueAs(ExperimentMetadata.class);

			logger.info("Received @POST for experiment : " + appmeta.getName());
			// logger.info("Received @POST for app.containers : " +
			// appmeta.getContainers().size());

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ExperimentMetadata appmeta = (ExperimentMetadata)
		// portalRepositoryRef.getProductByID(aid);

		for (ExperimentOnBoardDescriptor veDescriptor : appmeta.getExperimentOnBoardDescriptors()) {
			veDescriptor.setExperiment(appmeta);
		}

		appmeta = (ExperimentMetadata) updateProductMetadata(appmeta, getAttachmentByName("prodIcon", ats),
				getAttachmentByName("prodFile", ats), getListOfAttachmentsByName("screenshots", ats));

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

	// Attachment utils ///////////////////////
	private String saveFile(ByteArrayOutputStream att, String filePath) throws IOException {

		File f = new File(filePath);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(f);
			att.writeTo(fos);
			fos.close();
			return f.getAbsolutePath();
		} catch (IOException ioe) {
			// Handle exception here
			ioe.printStackTrace();
		} finally {
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

	@GET
	@Path("/admin/properties/")
	@Produces("application/json")
	public Response getProperties() {
		List<PortalProperty> props = portalRepositoryRef.getProperties();
		for (PortalProperty portalProperty : props) {
			if (portalProperty.getName().equals("mailpassword")) {
				portalProperty.setValue("***");
			}
		}
		return Response.ok().entity(props).build();
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

		if (sm.getName().equals("mailpassword")) {
			sm.setValue("");
		}
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

			if (u.getRoles().contains(UserRoleType.PORTALADMIN)) {
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
			deployment.setStatus(DeploymentDescriptorStatus.UNDER_REVIEW);

			u = portalRepositoryRef.getUserByID(u.getId());
			deployment.setOwner(u); // reattach from the DB model
			u.getDeployments().add(deployment);

			ExperimentMetadata baseApplication = (ExperimentMetadata) portalRepositoryRef
					.getProductByID(deployment.getExperiment().getId());
			deployment.setExperiment(baseApplication); // reattach from the
														// DB model

			u = portalRepositoryRef.updateUserInfo(u.getId(), u);

			String adminemail = PortalRepository.getPropertyByName("adminEmail").getValue();
			if ((adminemail != null) && (!adminemail.equals(""))) {
				String subj = "[5GinFIREPortal] New Deployment Request";
				EmailUtil.SendRegistrationActivationEmail(adminemail,
						"5GinFIREPortal New Deployment Request by user : " + u.getUsername() + ", " + u.getEmail()+ "\n<br/> Status: " + deployment.getStatus().name()+ "\n<br/> Description: " + deployment.getDescription()   ,
						subj);
			}

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
			if (u.getRoles().contains(UserRoleType.PORTALADMIN) || u.getId() == dep.getOwner().getId()) {
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

			if ((u.getRoles().contains(UserRoleType.PORTALADMIN)) || (deployment.getOwner().getId() == u.getId())) {
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
	public Response updateDeployment(@PathParam("id") int id, DeploymentDescriptor d) {

		PortalUser u = portalRepositoryRef.getUserBySessionID(ws.getHttpServletRequest().getSession().getId());

		if ((u != null)) {

			if ((u.getRoles().contains(UserRoleType.PORTALADMIN))) // only admin can alter a deployment
			{
				PortalUser deploymentOwner = portalRepositoryRef.getUserByID(d.getOwner().getId());
				d.setOwner(deploymentOwner); // reattach from the DB model

				ExperimentMetadata baseApplication = (ExperimentMetadata) portalRepositoryRef
						.getProductByID(d.getExperiment().getId());
				d.setExperiment(baseApplication); // reattach from the DB model

				DeploymentDescriptor deployment = portalRepositoryRef.updateDeploymentDescriptor(d);

				logger.info("updateDeployment for id: " + d.getId());

				String adminemail = PortalRepository.getPropertyByName("adminEmail").getValue();
				if ((adminemail != null) && (!adminemail.equals(""))) {
					String subj = "[5GinFIREPortal] Deployment Request";
					EmailUtil.SendRegistrationActivationEmail(deploymentOwner.getEmail(),
							"5GinFIREPortal Deployment Request for experiment: " + d.getName() + "\n<br/>Status: " + d.getStatus().name()+ "\n<br/>Feedback: " + d.getFeedback() + "\n\n<br/><br/> The 5GinFIRE team" ,
							subj);
				}

				return Response.ok().entity(deployment).build();

			}

		}

		ResponseBuilder builder = Response.status(Status.NOT_ACCEPTABLE);
		builder.entity("User not found in portal registry or not logged in as admin");
		throw new WebApplicationException(builder.build());

	}

	@POST
	@Path("/registerresource/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addANewAnauthSubscribedResource(SubscribedResource sm) {

		logger.info("Received SubscribedResource for client: " + sm.getUuid() + ", URLs:" + sm.getURL() + ", OwnerID:"
				+ sm.getOwner().getId());

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
			builder.entity("Requested SubscribedResource with rls=" + sm.getURL()
					+ " cannot be registered under not found user");
			throw new WebApplicationException(builder.build());
		}
	}

	/********************************************************************************
	 * 
	 * admin MANO platforms
	 * 
	 ********************************************************************************/

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
		return getMANOplatformById(mpid);
	}

	/********************************************************************************
	 * 
	 * admin MANO providers
	 * 
	 ********************************************************************************/

	/**
	 * @return
	 */
	@GET
	@Path("/admin/manoproviders/")
	@Produces("application/json")
	public Response getAdminMANOproviders() {
		return Response.ok().entity(portalRepositoryRef.getMANOproviders()).build();
	}

	@POST
	@Path("/admin/manoproviders/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addMANOprovider(MANOprovider c) {
		MANOprovider u = portalRepositoryRef.addMANOprovider(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested MANOprovider with name=" + c.getName() + " cannot be installed");
			throw new WebApplicationException(builder.build());
		}
	}

	@PUT
	@Path("/admin/manoproviders/{mpid}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateMANOprovider(@PathParam("mpid") int mpid, MANOprovider c) {

		MANOprovider u = portalRepositoryRef.updateMANOproviderInfo(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested MANOprovider with name=" + c.getName() + " cannot be updated");
			throw new WebApplicationException(builder.build());
		}

	}

	@DELETE
	@Path("/admin/manoproviders/{mpid}")
	public Response deleteMANOprovider(@PathParam("mpid") int mpid) {

		portalRepositoryRef.deleteMANOprovider(mpid);
		return Response.ok().build();

	}

	@GET
	@Path("/admin/manoproviders/{mpid}")
	@Produces("application/json")
	public Response getAdminMANOproviderById(@PathParam("mpid") int mpid) {
		MANOprovider sm = portalRepositoryRef.getMANOproviderByID(mpid);

		if (sm != null) {
			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("MANOprovider " + mpid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/manoprovider/{mpid}/vnfds/{vxfid}")
	@Produces("application/json")
	public Response getOSMVNFMetadataByKOSMMANOID(@PathParam("mpid") int manoprovid, @PathParam("vxfid") String vxfid) {
		logger.info("getOSMVNFMetadataByID  vxfid=" + vxfid);

		MANOprovider sm = portalRepositoryRef.getMANOproviderByID(manoprovid);

		Vnfd vnfd = OSMClient.getInstance(sm).getVNFDbyID(vxfid);

		if (vnfd != null) {
			return Response.ok().entity(vnfd).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("vxf with id=" + vxfid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/manoprovider/{mpid}/vnfds/")
	@Produces("application/json")
	public Response getOSMVNFMetadata(@PathParam("mpid") int manoprovid) {

		MANOprovider sm = portalRepositoryRef.getMANOproviderByID(manoprovid);

		List<Vnfd> vnfd = OSMClient.getInstance(sm).getVNFDs();

		if (vnfd != null) {
			return Response.ok().entity(vnfd).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("manoprovid with id=" + manoprovid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/manoprovider/{mpid}/nsds/{nsdid}")
	@Produces("application/json")
	public Response getOSM_NSD_MetadataByKOSMMANOID(@PathParam("mpid") int manoprovid,
			@PathParam("vxfid") String nsdid) {
		logger.info("getOSMVNFMetadataByID  nsdid=" + nsdid);

		MANOprovider sm = portalRepositoryRef.getMANOproviderByID(manoprovid);

		Nsd nsd = OSMClient.getInstance(sm).getNSDbyID(nsdid);

		if (nsd != null) {
			return Response.ok().entity(nsd).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("nsdid with id=" + nsdid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/manoprovider/{mpid}/nsds/")
	@Produces("application/json")
	public Response getOSM_NSD_Metadata(@PathParam("mpid") int manoprovid) {

		MANOprovider sm = portalRepositoryRef.getMANOproviderByID(manoprovid);

		List<Nsd> nsd = OSMClient.getInstance(sm).getNSDs();

		if (nsd != null) {
			return Response.ok().entity(nsd).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("manoprovid with id=" + manoprovid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

	/********************************************************************************
	 * 
	 * admin VxFOnBoardedDescriptors
	 * 
	 ********************************************************************************/

	@GET
	@Path("/admin/vxfobds/")
	@Produces("application/json")
	public Response getVxFOnBoardedDescriptors() {
		return Response.ok().entity(portalRepositoryRef.getVxFOnBoardedDescriptors()).build();
	}

	@POST
	@Path("/admin/vxfobds/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addVxFOnBoardedDescriptor(VxFOnBoardedDescriptor c) {
		VxFOnBoardedDescriptor u = portalRepositoryRef.addVxFOnBoardedDescriptor(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested VxFOnBoardedDescriptor with name=" + c.getId() + " cannot be installed");
			throw new WebApplicationException(builder.build());
		}
	}

	@PUT
	@Path("/admin/vxfobds/{mpid}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateVxFOnBoardedDescriptor(@PathParam("mpid") int mpid, VxFOnBoardedDescriptor c) {

		VxFOnBoardedDescriptor u = portalRepositoryRef.updateVxFOnBoardedDescriptor(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested VxFOnBoardedDescriptor with name=" + c.getId() + " cannot be updated");
			throw new WebApplicationException(builder.build());
		}

	}

	@DELETE
	@Path("/admin/vxfobds/{mpid}")
	public Response deleteVxFOnBoardedDescriptor(@PathParam("mpid") int mpid) {

		portalRepositoryRef.deleteVxFOnBoardedDescriptor(mpid);
		return Response.ok().build();

	}

	@GET
	@Path("/admin/vxfobds/{mpid}")
	@Produces("application/json")
	public Response getVxFOnBoardedDescriptorById(@PathParam("mpid") int mpid) {
		VxFOnBoardedDescriptor sm = portalRepositoryRef.getVxFOnBoardedDescriptorByID(mpid);

		if (sm != null) {
			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("VxFOnBoardedDescriptor " + mpid + " not found in portal registry");
			return builder.build();
			// throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/admin/vxfobds/{mpid}/status")
	@Produces("application/json")
	public Response getVxFOnBoardedDescriptorByIdCheckMANOProvider(@PathParam("mpid") int mpid) {

		VxFOnBoardedDescriptor sm = portalRepositoryRef.getVxFOnBoardedDescriptorByID(mpid);

		if (sm == null) {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("VxFOnBoardedDescriptor " + mpid + " not found in portal registry");
			return builder.build();
		}

		if (sm.getOnBoardingStatus().equals(OnBoardingStatus.ONBOARDING)) {

			Vnfd vnfd = null;
			List<Vnfd> vnfds = OSMClient.getInstance(sm.getObMANOprovider()).getVNFDs();
			for (Vnfd v : vnfds) {
				if (v.getId().equalsIgnoreCase(sm.getVxfMANOProviderID())
						|| v.getName().equalsIgnoreCase(sm.getVxfMANOProviderID())) {
					vnfd = v;
					break;
				}
			}

			if (vnfd == null) {
				sm.setOnBoardingStatus(OnBoardingStatus.UNKNOWN);
			} else {
				sm.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			}

			sm = portalRepositoryRef.updateVxFOnBoardedDescriptor(sm);

		}

		return Response.ok().entity(sm).build();

	}

	@PUT
	@Path("/admin/vxfobds/{mpid}/onboard")
	@Produces("application/json")
	@Consumes("application/json")
	public Response onBoardDescriptor(@PathParam("mpid") int mpid, final VxFOnBoardedDescriptor c) {

		c.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		c.setDeployId(UUID.randomUUID().toString());
		VxFMetadata vxf = c.getVxf();
		if (vxf == null) {
			vxf = (VxFMetadata) portalRepositoryRef.getProductByID(c.getVxfid());
		}

		/**
		 * The following is not OK. When we submit to OSMClient the createOnBoardPackage
		 * we just get a response something like response = {"output":
		 * {"transaction-id": "b2718ef9-4391-4a9e-97ad-826593d5d332"}} which does not
		 * provide any information. The OSM RIFTIO API says that we could get
		 * information about onboarding (create or update) jobs see
		 * https://open.riftio.com/documentation/riftware/4.4/a/api/orchestration/pkt-mgmt/rw-pkg-mgmt-download-jobs.htm
		 * with /api/operational/download-jobs, but this does not return pending jobs.
		 * So the only solution is to ask again OSM if something is installed or not, so
		 * for now the client (the portal ) must check via the
		 * getVxFOnBoardedDescriptorByIdCheckMANOProvider giving the VNF ID in OSM. OSM
		 * uses the ID of the yaml description Thus we asume that the vxf name can be
		 * equal to the VNF ID in the portal, and we use it for now as the OSM ID. Later
		 * in future, either OSM API provide more usefull response or we extract info
		 * from the VNFD package
		 * 
		 */

		c.setVxfMANOProviderID(vxf.getName());

		c.setLastOnboarding(new Date());

		VxFOnBoardedDescriptor u = portalRepositoryRef.updateVxFOnBoardedDescriptor(c);

		logger.info("VxF Package Location: " + vxf.getPackageLocation());
		OSMClient.getInstance(u.getObMANOprovider()).createOnBoardVNFDPackage(vxf.getPackageLocation(),
				c.getDeployId());

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested VxFOnBoardedDescriptor with ID=" + c.getId() + " cannot be onboarded");
			return builder.build();
		}

	}

	@PUT
	@Path("/admin/vxfobds/{mpid}/offboard")
	@Produces("application/json")
	@Consumes("application/json")
	public Response offBoardDescriptor(@PathParam("mpid") int mpid, final VxFOnBoardedDescriptor c) {

		c.setOnBoardingStatus(OnBoardingStatus.OFFBOARDED);
		VxFOnBoardedDescriptor u = portalRepositoryRef.updateVxFOnBoardedDescriptor(c);
		// TODO: Implement this towards MANO

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested VxFOnBoardedDescriptor with ID=" + c.getId() + " cannot be onboarded");
			return builder.build();
		}

	}

	/********************************************************************************
	 * 
	 * admin ExperimentOnBoardDescriptors
	 * 
	 ********************************************************************************/

	@GET
	@Path("/admin/experimentobds/")
	@Produces("application/json")
	public Response getExperimentOnBoardDescriptors() {
		return Response.ok().entity(portalRepositoryRef.getExperimentOnBoardDescriptors()).build();
	}

	@POST
	@Path("/admin/experimentobds/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addExperimentOnBoardDescriptor(ExperimentOnBoardDescriptor c) {
		ExperimentOnBoardDescriptor u = portalRepositoryRef.addExperimentOnBoardDescriptor(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested ExperimentOnBoardDescriptor with name=" + c.getId() + " cannot be installed");
			throw new WebApplicationException(builder.build());
		}
	}

	@PUT
	@Path("/admin/experimentobds/{mpid}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateExperimentOnBoardDescriptor(@PathParam("mpid") int mpid, ExperimentOnBoardDescriptor c) {

		ExperimentOnBoardDescriptor u = portalRepositoryRef.updateExperimentOnBoardDescriptor(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested ExperimentOnBoardDescriptor with name=" + c.getId() + " cannot be updated");
			throw new WebApplicationException(builder.build());
		}

	}

	@DELETE
	@Path("/admin/experimentobds/{mpid}")
	public Response deleteExperimentOnBoardDescriptor(@PathParam("mpid") int mpid) {

		portalRepositoryRef.deleteExperimentOnBoardDescriptor(mpid);
		return Response.ok().build();

	}

	@GET
	@Path("/admin/experimentobds/{mpid}")
	@Produces("application/json")
	public Response getExperimentOnBoardDescriptorById(@PathParam("mpid") int mpid) {
		ExperimentOnBoardDescriptor sm = portalRepositoryRef.getExperimentOnBoardDescriptorByID(mpid);

		if (sm != null) {
			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("ExperimentOnBoardDescriptor " + mpid + " not found in portal registry");
			return builder.build();
			// throw new WebApplicationException(builder.build());
		}
	}

	@GET
	@Path("/admin/experimentobds/{mpid}/status")
	@Produces("application/json")
	public Response getExperimentOnBoardDescriptorByIdCheckMANOProvider(@PathParam("mpid") int mpid) {

		ExperimentOnBoardDescriptor sm = portalRepositoryRef.getExperimentOnBoardDescriptorByID(mpid);

		if (sm == null) {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("ExperimentOnBoardDescriptor " + mpid + " not found in portal registry");
			return builder.build();
		}

		if (sm.getOnBoardingStatus().equals(OnBoardingStatus.ONBOARDING)) {

			Nsd nsd = null;
			List<Nsd> nsds = OSMClient.getInstance(sm.getObMANOprovider()).getNSDs();
			for (Nsd v : nsds) {
				if (v.getId().equalsIgnoreCase(sm.getVxfMANOProviderID())
						|| v.getName().equalsIgnoreCase(sm.getVxfMANOProviderID())) {
					nsd = v;
					break;
				}
			}

			if (nsd == null) {
				sm.setOnBoardingStatus(OnBoardingStatus.UNKNOWN);
			} else {
				sm.setOnBoardingStatus(OnBoardingStatus.ONBOARDED);
			}

			sm = portalRepositoryRef.updateExperimentOnBoardDescriptor(sm);

		}

		return Response.ok().entity(sm).build();

	}

	@PUT
	@Path("/admin/experimentobds/{mpid}/onboard")
	@Produces("application/json")
	@Consumes("application/json")
	public Response onExperimentBoardDescriptor(@PathParam("mpid") int mpid, final ExperimentOnBoardDescriptor c) {

		c.setOnBoardingStatus(OnBoardingStatus.ONBOARDING);
		c.setDeployId(UUID.randomUUID().toString());
		ExperimentMetadata em = c.getExperiment();
		if (em == null) {
			em = (ExperimentMetadata) portalRepositoryRef.getProductByID(c.getExperimentid());
		}

		/**
		 * The following is not OK. When we submit to OSMClient the createOnBoardPackage
		 * we just get a response something like response = {"output":
		 * {"transaction-id": "b2718ef9-4391-4a9e-97ad-826593d5d332"}} which does not
		 * provide any information. The OSM RIFTIO API says that we could get
		 * information about onboarding (create or update) jobs see
		 * https://open.riftio.com/documentation/riftware/4.4/a/api/orchestration/pkt-mgmt/rw-pkg-mgmt-download-jobs.htm
		 * with /api/operational/download-jobs, but this does not return pending jobs.
		 * So the only solution is to ask again OSM if something is installed or not, so
		 * for now the client (the portal ) must check via the
		 * getVxFOnBoardedDescriptorByIdCheckMANOProvider giving the VNF ID in OSM. OSM
		 * uses the ID of the yaml description Thus we asume that the vxf name can be
		 * equal to the VNF ID in the portal, and we use it for now as the OSM ID. Later
		 * in future, either OSM API provide more usefull response or we extract info
		 * from the VNFD package
		 * 
		 */

		c.setVxfMANOProviderID(em.getName());

		c.setLastOnboarding(new Date());

		ExperimentOnBoardDescriptor u = portalRepositoryRef.updateExperimentOnBoardDescriptor(c);

		logger.info("Experiment Package Location: " + em.getPackageLocation());
		OSMClient.getInstance(u.getObMANOprovider()).createOnBoardNSDPackage(em.getPackageLocation(), c.getDeployId());

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested ExperimentOnBoardDescriptor with ID=" + c.getId() + " cannot be onboarded");
			return builder.build();
		}

	}

	@PUT
	@Path("/admin/experimentobds/{mpid}/offboard")
	@Produces("application/json")
	@Consumes("application/json")
	public Response offBoardExperimentDescriptor(@PathParam("mpid") int mpid, final ExperimentOnBoardDescriptor c) {

		c.setOnBoardingStatus(OnBoardingStatus.OFFBOARDED);
		ExperimentOnBoardDescriptor u = portalRepositoryRef.updateExperimentOnBoardDescriptor(c);
		// TODO: Implement this towards MANO

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested ExperimentOnBoardDescriptor with ID=" + c.getId() + " cannot be onboarded");
			return builder.build();
		}

	}

	@GET
	@Path("/admin/infrastructures/")
	@Produces("application/json")
	public Response getAdminInfrastructures() {
		return Response.ok().entity(portalRepositoryRef.getInfrastructures()).build();
	}

	@POST
	@Path("/admin/infrastructures/")
	@Produces("application/json")
	@Consumes("application/json")
	public Response addInfrastructure(Infrastructure c) {
		Infrastructure u = portalRepositoryRef.addInfrastructure(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested Infrastructure with name=" + c.getName() + " cannot be created");
			throw new WebApplicationException(builder.build());
		}
	}

	@PUT
	@Path("/admin/infrastructures/{infraid}")
	@Produces("application/json")
	@Consumes("application/json")
	public Response updateInfrastructure(@PathParam("infraid") int infraid, Infrastructure c) {
		Infrastructure previousCategory = portalRepositoryRef.getInfrastructureByID(infraid);

		Infrastructure u = portalRepositoryRef.updateInfrastructureInfo(c);

		if (u != null) {
			return Response.ok().entity(u).build();
		} else {
			ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR);
			builder.entity("Requested Infrastructure with name=" + c.getName() + " cannot be updated");
			throw new WebApplicationException(builder.build());
		}

	}

	@DELETE
	@Path("/admin/infrastructures/{infraid}")
	public Response deleteInfrastructure(@PathParam("infraid") int infraid) {
		portalRepositoryRef.deleteInfrastructure(infraid);
		return Response.ok().build();

	}

	@GET
	@Path("/admin/infrastructures/{infraid}")
	@Produces("application/json")
	public Response getInfrastructureById(@PathParam("infraid") int infraid) {
		Infrastructure sm = portalRepositoryRef.getInfrastructureByID(infraid);

		if (sm != null) {
			return Response.ok().entity(sm).build();
		} else {
			ResponseBuilder builder = Response.status(Status.NOT_FOUND);
			builder.entity("Infrastructure " + infraid + " not found in portal registry");
			throw new WebApplicationException(builder.build());
		}
	}

}
