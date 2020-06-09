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

import static org.junit.Assert.assertEquals;

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
    private final String BASE_URL = "http://localhost:9080/api/inventory";

    @RESTClient
    public static GatewayResource gatewayResource;

    @BeforeAll
    public static void setup() throws InterruptedException {
        AppContainerConfig.mockClient
                              .when(HttpRequest.request()
                                  .withMethod("GET")
                                  .withPath("inventory/systems"))
                              .respond(HttpResponse.response()
                                  .withStatusCode(200)
                                  .withBody("{}")
                                  .withHeader("Content-Type", "application/json")
                              );
    }
}
