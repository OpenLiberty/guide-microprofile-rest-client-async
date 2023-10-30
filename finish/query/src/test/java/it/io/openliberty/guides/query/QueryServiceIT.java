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
package it.io.openliberty.guides.query;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.UriBuilder;

public class QueryServiceIT {

    private static Logger logger = LoggerFactory.getLogger(QueryServiceIT.class);

    public static QueryResourceClient client;

    private static Network network = Network.newNetwork();
    
    public static String restClientClass = "InventoryClient.class";

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

    private static ImageFromDockerfile queryImage
        = new ImageFromDockerfile("query:1.0-SNAPSHOT")
              .withDockerfile(Paths.get("./Dockerfile"));

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
        .parse("mockserver/mockserver")
        .withTag("mockserver-" + MockServerClient.class
                .getPackage().getImplementationVersion());

    public static MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)
                    .withNetworkAliases("mock-server")
                    .withNetwork(network);
    public static MockServerClient mockClient;

    private static KafkaContainer kafkaContainer = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withListener(() -> "kafka:19092")
            .withNetwork(network);

    private static GenericContainer<?> queryContainer =
        new GenericContainer(queryImage)
            .withNetwork(network)
            .withExposedPorts(9080)
            .waitingFor(Wait.forHttp("/health/ready"))
            .withStartupTimeout(Duration.ofMinutes(3))
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .dependsOn(kafkaContainer);

    private static QueryResourceClient createRestClient(String urlPath) {
        ClientBuilder builder = ResteasyClientBuilder.newBuilder();
        ResteasyClient client = (ResteasyClient) builder.build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(urlPath));
        return target.proxy(QueryResourceClient.class);
    }

    @BeforeAll
    public static void startContainers() {
        mockServer.start();
        mockClient = new MockServerClient(
            mockServer.getHost(), 
            mockServer.getServerPort());

        kafkaContainer.start();

        queryContainer.withEnv(
            "InventoryClient/mp-rest/uri", "http://mock-server:" + MockServerContainer.PORT);
        queryContainer.start();

        client = createRestClient("http://"
            + queryContainer.getHost()
            + ":" + queryContainer.getFirstMappedPort());
    }
    @BeforeEach
    public void setup() throws InterruptedException {
        mockClient.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/inventory/systems"))
                    .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody("[\"testHost1\"," + 
                                "\"testHost2\"," +
                                "\"testHost3\"]")
                        .withHeader("Content-Type", "application/json"));

        mockClient.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/inventory/systems/testHost1"))
                    .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(testHost1)
                        .withHeader("Content-Type", "application/json"));

        mockClient.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/inventory/systems/testHost2"))
                    .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(testHost2)
                        .withHeader("Content-Type", "application/json"));

        mockClient.when(HttpRequest.request()
                        .withMethod("GET")
                        .withPath("/inventory/systems/testHost3"))
                    .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody(testHost3)
                        .withHeader("Content-Type", "application/json"));
    }

    @AfterAll
    public static void stopContainers() {
        queryContainer.stop();
        kafkaContainer.stop();
        mockServer.stop();
        network.close();
    }

    @Test
    public void testLoads(){
        Map<String, Properties> response = client.systemLoad();

        assertEquals(
            "testHost2",
            response.get("highest").get("hostname"),
            "Returned highest system load incorrect"
        );
        assertEquals(
            "testHost1",
            response.get("lowest").get("hostname"),
            "Returned lowest system load incorrect"
        );
    }
}
