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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import portal.api.impl.PortalInstallationMgmt;
import portal.api.impl.PortalJpaController;
import portal.api.model.VxFMetadata;
import portal.api.model.InstalledVxF;
import portal.api.model.InstalledVxFStatus;
import portal.api.testclasses.MockRepositoryWebClient;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:contextTest.xml" })
//@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
//@Transactional
public class PortalClientTest {

	@Autowired
	private PortalJpaController portalJpaControllerTest;

	private static final transient Log logger = LogFactory.getLog(PortalClientTest.class.getName());

	@Test
	public void testGetManagedServices() {
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		assertNotNull(bs.getManagedInstalledVxFs());
		logger.info("	 	>>>>	portalJpaControllerTest = " + portalJpaControllerTest);
	}

	@Test
	public void testWriteReadDB() {

		portalJpaControllerTest.deleteAllInstalledVxFs();

		String uuid = UUID.randomUUID().toString();
		InstalledVxF ivxftest = new InstalledVxF(uuid, "www.repoexample.com/repo/EVXFID/" + uuid);
		ivxftest.setInstalledVersion("1.0.0v");
		ivxftest.setName("NONMAE");
		ivxftest.setStatus(InstalledVxFStatus.INSTALLING);

		portalJpaControllerTest.saveInstalledVxF(ivxftest);
		// portalJpaControllerTest.getAll();

		InstalledVxF retIs = portalJpaControllerTest.readInstalledVxFByUUID(uuid);
		assertEquals(uuid, retIs.getUuid());
		assertEquals(InstalledVxFStatus.INSTALLING, retIs.getStatus());
		assertEquals("NONMAE", retIs.getName());
		assertEquals(1, portalJpaControllerTest.countInstalledVxFs());

		// second one with metadata
		uuid = UUID.randomUUID().toString();
		ivxftest = new InstalledVxF(uuid, "www.repoexample.com/repo/EVXFID/" + uuid);
		ivxftest.setInstalledVersion("1.0.0v");
		ivxftest.setName("NONMAE2");
		ivxftest.setStatus(InstalledVxFStatus.STARTING);
		ivxftest.setPackageLocalPath("packageLocalPath");
		ivxftest.setPackageURL("packageURL");

		portalJpaControllerTest.saveInstalledVxF(ivxftest);
		// portalJpaControllerTest.getAll();
		retIs = portalJpaControllerTest.readInstalledVxFByUUID(uuid);
		assertEquals(uuid, retIs.getUuid());
		assertEquals(InstalledVxFStatus.STARTING, retIs.getStatus());
		assertEquals("NONMAE2", retIs.getName());
		assertEquals("packageLocalPath", retIs.getPackageLocalPath() );
		assertEquals("packageURL", retIs.getPackageURL() );
		assertEquals(2, portalJpaControllerTest.countInstalledVxFs());

		// update it
		ivxftest.setStatus(InstalledVxFStatus.STARTED);
		portalJpaControllerTest.updateInstalledVxF(ivxftest);
		retIs = portalJpaControllerTest.readInstalledVxFByUUID(uuid);
		assertEquals(InstalledVxFStatus.STARTED, retIs.getStatus());
		// portalJpaControllerTest.getAll();
		assertEquals(2, portalJpaControllerTest.countInstalledVxFs());

	}

	

	

	// helper functions

	public PortalInstallationMgmt PortalServiceInit(MockRepositoryWebClient mockRepositoryWebClient, PortalJpaController portalJpaControllerTest2) {

		PortalInstallationMgmt bs = new PortalInstallationMgmt();
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));
		bs.setPortalJpaController(portalJpaControllerTest);
		return bs;
	}
}
