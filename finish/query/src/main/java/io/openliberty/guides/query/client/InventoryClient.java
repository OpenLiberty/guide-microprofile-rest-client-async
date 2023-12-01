// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.query.client;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/inventory")
@RegisterRestClient(configKey = "InventoryClient", baseUri = "http://localhost:9085")
public interface InventoryClient extends AutoCloseable {

    @GET
    @Path("/systems")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> getSystems();

    // tag::getSystem[]
    @GET
    @Path("/systems/{hostname}")
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Properties> getSystem(
        @PathParam("hostname") String hostname);
    // end::getSystem[]

}
