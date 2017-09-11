package portal.api.osm.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

import portal.api.cloudOAuth.OAuthUser;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.Nsd;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.NsdBuilder.NsdImpl;
import urn.ietf.params.xml.ns.yang.nfvo.vnfd.rev150910.vnfd.catalog.Vnfd;

public class OSMClient {

	/**	 */
	private static final String keyStoreLoc = "src/main/config/clientKeystore.jks";

	/**	 */
	//private static final String BASE_SERVICE_URL = "https://localhost:8008/api/running";
	private static final String BASE_SERVICE_URL = "https://10.0.2.2:8008/api/running";
	
	
	
	/** */
	private static final int OSMAPI_HTTPS_PORT = 8008;
	
	/**
	 * 
	 */
	private static Scheme httpsScheme = null;

	/**
	 * 
	 */
	private static void init() {
		/**
		 * the following can be used with a good certificate, for now we just
		 * trust it import a certificate keytool -import -alias riftiolocalvm
		 * -file Riftio.crt -keystore cacerts -storepass changeit
		 */

		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance("JKS");
			keyStore.load(new FileInputStream(keyStoreLoc), "123456".toCharArray());

			/*
			 * Send HTTP GET request to query customer info using portable
			 * HttpClient object from Apache HttpComponents
			 */
			SSLSocketFactory sf = new SSLSocketFactory("TLS", keyStore, "123456", keyStore,
					new java.security.SecureRandom(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			httpsScheme = new Scheme("https", OSMAPI_HTTPS_PORT, sf);

		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args)  {
		
		OSMClient.init();
		
		
		Nsd fu = getNSDbyID( "cirros_2vnf_nsd" );
		System.out.println("=== NSD POJO object response: " + fu.toString());


		Vnfd vnfd = getVNFDbyID( "cirros_vnfd" );
		System.out.println("=== VNFD POJO object response: " + vnfd.toString());
		
		// /*
		// * Send HTTP PUT request to update customer info, using CXF WebClient
		// * method Note: if need to use basic authentication, use the
		// * WebClient.create(baseAddress, username,password,configFile)
		// variant,
		// * where configFile can be null if you're not using certificates.
		// */
		// System.out.println("\n\nSending HTTPS PUT to update customer name");
		// WebClient wc = WebClient.create(BASE_SERVICE_URL,
		// CLIENT_CONFIG_FILE);
		// Customer customer = new Customer();
		// customer.setId(123);
		// customer.setName("Mary");
		// Response resp = wc.put(customer);
		//
		// /*
		// * Send HTTP POST request to add customer, using JAXRSClientProxy
		// Note:
		// * if need to use basic authentication, use the
		// * JAXRSClientFactory.create(baseAddress,
		// username,password,configFile)
		// * variant, where configFile can be null if you're not using
		// * certificates.
		// */
		// System.out.println("\n\nSending HTTPS POST request to add customer");
		// CustomerService proxy = JAXRSClientFactory.create(BASE_SERVICE_URL,
		// CustomerService.class, CLIENT_CONFIG_FILE);
		// customer = new Customer();
		// customer.setName("Jack");
		// resp = wc.post(customer);

		System.out.println("\n");
		System.exit(0);
	}

	
	/**
	 * @param reqURL
	 * @return
	 */
	public static String getOSMResponse(String reqURL) {

		System.out.println("Sending HTTPS GET request to query  info");
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getConnectionManager().getSchemeRegistry().register(httpsScheme);
		HttpGet httpget = new HttpGet( reqURL );
		BasicHeader bh = new BasicHeader("Accept", "application/json");
		httpget.addHeader(bh);
		BasicHeader bh2 = new BasicHeader("Authorization", "Basic YWRtaW46YWRtaW4="); //this is hardcoded admin/admin
		httpget.addHeader(bh2);

		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			InputStream inStream = (InputStream) entity.getContent();
			String s = IOUtils.toString( inStream ) ; 
			System.out.println( "response = " + s  );
			httpclient.getConnectionManager().shutdown();
			return s;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return null;
	}

	/**
	 * @param aNSDid
	 * @return
	 */
	public static Nsd getNSDbyID(String aNSDid) {


		String response = getOSMResponse( BASE_SERVICE_URL + "/nsd-catalog/nsd/" + aNSDid );

		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createJsonParser( response );
			
			JsonNode tr = parser.readValueAsTree().get("nsd:nsd"); //needs a massage
			String s = tr.toString();
			JsonParser jnsd = factory.createJsonParser(s);
			Nsd nsd = jnsd.readValueAs(Nsd.class);
			return nsd;
			
		} catch (IllegalStateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	public static Vnfd getVNFDbyID(String aVNFDid) {


		OSMClient.init();
		String response = getOSMResponse( BASE_SERVICE_URL + "/vnfd-catalog/vnfd/" + aVNFDid );
		

		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createJsonParser( response );
			//entity.writeTo(System.out);
			JsonNode tr = parser.readValueAsTree().get("vnfd:vnfd"); //needs a massage
			String s = tr.toString();
			JsonParser jnsd = factory.createJsonParser(s);
			Vnfd vnfd = jnsd.readValueAs(Vnfd.class);
			return vnfd;
			
		} catch (IllegalStateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	
}
