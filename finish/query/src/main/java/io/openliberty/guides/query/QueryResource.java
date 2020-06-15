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
package io.openliberty.guides.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
@Path("/query")
public class QueryResource {
    
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
        return inventoryClient.getSystem(hostname)
                              .thenAcceptAsync(r -> {
                                  return r;
                              })
                              .exceptionally(ex -> {
                                  return null;
                              });
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
    @PATH("/systemLoad")
    @Produces(MediaType.APPLICATION_JSON)
    public Response systemLoad() {
        List<String> systems = inventoryClient.getSystems();
        // tag::countdown[]
        CountDownLatch remainingSystems = new CountdownLatch(systems.length);
        // end::countdown[]
        volatile Map<Map<String, Properties> systemLoads = new ConcurrentHashMap(2);

        for (String system : systems) {
            inventoryClient.getSystem(system)
                           // tag::thenApplyAsync[]
                           .thenApplyAsync(r -> {
                                Properties p = r.readEntity(Properties.class());
                                if (systemLoads.containsKey("highest")) {
                                    if (systemLoads.get("highest")
                                                    .getProperty("systemLoad") 
                                                    < p.getProperty("systemLoad")) {
                                        systemLoads.put("highest", p);
                                    }
                                } else {
                                    systemLoads.put("highest", p);
                                }
                                if (systemLoads.containsKey("lowest")) {
                                    if (systemLoads.get("lowest")
                                                    .getProperty("systemLoad") 
                                                    > p.getProperty("systemLoad")) {
                                        systemLoads.put("lowest", p);
                                    }
                                } else {
                                    systemLoads.put("lowest", p);
                                }
                                // tag::countdown[]
                                remainingSystems.countDown();
                                // end::countdown[]
                           })
                           // end::thenApplyAsync[]
                           // tag::exceptionally[]
                           .exceptionally(ex -> {
                                // tag::countdown[]
                                remainingSystems.countDown();
                                // end::countdown[]
                                return null;
                           });
                           // end::exceptionally[]
        }

        // Wait for all remaining systems to be checked
        try {
            remainingSystems.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return Response.status(Response.Status.OK)
                       .entity(systemLoads)
                       .build();
    }

    @GET
    @Path("/data/os")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOSProperties() {
        final String[] osProperties = new String[] {"os.name", "os.arch", "os.version"};
        final Holder<List<List<String>>> holder = new Holder<List<List<String>>>();
        // tag::countdown[]
        CountDownLatch countdownLatch = new CountDownLatch(osProperties.length);
        // end::countdown[]

        for (String osProperty : osProperties) {
            inventoryClient
                .getProperty(osProperty)
                // tag::thenApplyAsync[]
                .thenAcceptAsync(r->{
                    holder.value.add(r);
                    // tag::countdown[]
                    countdownLatch.countDown();
                    // end::countdown[]
                })
                // end::thenApplyAsync[]
                // tag::exceptionally[]
                .exceptionally(ex -> {
                    // tag::countdown[]
                    countdownLatch.countDown();
                    // end::countdown[]
                    return null;
                });
                // end::exceptionally[]
        }
        
        // tag::countdown[]
        // Wait for all asynchronous inventoryClient.getProperty to be completed
        try {
            countdownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // end::countdown[]
            
        return Response.status(Response.Status.OK)
                       .entity(holder.value)
                       .build();
    }

    // tag::holder[]
    private class Holder<T> {
        @SuppressWarnings("unchecked")
        public volatile T value = (T) new ArrayList<List<String>>();
    }
    // end::holder[]
}
