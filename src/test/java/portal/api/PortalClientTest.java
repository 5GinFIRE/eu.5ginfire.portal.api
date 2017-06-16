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

	/**
	 * This requests from Portal to INSTALL a VxF. Portal should bring it to STARTED status
	 */
	@Test
	public void testReqInstall_toSTARTEDStatus() {
		portalJpaControllerTest.deleteAllInstalledVxFs();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		// we don;t care about repo...we provide a local package hardcoded by MockRepositoryWebClient
		InstalledVxF is = bs.installVxFAndStart(uuid, "www.repoexample.com/repo/EVXFID/" + uuid);
		assertNotNull(is);
		assertEquals(1, bs.getManagedInstalledVxFs().size());
		assertEquals(is.getStatus(), InstalledVxFStatus.INIT);

		logger.info(" test service UUID=" + uuid + " . Now is: " + is.getStatus());

		int guard = 0;
		while ((is.getStatus() != InstalledVxFStatus.STARTED) && (is.getStatus() != InstalledVxFStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				
				guard++;
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

		InstalledVxF istest = bs.getVxF(uuid);
		assertNotNull(istest);
		assertEquals(uuid, istest.getUuid());
		assertEquals(is.getUuid(), istest.getUuid());
		assertEquals(InstalledVxFStatus.STARTED, istest.getStatus());
		assertEquals("www.repoexample.com/repo/EVXFID/" + uuid, istest.getRepoUrl());
		assertEquals("/files/examplevxf.tar.gz", istest.getPackageURL() );
		assertEquals("TemporaryServiceFromMockClass", istest.getName());
		assertEquals("1.0.0.test", istest.getInstalledVersion());
		assertEquals(1, bs.getManagedInstalledVxFs().size());

		portalJpaControllerTest.deleteAllInstalledVxFs();
	}

	/**
	 * This requests from Portal to INSTALL a VxF. Portal should bring it to STARTED status and then request to STOP it and then UNINSTALL
	 */
	@Test
	public void testReqInstall_toSTARTED_STOPPED_UNINSTALL_Status() {
		portalJpaControllerTest.deleteAllInstalledVxFs();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		InstalledVxF is = bs.installVxFAndStart(uuid, "www.repoexample.com/repo/EVXFID/" + uuid);

		int guard = 0;
		while ((is.getStatus() != InstalledVxFStatus.STARTED) && (is.getStatus() != InstalledVxFStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		assertEquals(InstalledVxFStatus.STARTED, is.getStatus());
		bs.stopVxF(uuid);

		guard = 0;
		while ((is.getStatus() != InstalledVxFStatus.STOPPED) && (guard <= 10)) {
			logger.info("Waiting for STOPPED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		InstalledVxF istest = bs.getVxF(uuid);
		assertEquals(InstalledVxFStatus.STOPPED, istest.getStatus());

		bs.uninstallVxF(uuid);
		guard = 0;
		while ((is.getStatus() != InstalledVxFStatus.UNINSTALLED) && (guard <= 10)) {
			logger.info("Waiting for UNINSTALLED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		istest = bs.getVxF(uuid);
		assertEquals(InstalledVxFStatus.UNINSTALLED, istest.getStatus());

		portalJpaControllerTest.deleteAllInstalledVxFs();
	}

	/**
	 * This requests from Portal to INSTALL a VxF. Portal should bring it to STARTED status and then request to UNINSTALL it. STOP should happen by default
	 */
	@Test
	public void testReqInstall_toSTARTED_and_UNINSTALL_Status() {
		portalJpaControllerTest.deleteAllInstalledVxFs();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		InstalledVxF is = bs.installVxFAndStart(uuid, "www.repoexample.com/repo/EVXFID/" + uuid);

		int guard = 0;
		while ((is.getStatus() != InstalledVxFStatus.STARTED) && (is.getStatus() != InstalledVxFStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		bs.uninstallVxF(uuid);
		guard = 0;
		while ((is.getStatus() != InstalledVxFStatus.UNINSTALLED) && (guard <= 10)) {
			logger.info("Waiting for UNINSTALLED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		InstalledVxF istest = bs.getVxF(uuid);
		assertEquals(InstalledVxFStatus.UNINSTALLED, istest.getStatus());

		portalJpaControllerTest.deleteAllInstalledVxFs();
	}

	/**
	 * This requests from Portal to INSTALL a VxF. Portal should bring it to STARTED status and then request to UNINSTALL it. STOP should happen by default
	 */
	@Test
	public void testReqInstall_toSTARTED_CONFIGURE_and_RESTART() {
		portalJpaControllerTest.deleteAllInstalledVxFs();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		InstalledVxF is = bs.installVxFAndStart(uuid, "www.repoexample.com/repo/EVXFID/" + uuid);

		int guard = 0;
		while ((is.getStatus() != InstalledVxFStatus.STARTED) && (is.getStatus() != InstalledVxFStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		assertEquals(InstalledVxFStatus.STARTED, is.getStatus());
		
		logger.info("===========================================================================");
		logger.info("Service STARTED UUID=" + uuid + " . Now will reconfigure and restart");

		bs.configureVxF(uuid);

		try {

			guard = 0;
			while ((is.getStatus() != InstalledVxFStatus.STOPPING) && (guard <= 50)) {
				logger.info("Waiting for STOPPED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
				Thread.sleep(200);
				guard++;
			}
			assertEquals(InstalledVxFStatus.STOPPING, is.getStatus());

			guard = 0;
			while ((is.getStatus() != InstalledVxFStatus.CONFIGURING) && (guard <= 50)) {
				logger.info("Waiting for CONFIGURING for test service UUID=" + uuid + " . Now is: " + is.getStatus());

				Thread.sleep(200);
				guard++;
			}
			assertEquals(InstalledVxFStatus.CONFIGURING, is.getStatus());

			guard = 0;
			while ((is.getStatus() != InstalledVxFStatus.STARTED) && (guard <= 20)) {
				logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());

				Thread.sleep(1000);
				guard++;
			}
			assertEquals(InstalledVxFStatus.STARTED, is.getStatus());

		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		logger.info("Service CONFIGURED and reSTARTED UUID=" + uuid + ". ");
		InstalledVxF istest = bs.getVxF(uuid); // check also DB
		assertEquals(InstalledVxFStatus.STARTED, istest.getStatus());

		portalJpaControllerTest.deleteAllInstalledVxFs();
	}

	/**
	 * This requests from Portal to INSTALL a VxF which contains an error on the onInstall recipe
	 */
	@Test
	public void testReqInstall_ErrScript() {
		portalJpaControllerTest.deleteAllInstalledVxFs();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));

		String uuid = UUID.randomUUID().toString();
		// we don;t care about repo...we provide a local package hardcoded by MockRepositoryWebClient
		InstalledVxF is = bs.installVxFAndStart(uuid, "www.repoexample.com/repo/EVXFERR/" + uuid);
		assertNotNull(is);
		assertEquals(1, bs.getManagedInstalledVxFs().size());
		assertEquals(is.getStatus(), InstalledVxFStatus.INIT);

		logger.info(" test service UUID=" + uuid + " . Now is: " + is.getStatus());

		int guard = 0;
		while ((is.getStatus() != InstalledVxFStatus.STARTED) && (is.getStatus() != InstalledVxFStatus.FAILED) && (guard <= 30)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

		InstalledVxF istest = bs.getVxF(uuid);
		assertNotNull(istest);
		assertEquals(uuid, istest.getUuid());
		assertEquals(is.getUuid(), istest.getUuid());
		assertEquals(InstalledVxFStatus.FAILED, istest.getStatus());
		assertEquals("www.repoexample.com/repo/EVXFERR/" + uuid, istest.getRepoUrl());
		assertEquals("(pending url)", istest.getPackageURL() );
		assertEquals("(pending)", istest.getName());
		assertNull(istest.getInstalledVersion());
		assertEquals(1, bs.getManagedInstalledVxFs().size());

		portalJpaControllerTest.deleteAllInstalledVxFs();
	}

	/**
	 * This requests from Portal to INSTALL a VxF. Portal should bring it to STARTED status. WE then destroy the portal service instance and create a new one. The
	 * VxF status should be there installed
	 */
	@Test
	public void testReqInstall_AndPersistence() {

		portalJpaControllerTest.deleteAllInstalledVxFs();
		PortalInstallationMgmt bs = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);

		String uuid = UUID.randomUUID().toString();
		// we don;t care about repo...we provide a local package hardcoded by MockRepositoryWebClient
		InstalledVxF is = bs.installVxFAndStart(uuid, "www.repoexample.com/repo/EVXFID/" + uuid);
		assertNotNull(is);
		assertEquals(1, bs.getManagedInstalledVxFs().size());
		assertEquals(is.getStatus(), InstalledVxFStatus.INIT);
		assertEquals(1, portalJpaControllerTest.countInstalledVxFs());

		// portalJpaControllerTest.getAll();

		int guard = 0;
		while ((is.getStatus() != InstalledVxFStatus.STARTED) && (is.getStatus() != InstalledVxFStatus.FAILED) && (guard <= 40)) {
			logger.info("Waiting for STARTED for test service UUID=" + uuid + " . Now is: " + is.getStatus());
			try {
				Thread.sleep(1000);
				guard++;
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}

		InstalledVxF istest = bs.getVxF(uuid);
		assertEquals(uuid, istest.getUuid());
		assertEquals(InstalledVxFStatus.STARTED, istest.getStatus());
		assertEquals(1, bs.getManagedInstalledVxFs().size());
		InstalledVxF retIs = portalJpaControllerTest.readInstalledVxFByUUID(istest.getUuid());
		assertEquals(InstalledVxFStatus.STARTED, retIs.getStatus());

		bs = null; // remove the old one

		// create new one..It should persist any installed service
		PortalInstallationMgmt bsNew = PortalServiceInit(new MockRepositoryWebClient("NORMAL"), portalJpaControllerTest);
		// portalJpaControllerTest.getAll();

		assertEquals("Persistence not implemented yet?!?", 1, bsNew.getManagedInstalledVxFs().size());// there should be one
		InstalledVxF istestNew = bsNew.getVxF(uuid); // req the service with the previous uuid
		assertEquals(uuid, istestNew.getUuid());
		assertEquals(InstalledVxFStatus.STARTED, istestNew.getStatus());

		portalJpaControllerTest.deleteAllInstalledVxFs();
	}

	// helper functions

	public PortalInstallationMgmt PortalServiceInit(MockRepositoryWebClient mockRepositoryWebClient, PortalJpaController portalJpaControllerTest2) {

		PortalInstallationMgmt bs = new PortalInstallationMgmt();
		bs.setRepoWebClient(new MockRepositoryWebClient("NORMAL"));
		bs.setPortalJpaController(portalJpaControllerTest);
		return bs;
	}
}
