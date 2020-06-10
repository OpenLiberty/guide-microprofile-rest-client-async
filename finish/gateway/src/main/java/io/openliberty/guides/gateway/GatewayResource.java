// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.openliberty.guides.gateway.client.InventoryClient;

@ApplicationScoped
@Path("/gateway")
public class GatewayResource {
    
    @Inject
    @RestClient
    private InventoryClient inventoryClient;

    @GET
    @Path("/systems")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystems() {
        return inventoryClient.getSystems();
    }

    @GET
    @Path("/systems/{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSystem(@PathParam("hostname") String hostname) {
        return inventoryClient.getSystem(hostname);
    }

    @PUT
    @Path("/data")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addProperties(List<String> propertyNames) {
        for (String propertyName : propertyNames)
            inventoryClient.addProperty(propertyName);
        return Response.status(Response.Status.OK)
               .entity("Request successful for " + propertyNames.size() + " properties\n")
               .build();
    }

    @GET
    @Path("/data/os")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOSProperties() {
        final String[] osProperties = new String[] {"os.name", "os.arch", "os.version"};
        final Holder<List<List<String>>> holder = new Holder<List<List<String>>>();
        CountDownLatch countdownLatch = new CountDownLatch(osProperties.length);

        for (String osProperty : osProperties) {
           inventoryClient.getProperty(osProperty).thenAcceptAsync(r->{
               holder.value.add(r);
               countdownLatch.countDown();
           });
        }
        
        // wait all asynchronous inventoryClient.getProperty to be completed
        try {
            countdownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
            
        return Response.status(Response.Status.OK)
                       .entity(holder.value)
                       .build();
    }

    private class Holder<T> {
        @SuppressWarnings("unchecked")
        public volatile T value = (T) new ArrayList<List<String>>();
    }
}
