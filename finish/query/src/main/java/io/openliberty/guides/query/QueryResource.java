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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.openliberty.guides.query.client.InventoryClient;

@ApplicationScoped
@Path("/query")
public class QueryResource {
    
    @Inject
    @RestClient
    private InventoryClient inventoryClient;

    // tag::systemLoad[]
    @GET
    @Path("/systemLoad")
    @Produces(MediaType.APPLICATION_JSON)
    public Response systemLoad() {
        List<String> systems = inventoryClient.getSystems().readEntity(List.class);
        // tag::countdown[]
        CountDownLatch remainingSystems = new CountDownLatch(systems.size());
        // end::countdown[]
        final Holder<Map<String, Properties>> systemLoads = new Holder<Map<String, Properties>>();
        systemLoads.value = new ConcurrentHashMap<String, Properties>();

        for (String system : systems) {
            inventoryClient.getSystem(system)
                           // tag::thenAcceptAsync[]
                           .thenAcceptAsync(r -> {
                                Properties p = r.readEntity(Properties.class);
                                BigDecimal load = (BigDecimal) p.get("systemLoad");
                                if (systemLoads.value.containsKey("highest")) {
                                    BigDecimal highest = (BigDecimal) 
                                        systemLoads.value
                                                   .get("highest")
                                                   .get("systemLoad");
                                    if (load.compareTo(highest) > 0) {
                                        systemLoads.value.put("highest", p);
                                    }
                                } else {
                                    systemLoads.value.put("highest", p);
                                }
                                if (systemLoads.value.containsKey("lowest")) {
                                    BigDecimal lowest = (BigDecimal)
                                        systemLoads.value
                                                   .get("lowest")
                                                   .get("systemLoad");
                                    if (load.compareTo(lowest) < 0) {
                                        systemLoads.value.put("lowest", p);
                                    }
                                } else {
                                    systemLoads.value.put("lowest", p);
                                }
                                // tag::countdown[]
                                remainingSystems.countDown();
                                // end::countdown[]
                           })
                           // end::thenAcceptAsync[]
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
            // tag::await[]
            remainingSystems.await();
            // end::await[]
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return Response.status(Response.Status.OK)
                       .entity(systemLoads.value)
                       .build();
    }
    // end::systemLoad[]

    // tag::holder[]
    private class Holder<T> {
        @SuppressWarnings("unchecked")
        public volatile T value;
    }
    // end::holder[]
}
