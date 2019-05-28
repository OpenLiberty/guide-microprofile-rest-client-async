// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.inventory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import io.openliberty.guides.inventory.model.InventoryList;
import io.openliberty.guides.inventory.client.SystemClient;
import io.openliberty.guides.inventory.client.UnknownUrlException;

@RequestScoped
@Path("/systems")
public class InventoryResource {

  @Inject
  InventoryManager manager;

  @GET
  @Path("/{hostname}")
  @Produces(MediaType.APPLICATION_JSON)
  public CompletionStage<Response> getPropertiesForHost(
    @PathParam("hostname") String hostname) throws MalformedURLException {

    // Get properties for host
    SystemClient client = RestClientBuilder.newBuilder()
      .baseUrl(new URL("http://" + hostname + ":9080"))
      .register(UnknownUrlException.class)
      .build(SystemClient.class);

    CompletionStage<Response> response = client.getProperties()
      // tag::thenApplyAsync[]
      .thenApplyAsync((props) -> {
        manager.add(hostname, props);
        return Response.ok(props).build();
      })
      // end::thenApplyAsync[]
      // tag::exceptionally[]
      .exceptionally((e) -> {
        return Response
          .status(Response.Status.NOT_FOUND)
          .entity("ERROR: Unknown hostname or the system service may not be "
                  + "running on " + hostname)
          .build();
      });
      // end::exceptionally[]

    return response;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public InventoryList listContents() {
    return manager.list();
  }
}
