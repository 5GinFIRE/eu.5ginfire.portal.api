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

package portal.api.osm.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import portal.api.model.MANOprovider;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.Nsd;
import urn.ietf.params.xml.ns.yang.nfvo.vnfd.rev150910.vnfd.catalog.Vnfd;

/**
 * @author ctranoris
 *
 */
public class OSMClient {

	/**	 */
	private static final String keyStoreLoc = "src/main/config/clientKeystore.jks";

	/**	 */
	private static final String BASE_INIT_URL = "https://localhost:8008";
	private String BASE_URL ;
	private String BASE_SERVICE_URL;
	private String BASE_OPERATIONS_URL;
	private String BASE_OPERATIONAL_URL;

	// private static final String BASE_SERVICE_URL =
	// "https://10.0.2.15:8008/api/running";

	
	private static Map<Integer, OSMClient> instances = new HashMap<>();


	public static OSMClient getInstance( MANOprovider  mano) {
		OSMClient osmapi = instances.get( mano.getId() );
		if (osmapi == null) {
			osmapi = new OSMClient( mano.getApiEndpoint() );
			osmapi.init();
			instances.put( mano.getId(), osmapi);
		}
		return osmapi;
	}

	/** */
	private static final int OSMAPI_HTTPS_PORT = 8008;

	/**
	 * 
	 */
	private static Scheme httpsScheme = null;

	
	public OSMClient() {
		this( BASE_INIT_URL );
	}
	
	public OSMClient(String apiEndpoint) {
		BASE_URL = apiEndpoint + "/api";
		BASE_SERVICE_URL = BASE_URL + "/running";
		BASE_OPERATIONS_URL = BASE_URL + "/operations";
		BASE_OPERATIONAL_URL = BASE_URL + "/operational";
	}


