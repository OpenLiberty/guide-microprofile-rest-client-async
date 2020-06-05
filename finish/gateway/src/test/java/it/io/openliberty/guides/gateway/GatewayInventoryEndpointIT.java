// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package it.io.openliberty.guides.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.json.JsonObject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

@ExtendWith(MockServerExtension.class)
public class GatewayInventoryEndpointIT {

    private final String BASE_URL = "http://localhost:9080/api/systems";

    private Client client;
    
    private MockServerClient mockServerClient;
    private Response response;

    @BeforeEach
    public void setup(MockServerClient mockServerClient) throws InterruptedException {
        response = null;
        client = ClientBuilder.newBuilder()
                    .hostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    })
                    .register(JsrJsonpProvider.class)
                    .build();

        this.mockServerClient = mockServerClient;
        
        mockServerClient
                    .when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/inventory/systems"))
                    .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody("{ \"systems\": [ { \"hostname\": \"banana\", \"properties\": { \"java.vendor\": \"you\", \"system.busy\": \"false\" } } ], \"total\": 1 }")
                        .withHeader("Content-Type", "application/json"));

        mockServerClient
                    .when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/inventory/systems/coconut"))
                    .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody("{ \"hostname\": \"coconut\", \"properties\": { \"java.vendor\": \"me\" } }")
                        .withHeader("Content-Type", "application/json"));
    }

    @AfterEach
    public void teardown() {
        client.close();
    }
    
    @Test
    public void testAddSystem() throws InterruptedException {
        response = client
            .target(BASE_URL + "/coconut")
            .request()
            .get();

        assertEquals(200, response.getStatus());

        JsonObject obj = response.readEntity(JsonObject.class);
        assertEquals("coconut", obj.getString("hostname"));
    }

    @Test
    public void testGetSystems() {
        response = client
            .target(BASE_URL)
            .request()
            .get();

        assertEquals(200, response.getStatus());

        JsonObject obj = response.readEntity(JsonObject.class);
        assertEquals(1, obj.getInt("total"));
        assertEquals(1, obj.getJsonArray("systems").size());
    }

}
