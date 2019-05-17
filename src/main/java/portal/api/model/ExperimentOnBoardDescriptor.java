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

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author ctranoris
 * maintains information and status of an Experiment (in terms of NSD currently) on which MANO providers is on-boarded
 * see https://github.com/5GinFIRE/eu.5ginfire.portal.api/issues/7 
 */
@Entity(name = "ExperimentOnBoardDescriptor")
@JsonIgnoreProperties(value = { "experiment" })

public class ExperimentOnBoardDescriptor extends OnBoardDescriptor{

		

	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.EAGER)
	@JoinColumns({ @JoinColumn() })
	private ExperimentMetadata experiment;
		
	
	private String experimentMANOProviderID;	
	
	private long tempExperimentID;
	
	public ExperimentOnBoardDescriptor() {		
	}
	
	public ExperimentOnBoardDescriptor(ExperimentMetadata v) {
		this.experiment = v;
	}

	

	public ExperimentMetadata getExperiment() {
		return experiment;
	}

	public void setExperiment(ExperimentMetadata e) {
		this.experiment = e;
	}
	
		
	public long getExperimentid() {
		if ( experiment != null ) {
			return experiment.getId();
		}else {
			return this.tempExperimentID;
		}
	}

	public void setExperimentid(long e) {
		this.tempExperimentID = e; 
	}

	public String getExperimentMANOProviderID() {
		return experimentMANOProviderID;
	}

	public void setExperimentMANOProviderID(String experimentMANOProviderID) {
		this.experimentMANOProviderID = experimentMANOProviderID;
	}

}
