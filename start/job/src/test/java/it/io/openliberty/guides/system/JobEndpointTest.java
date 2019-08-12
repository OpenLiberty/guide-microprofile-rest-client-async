// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package it.io.openliberty.guides.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.json.JsonObject;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;

import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

public class JobEndpointTest {

    private final String BASE_URL = "http://localhost:9080/jobs";
    private final String KAFKA_SERVER = "localhost:9092";
    private final int RETRIES = 5;
    private final int BACKOFF_MULTIPLIER = 2;

    private Client client;
    private Response response;
    private KafkaProducer<String, String> producer;

    @Rule
    public Network network = Network.newNetwork();

    @Rule
    public FixedHostPortGenericContainer zookeeper = new FixedHostPortGenericContainer<>("bitnami/zookeeper:3")
        .withFixedExposedPort(2181, 2181)
        .withNetwork(network)
        .withNetworkAliases("zookeeper")
        .withEnv("ALLOW_ANONYMOUS_LOGIN", "yes");

    @Rule
    public FixedHostPortGenericContainer kafka = new FixedHostPortGenericContainer<>("bitnami/kafka:2")
        .withFixedExposedPort(9092, 9092)
        .withNetwork(network)
        .withNetworkAliases("kafka")
        .withEnv("KAFKA_CFG_ZOOKEEPER_CONNECT", "zookeeper:2181")
        .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
        .withEnv("KAFKA_CFG_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9092");

    @Before
    public void setup() throws InterruptedException {
        response = null;
        client = ClientBuilder.newBuilder()
                    .hostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    })
                    .register(JsrJsonpProvider.class)
                    .build();

        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        this.producer = new KafkaProducer<>(properties);
    }

    @After
    public void teardown() {
        client.close();
    }
    
    @Test
    public void testCreateJob() {
        this.response = client
            .target(BASE_URL)
            .request()
            .post(null);

        assertEquals(200, response.getStatus());

        JsonObject obj = response.readEntity(JsonObject.class);
        String jobId = obj.getString("jobId");
        assertTrue("jobId not returned from service", jobId.matches("^\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}$"));
    }

    @Test
    public void testJobNotExists() {
        this.response = client
            .target(String.format("%s/%s", BASE_URL, "my-job-id"))
            .request()
            .get();

        assertEquals(404, response.getStatus());
    }

    @Test
    public void testConsumeJob() throws InterruptedException {
        producer.send(new ProducerRecord<String,String>("job-result-topic", "{ \"jobId\": \"my-produced-job-id\", \"result\": 7 }"));
        this.response = client
            .target(String.format("%s/%s", BASE_URL, "my-produced-job-id"))
            .request()
            .get();

        int backoff = 500;
        for (int i = 0; i < RETRIES && this.response.getStatus() != 200; i++) {
            this.response = client
                .target(String.format("%s/%s", BASE_URL, "my-produced-job-id"))
                .request()
                .get();

            Thread.sleep(backoff);
            backoff *= BACKOFF_MULTIPLIER;
        }

        assertEquals(200, response.getStatus());

        JsonObject obj = response.readEntity(JsonObject.class);
        assertEquals(7, obj.getInt("result"));
    }

}
