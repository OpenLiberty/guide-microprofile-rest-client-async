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
package it.io.openliberty.guides.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.SharedContainerConfig;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import io.openliberty.guides.query.QueryResource;

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
public class QueryServiceIT {

    Response response;

    @RESTClient
    public static QueryResource queryResource;


    private static String testHost1 = 
        "{" + 
            "\"hostname\" : \"testHost1\"," +
            "\"systemLoad\" : 1.23" +
        "}";
    private static String testHost2 = 
        "{" + 
            "\"hostname\" : \"testHost2\"," +
            "\"systemLoad\" : 3.21" +
        "}";
    private static String testHost3 =
        "{" + 
            "\"hostname\" : \"testHost3\"," +
            "\"systemLoad\" : 2.13" +
        "}";

    @BeforeAll
    public static void setup() throws InterruptedException {
        AppContainerConfig.mockClient.when(HttpRequest.request()
                                         .withMethod("GET")
                                         .withPath("/inventory/systems"))
                                     .respond(HttpResponse.response()
                                         .withStatusCode(200)
                                         .withBody("[\"testHost1\", \"testHost2\", \"testHost3\"]")
                                         .withHeader("Content-Type", "application/json"));

        AppContainerConfig.mockClient.when(HttpRequest.request()
                                         .withMethod("GET")
                                         .withPath("/inventory/systems/testHost1"))
                                     .respond(HttpResponse.response()
                                         .withStatusCode(200)
                                         .withBody(testHost1)
                                         .withHeader("Content-Type", "application/json"));

        AppContainerConfig.mockClient.when(HttpRequest.request()
                                         .withMethod("GET")
                                         .withPath("/inventory/systems/testHost2"))
                                     .respond(HttpResponse.response()
                                         .withStatusCode(200)
                                         .withBody(testHost2)
                                         .withHeader("Content-Type", "application/json"));

        AppContainerConfig.mockClient.when(HttpRequest.request()
                                         .withMethod("GET")
                                         .withPath("/inventory/systems/testHost3"))
                                     .respond(HttpResponse.response()
                                         .withStatusCode(200)
                                         .withBody(testHost3)
                                         .withHeader("Content-Type", "application/json"));
    }

    // tag::getSystems[]
    @Test
    public void testGetSystems() {
        response = queryResource.getSystems();
        assertEquals(200, response.getStatus());

        List<String> contents = response.readEntity(List.class);

        assertTrue(contents.contains("testHost1"),
            "testHost1 not returned");
        assertTrue(contents.contains("testHost2"),
            "testHost2 not returned");
        assertTrue(contents.contains("testHost3"),
            "testHost3 not returned");
    }
    // end::getSystems[]

    // tag::testLoads[]
    @Test
    public void testLoads() {
        response = queryResource.systemLoad();
        assertEquals(200, response.getStatus());

        Map<String, Map<String, String>> contents = response.readEntity(Map.class);

        assertEquals(
            "testHost2",
            contents.get("highest").get("hostname"),
            "Returned highest system load incorrect"
        );
        assertEquals(
            "testHost1",
            contents.get("lowest").get("hostname"),
            "Returned lowest system load incorrect"
        );
    }
    // end::testLoads[]

}
