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
package it.io.openliberty.guides.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.core.Response;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.SharedContainerConfig;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import io.openliberty.guides.gateway.GatewayResource;

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
public class GatewayServiceIT {

    Response response;

    @RESTClient
    public static GatewayResource gatewayResource;


    private static String testHost1 = 
        "{" + 
            "'hostname' : 'testHost1'," +
            "'systemLoad' : 1.23" +
            "'os.name' : 'Windows'" + 
            "'os.arch' : 'x86'" +
        "}"
    private static String testHost2 = 
        "{" + 
            "'hostname' : 'testHost2'," +
            "'systemLoad' : 3.21," +
            "'os.name' : 'Linux'" + 
            "'os.arch' : 'amd64'" +
        "}"

    @BeforeAll
    public static void setup() throws InterruptedException {
        AppContainerConfig.mockClient.when(HttpRequest.request()
                                        .withMethod("GET")
                                        .withPath("/inventory/systems"))
                                    .respond(HttpResponse.response()
                                        .withStatusCode(200)
                                        .withBody("[" + testHost1 + "," + testHost2 + "]")
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
                                        .withPath("inventory/data/os.name"))
                                    .respond(HttpResponse.response()
                                        .withStatusCode(200)
                                        .withBody("[" + 
                                                    "\"testHost1:os.name=Windows\"," +
                                                    "\"testHost2:os.name=Linux\"" +
                                                "]")
                                        .withHeader("Content-Type", "application/json"));

        AppContainerConfig.mockClient.when(HttpRequest.request()
                                        .withMethod("GET")
                                        .withPath("inventory/data/os.arch"))
                                    .respond(HttpResponse.response()
                                        .withStatusCode(200)
                                        .withBody("[" + 
                                                    "\"testHost1:os.arch=x86\"," +
                                                    "\"testHost2:os.arch=amd64\"" +
                                                "]")
                                        .withHeader("Content-Type", "application/json"));

        AppContainerConfig.mockClient.when(HttpRequest.request()
                                        .withMethod("GET")
                                        .withPath("inventory/data/os.version"))
                                    .respond(HttpResponse.response()
                                        .withStatusCode(200)
                                        .withBody("[" + 
                                                    "\"testHost1:os.version=not available\"," +
                                                    "\"testHost2:os.version=not available\"" +
                                                "]")
                                        .withHeader("Content-Type", "application/json"));
    }

    @Test
    public void testGetSystems() {
        response = gatewayResource.getSystems();
        assertEquals(200, response.getStatus());

    }

    @Test
    public void testGetSystem() {
        response = gatewayResource.getSystem("testHost1");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testBadSystem() {
        response = gatewayResource.getSystem("badhost");
        assertEquals(404, response.getStatus(), 
            "request for badhost should have failed but did not");
    }

}
