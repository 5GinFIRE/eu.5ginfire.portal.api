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

package portal.api.mano;
import java.util.UUID;
import portal.api.bus.BusController;

public class MANOStatus
{
	//1: Active, 0:Failed
	private static Status osm4CommunicationStatus = Status.Active;
	private static String osm4CommunicationStatusUUID = null;	
	private static String message;
	public static enum Status {Failed,Active};
	
	public static String getMessage() {
		return message;
	}

	public static void setMessage(String message) {
		MANOStatus.message = message;
	}

	public static Status getOsm4CommunicationStatus() {
		return osm4CommunicationStatus;
	}
	
	public static String getOsm4CommunicationStatusUUID() {
		return osm4CommunicationStatusUUID;
	}
	
	public static void setOsm4CommunicationStatusUUID(String osm4CommunicationStatusUUID) {
		MANOStatus.osm4CommunicationStatusUUID = osm4CommunicationStatusUUID;
	}
	
	public static void setOsm4CommunicationStatusFailed(String message) {
		if(message == null)
		{
			message="";
		}
		if(MANOStatus.osm4CommunicationStatus == Status.Active)
		{			
			MANOStatus.osm4CommunicationStatus = Status.Failed ;
			MANOStatus.setMessage("OSM4 communication failed." + message);
			MANOStatus.setOsm4CommunicationStatusUUID(UUID.randomUUID().toString());
			System.out.println("Inside setOSM4CommunicationStatusFailed. "+MANOStatus.getOsm4CommunicationStatusUUID().toString()+","+MANOStatus.getMessage().toString());
			BusController.getInstance().osm4CommunicationFailed(MANOStatus.class);					
		}
	}
	
	public static void setOsm4CommunicationStatusActive(String message)
	{
		if(message == null)
		{
			message="";
		}
		if(MANOStatus.osm4CommunicationStatus == Status.Failed)
		{
			MANOStatus.osm4CommunicationStatus = Status.Active ;
			MANOStatus.setMessage("OSM4 communication restored." + message);
			BusController.getInstance().osm4CommunicationRestored(MANOStatus.class);					
		}		
	}
}