	/**
	 * 
	 */
	private void init() {
		/**
		 * the following can be used with a good certificate, for now we just trust it
		 * import a certificate keytool -import -alias riftiolocalvm -file Riftio.crt
		 * -keystore cacerts -storepass changeit
		 */

		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance("JKS");
			keyStore.load(new FileInputStream(keyStoreLoc), "123456".toCharArray());

			/*
			 * Send HTTP GET request to query customer info using portable HttpClient object
			 * from Apache HttpComponents
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

	public static void main(String[] args) {

		

		//
		// Nsd fu = getNSDbyID( "cirros_2vnf_nsd" );
		// System.out.println("=== NSD POJO object response: " + fu.toString());
		//
		//
//		Vnfd vnfd = OSMClient.getInstance().getVNFDbyID("cirros_vnfd");
//		System.out.println("=== VNFD POJO object response: " + vnfd.toString());
//
//		vnfd = OSMClient.getInstance().getVNFDbyID("af3cf174-f942-4e26-bc00-c8246fa71b05");
//		System.out.println("=== VNFD POJO object response: " + vnfd.toString());

		//Vnfd vnfd = OSMClient.getInstance().getVNFDbyID("d0cb056a-8995-11e7-8b54-00163e11a1e2");
		//System.out.println("=== VNFD POJO object response: " + vnfd.toString());

//		Vnfd vnfd = OSMClient.getInstance().getVNFDbyID("d0cb056b-8995-11e7-8b54-00163e11a1e2");
//		System.out.println("=== VNFD POJO object response: " + vnfd.toString());
		
		OSMClient osm = new OSMClient();
		osm.init();
//		List<Vnfd> vnfds = osm.getVNFDs();
//		for (Vnfd v : vnfds) {
//			System.out.println("=== LIST VNFDs POJO object response: " + v.toString());			
//		}
//		for (Vnfd v : vnfds) {
//			System.out.println("=== LIST VNFDs POJO object id: " + v.getId()+", Name: " + v.getName());			
//		}
		
		
		
		List<Nsd> nsds = osm.getNSDs();
		for (Nsd v : nsds) {
			System.out.println("=== LIST NSDs POJO object response: " + v.toString());			
		}
		for (Nsd v : nsds) {
			System.out.println("=== LIST NSDs POJO object id: " + v.getId()+", Name: " + v.getName());
			
		}
		
		// createOnBoardPackage();

		// getOSMdownloadjobs("");

		System.out.println("\n");
		System.exit(0);
	}


	public void createOnBoardVNFDPackage(String packageURL, String packageID) {

		// BASE_SERVICE_URL + "/vnfd-catalog/vnfd/"
		//

		System.out.println("Sending HTTPS createOnBoardPackage towards: " + BASE_OPERATIONS_URL);
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getConnectionManager().getSchemeRegistry().register(httpsScheme);
		HttpPost httppost = new HttpPost(BASE_OPERATIONS_URL + "/package-create");
		BasicHeader bh = new BasicHeader("Accept", "application/vnd.yang.collection+json");
		httppost.addHeader(bh);
		BasicHeader bh2 = new BasicHeader("Authorization", "Basic YWRtaW46YWRtaW4="); // this is hardcoded admin/admin
		httppost.addHeader(bh2);
		BasicHeader bh3 = new BasicHeader("Content-Type", "application/vnd.yang.data+json");
		httppost.addHeader(bh3);

		HttpResponse response;
		try {
			StringEntity params = new StringEntity("{" + "\"input\":{" + "\"external-url\": \"" + packageURL + "\","
					+ "\"package-type\":\"VNFD\"," + "\"package-id\":\"" + packageID + "\"" + "}" + "}");
			httppost.setEntity(params);

			response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			InputStream inStream = (InputStream) entity.getContent();
			String s = IOUtils.toString(inStream);
			System.out.println("response = " + s);
			httpclient.getConnectionManager().shutdown();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void createOnBoardNSDPackage(String packageURL, String packageID) {

		System.out.println("Sending HTTPS createOnBoardPackage towards: " + BASE_OPERATIONS_URL);
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getConnectionManager().getSchemeRegistry().register(httpsScheme);
		HttpPost httppost = new HttpPost(BASE_OPERATIONS_URL + "/package-create");
		BasicHeader bh = new BasicHeader("Accept", "application/vnd.yang.collection+json");
		httppost.addHeader(bh);
		BasicHeader bh2 = new BasicHeader("Authorization", "Basic YWRtaW46YWRtaW4="); // this is hardcoded admin/admin
		httppost.addHeader(bh2);
		BasicHeader bh3 = new BasicHeader("Content-Type", "application/vnd.yang.data+json");
		httppost.addHeader(bh3);

		HttpResponse response;
		try {
			StringEntity params = new StringEntity("{" + "\"input\":{" + "\"external-url\": \"" + packageURL + "\","
					+ "\"package-type\":\"NSD\"," + "\"package-id\":\"" + packageID + "\"" + "}" + "}");
			httppost.setEntity(params);

			response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			InputStream inStream = (InputStream) entity.getContent();
			String s = IOUtils.toString(inStream);
			System.out.println("response = " + s);
			httpclient.getConnectionManager().shutdown();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	/**
	 * @param reqURL
	 * @return
	 */
	public String getOSMdownloadjobs(String jobid) {

		System.out.println("Sending HTTPS GET request to query  info");
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getConnectionManager().getSchemeRegistry().register(httpsScheme);
		HttpGet httpget = new HttpGet(BASE_OPERATIONAL_URL + "/download-jobs");
		BasicHeader bh = new BasicHeader("Accept", "application/json");
		httpget.addHeader(bh);
		BasicHeader bh2 = new BasicHeader("Authorization", "Basic YWRtaW46YWRtaW4="); // this is hardcoded admin/admin
		httpget.addHeader(bh2);

		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			String s = response.getStatusLine().toString();
			if (response.getEntity() != null) {
				InputStream inStream = (InputStream) entity.getContent();
				s = IOUtils.toString(inStream);
			}
			System.out.println("response = " + s);
			httpclient.getConnectionManager().shutdown();
			return s;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * @param reqURL
	 * @return
	 */
	public static String getOSMResponse(String reqURL) {

		System.out.println("Sending HTTPS GET request to query  info");
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient.getConnectionManager().getSchemeRegistry().register(httpsScheme);
		HttpGet httpget = new HttpGet(reqURL);
		BasicHeader bh = new BasicHeader("Accept", "application/json");
		httpget.addHeader(bh);
		BasicHeader bh2 = new BasicHeader("Authorization", "Basic YWRtaW46YWRtaW4="); // this is hardcoded admin/admin
		httpget.addHeader(bh2);

		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			InputStream inStream = (InputStream) entity.getContent();
			String s = IOUtils.toString(inStream);
			System.out.println("response = " + s);
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
	public Nsd getNSDbyID(String aNSDid) {

		String response = getOSMResponse(BASE_SERVICE_URL + "/nsd-catalog/nsd/" + aNSDid);

		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createJsonParser(response);

			JsonNode tr = parser.readValueAsTree().get("nsd:nsd"); // needs a massage
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

	public Vnfd getVNFDbyID(String aVNFDid) {

		String response = getOSMResponse(BASE_SERVICE_URL + "/vnfd-catalog/vnfd/" + aVNFDid);

		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createJsonParser(response);
			// entity.writeTo(System.out);
			JsonNode tr = parser.readValueAsTree().get("vnfd:vnfd"); // needs a massage
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

	public List<Vnfd> getVNFDs() {

		String response = getOSMResponse(BASE_SERVICE_URL + "/vnfd-catalog/vnfd");

		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createJsonParser(response);
			JsonNode tr = parser.readValueAsTree().get("vnfd:vnfd"); // needs a massage
			
			ArrayList<Vnfd> vnfds = new ArrayList<>();
			
			for (JsonNode jsonNode : tr) {
				String s = jsonNode.toString();
				JsonParser jnsd = factory.createJsonParser(s);
				Vnfd vnfd = jnsd.readValueAs(Vnfd.class);
				vnfds.add(vnfd);
			}
			
			
			return vnfds;

		} catch (IllegalStateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	public List<Nsd> getNSDs() {

		String response = getOSMResponse(BASE_SERVICE_URL + "/nsd-catalog/nsd");

		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser;
		try {
			parser = factory.createJsonParser(response);
			JsonNode tr = parser.readValueAsTree().get("nsd:nsd"); // needs a massage
			
			ArrayList<Nsd> nsds = new ArrayList<>();
			
			for (JsonNode jsonNode : tr) {
				String s = jsonNode.toString();
				JsonParser jnsd = factory.createJsonParser(s);
				Nsd nsd = jnsd.readValueAs(Nsd.class);
				nsds.add(nsd);
			}
			
			
			return nsds;

		} catch (IllegalStateException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

}
