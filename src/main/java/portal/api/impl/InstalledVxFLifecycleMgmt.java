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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import portal.api.model.VxFMetadata;
import portal.api.model.IRepositoryWebClient;
import portal.api.model.InstalledVxF;
import portal.api.model.InstalledVxFStatus;

public class InstalledVxFLifecycleMgmt {

	private static final transient Log logger = LogFactory.getLog(InstalledVxFLifecycleMgmt.class.getName());

	InstalledVxF installedVxF;
	IRepositoryWebClient repoWebClient;

	PortalJpaController portalJpaController;
	private InstalledVxFStatus targetStatus;
	private Boolean restartTriggered = false;
	private VxFMetadata vxfMetadata = null;

	public InstalledVxFLifecycleMgmt(InstalledVxF b, IRepositoryWebClient rwc, PortalJpaController jpactr, InstalledVxFStatus ts) {
		installedVxF = b;
		repoWebClient = rwc;
		portalJpaController = jpactr;
		targetStatus = ts;

		logger.info("ServiceLifecycleMgmt uuid:" + installedVxF.getUuid() + " name:" + installedVxF.getName());
		processState();
	}

	public void processState() {

		logger.info("Task for uuid:" + installedVxF.getUuid() + " is:" + installedVxF.getStatus());
		InstalledVxFStatus entryState = installedVxF.getStatus();
		
		//this is used to restart the service. usefull if reconfiguring.
		if ((targetStatus == InstalledVxFStatus.STARTED) &&
				(entryState == InstalledVxFStatus.STARTED ) &&
				(!restartTriggered) ){
			logger.info("Entry and Target state are STARTED. A restart will be triggered, with confguration applied.");
			restartTriggered = true;
		}else
			restartTriggered = false;


		switch (entryState) {
		case INIT:
			downLoadMetadataInfo();
			break;

		case DOWNLOADING:
			if (targetStatus == InstalledVxFStatus.STOPPED) {
				installedVxF.setStatus(InstalledVxFStatus.STOPPING);
			} else if (targetStatus == InstalledVxFStatus.UNINSTALLED) {
				installedVxF.setStatus(InstalledVxFStatus.STOPPING);
			}else{
				startPackageDownloading();
			}
			break;

		case DOWNLOADED:
			if (targetStatus == InstalledVxFStatus.STOPPED) {
				installedVxF.setStatus(InstalledVxFStatus.STOPPING);
			} else if (targetStatus == InstalledVxFStatus.UNINSTALLED) {
				installedVxF.setStatus(InstalledVxFStatus.STOPPING);
			}else{
				startPackageInstallation();
					
			}
			break;

		case INSTALLING:

			break;

		case INSTALLED:
			execInstalledPhase();
			break;

		case CONFIGURING:
			execConfiguringPhase();
			break;
		case STARTING:
			execStartingPhase();
			break;
		case STARTED:
			if (targetStatus == InstalledVxFStatus.STOPPED) {
				installedVxF.setStatus(InstalledVxFStatus.STOPPING);
			} else if (targetStatus == InstalledVxFStatus.UNINSTALLED) {
				installedVxF.setStatus(InstalledVxFStatus.STOPPING);
			} else if ( (targetStatus == InstalledVxFStatus.STARTED) && restartTriggered) {
				installedVxF.setStatus(InstalledVxFStatus.STOPPING);
			}

			break;
		case STOPPING:
			execStoppingPhase();

			break;

		case STOPPED:
			if (targetStatus == InstalledVxFStatus.UNINSTALLED) {
				installedVxF.setStatus(InstalledVxFStatus.UNINSTALLING);
			} else if (targetStatus == InstalledVxFStatus.STARTED) {
				installedVxF.setStatus(InstalledVxFStatus.CONFIGURING);
			}
			break;

		case UNINSTALLING:

			execUninstallingPhase();

			break;

		case UNINSTALLED:

			break;

		default:
			break;
		}

		portalJpaController.updateInstalledVxF(installedVxF);

		if ((targetStatus != installedVxF.getStatus()) && (installedVxF.getStatus() != InstalledVxFStatus.FAILED))
			processState();

	}

	private void downLoadMetadataInfo() {
		
		logger.info("Downloading metadata info...");
		
		vxfMetadata = null;
		if (repoWebClient != null)
			vxfMetadata = repoWebClient.fetchMetadata(installedVxF.getUuid(), installedVxF.getRepoUrl());
		else
			logger.info("repoWebClient == null...FAILED PLEASE CHECK");

		if (vxfMetadata != null) {
			installedVxF.setStatus(InstalledVxFStatus.DOWNLOADING);
		} else {
			logger.info("smetadata == null...FAILED");
			installedVxF.setStatus(InstalledVxFStatus.FAILED);
		}

	}

