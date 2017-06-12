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
import portal.api.model.BunMetadata;
import portal.api.model.InstalledBun;
import portal.api.model.InstalledBunStatus;
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
		assertNotNull(bs.getManagedInstalledBuns());
		logger.info("	 	>>>>	portalJpaControllerTest = " + portalJpaControllerTest);
	}

	@Test
	public void testWriteReadDB() {

		portalJpaControllerTest.deleteAllInstalledBuns();

		String uuid = UUID.randomUUID().toString();
		InstalledBun ibuntest = new InstalledBun(uuid, "www.repoexample.com/repo/EBUNID/" + uuid);
		ibuntest.setInstalledVersion("1.0.0v");
		ibuntest.setName("NONMAE");
		ibuntest.setStatus(InstalledBunStatus.INSTALLING);

		portalJpaControllerTest.saveInstalledBun(ibuntest);
		// portalJpaControllerTest.getAll();

		InstalledBun retIs = portalJpaControllerTest.readInstalledBunByUUID(uuid);
		assertEquals(uuid, retIs.getUuid());
		assertEquals(InstalledBunStatus.INSTALLING, retIs.getStatus());
		assertEquals("NONMAE", retIs.getName());
		assertEquals(1, portalJpaControllerTest.countInstalledBuns());

		// second one with metadata
		uuid = UUID.randomUUID().toString();
		ibuntest = new InstalledBun(uuid, "www.repoexample.com/repo/EBUNID/" + uuid);
		ibuntest.setInstalledVersion("1.0.0v");
		ibuntest.setName("NONMAE2");
		ibuntest.setStatus(InstalledBunStatus.STARTING);
		ibuntest.setPackageLocalPath("packageLocalPath");
		ibuntest.setPackageURL("packageURL");

		portalJpaControllerTest.saveInstalledBun(ibuntest);
		// portalJpaControllerTest.getAll();
		retIs = portalJpaControllerTest.readInstalledBunByUUID(uuid);
		assertEquals(uuid, retIs.getUuid());
		assertEquals(InstalledBunStatus.STARTING, retIs.getStatus());
		assertEquals("NONMAE2", retIs.getName());
		assertEquals("packageLocalPath", retIs.getPackageLocalPath() );
		assertEquals("packageURL", retIs.getPackageURL() );
		assertEquals(2, portalJpaControllerTest.countInstalledBuns());

		// update it
		ibuntest.setStatus(InstalledBunStatus.STARTED);
		portalJpaControllerTest.updateInstalledBun(ibuntest);
		retIs = portalJpaControllerTest.readInstalledBunByUUID(uuid);
		assertEquals(InstalledBunStatus.STARTED, retIs.getStatus());
		// portalJpaControllerTest.getAll();
		assertEquals(2, portalJpaControllerTest.countInstalledBuns());

	}

	/**
	 * This requests from Portal to INSTALL a Bun. Portal should bring it to STARTED status
	 */
	@Test
	public void testReqInstall_toSTARTEDStatus() {
		portalJpaControllerTest.deleteAllInstalledBuns();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		// we don;t care about repo...we provide a local package hardcoded by MockRepositoryWebClient
		InstalledBun is = bs.installBunAndStart(uuid, "www.repoexample.com/repo/EBUNID/" + uuid);
		assertNotNull(is);
		assertEquals(1, bs.getManagedInstalledBuns().size());
		assertEquals(is.getStatus(), InstalledBunStatus.INIT);

		logger.info(" test service UUID=" + uuid + " . Now is: " + is.getStatus());

		int guard = 0;
		while ((is.getStatus() != InstalledBunStatus.STARTED) && (is.getStatus() != InstalledBunStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				
				guard++;
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

		InstalledBun istest = bs.getBun(uuid);
		assertNotNull(istest);
		assertEquals(uuid, istest.getUuid());
		assertEquals(is.getUuid(), istest.getUuid());
		assertEquals(InstalledBunStatus.STARTED, istest.getStatus());
		assertEquals("www.repoexample.com/repo/EBUNID/" + uuid, istest.getRepoUrl());
		assertEquals("/files/examplebun.tar.gz", istest.getPackageURL() );
		assertEquals("TemporaryServiceFromMockClass", istest.getName());
		assertEquals("1.0.0.test", istest.getInstalledVersion());
		assertEquals(1, bs.getManagedInstalledBuns().size());

		portalJpaControllerTest.deleteAllInstalledBuns();
	}

	/**
	 * This requests from Portal to INSTALL a Bun. Portal should bring it to STARTED status and then request to STOP it and then UNINSTALL
	 */
	@Test
	public void testReqInstall_toSTARTED_STOPPED_UNINSTALL_Status() {
		portalJpaControllerTest.deleteAllInstalledBuns();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		InstalledBun is = bs.installBunAndStart(uuid, "www.repoexample.com/repo/EBUNID/" + uuid);

		int guard = 0;
		while ((is.getStatus() != InstalledBunStatus.STARTED) && (is.getStatus() != InstalledBunStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(InstalledBunStatus.STARTED, is.getStatus());
		bs.stopBun(uuid);

		guard = 0;
		while ((is.getStatus() != InstalledBunStatus.STOPPED) && (guard <= 10)) {
			logger.info("Waiting for STOPPED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		InstalledBun istest = bs.getBun(uuid);
		assertEquals(InstalledBunStatus.STOPPED, istest.getStatus());

		bs.uninstallBun(uuid);
		guard = 0;
		while ((is.getStatus() != InstalledBunStatus.UNINSTALLED) && (guard <= 10)) {
			logger.info("Waiting for UNINSTALLED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		istest = bs.getBun(uuid);
		assertEquals(InstalledBunStatus.UNINSTALLED, istest.getStatus());

		portalJpaControllerTest.deleteAllInstalledBuns();
	}

	/**
	 * This requests from Portal to INSTALL a Bun. Portal should bring it to STARTED status and then request to UNINSTALL it. STOP should happen by default
	 */
	@Test
	public void testReqInstall_toSTARTED_and_UNINSTALL_Status() {
		portalJpaControllerTest.deleteAllInstalledBuns();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		InstalledBun is = bs.installBunAndStart(uuid, "www.repoexample.com/repo/EBUNID/" + uuid);

		int guard = 0;
		while ((is.getStatus() != InstalledBunStatus.STARTED) && (is.getStatus() != InstalledBunStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		bs.uninstallBun(uuid);
		guard = 0;
		while ((is.getStatus() != InstalledBunStatus.UNINSTALLED) && (guard <= 10)) {
			logger.info("Waiting for UNINSTALLED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		InstalledBun istest = bs.getBun(uuid);
		assertEquals(InstalledBunStatus.UNINSTALLED, istest.getStatus());

		portalJpaControllerTest.deleteAllInstalledBuns();
	}

	/**
	 * This requests from Portal to INSTALL a Bun. Portal should bring it to STARTED status and then request to UNINSTALL it. STOP should happen by default
	 */
	@Test
	public void testReqInstall_toSTARTED_CONFIGURE_and_RESTART() {
		portalJpaControllerTest.deleteAllInstalledBuns();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		InstalledBun is = bs.installBunAndStart(uuid, "www.repoexample.com/repo/EBUNID/" + uuid);

		int guard = 0;
		while ((is.getStatus() != InstalledBunStatus.STARTED) && (is.getStatus() != InstalledBunStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		assertEquals(InstalledBunStatus.STARTED, is.getStatus());
		
		logger.info("===========================================================================");
		logger.info("Service STARTED UUID=" + uuid + " . Now will reconfigure and restart");

		bs.configureBun(uuid);

		try {

			guard = 0;
			while ((is.getStatus() != InstalledBunStatus.STOPPING) && (guard <= 50)) {
				logger.info("Waiting for STOPPED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
				Thread.sleep(200);
				guard++;
			}
			assertEquals(InstalledBunStatus.STOPPING, is.getStatus());

			guard = 0;
			while ((is.getStatus() != InstalledBunStatus.CONFIGURING) && (guard <= 50)) {
				logger.info("Waiting for CONFIGURING for test service UUID=" + uuid + " . Now is: " + is.getStatus());

				Thread.sleep(200);
				guard++;
			}
			assertEquals(InstalledBunStatus.CONFIGURING, is.getStatus());

			guard = 0;
			while ((is.getStatus() != InstalledBunStatus.STARTED) && (guard <= 20)) {
				logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());

				Thread.sleep(1000);
				guard++;
			}
			assertEquals(InstalledBunStatus.STARTED, is.getStatus());

		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		logger.info("Service CONFIGURED and reSTARTED UUID=" + uuid + ". ");
		InstalledBun istest = bs.getBun(uuid); // check also DB
		assertEquals(InstalledBunStatus.STARTED, istest.getStatus());

		portalJpaControllerTest.deleteAllInstalledBuns();
	}

	/**
	 * This requests from Portal to INSTALL a Bun which contains an error on the onInstall recipe
	 */
	@Test
	public void testReqInstall_ErrScript() {
		portalJpaControllerTest.deleteAllInstalledBuns();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		// we don;t care about repo...we provide a local package hardcoded by MockRepositoryWebClient
		InstalledBun is = bs.installBunAndStart(uuid, "www.repoexample.com/repo/EBUNERR/" + uuid);
		assertNotNull(is);
		assertEquals(1, bs.getManagedInstalledBuns().size());
		assertEquals(is.getStatus(), InstalledBunStatus.INIT);

		logger.info(" test service UUID=" + uuid + " . Now is: " + is.getStatus());

		int guard = 0;
		while ((is.getStatus() != InstalledBunStatus.STARTED) && (is.getStatus() != InstalledBunStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

		InstalledBun istest = bs.getBun(uuid);
		assertNotNull(istest);
		assertEquals(uuid, istest.getUuid());
		assertEquals(is.getUuid(), istest.getUuid());
		assertEquals(InstalledBunStatus.FAILED, istest.getStatus());
		assertEquals("www.repoexample.com/repo/EBUNERR/" + uuid, istest.getRepoUrl());
		assertEquals("(pending url)", istest.getPackageURL() );
		assertEquals("(pending)", istest.getName());
		assertNull(istest.getInstalledVersion());
		assertEquals(1, bs.getManagedInstalledBuns().size());

		portalJpaControllerTest.deleteAllInstalledBuns();
	}

	/**
	 * This requests from Portal to INSTALL a Bun. Portal should bring it to STARTED status. WE then destroy the portal service instance and create a new one. The
	 * Bun status should be there installed
	 */
	@Test
	public void testReqInstall_AndPersistence() {

		portalJpaControllerTest.deleteAllInstalledBuns();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);

		String uuid = UUID.randomUUID().toString();
		// we don;t care about repo...we provide a local package hardcoded by MockRepositoryWebClient
		InstalledBun is = bs.installBunAndStart(uuid, "www.repoexample.com/repo/EBUNID/" + uuid);
		assertNotNull(is);
		assertEquals(1, bs.getManagedInstalledBuns().size());
		assertEquals(is.getStatus(), InstalledBunStatus.INIT);
		assertEquals(1, portalJpaControllerTest.countInstalledBuns());

		// portalJpaControllerTest.getAll();

		int guard = 0;
		while ((is.getStatus() != InstalledBunStatus.STARTED) && (is.getStatus() != InstalledBunStatus.FAILED) && (guard <= 40)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

		InstalledBun istest = bs.getBun(uuid);
		assertEquals(uuid, istest.getUuid());
		assertEquals(InstalledBunStatus.STARTED, istest.getStatus());
		assertEquals(1, bs.getManagedInstalledBuns().size());
		InstalledBun retIs = portalJpaControllerTest.readInstalledBunByUUID(istest.getUuid());
		assertEquals(InstalledBunStatus.STARTED, retIs.getStatus());

		bs = null; // remove the old one

		// create new one..It should persist any installed service
		PortalInstallationMgmt bsNew = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		// portalJpaControllerTest.getAll();

		assertEquals("Persistence not implemented yet?!?", 1, bsNew.getManagedInstalledBuns().size());// there should be one
		InstalledBun istestNew = bsNew.getBun(uuid); // req the service with the previous uuid
		assertEquals(uuid, istestNew.getUuid());
		assertEquals(InstalledBunStatus.STARTED, istestNew.getStatus());

		portalJpaControllerTest.deleteAllInstalledBuns();
	}

	// helper functions

	public PortalInstallationMgmt PortalServiceInit(MockRepositoryWebClient mockRepositoryWebClient, PortalJpaController portalJpaControllerTest2) {

		PortalInstallationMgmt bs = new PortalInstallationMgmt();
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));
		bs.setPortalJpaController(portalJpaControllerTest);
		return bs;
	}
}
