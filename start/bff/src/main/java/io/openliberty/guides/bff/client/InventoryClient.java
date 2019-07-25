package io.openliberty.guides.bff.client;

import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.openliberty.guides.bff.model.InventoryList;

@RegisterRestClient(baseUri = "http://inventory-service:9080")
@Path("/inventory")
public interface InventoryClient {

    @GET
    @Path("systems")
    @Produces(MediaType.APPLICATION_JSON)
    public InventoryList getInventory();

    @GET
    @Path("systems/{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    public Properties getProperties(@PathParam("hostname") String hostname);

}
