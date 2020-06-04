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
package it.io.openliberty.guides.system;


import static org.junit.jupiter.api.assertEquals;
import static org.junit.jupiter.api.assertTrue;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemEndpointIT {

    private static final String port = System.getProperty("test.http.port");
    private static final String BASE_URL = "http://localhost:" + port + "/system/properties";
    
    private Client client;

    @BeforeEach
    public void setup() throws InterruptedException {
        client = ClientBuilder.newBuilder()
                    .hostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    })
                    .register(JsrJsonpProvider.class)
                    .build();
    }

    @AfterEach
    public void teardown() {
        client.close();
    }

    @Test
    public void testGetProperties() {
    	Response response = client
            .target(BASE_URL)
            .request()
            .get();
        assertEquals(200, response.getStatus());
        
        String json = response.readEntity(String.class);
        assertTrue(json.contains("os.name"),
        		   "The system property should contain os.name.");
        
        response.close();
    }

}
