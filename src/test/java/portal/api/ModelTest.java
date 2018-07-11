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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import portal.api.model.MANOprovider;
import urn.ietf.params.xml.ns.yang.nfvo.nsd.rev141027.nsd.catalog.Nsd;


public class ModelTest {


	@Before
	public void deletePreviousobjectsDB() {


	}



	@Test
	public void testOSMNSD() {
		List<Nsd> nsds = getNSDs();
		assertEquals( 4, nsds.size() );
	}
	
	
	
	public List<Nsd> getNSDs() {

		//String response = getOSMResponse(BASE_SERVICE_URL + "/nsd-catalog/nsd");
		String response = "{\"nsd:nsd\":[{\"constituent-vnfd\" : [{\"member-vnf-index\" : 1,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"residentialGW_vnfd\"},{\"member-vnf-index\" : 2,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"router_5tonic_vnfd\"},{\"member-vnf-index\" : 3,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"videoServer_vnfd\"}],\"meta\" : \"{\\\"containerPositionMap\\\":{\\\"1\\\":{\\\"top\\\":345,\\\"left\\\":150,\\\"right\\\":400,\\\"bottom\\\":400,\\\"width\\\":250,\\\"height\\\":55},\\\"2\\\":{\\\"top\\\":225,\\\"left\\\":690,\\\"right\\\":940,\\\"bottom\\\":280,\\\"width\\\":250,\\\"height\\\":55},\\\"3\\\":{\\\"top\\\":225,\\\"left\\\":1065,\\\"right\\\":1315,\\\"bottom\\\":280,\\\"width\\\":250,\\\"height\\\":55},\\\"aa4324e0-92f0-483f-9e59-5afe017a2ce9\\\":{\\\"top\\\":30,\\\"left\\\":135,\\\"right\\\":385,\\\"bottom\\\":85,\\\"width\\\":250,\\\"height\\\":55},\\\"vld-1\\\":{\\\"top\\\":180,\\\"left\\\":240,\\\"right\\\":490,\\\"bottom\\\":218,\\\"width\\\":250,\\\"height\\\":38},\\\"uc3m_mgmt\\\":{\\\"top\\\":210,\\\"left\\\":135,\\\"right\\\":385,\\\"bottom\\\":248,\\\"width\\\":250,\\\"height\\\":38},\\\"vld-2\\\":{\\\"top\\\":300,\\\"left\\\":822.5,\\\"right\\\":1072.5,\\\"bottom\\\":338,\\\"width\\\":250,\\\"height\\\":38},\\\"inter_site\\\":{\\\"top\\\":390,\\\"left\\\":495,\\\"right\\\":745,\\\"bottom\\\":428,\\\"width\\\":250,\\\"height\\\":38},\\\"vld-3\\\":{\\\"top\\\":300,\\\"left\\\":1197.5,\\\"right\\\":1447.5,\\\"bottom\\\":338,\\\"width\\\":250,\\\"height\\\":38},\\\"uc3m_data\\\":{\\\"top\\\":510,\\\"left\\\":135,\\\"right\\\":385,\\\"bottom\\\":548,\\\"width\\\":250,\\\"height\\\":38},\\\"vld-4\\\":{\\\"top\\\":300,\\\"left\\\":1572.5,\\\"right\\\":1822.5,\\\"bottom\\\":338,\\\"width\\\":250,\\\"height\\\":38},\\\"5tonic_mgmr\\\":{\\\"top\\\":300,\\\"left\\\":1572.5,\\\"right\\\":1822.5,\\\"bottom\\\":338,\\\"width\\\":250,\\\"height\\\":38},\\\"5tonic_mgmt\\\":{\\\"top\\\":105,\\\"left\\\":855,\\\"right\\\":1105,\\\"bottom\\\":143,\\\"width\\\":250,\\\"height\\\":38},\\\"vld-5\\\":{\\\"top\\\":300,\\\"left\\\":1947.5,\\\"right\\\":2197.5,\\\"bottom\\\":338,\\\"width\\\":250,\\\"height\\\":38},\\\"5tonic_data\\\":{\\\"top\\\":390,\\\"left\\\":900,\\\"right\\\":1150,\\\"bottom\\\":428,\\\"width\\\":250,\\\"height\\\":38},\\\"vod_use_case_nsd\\\":{\\\"top\\\":30,\\\"left\\\":135,\\\"right\\\":385,\\\"bottom\\\":85,\\\"width\\\":250,\\\"height\\\":55}}}\",\"short-name\" : \"vod_use_case_nsd\",\"vld\" : [{\"vim-network-name\" : \"provider\",\"name\" : \"uc3m_mgmt\",\"vnfd-connection-point-ref\" : [{\"member-vnf-index-ref\" : 1,\"vnfd-id-ref\" : \"residentialGW_vnfd\",\"vnfd-connection-point-ref\" : \"eth0\"}],\"id\" : \"uc3m_mgmt\",\"mgmt-network\" : \"true\"},{\"vim-network-name\" : \"provider2\",\"name\" : \"inter_site\",\"vnfd-connection-point-ref\" : [{\"member-vnf-index-ref\" : 1,\"vnfd-id-ref\" : \"residentialGW_vnfd\",\"vnfd-connection-point-ref\" : \"eth1\"},{\"member-vnf-index-ref\" : 2,\"vnfd-id-ref\" : \"router_5tonic_vnfd\",\"vnfd-connection-point-ref\" : \"eth1\"}],\"id\" : \"inter_site\",\"mgmt-network\" : \"false\"},{\"vim-network-name\" : \"dataSite2\",\"name\" : \"uc3m_data\",\"vnfd-connection-point-ref\" : [{\"member-vnf-index-ref\" : 1,\"vnfd-id-ref\" : \"residentialGW_vnfd\",\"vnfd-connection-point-ref\" : \"eth2\"}],\"id\" : \"uc3m_data\",\"mgmt-network\" : \"false\"},{\"vim-network-name\" : \"provider\",\"name\" : \"5tonic_mgmt\",\"vnfd-connection-point-ref\" : [{\"member-vnf-index-ref\" : 2,\"vnfd-id-ref\" : \"router_5tonic_vnfd\",\"vnfd-connection-point-ref\" : \"eth0\"},{\"member-vnf-index-ref\" : 3,\"vnfd-id-ref\" : \"videoServer_vnfd\",\"vnfd-connection-point-ref\" : \"eth0\"}],\"id\" : \"5tonic_mgmt\",\"mgmt-network\" : \"true\"},{\"vim-network-name\" : \"data5tonic\",\"name\" : \"5tonic_data\",\"vnfd-connection-point-ref\" : [{\"member-vnf-index-ref\" : 2,\"vnfd-id-ref\" : \"router_5tonic_vnfd\",\"vnfd-connection-point-ref\" : \"eth2\"},{\"member-vnf-index-ref\" : 3,\"vnfd-id-ref\" : \"videoServer_vnfd\",\"vnfd-connection-point-ref\" : \"eth1\"}],\"id\" : \"5tonic_data\",\"mgmt-network\" : \"false\"}],\"name\" : \"vod_use_case_nsd\",\"id\" : \"vod_use_case_nsd\",\"logo\" : \"Video.png\"},{\"vendor\" : \"OSM\",\"description\" : \"Generated by OSM pacakage generator\",\"constituent-vnfd\" : [{\"member-vnf-index\" : 1,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"cirros_vnfd\"},{\"member-vnf-index\" : 2,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"cirros_vnfd\"}],\"vld\" : [{\"id\" : \"cirros_2vnf_nsd_vld1\",\"mgmt-network\" : \"true\",\"type\" : \"ELAN\",\"vim-network-name\" : \"provider\",\"name\" : \"cirros_2vnf_nsd_vld1\",\"vnfd-connection-point-ref\" : [{\"member-vnf-index-ref\" : 1,\"vnfd-id-ref\" : \"cirros_vnfd\",\"vnfd-connection-point-ref\" : \"eth0\"},{\"member-vnf-index-ref\" : 2,\"vnfd-id-ref\" : \"cirros_vnfd\",\"vnfd-connection-point-ref\" : \"eth0\"}],\"short-name\" : \"cirros_2vnf_nsd_vld1\"}],\"meta\" : \"{\\\"containerPositionMap\\\":{\\\"1\\\":{\\\"top\\\":130,\\\"left\\\":260,\\\"right\\\":510,\\\"bottom\\\":185,\\\"width\\\":250,\\\"height\\\":55},\\\"2\\\":{\\\"top\\\":130,\\\"left\\\":635,\\\"right\\\":885,\\\"bottom\\\":185,\\\"width\\\":250,\\\"height\\\":55},\\\"cirros_2vnf_nsd\\\":{\\\"top\\\":30,\\\"left\\\":135,\\\"right\\\":385,\\\"bottom\\\":85,\\\"width\\\":250,\\\"height\\\":55},\\\"cirros_2vnf_nsd_vld1\\\":{\\\"top\\\":300,\\\"left\\\":447.5,\\\"right\\\":697.5,\\\"bottom\\\":338,\\\"width\\\":250,\\\"height\\\":38}}}\",\"version\" : \"1.0\",\"short-name\" : \"cirros_2vnf_nsd\",\"name\" : \"cirros_2vnf_nsd\",\"id\" : \"cirros_2vnf_nsd\",\"logo\" : \"osm_2x.png\"},{\"description\" : \"RIFT.io sample ping pong network service\",\"constituent-vnfd\" : [{\"member-vnf-index\" : 1,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"rift_ping_vnf\"},{\"member-vnf-index\" : 2,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"rift_pong_vnf\"}],\"input-parameter-xpath\" : [{\"xpath\" : \"/nsd:nsd-catalog/nsd:nsd/nsd:vendor\"}],\"version\" : \"1.1\",\"id\" : \"rift_ping_pong_ns\",\"logo\" : \"rift_logo.png\",\"vendor\" : \"RIFT.io\",\"meta\" : \"{\\\"containerPositionMap\\\":{\\\"1\\\":{\\\"top\\\":130,\\\"left\\\":260,\\\"right\\\":510,\\\"bottom\\\":185,\\\"width\\\":250,\\\"height\\\":55},\\\"2\\\":{\\\"top\\\":130,\\\"left\\\":635,\\\"right\\\":885,\\\"bottom\\\":185,\\\"width\\\":250,\\\"height\\\":55},\\\"rift_ping_pong_ns\\\":{\\\"top\\\":30,\\\"left\\\":135,\\\"right\\\":385,\\\"bottom\\\":85,\\\"width\\\":250,\\\"height\\\":55},\\\"mgmt_vl\\\":{\\\"top\\\":300,\\\"left\\\":447.5,\\\"right\\\":697.5,\\\"bottom\\\":338,\\\"width\\\":250,\\\"height\\\":38},\\\"ping_pong_vl1\\\":{\\\"top\\\":300,\\\"left\\\":822.5,\\\"right\\\":1072.5,\\\"bottom\\\":338,\\\"width\\\":250,\\\"height\\\":38}}}\",\"placement-groups\" : [{\"requirement\" : \"Place this VM on the Kuiper belt object Orcus\",\"strategy\" : \"COLOCATION\",\"name\" : \"Orcus\",\"member-vnfd\" : [{\"member-vnf-index-ref\" : 1,\"vnfd-id-ref\" : \"rift_ping_vnf\"},{\"member-vnf-index-ref\" : 2,\"vnfd-id-ref\" : \"rift_pong_vnf\"}]},{\"requirement\" : \"Place this VM on the Kuiper belt object Quaoar\",\"strategy\" : \"COLOCATION\",\"name\" : \"Quaoar\",\"member-vnfd\" : [{\"member-vnf-index-ref\" : 1,\"vnfd-id-ref\" : \"rift_ping_vnf\"},{\"member-vnf-index-ref\" : 2,\"vnfd-id-ref\" : \"rift_pong_vnf\"}]}],\"initial-config-primitive\" : [{\"user-defined-script\" : \"start_traffic.py\",\"name\" : \"start traffic\",\"seq\" : 1,\"parameter\" : [{\"value\" : \"5555\",\"name\" : \"port\"},{\"value\" : \"fedora\",\"name\" : \"ssh-username\"},{\"value\" : \"fedora\",\"name\" : \"ssh-password\"}]}],\"vld\" : [{\"vendor\" : \"RIFT.io\",\"description\" : \"Management VL\",\"vnfd-connection-point-ref\" : [{\"member-vnf-index-ref\" : 1,\"vnfd-id-ref\" : \"rift_ping_vnf\",\"vnfd-connection-point-ref\" : \"ping_vnfd/cp0\"},{\"member-vnf-index-ref\" : 2,\"vnfd-id-ref\" : \"rift_pong_vnf\",\"vnfd-connection-point-ref\" : \"pong_vnfd/cp0\"}],\"version\" : \"1.0\",\"type\" : \"ELAN\",\"vim-network-name\" : \"provider\",\"mgmt-network\" : \"true\",\"name\" : \"mgmt_vl\",\"id\" : \"mgmt_vl\",\"short-name\" : \"mgmt_vl\"},{\"vendor\" : \"RIFT.io\",\"description\" : \"Data VL\",\"vnfd-connection-point-ref\" : [{\"member-vnf-index-ref\" : 1,\"vnfd-id-ref\" : \"rift_ping_vnf\",\"vnfd-connection-point-ref\" : \"ping_vnfd/cp1\"},{\"member-vnf-index-ref\" : 2,\"vnfd-id-ref\" : \"rift_pong_vnf\",\"vnfd-connection-point-ref\" : \"pong_vnfd/cp1\"}],\"version\" : \"1.0\",\"type\" : \"ELAN\",\"vim-network-name\" : \"provider2\",\"mgmt-network\" : \"false\",\"name\" : \"data_vl\",\"id\" : \"ping_pong_vl1\",\"short-name\" : \"data_vl\"}],\"name\" : \"ping_pong_ns\",\"ip-profiles\" : [{\"description\" : \"Inter VNF Link\",\"name\" : \"InterVNFLink\",\"ip-profile-params\":{\"dhcp-params\":{\"enabled\" : \"true\",\"start-address\" : \"31.31.31.2\",\"count\" : 200},\"subnet-address\" : \"31.31.31.0/24\",\"ip-version\" : \"ipv4\",\"gateway-address\" : \"31.31.31.210\"}}],\"short-name\" : \"ping_pong_ns\"},{\"vendor\" : \"CNIT\",\"description\" : \"SFC NS\",\"constituent-vnfd\" : [{\"member-vnf-index\" : 1,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"sfc_vnf\"},{\"member-vnf-index\" : 2,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"sfc_vnf\"},{\"member-vnf-index\" : 3,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"sfc_vnf\"},{\"member-vnf-index\" : 4,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"sfc_vnf\"},{\"member-vnf-index\" : 5,\"start-by-default\" : \"true\",\"vnfd-id-ref\" : \"sfc_vnf\"}],\"vld\" : [{\"vendor\" : \"CNIT\",\"description\" : \"SFC Network\",\"vnfd-connection-point-ref\" : [{\"member-vnf-index-ref\" : 1,\"vnfd-id-ref\" : \"sfc_vnf\",\"vnfd-connection-point-ref\" : \"sfc_vnfd/cp0\"},{\"member-vnf-index-ref\" : 2,\"vnfd-id-ref\" : \"sfc_vnf\",\"vnfd-connection-point-ref\" : \"sfc_vnfd/cp0\"},{\"member-vnf-index-ref\" : 3,\"vnfd-id-ref\" : \"sfc_vnf\",\"vnfd-connection-point-ref\" : \"sfc_vnfd/cp0\"},{\"member-vnf-index-ref\" : 4,\"vnfd-id-ref\" : \"sfc_vnf\",\"vnfd-connection-point-ref\" : \"sfc_vnfd/cp0\"},{\"member-vnf-index-ref\" : 5,\"vnfd-id-ref\" : \"sfc_vnf\",\"vnfd-connection-point-ref\" : \"sfc_vnfd/cp0\"}],\"version\" : \"1.0\",\"type\" : \"ELAN\",\"mgmt-network\" : \"false\",\"name\" : \"sfc_vl\",\"id\" : \"sfc_vl\",\"short-name\" : \"sfc_vl\"}],\"version\" : \"1.0\",\"short-name\" : \"sfc_ns\",\"name\" : \"sfc_ns\",\"id\" : \"sfc_ns\",\"logo\" : \"logo.png\"}]}\n" + 
				"";
		ObjectMapper mapper = new ObjectMapper(new JsonFactory());
		try {

			JsonNode tr = mapper.readTree(response).findValue("nsd:nsd");
			if (tr == null) {
				tr = mapper.readTree(response).findValue("nsd");
			}
			ArrayList<Nsd> nsds = new ArrayList<>();

			for (JsonNode jsonNode : tr) {
				Nsd nsd = mapper.readValue(jsonNode.toString(), Nsd.class);
				nsds.add(nsd);
			}

			return nsds;

		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}

		return null;
	}

}