	private void startPackageDownloading() {
		logger.info("Downloading installation package: " + vxfMetadata.getPackageLocation() );

		Path destFile = repoWebClient.fetchPackageFromLocation(installedVxF.getUuid(), vxfMetadata.getPackageLocation());

		if ((destFile != null) && (extractPackage(destFile) == 0)) {
			installedVxF.setStatus(InstalledVxFStatus.DOWNLOADED);
			Path packageLocalPath = destFile.getParent();
			installedVxF.setPackageLocalPath(packageLocalPath.toString());
		} else {
			logger.info("FAILED Downloading installation package from: " + vxfMetadata.getPackageLocation());
			installedVxF.setStatus(InstalledVxFStatus.FAILED);
		}

	}

	public int extractPackage(Path targetPath) {
		String cmdStr = "tar --strip-components=1 -xvzf " + targetPath + " -C " + targetPath.getParent() + File.separator;
		return executeSystemCommand(cmdStr);
	}

	private void startPackageInstallation() {

		installedVxF.setStatus(InstalledVxFStatus.INSTALLING);
		logger.info("Installing...");

		String cmdStr = installedVxF.getPackageLocalPath() + "/recipes/onInstall";
		logger.info("Will execute recipe 'onInstall' of:" + cmdStr);

		if (executeSystemCommand(cmdStr) == 0) {

			installedVxF.setStatus(InstalledVxFStatus.INSTALLED);
		} else
			installedVxF.setStatus(InstalledVxFStatus.FAILED);

	}

	private void execInstalledPhase() {
		logger.info("execInstalledPhase...");
		String cmdStr = installedVxF.getPackageLocalPath() + "/recipes/onInstallFinish";
		logger.info("Will execute recipe 'onInstallFinish' of:" + cmdStr);

		
		executeSystemCommand(cmdStr); // we don't care for the exit code
		if (executeSystemCommand(cmdStr) == 0) {
			installedVxF.setStatus(InstalledVxFStatus.CONFIGURING);
			installedVxF.setName(vxfMetadata.getName());
			installedVxF.setPackageURL(vxfMetadata.getPackageLocation() );
			installedVxF.setInstalledVersion(vxfMetadata.getVersion());			
		} else
			installedVxF.setStatus(InstalledVxFStatus.FAILED);

	}

	private void execConfiguringPhase() {
		logger.info("execInstalledPhase...");
		String cmdStr = installedVxF.getPackageLocalPath() + "/recipes/onApplyConf";
		logger.info("Will execute recipe 'onApplyConf' of:" + cmdStr);

		executeSystemCommand(cmdStr); // we don't care for the exit code
		if (executeSystemCommand(cmdStr) == 0) {
			installedVxF.setStatus(InstalledVxFStatus.STARTING);
		} else
			installedVxF.setStatus(InstalledVxFStatus.FAILED);

	}

	private void execStartingPhase() {
		logger.info("execStartingPhase...");
		String cmdStr = installedVxF.getPackageLocalPath() + "/recipes/onStart";
		logger.info("Will execute recipe 'onStart' of:" + cmdStr);

		if (executeSystemCommand(cmdStr) == 0) {
			installedVxF.setStatus(InstalledVxFStatus.STARTED);
		} else
			installedVxF.setStatus(InstalledVxFStatus.STOPPED);

	}

	private void execStoppingPhase() {

		logger.info("execStoppingPhase...");
		String cmdStr = installedVxF.getPackageLocalPath() + "/recipes/onStop";
		logger.info("Will execute recipe 'onStop' of:" + cmdStr);

		// if (executeSystemCommand(cmdStr) == 0) {
		// whatever is the return value...it will go to stopped
		executeSystemCommand(cmdStr);
		installedVxF.setStatus(InstalledVxFStatus.STOPPED);

	}

	private void execUninstallingPhase() {

		logger.info("execUninstallingPhase...");
		String cmdStr = installedVxF.getPackageLocalPath() + "/recipes/onUninstall";
		logger.info("Will execute recipe 'onUninstall' of:" + cmdStr);

		// if (executeSystemCommand(cmdStr) == 0) {
		// whatever is the return value...it will go to stopped
		executeSystemCommand(cmdStr);
		installedVxF.setStatus(InstalledVxFStatus.UNINSTALLED);

	}

	public int executeSystemCommand(String cmdStr) {

		logger.info(" ================> Execute :" + cmdStr);

		CommandLine cmdLine = CommandLine.parse(cmdStr);
		final Executor executor = new DefaultExecutor();
		// create the executor and consider the exitValue '0' as success
		executor.setExitValue(0);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(out);
		executor.setStreamHandler(streamHandler);

		int exitValue = -1;
		try {
			exitValue = executor.execute(cmdLine);
			logger.info(" ================> EXIT ("+exitValue+") FROM :" + cmdStr);

		} catch (ExecuteException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		logger.info("out>" + out);

		return exitValue;

	}

}
