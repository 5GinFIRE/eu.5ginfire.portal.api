package portal.api.model;

import OSM4NBIClient.OSM4Client;

public class NSCreateInstanceRequestPayload extends NSInstantiateInstanceRequestPayload
{	
	public String notificationType="NsIdentifierCreationNotification";
	
	public NSCreateInstanceRequestPayload(OSM4Client osm4client, DeploymentDescriptor deploymentdescriptor)
	{
		super(osm4client, deploymentdescriptor);
	}	
}

