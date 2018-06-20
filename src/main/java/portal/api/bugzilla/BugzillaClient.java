package portal.api.bugzilla;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

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
import portal.api.bugzilla.model.User;
import portal.api.bugzilla.model.Users;
import portal.api.model.DeploymentDescriptor;

public class BugzillaClient {

	private static final transient Log logger = LogFactory.getLog(BugzillaClient.class.getName());

	private static BugzillaClient instance;

	public static BugzillaClient getInstance() {
		if (instance == null) {
			instance = new BugzillaClient();
		}
		return instance;
	}

	private static final String BASE_SERVICE_URL = "https://portal.5ginfire.eu/bugstaging/rest.cgi";

	private static final String API_KEY = "VH2Vw0iI5aYgALFFzVDWqhACwt6Hu3bXla9kSC1Z";

	
	
	public static String transformDeployment2BugBody(DeploymentDescriptor descriptor) {

		String product = "5GinFIRE Operations";
		String component = "Operations Support" ;
		String summary = "[BYPORTAL] New Deployment Request";
		String description = "By owner " + descriptor.getOwner().getUsername() ;
		String alias = descriptor.getUuid() ;
		
		StringBuilder str = new StringBuilder();
		str.append("{");
		str.append("\"product\": \"" + product + "\",");
		str.append("\"component\": \"" + component + "\",");
		str.append("\"alias\": \"" + alias + "\",");
		str.append("\"summary\": \"" + summary + "\",");
		
		str.append("\"description\": \"" + description + "\",");
		str.append("\"version\" : \"unspecified\"");
		str.append("}");

		return str.toString();
	}
	
	
	
	
	
	
	
	
	/**
	 * Create a user in Bugzilla, 
	 * @param email
	 * @param realName
	 * @param password
	 * @return true if user is created otherwise false
	 */
	public boolean createUser(String email, String realName, String password) {

		String url = BASE_SERVICE_URL + "/user?&api_key=" + API_KEY;

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
	 * @param product
	 * @param component
	 * @param summary
	 * @param summary
	 * @return bug ID or null if fails to create a new Bug
	 */
	public String createBug(String product, String component, String summary, String description, String[] cclist ) {

		String url = BASE_SERVICE_URL + "/bug?&api_key=" + API_KEY;

		logger.info("Sending HTTPS createUser towards: " + url);

		CloseableHttpClient httpclient = returnHttpClient();

		HttpPost httppost = new HttpPost(url);
		BasicHeader bh = new BasicHeader("Accept", "application/json");
		httppost.addHeader(bh);

		HttpResponse response;
		try {
			StringBuilder str = new StringBuilder();
			str.append( "{" );
			str.append( "\"product\": \"" + product + "\"," );
			str.append( "\"component\": \"" + component + "\"," );
			str.append( "\"summary\": \"" + summary + "\"," );
			
			if (cclist!=null) {
				str.append( "\"cc\": [" );			
				String list = "\"" + String.join("\", \"", cclist) + "\"";
				str.append( list );
				str.append( "]," );
			}
			str.append( "\"description\": \"" + description + "\"," );
			str.append(  "\"version\" : \"unspecified\"" );
			 str.append( "}" );
					
					
			StringEntity params = new StringEntity( str.toString() );
			httppost.setEntity(params);

			response = httpclient.execute(httppost);
			
			if ( ( response.getStatusLine().getStatusCode() == HttpStatus.SC_OK )  || ( response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED )  ) {			
				HttpEntity entity = response.getEntity();
				InputStream inStream = (InputStream) entity.getContent();
				String s = IOUtils.toString(inStream);
				logger.info("response = " + s);
				
				ObjectMapper mapper = new ObjectMapper(new JsonFactory());
				
				
				JsonNode tr = mapper.readTree( s ).get("id");
				//tr = tr.get(0);
				String bugID = tr.toString();

				logger.info("Bug created with id = " + bugID + ". Summary = " + summary );
				return bugID;
			} else {
				logger.info( response.getStatusLine().getReasonPhrase() + " - Bug cannot be created. Summary = " + summary );
				return null;
				
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	
	
	/**
	 * @param username
	 * @return the {@link User} found or null
	 */
	public User getUser(String username) {
		String response = BugzillaClient.getInstance()
				.getBzResponse(BASE_SERVICE_URL + "/user/" + username + "?&api_key=" + API_KEY);

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
				.getBzResponse(BASE_SERVICE_URL + "/bug/" + bugID + "?&api_key=" + API_KEY);

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
