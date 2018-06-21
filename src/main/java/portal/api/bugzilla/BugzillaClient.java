package portal.api.bugzilla;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import portal.api.bugzilla.model.Bug;
import portal.api.bugzilla.model.Bugs;
import portal.api.bugzilla.model.Comment;
import portal.api.bugzilla.model.User;
import portal.api.bugzilla.model.Users;
import portal.api.model.DeploymentDescriptor;
import portal.api.model.DeploymentDescriptorStatus;
import portal.api.model.DeploymentDescriptorVxFPlacement;

public class BugzillaClient {

	private static final transient Log logger = LogFactory.getLog(BugzillaClient.class.getName());

	/** */
	private static BugzillaClient instance;


	/** */
	private static final String BASE_SERVICE_URL = "https://portal.5ginfire.eu";
	/** */
	private static final String BUGZILLA_BASE_SERVICE_URL = "https://portal.5ginfire.eu/bugstaging/rest.cgi";
	/** */
	private static final String API_KEY = "VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z";

	/** */
	private static final String BUGHEADER =   "*************************************************\n"
											+ "THIS IS AN AUTOMATED ISSUE CREATED BY PORTAL API.\n"
											+ "*************************************************\n";


	public static BugzillaClient getInstance() {
		if (instance == null) {
			instance = new BugzillaClient();
		}
		return instance;
	}
		
	public static Bug transformDeployment2BugBody(DeploymentDescriptor descriptor) {

		String product = "5GinFIRE Operations";
		String component = "Operations Support" ;
		String summary = "[PORTAL] Deployment Request of NSD:" + descriptor.getExperiment().getName() + ",User: " + descriptor.getOwner().getUsername();
		String alias = descriptor.getUuid() ;

		String description = getDeploymentDescription( descriptor );		
		
		String status= "CONFIRMED";
		String resolution = null;
		if ( ( descriptor.getStatus() == DeploymentDescriptorStatus.SCHEDULED ) || ( descriptor.getStatus() == DeploymentDescriptorStatus.RUNNING )) {
			status = "IN_PROGRESS";
		} else  if ( ( descriptor.getStatus() == DeploymentDescriptorStatus.COMPLETED ) ) {
			status = "RESOLVED";
			resolution = "FIXED";
		} else if ( ( descriptor.getStatus() == DeploymentDescriptorStatus.REJECTED ) ) {
			status = "RESOLVED";
			resolution = "INVALID";
		}
		
		
		Bug b = createBug(product, component, summary, alias, description, descriptor.getOwner().getEmail(), status, resolution);
		
		return b;
	}
	
	
	public static Comment transformDeployment2BugComment(DeploymentDescriptor descriptor) {
		
		String description = getDeploymentDescription( descriptor );
				
		Comment b = createComment( description);
		
		return b;
	}
	
	
	/**
	 * @param descriptor
	 * @return
	 */
	private static String getDeploymentDescription(DeploymentDescriptor descriptor) {
		StringBuilder description =  new StringBuilder( BUGHEADER );

		description.append( "\nSTATUS: " + descriptor.getStatus() + "\n");
		if ( descriptor.getStartDate() != null ) {
			description.append( "\nFeedback: " + descriptor.getFeedback() );
			description.append( "\nScheduled Start Date: " + descriptor.getStartDate().toString() );
			description.append( "\nScheduled End Date: " + descriptor.getEndDate().toString() );
		} else {
			description.append( "\nNOT YET SCHEDULED \n");			
		}
		
		
		description.append(
						"\nDeployment Request by user :" + descriptor.getOwner().getUsername() 
						+"\nHere are the details:\n"
						+ "\nExperiment name: " + descriptor.getName() 
						+ "\nDescription: " + descriptor.getDescription() 
						+ "\nDate Created: " + descriptor.getDateCreated().toString() 
						+ "\nRequested Tentative Start date: " + descriptor.getStartReqDate().toString() 
						+ "\nRequested Tentative End date: " + descriptor.getEndReqDate().toString() 
						+ "\nExperiment (NSD) requested: " + descriptor.getExperiment().getName() );
		

		description.append( "\nConstituent VxF Placement " ) ;
		for (DeploymentDescriptorVxFPlacement pl : descriptor.getVxfPlacements()) {
			description.append( "\n  Constituent VxF: " + pl.getConstituentVxF().getVxfref().getName() + " - Infrastructure: " + pl.getInfrastructure().getName() );			
		}
		
		
				
						 
		description.append( "\n*************************************************\n");
		description.append( "\nTo manage this Request, go to: " + BASE_SERVICE_URL + "/#!/edit_deployment/" + descriptor.getId() ); 
		return description.toString();
	}


