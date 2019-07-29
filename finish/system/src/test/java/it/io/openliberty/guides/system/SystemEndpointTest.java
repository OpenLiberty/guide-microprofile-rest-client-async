// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import java.util.Arrays;
import java.util.Properties;
import java.io.IOException;
import java.time.Duration;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;

import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

public class SystemEndpointTest {

    private final String BASE_URL = "http://localhost:9080/system/properties";
    private final String KAFKA_SERVER = "localhost:9092";
    private final int RETRIES = 5;
    private final int BACKOFF_MULTIPLIER = 2;
    private final String CONSUMER_OFFSET_RESET = "earliest";

    private Client client;
    private Response response;
    private KafkaProducer<String, String> producer;
    private KafkaConsumer<String, String> consumer;

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

        properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_SERVER);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "junit-integration-test-client");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, CONSUMER_OFFSET_RESET);
        this.consumer = new KafkaConsumer<>(properties);
        this.consumer.subscribe(Arrays.asList("job-result-topic"));
    }

    @After
    public void teardown() {
        client.close();
    }

    @Test
    public void testGetProperties() {
        this.response = client
            .target(BASE_URL)
            .request()
            .get();

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testRunJob() throws JsonParseException, JsonMappingException, IOException, InterruptedException {
        producer.send(new ProducerRecord<String, String>("job-topic", "{ \"jobId\": \"my-job\" }"));

        int recordsProcessed = 0;
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0;

        while (recordsProcessed == 0 && elapsedTime < 30000) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));
            for (ConsumerRecord<String, String> record : records) {
                ObjectMapper mapper = new ObjectMapper();
                JsonFactory factory = mapper.getFactory();
                JsonParser parser = factory.createParser(record.value());
                JsonNode node = mapper.readTree(parser);
                
                int result = node.get("result").asInt();
                String jobId = node.get("jobId").asText();

                assertEquals("my-job", jobId);
                assertTrue(String.format("Result (%s) must be between 5 and 10 (inclusive)", result), result >= 5 && result <= 10);
                recordsProcessed++;
            }

            elapsedTime = System.currentTimeMillis() - startTime;
            consumer.commitAsync();
        }

        assertTrue("No records processed", recordsProcessed > 0);
    }

}
