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

package portal.api;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import portal.api.model.InstalledVxF;
import portal.api.model.InstalledVxFStatus;

public class PortalClientIT {

	private static String endpointUrl;
	private static final transient Log logger = LogFactory.getLog(PortalClientIT.class.getName());

	@BeforeClass
	public static void beforeClass() {
		endpointUrl = System.getProperty("service.url");
		// portalJpaControllerTest.delete(message);
	}

	@Test
	public void testPortalClientInstallServiceNotFoundAndFail() throws Exception {

		logger.info("Executing TEST = testPortalRSInstallServiceNotFound");
		List<Object> providers = new ArrayList<Object>();
		providers.add(new com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider());
		String uuid = UUID.fromString("55cab8b8-668b-4c75-99a9-39b24ed3d8be").toString();
		InstalledVxF is = prepareInstalledService(uuid);

		WebClient client = WebClient.create(endpointUrl + "/services/api/client/ivxfs/", providers);
		Response r = client.accept("application/json").type("application/json").post(is);
		assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
		

		String portalAPIVersionListHeaders = (String) r.getHeaders().getFirst("X-Portal-API-Version");
		assertEquals("1.0.0", portalAPIVersionListHeaders);

		MappingJsonFactory factory = new MappingJsonFactory();
		JsonParser parser = factory.createJsonParser((InputStream) r.getEntity());
		InstalledVxF output = parser.readValueAs(InstalledVxF.class);
		logger.info("InstalledServiceoutput = " + output.getUuid() + ", status=" + output.getStatus());
		assertEquals(InstalledVxFStatus.INIT, output.getStatus());

		// wait for 2 seconds
		Thread.sleep(2000);
		// ask again about this task
		client = WebClient.create(endpointUrl + "/services/api/client/ivxfs/" + uuid);
		r = client.accept("application/json").type("application/json").get();

		factory = new MappingJsonFactory();
		parser = factory.createJsonParser((InputStream) r.getEntity());
		output = parser.readValueAs(InstalledVxF.class);

		assertEquals(uuid, output.getUuid());
		assertEquals(InstalledVxFStatus.FAILED, output.getStatus());
		assertEquals("(pending)", output.getName());
	}

	@Test
	public void testPortalClientInstallVxFAndGetStatus() throws Exception {

		logger.info("Executing TEST = testPortalRSInstallServiceAndGetStatus");

		List<Object> providers = new ArrayList<Object>();
		providers.add(new com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider());
		String uuid = UUID.fromString("77777777-668b-4c75-99a9-39b24ed3d8be").toString();

		// first delete an existing installation if exists

		WebClient client = WebClient.create(endpointUrl + "/services/api/client/ivxfs/" + uuid, providers);
		Response r = client.accept("application/json").type("application/json").delete();
		if (Response.Status.NOT_FOUND.getStatusCode() != r.getStatus()) {

			assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());
			logger.info("VxF is already installed! We uninstall it first!");
			int guard = 0;
			InstalledVxF insvxf = null;
			do {

				// ask again about this task
				client = WebClient.create(endpointUrl + "/services/api/client/ivxfs/" + uuid);
				r = client.accept("application/json").type("application/json").get();

				MappingJsonFactory factory = new MappingJsonFactory();
				JsonParser parser = factory.createJsonParser((InputStream) r.getEntity());
				insvxf = parser.readValueAs(InstalledVxF.class);

				logger.info("Waiting for UNINSTALLED for test vxf UUID=" + uuid + " . Now is: " + insvxf.getStatus());
				Thread.sleep(2000);
				guard++;

			} while ((insvxf != null) && (insvxf.getStatus() != InstalledVxFStatus.UNINSTALLED) && (insvxf.getStatus() != InstalledVxFStatus.FAILED)
					&& (guard <= 30));

			if (insvxf.getStatus() != InstalledVxFStatus.FAILED)
				assertEquals(InstalledVxFStatus.UNINSTALLED, insvxf.getStatus());

		}

		// now post a new installation
		client = WebClient.create(endpointUrl + "/services/api/client/ivxfs/", providers);
		InstalledVxF is = prepareInstalledService(uuid);
		r = client.accept("application/json").type("application/json").post(is);
		assertEquals(Response.Status.OK.getStatusCode(), r.getStatus());

		int guard = 0;

		InstalledVxF insvxf = null;
		do {

			// ask again about this task
			client = WebClient.create(endpointUrl + "/services/api/client/ivxfs/" + uuid);
			r = client.accept("application/json").type("application/json").get();

			MappingJsonFactory factory = new MappingJsonFactory();
			JsonParser parser = factory.createJsonParser((InputStream) r.getEntity());
			insvxf = parser.readValueAs(InstalledVxF.class);

			logger.info("Waiting for STARTED for test vxf UUID=" + uuid + " . Now is: " + insvxf.getStatus());
			Thread.sleep(1000);
			guard++;

		} while ((insvxf != null) && (insvxf.getStatus() != InstalledVxFStatus.STARTED) && (guard <= 30));

		assertEquals(uuid, insvxf.getUuid());
		assertEquals(InstalledVxFStatus.STARTED, insvxf.getStatus());
		assertEquals("IntegrTestLocal example service", insvxf.getName());

	}

	// helpers
	private InstalledVxF prepareInstalledService(String uuid) {
		InstalledVxF is = new InstalledVxF();
		is.setUuid(uuid);
		is.setRepoUrl(endpointUrl + "/services/api/repo/vxfs/uuid/" + uuid);
		return is;
	}
}
