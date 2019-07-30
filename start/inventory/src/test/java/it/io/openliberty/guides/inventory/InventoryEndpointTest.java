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
package it.io.openliberty.guides.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
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

public class InventoryEndpointTest {

    private final String BASE_URL = "http://localhost:9080/inventory/systems";
    private final String KAFKA_SERVER = "localhost:9092";
    private final int RETRIES = 8;
    private final int BACKOFF_MULTIPLIER = 2;
    private final int BASE_BACKOFF = 500;

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
    public void testConsumeSystem() throws InterruptedException, IOException {
        // Get size of inventory
        this.response = client
            .target(BASE_URL)
            .request()
            .get();

        assertEquals(200, response.getStatus());

        JsonObject obj = response.readEntity(JsonObject.class);
        int initialTotal = obj.getInt("total");
        
        // Add a system to the inventory via kafka
        String props = getResource("props.json");
        producer.send(new ProducerRecord<String,String>("system-topic", props));
        this.response = client
            .target(BASE_URL)
            .request()
            .get();


        obj = response.readEntity(JsonObject.class);
        int total = obj.getInt("total");

        int backoff = BASE_BACKOFF;
        for (int i = 0; i < RETRIES && (total <= initialTotal); i++) {
            Thread.sleep(backoff);
            backoff *= BACKOFF_MULTIPLIER;

            this.response = client
                .target(BASE_URL)
                .request()
                .get();
            
            obj = response.readEntity(JsonObject.class);
            total = obj.getInt("total");
        }

        assertTrue(String.format("Total (%s) is not greater than inital total (%s)", total, initialTotal), total > initialTotal);

        // Make system busy
        String busyProps = getResource("props.busy.json");
        producer.send(new ProducerRecord<String, String>("system-topic", busyProps));

        this.response = client
            .target(BASE_URL)
            .request()
            .get();

        obj = response.readEntity(JsonObject.class);
        JsonArray systems = obj.getJsonArray("systems");

        backoff = BASE_BACKOFF;
        for (int i = 0; i < RETRIES && !getPropertyFromJsonArray("myhost", "system.busy", systems).equals("true"); i++) {
            Thread.sleep(backoff);
            backoff *= BACKOFF_MULTIPLIER;

            this.response = client
                .target(BASE_URL)
                .request()
                .get();

            obj = response.readEntity(JsonObject.class);
            systems = obj.getJsonArray("systems");
        }

        assertEquals("true", getPropertyFromJsonArray("myhost", "system.busy", systems));
    }

    private String getResource(String filename) throws IOException {
        ClassLoader loader = getClass().getClassLoader();
        File file = new File(loader.getResource(filename).getFile());
        BufferedReader reader = new BufferedReader(new FileReader(file));

        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }

        reader.close();
        return builder.toString();
    }

    private String getPropertyFromJsonArray(String hostname, String property, JsonArray array) {
        for (JsonValue v : array) {
            JsonObject obj = v.asJsonObject();
            String h = obj.getString("hostname");

            if (h != null && h.equals(hostname)) {
                String result = obj.getJsonObject("properties").getString(property);
                if (result != null) return result;
            }
        }

        return "";
    }

}
