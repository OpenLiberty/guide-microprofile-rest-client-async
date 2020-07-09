package io.openliberty.guides.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.openliberty.guides.query.client.InventoryClient;

@ApplicationScoped
@Path("/query")
public class QueryResource {
    
    @Inject
    @RestClient
    private InventoryClient inventoryClient;

    @GET
    @Path("/systemLoad")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Properties> systemLoad() {
        List<String> systems = inventoryClient.getSystems();
        Holder systemLoads = new Holder();

        for (String system : systems) {
            Properties p = inventoryClient.getSystem(system);
            
            systemLoads.updateHighest(p);
            systemLoads.updateLowest(p);
        }

        return systemLoads.values;
    }

    private class Holder {
        public Map<String, Properties> values;

        public Holder() {
            this.values = new HashMap<String, Properties>();
            init();
        }

        public void updateHighest(Properties p) {
            BigDecimal load = (BigDecimal) p.get("systemLoad");
            BigDecimal highest = (BigDecimal) this.values
                                                  .get("highest")
                                                  .get("systemLoad");
            if (load.compareTo(highest) > 0) {
                this.values.put("highest", p);
            }
        }

        public void updateLowest(Properties p) {
            BigDecimal load = (BigDecimal) p.get("systemLoad");
            BigDecimal lowest = (BigDecimal) this.values
                                                 .get("lowest")
                                                 .get("systemLoad");
            if (load.compareTo(lowest) < 0) {
                this.values.put("lowest", p);
            }
        }

        private void init() {
            // Initialize highest and lowest values
            this.values.put("highest", new Properties());
            this.values.put("lowest", new Properties());
            this.values.get("highest").put("hostname", "temp_max");
            this.values.get("lowest").put("hostname", "temp_min");
            this.values.get("highest").put("systemLoad", new BigDecimal(Double.MIN_VALUE));
            this.values.get("lowest").put("systemLoad", new BigDecimal(Double.MAX_VALUE));
        }
    }
}
