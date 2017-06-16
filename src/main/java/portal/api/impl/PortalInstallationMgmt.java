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

package portal.api.impl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import portal.api.model.IRepositoryWebClient;
import portal.api.model.InstalledVxF;
import portal.api.model.InstalledVxFStatus;

public class PortalInstallationMgmt {

	private ConcurrentHashMap<String, InstalledVxF> managedInstalledVxFs;
	private IRepositoryWebClient repoWebClient;
	private PortalJpaController portalJpaController;

	private static final transient Log logger = LogFactory.getLog(PortalInstallationMgmt.class.getName());

	public PortalInstallationMgmt() {
		managedInstalledVxFs = new ConcurrentHashMap<>();
		// this.setRepoWebClient(new RepositoryWebClient());
	}

	public ConcurrentHashMap<String, InstalledVxF> getManagedInstalledVxFs() {
		return managedInstalledVxFs;
	}

	/**
	 * Add installed service object to ManagedServices list
	 * 
	 * @param s
	 *            InstalledVxF to add
	 * @return the same service
	 */
	private InstalledVxF addVxFToManagedVxFs(InstalledVxF s) {
		managedInstalledVxFs.put(s.getUuid(), s);


		return s;
	}

//	private Boolean removeServiceFromManagedServices(InstalledVxF s) {
//		InstalledVxF is = managedInstalledVxFs.remove(s.getUuid());
//		portalJpaController.delete(s);
//		return (is != null);
//	}

	/**
	 * Starts the installation of a vxf. If found already in local registry (managedInstalledVxF list) returns a ref to an existing instance if found. Otherwise
	 * starts a new installation.
	 * 
	 * @param reqVxFUUID
	 *            The uuid of the requested VxF
	 * @param vxfRepoURL
	 *            The endpoint of the repository
	 * @return an InstalledVxF object
	 */
	public InstalledVxF installVxFAndStart(String reqVxFUUID, String vxfRepoURL) {

		logger.info("reqVxFUUID= " + reqVxFUUID+", vxfRepoURL= "+ vxfRepoURL);
		InstalledVxF s = managedInstalledVxFs.get(reqVxFUUID); // return existing if
														// found
		if ((s != null) && (s.getStatus() != InstalledVxFStatus.FAILED)
				 && (s.getStatus() != InstalledVxFStatus.INIT)
				  && (s.getStatus() != InstalledVxFStatus.DOWNLOADING)
				  && (s.getStatus() != InstalledVxFStatus.DOWNLOADED)
				  && (s.getStatus() != InstalledVxFStatus.UNINSTALLED)) {
			return s;
		}

		logger.info("will start installation");

		if (s == null) {
			s = new InstalledVxF(reqVxFUUID, vxfRepoURL);
			addVxFToManagedVxFs(s);
			portalJpaController.saveInstalledVxF(s);
		} else if ((s.getStatus() == InstalledVxFStatus.FAILED) ||
				(s.getStatus() == InstalledVxFStatus.INIT) ||
				(s.getStatus() == InstalledVxFStatus.DOWNLOADING) ||
				(s.getStatus() == InstalledVxFStatus.DOWNLOADED) ||(s.getStatus() == InstalledVxFStatus.UNINSTALLED)) {

			logger.info("Will RESTART installation of existing " + s.getUuid() + ". HAD Status= " + s.getStatus());
			s.setStatus(InstalledVxFStatus.INIT); // restart installation
			s.setRepoUrl(vxfRepoURL);
		}

		processVxFLifecycleJob(s, this.portalJpaController, InstalledVxFStatus.STARTED);
		return s;
	}

	/**
	 * It executes the installation of the vxf in a thread job, following the vxf installation state resource
	 * 
	 * @param s
	 *            InstalledVxF object to manage the lifecycle
	 */
	private void processVxFLifecycleJob(final InstalledVxF s, final PortalJpaController jpsctr, final InstalledVxFStatus targetStatus) {

		logger.info("Creating new thread of " + s.getUuid() + " for target action = " + targetStatus);
		Thread t1 = new Thread(new Runnable() {
			public void run() {

				new InstalledVxFLifecycleMgmt(s, repoWebClient, jpsctr, targetStatus);

			}
		});
		t1.start();

		// Runnable run = new InstallationTask(s, repoWebClient);
		// Thread thread = new Thread(run);
		// thread.start();

	}

	public InstalledVxF getVxF(String uuid) {
		InstalledVxF is = managedInstalledVxFs.get(uuid);
		return is;
	}

	public IRepositoryWebClient getRepoWebClient() {
		return repoWebClient;
	}

	public void setRepoWebClient(IRepositoryWebClient repoWebClient) {
		this.repoWebClient = repoWebClient;
	}

	public PortalJpaController getPortalJpaController() {
		return portalJpaController;
	}

	public void setPortalJpaController(PortalJpaController b) {
		this.portalJpaController = b;

		this.portalJpaController = b;
		List<InstalledVxF> ls = b.readInstalledVxFs(0, 100000);

		for (InstalledVxF installedVxF : ls) {
			managedInstalledVxFs.put(installedVxF.getUuid(), installedVxF);
		}
	}

	public void stopVxF(String uuid) {
		InstalledVxF is = managedInstalledVxFs.get(uuid);

		if (is.getStatus() != InstalledVxFStatus.STARTED)
			return;

		logger.info("will stop service uuid= " + uuid);


		processVxFLifecycleJob(is, this.portalJpaController, InstalledVxFStatus.STOPPED);

	}
	
	public void startVxF(String uuid) {
		InstalledVxF is = managedInstalledVxFs.get(uuid);

		if (is.getStatus() != InstalledVxFStatus.STOPPED)
			return;

		logger.info("will start service uuid= " + uuid);


		processVxFLifecycleJob(is, this.portalJpaController, InstalledVxFStatus.STARTED);

	}

	public void uninstallVxF(String uuid) {
		InstalledVxF is = managedInstalledVxFs.get(uuid);

		logger.info("will uninstall vxf uuid= " + uuid);

		processVxFLifecycleJob(is, this.portalJpaController, InstalledVxFStatus.UNINSTALLED);

	}

	public void configureVxF(String uuid) {
		InstalledVxF is = managedInstalledVxFs.get(uuid);

		logger.info("will configure vxf uuid= " + uuid);

		processVxFLifecycleJob(is, this.portalJpaController, InstalledVxFStatus.STARTED);

	}

}
