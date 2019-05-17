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

package portal.api.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import OSM4NBIClient.OSM4Client;
import ns.yang.nfvo.nsd.rev170228.nsd.catalog.Nsd;

public class NSInstantiateInstanceRequestPayload
{
	public String nsName;
	public String vimAccountId;
	public String nsdId;
	class VnF
	{
		@JsonProperty("member-vnf-index")
		public String memberVnFIndex;
		@JsonProperty("vimAccountId")
		public String vimAccount;
	}
	class Vld
	{
		public String name;
		@JsonProperty("vim-network-name")
		public LinkedHashMap<String,String> vimNetworkName = new LinkedHashMap<>();
	}
	public List<VnF> vnf = new ArrayList<>();
	//public List<Vld> vld = new ArrayList<>();
	
	public NSInstantiateInstanceRequestPayload(OSM4Client osm4client, DeploymentDescriptor deploymentdescriptor)
	{
		this.nsName = deploymentdescriptor.getName();
		this.vimAccountId = deploymentdescriptor.getInfrastructureForAll().getVIMid();
		// Here we need to get the ExperimentOnBoardDescriptor based on the Experiment.
		// An Experiment might have multiple OnBoardDescriptors if it is OnBoarded to multiple OSM MANOs.
		// We temporarily select the first (and most probably the only one). 
		// Otherwise the user needs to define the OSM MANO where the Experiment is OnBoarded in order to instantiate.
		this.nsdId = deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0).getDeployId();
		
		Integer count=1;
		for(DeploymentDescriptorVxFPlacement tmp : deploymentdescriptor.getVxfPlacements())
		{
			VnF vnf_tmp = new VnF();
			vnf_tmp.memberVnFIndex=count.toString();
			vnf_tmp.vimAccount = tmp.getInfrastructure().getVIMid();
			this.vnf.add(vnf_tmp);
			count++;
		}
//		// Here we need to define the VLDs. We need to get these from the NS Instance.
//		// Get the id of the NS.
//		//String nsd_instance_id = deploymentdescriptor.getInstanceId();
//		// Load the NSD
//		Nsd nsd_descriptor = osm4client.getNSDbyID(this.nsdId);		
//		// Get the VLDs
//		List<ns.yang.nfvo.nsd.rev170228.nsd.vld.Vld> vld_list = nsd_descriptor.getVld();
//		// For each VLD add the related VIM network.
//		for(ns.yang.nfvo.nsd.rev170228.nsd.vld.Vld currentVld : vld_list)
//		{
//			Vld vld_tmp = new Vld();
//			vld_tmp.name=currentVld.getName();
//			String vimNetworkName_tmp = "provider";
//			if(currentVld.getName().contains("_mgmt"))
//			{
//				vimNetworkName_tmp = "provider";
//			}
//			if(currentVld.getName().contains("_data"))
//			{
//				vimNetworkName_tmp = "provider2";
//			}				
////			
////			try {
////				if(currentVld.getName().contains("_mgmt") || currentVld.isMgmtNetwork())
////				{
////					vimNetworkName_tmp = "provider";
////				}
////				if(currentVld.getName().contains("_data") || !currentVld.isMgmtNetwork())
////				{
////					vimNetworkName_tmp = "provider2";
////				}				
////			}
////			catch(Exception e)
////			{
////				System.out.println("EXCEPTION"+e.getMessage());
////			}
//			
//			for(DeploymentDescriptorVxFPlacement tmp : deploymentdescriptor.getVxfPlacements())
//			{				
//				vld_tmp.vimNetworkName.put(tmp.getInfrastructure().getVIMid(), vimNetworkName_tmp);
//				System.out.println("Current VLD name: "+currentVld.getName()+",VIM: "+tmp.getInfrastructure().getVIMid()+", VIM network: "+vimNetworkName_tmp);
//			}						
//			this.vld.add(vld_tmp);
//		}
	}

	public NSInstantiateInstanceRequestPayload(DeploymentDescriptor deploymentdescriptor)
	{
		this.nsName = deploymentdescriptor.getName();
		this.vimAccountId = deploymentdescriptor.getInfrastructureForAll().getVIMid();
		// Here we need to get the ExperimentOnBoardDescriptor based on the Experiment.
		// An Experiment might have multiple OnBoardDescriptors if it is OnBoarded to multiple OSM MANOs.
		// We temporarily select the first (and most probably the only one). 
		// Otherwise the user needs to define the OSM MANO where the Experiment is OnBoarded in order to instantiate.
		this.nsdId = deploymentdescriptor.getExperimentFullDetails().getExperimentOnBoardDescriptors().get(0).getDeployId();
		
		Integer count=1;
		for(DeploymentDescriptorVxFPlacement tmp : deploymentdescriptor.getVxfPlacements())
		{
			VnF vnf_tmp = new VnF();
			vnf_tmp.memberVnFIndex=count.toString();
			vnf_tmp.vimAccount = tmp.getInfrastructure().getVIMid();
			this.vnf.add(vnf_tmp);
			count++;
		}
	}
	
	public String toJSON()
	{
		String jsonInString=null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			jsonInString = mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return jsonInString;
	}
}