	public static Comment createComment( String description ) {
		
		Comment c = new Comment();
		c.setComment(description);
		c.setIs_markdown( false );
		c.setIs_private( false );	
		return c;
	}
	
	
	/**
	 * @param product
	 * @param component
	 * @param summary
	 * @param alias
	 * @param description
	 * @param ccemail
	 * @return
	 */
	public static Bug createBug(String product, String component, String summary, String alias, String description, String ccemail, String status, String resolution ) {
		
		Bug b = new Bug();
		b.setProduct(product);
		b.setComponent(component);
		b.setSummary(summary);
		b.setVersion( "unspecified" );
		List<Object> aliaslist = new ArrayList<>();
		aliaslist.add(alias);		
		b.setAlias( aliaslist );
		List<String> cclist = new ArrayList<>();
		cclist.add( ccemail );		
		b.setCc(cclist); 
		b.setDescription(description.toString());		
		b.setStatus(status);
		b.setResolution(resolution);
				
		return b;
	}
	
	
	
	/**
	 * Create a user in Bugzilla, 
	 * @param email
	 * @param realName
	 * @param password
	 * @return true if user is created otherwise false
	 */
	public boolean createUser(String email, String realName, String password) {

		String url = BUGZILLA_BASE_SERVICE_URL + "/user?&api_key=" + API_KEY;

		logger.info("Sending HTTPS createUser towards: " + url);

		CloseableHttpClient httpclient = returnHttpClient();

		HttpPost httppost = new HttpPost(url);
		BasicHeader bh = new BasicHeader("Accept", "application/json");
		httppost.addHeader(bh);

		HttpResponse response;
		try {
			StringEntity params = new StringEntity("{" + "\"email\": \"" + email + "\"," + "\"full_name\": \""
					+ realName + "\"," + "\"password\": \"" + password + "\"" + "}");
			httppost.setEntity(params);

			response = httpclient.execute(httppost);
			
			if ( ( response.getStatusLine().getStatusCode() == HttpStatus.SC_OK )  || ( response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED )  ) {			
				HttpEntity entity = response.getEntity();
				InputStream inStream = (InputStream) entity.getContent();
				String s = IOUtils.toString(inStream);
				logger.info("response = " + s);
				return true;
			} else {
				logger.info("User email already exists : " + email );
				return false;
				
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	
	
	
	
	/**
	 * @param username
	 * @return the {@link User} found or null
	 */
	public User getUser(String username) {
		String response = BugzillaClient.getInstance()
				.getBzResponse(BUGZILLA_BASE_SERVICE_URL + "/user/" + username + "?&api_key=" + API_KEY);

		ObjectMapper mapper = new ObjectMapper(new JsonFactory());

		try {
			Users users = mapper.readValue(response.toString(), Users.class);

			if ((users != null) && (users.getUsers() != null) && (users.getUsers().size() > 0)) {
				return users.getUsers().get(0);
			}

		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param bugID
	 * @return the {@link Bug} found or null
	 */
	public Bug getBugById(String bugID) {
		String response = BugzillaClient.getInstance()
				.getBzResponse(BUGZILLA_BASE_SERVICE_URL + "/bug/" + bugID + "?&api_key=" + API_KEY);

		ObjectMapper mapper = new ObjectMapper(new JsonFactory());

		try {
			Bugs b = mapper.readValue(response.toString(), Bugs.class);

			if ((b != null) && (b.getBugs() != null) && (b.getBugs().size() > 0)) {
				return b.getBugs().get(0);
			}

		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param reqURL
	 * @return
	 */
	private String getBzResponse(String reqURL) {

		logger.info("Sending HTTPS GET request to query  info: " + reqURL);

		CloseableHttpClient httpclient = returnHttpClient();

		HttpGet httpget = new HttpGet(reqURL);
		BasicHeader bh = new BasicHeader("Accept", "application/json");
		httpget.addHeader(bh);
		// BasicHeader bh2 = new BasicHeader("Authorization", "Basic " +
		// this.manoProvider.getAuthorizationBasicHeader()); // this is hardcoded
		// admin/admin
		// httpget.addHeader(bh2);

		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			InputStream inStream = (InputStream) entity.getContent();
			String s = IOUtils.toString(inStream);
			logger.info("response = " + s);

			return s;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private CloseableHttpClient returnHttpClient() {
		try {
			HttpClientBuilder h = HttpClientBuilder.create();

			SSLContext sslContext;
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
			CloseableHttpClient httpclient = HttpClients.custom().setSSLContext(sslContext)
					.setSSLHostnameVerifier(new NoopHostnameVerifier()).build();

			return httpclient;
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

}
