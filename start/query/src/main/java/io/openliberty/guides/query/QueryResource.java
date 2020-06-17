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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
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

    @GET
    @Path("/systemLoad")
    @Produces(MediaType.APPLICATION_JSON)
    public Response systemLoad() {
        List<String> systems = inventoryClient.getSystems().readEntity(List.class);
        Map<String, Properties> systemLoads = new HashMap<String, Properties>();

        for (String system : systems) {
            Properties p = inventoryClient.getSystem(system)
                                          .readEntity(Properties.class);
            BigDecimal load = (BigDecimal) p.get("systemLoad");

            if (systemLoads.containsKey("highest")) {
                BigDecimal highest = (BigDecimal) 
                    systemLoads.get("highest")
                               .get("systemLoad");
                if (load.compareTo(highest) > 0) {
                    systemLoads.put("highest", p);
                }
            } else {
                systemLoads.put("highest", p);
            }
            if (systemLoads.containsKey("lowest")) {
                BigDecimal lowest = (BigDecimal)
                    systemLoads.get("lowest")
                               .get("systemLoad");
                if (load.compareTo(lowest) < 0) {
                    systemLoads.put("lowest", p);
                }
            } else {
                systemLoads.put("lowest", p);
            }
        }

        return Response.status(Response.Status.OK)
                       .entity(systemLoads)
                       .build();
    }
}
