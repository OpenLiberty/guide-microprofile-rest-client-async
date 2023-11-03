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
package it.io.openliberty.guides.inventory;

import java.util.List;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Properties;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.client.ClientBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import io.openliberty.guides.models.SystemLoad;
import io.openliberty.guides.models.SystemLoad.SystemLoadSerializer;

@Testcontainers
public class InventoryServiceIT {

    private static Logger logger = LoggerFactory.getLogger(InventoryServiceIT.class);

    public static InventoryResourceCleint client;

    private static Network network = Network.newNetwork();

    public static KafkaProducer<String, SystemLoad> producer;

    private static ImageFromDockerfile inventoryImage
        = new ImageFromDockerfile("inventory:1.0-SNAPSHOT")
            .withDockerfile(Paths.get("./Dockerfile"));

    private static KafkaContainer kafkaContainer = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withListener(() -> "kafka:19092")
            .withNetwork(network);

    private static GenericContainer<?> inventoryContainer =
        new GenericContainer(inventoryImage)
            .withNetwork(network)
            .withExposedPorts(9085)
            .waitingFor(Wait.forHttp("/health/ready").forPort(9085))
            .withStartupTimeout(Duration.ofMinutes(2))
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .dependsOn(kafkaContainer);

    private static InventoryResourceCleint createRestClient(String urlPath) {
        ClientBuilder builder = ResteasyClientBuilder.newBuilder();
        ResteasyClient client = (ResteasyClient) builder.build();
        ResteasyWebTarget target = client.target(UriBuilder.fromPath(urlPath));
        return target.proxy(InventoryResourceCleint.class);
    }

    @BeforeAll
    public static void startContainers() {
        kafkaContainer.start();
        inventoryContainer.withEnv(
            "mp.messaging.connector.liberty-kafka.bootstrap.servers", "kafka:19092");
        inventoryContainer.start();
        client = createRestClient("http://"
            + inventoryContainer.getHost()
            + ":" + inventoryContainer.getFirstMappedPort());
    }

    @BeforeEach
    public void setUp() {
        Properties producerProps = new Properties();
        producerProps.put(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers());
        producerProps.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        producerProps.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                SystemLoadSerializer.class.getName());

        producer = new KafkaProducer<String, SystemLoad>(producerProps);
    }

    @AfterAll
    public static void stopContainers() {
        client.resetSystems();
        inventoryContainer.stop();
        kafkaContainer.stop();
        network.close();
    }

    @AfterEach
    public void tearDown() {
        producer.close();
    }

    @Test
    public void testCpuUsage() throws InterruptedException {
        SystemLoad sl = new SystemLoad("localhost", 1.1);
        producer.send(new ProducerRecord<String, SystemLoad>("system.load", sl));
        Thread.sleep(5000);
        List<String> response = client.getSystems();
        assertNotNull(response);
        assertEquals(response.size(), 1);
        for (String system : response) {
            Properties sp = client.getSystem(system);
            assertEquals(sl.hostname, sp.get("hostname"),
                    "Hostname doesn't match!");
            BigDecimal systemLoad = (BigDecimal) sp.get("systemLoad");
            assertEquals(sl.loadAverage, systemLoad.doubleValue(),
                    "CPU load doesn't match!");
        }
    }
}
