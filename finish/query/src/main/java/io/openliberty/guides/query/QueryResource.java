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
    public Map<String, Properties> systemLoad() {
        // tag::getSystems[]
        List<String> systems = inventoryClient.getSystems();
        // end::getSystems[]
        // tag::countdown1[]
        CountDownLatch remainingSystems = new CountDownLatch(systems.size());
        // end::countdown1[]
        final Holder systemLoads = new Holder();

        for (String system : systems) {
            // tag::getSystem[]
            inventoryClient.getSystem(system)
            // end::getSystem[]
                           // tag::thenAcceptAsync[]
                           .thenAcceptAsync(p -> {
                                if (p != null) {
                                    systemLoads.updateHighest(p);
                                    systemLoads.updateLowest(p);
                                }
                                // tag::countdown2[]
                                remainingSystems.countDown();
                                // end::countdown2[]
                           })
                           // end::thenAcceptAsync[]
                           // tag::exceptionally[]
                           .exceptionally(ex -> {
                                // tag::countdown3[]
                                remainingSystems.countDown();
                                // end::countdown3[]
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

        return systemLoads.values;
    }
    // end::systemLoad[]

    private class Holder {
        // tag::volatile[]
        public volatile Map<String, Properties> values;
        // end::volatile[]

        public Holder() {
            // tag::concurrentHashMap[]
            this.values = new ConcurrentHashMap<String, Properties>();
            // end::concurrentHashMap[]
            
            // Initialize highest and lowest values
            this.values.put("highest", new Properties());
            this.values.put("lowest", new Properties());
            this.values.get("highest").put("hostname", "temp_max");
            this.values.get("lowest").put("hostname", "temp_min");
            this.values.get("highest").put("systemLoad", new BigDecimal(Double.MIN_VALUE));
            this.values.get("lowest").put("systemLoad", new BigDecimal(Double.MAX_VALUE));
        }

        public void updateHighest(Properties p) {
            BigDecimal load = (BigDecimal) p.get("systemLoad");
            BigDecimal highest = (BigDecimal) this.values
                                                  .get("highest")
                                                  .get("systemLoad");
            if (load.compareTo(highest) > 0) {
                this.values.put("highest", p);
            }
        }

        public void updateLowest(Properties p) {
            BigDecimal load = (BigDecimal) p.get("systemLoad");
            BigDecimal lowest = (BigDecimal) this.values
                                                 .get("lowest")
                                                 .get("systemLoad");
            if (load.compareTo(lowest) < 0) {
                this.values.put("lowest", p);
            }
        }
    }
}
