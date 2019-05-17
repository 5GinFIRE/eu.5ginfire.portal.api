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

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author ctranoris
 * maintains information and status of a VNF or NSD on which MANO providers is on-boarded
 * see https://github.com/5GinFIRE/eu.5ginfire.portal.api/issues/10 
 */
@Entity(name = "OnBoardDescriptor")
@JsonIgnoreProperties(value = { "vxf" })

public class OnBoardDescriptor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id = 0;
	
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@JoinColumns({ @JoinColumn() })
	private MANOprovider obMANOprovider;

	private  OnBoardingStatus onBoardingStatus = OnBoardingStatus.UNKNOWN;
	
	private Date lastOnboarding;
	
	private String deployId = "(N/A)";
	
	@Lob
	@Column(name = "LDETAILEDSTATUS", columnDefinition = "LONGTEXT")	
	private String feedbackMessage;

	@Basic()
	private String uuid = null;

	/**
	 * The name is a little bit misleading. However,
	 * this field keeps the ID of a VxF or NSD as it is on the OSM TWO. In future OSM versions this perhaps
	 * will be replaced by an ID
	 */
	
	private String vxfMANOProviderID;	
	

	public OnBoardDescriptor() {		
	}
	
	
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public MANOprovider getObMANOprovider() {
		return obMANOprovider;
	}

	public void setObMANOprovider(MANOprovider obMANOprovider) {
		this.obMANOprovider = obMANOprovider;
	}

	
	public OnBoardingStatus getOnBoardingStatus() {
		return onBoardingStatus;
	}

	public void setOnBoardingStatus(OnBoardingStatus onBoardingStatus) {
		this.onBoardingStatus = onBoardingStatus;
	}

	public Date getLastOnboarding() {
		return lastOnboarding;
	}

	public void setLastOnboarding(Date lastOnboarding) {
		this.lastOnboarding = lastOnboarding;
	}

	public String getDeployId() {
		return deployId;
	}

	public void setDeployId(String deployId) {
		this.deployId = deployId;
	}
		
	

	public String getVxfMANOProviderID() {
		return vxfMANOProviderID;
	}

	public void setVxfMANOProviderID(String vxfMANOProviderID) {
		this.vxfMANOProviderID = vxfMANOProviderID;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the feedbackMessage
	 */
	public String getFeedbackMessage() {
		return feedbackMessage;
	}

	/**
	 * @param feedbackMessage the feedbackMessage to set
	 */
	public void setFeedbackMessage(String feedbackMessage) {
		this.feedbackMessage = feedbackMessage;
	}	
}
