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
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
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

    @POST
    @Path("/data")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addProperties(ArrayList<String> properties) {
        for (String property : properties) {
            inventoryClient.addProperty(property);
        }

        return Response.status(Response.Status.OK)
                       .entity("Added properties " + properties)
                       .build();
    }

}
