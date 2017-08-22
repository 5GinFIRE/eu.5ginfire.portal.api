package portal.api.osm.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
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

import portal.api.fiware.FIWAREUser;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.Nsd;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.NsdBuilder.NsdImpl;

public class OSMClient {

	private static final String BASE_SERVICE_URL = "https://localhost:8008/api/running/nsd-catalog/nsd";

	public static void main(String[] args) throws ClientProtocolException, IOException, KeyStoreException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException {


		/**
		 * the following can be used with a good certificate, for now we just trust it
		 * import a certificate
		 * keytool -import -alias riftiolocalvm -file Riftio.crt -keystore cacerts -storepass changeit
		 */
		String keyStoreLoc = "src/main/config/clientKeystore.jks";

		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(keyStoreLoc), "123456".toCharArray());

		/*
		 * Send HTTP GET request to query customer info using portable
		 * HttpClient object from Apache HttpComponents
		 */
		SSLSocketFactory sf = new SSLSocketFactory("TLS", keyStore, "123456", keyStore, new java.security.SecureRandom( ), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Scheme httpsScheme = new Scheme("https", 8008, sf);

		
		
		System.out.println("Sending HTTPS GET request to query  info");
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getConnectionManager().getSchemeRegistry().register(httpsScheme);
		HttpGet httpget = new HttpGet(BASE_SERVICE_URL + "/cirros_2vnf_nsd");
		BasicHeader bh = new BasicHeader("Accept", "application/json");
		httpget.addHeader(bh);
		BasicHeader bh2 = new BasicHeader("Authorization", "Basic YWRtaW46YWRtaW4=");
		httpget.addHeader(bh2);

		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		
		MappingJsonFactory factory = new MappingJsonFactory();
		 JsonParser parser;
		 parser = factory.createJsonParser((InputStream) entity.getContent() );
		 JsonNode tr = parser.readValueAsTree().get("nsd:nsd");
		 String s = tr.toString();
		 JsonParser nsd = factory.createJsonParser(s);
		 Nsd fu = nsd.readValueAs(Nsd.class);
		 System.out.println("=== NSD POJO object response: " + fu.toString());

		entity.writeTo(System.out);
		httpclient.getConnectionManager().shutdown();

		
		
		
		
		
		
		
		
		
//		/*
//		 * Send HTTP PUT request to update customer info, using CXF WebClient
//		 * method Note: if need to use basic authentication, use the
//		 * WebClient.create(baseAddress, username,password,configFile) variant,
//		 * where configFile can be null if you're not using certificates.
//		 */
//		System.out.println("\n\nSending HTTPS PUT to update customer name");
//		WebClient wc = WebClient.create(BASE_SERVICE_URL, CLIENT_CONFIG_FILE);
//		Customer customer = new Customer();
//		customer.setId(123);
//		customer.setName("Mary");
//		Response resp = wc.put(customer);
//
//		/*
//		 * Send HTTP POST request to add customer, using JAXRSClientProxy Note:
//		 * if need to use basic authentication, use the
//		 * JAXRSClientFactory.create(baseAddress, username,password,configFile)
//		 * variant, where configFile can be null if you're not using
//		 * certificates.
//		 */
//		System.out.println("\n\nSending HTTPS POST request to add customer");
//		CustomerService proxy = JAXRSClientFactory.create(BASE_SERVICE_URL, CustomerService.class, CLIENT_CONFIG_FILE);
//		customer = new Customer();
//		customer.setName("Jack");
//		resp = wc.post(customer);

		System.out.println("\n");
		System.exit(0);
	}

}
