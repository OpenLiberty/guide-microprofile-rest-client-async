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
package io.openliberty.guides.openlibertycafe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.openliberty.guides.models.Order;
import io.openliberty.guides.models.OrderRequest;
import io.openliberty.guides.models.Type;
import io.openliberty.guides.openlibertycafe.client.OrderClient;

@ApplicationScoped
@Path("/orders")
public class OpenLibertyCafeOrderResource {

    @Inject
    private Validator validator;

    @Inject
    @RestClient
    private OrderClient orderClient;

    //OrderRequest object validator
    private Response validate(OrderRequest orderRequest) {
        Set<ConstraintViolation<OrderRequest>> violations =
                validator.validate(orderRequest);

        if (violations.size() > 0) {
            JsonArrayBuilder messages = Json.createArrayBuilder();

            for (ConstraintViolation<OrderRequest> v : violations) {
                messages.add(v.getMessage());
            }

            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(messages.build().toString())
                    .build();
        }
        return null;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(operationId = "createOrder",
               summary = "Create orders",
               description = "This operation creates orders by using " + 
                   "an OrderRequest and sends them to the Bar and Kitchen services.")
    @Tag(name = "Order")
    public Response createOrder(OrderRequest orderRequest) {

        //Validate OrderRequest object
        Response validateResponse = validate(orderRequest);
        if (validateResponse != null){
            return validateResponse;
        }

        String tableId = orderRequest.getTableId();

        final Holder<List<String>> holder = new Holder<List<String>>();
        // tag::countDownLatch[]
        CountDownLatch countdownLatch = new CountDownLatch(orderRequest.getFoodList().size()
                                                + orderRequest.getBeverageList().size());
        // end::countDownLatch[]

        // Send individual food order requests to the Order service through the client
        for (String foodItem : orderRequest.getFoodList()) {
            Order order = new Order().setTableId(tableId)
                                     .setItem(foodItem).setType(Type.FOOD);
            // tag::thenAcceptAsync1[]
            orderClient.createOrder(order).thenAcceptAsync(r -> {
                holder.value.add(r.readEntity(Order.class).getOrderId());
                // tag::countDown1[]
                countdownLatch.countDown();
                // end::countDown1[]
            });
            // end::thenAcceptAsync1[]
        }

        // Send individual beverage order requests to the Order service through the client
        for (String beverageItem : orderRequest.getBeverageList()) {
            Order order = new Order().setTableId(tableId)
                                     .setItem(beverageItem).setType(Type.BEVERAGE);
            // tag::thenAcceptAsync2[]
            orderClient.createOrder(order).thenAcceptAsync(r -> {
                holder.value.add(r.readEntity(Order.class).getOrderId());
                // tag::countDown2[]
                countdownLatch.countDown();
                // end::countDown2[]
            });
            // end::thenAcceptAsync2[]
        }

        // wait all asynchronous orderClient.createOrder to be completed
        try {
            // tag::await[]
            countdownLatch.await();
            // end::await[]
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return Response
                .status(Response.Status.OK)
                .entity(holder.value)
                .build();
    }

    private class Holder<T> {
        @SuppressWarnings("unchecked")
		public volatile T value = (T) new ArrayList<String>();
    }
}