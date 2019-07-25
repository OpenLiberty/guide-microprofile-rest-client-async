package io.openliberty.guides.bff.model;

import java.util.Properties;

public class SystemData {

    private String hostname;
    private Properties properties;

    public SystemData() {
      this.hostname = "";
      this.properties = null;
    }

    public SystemData(String hostname, Properties properties) {
      this.hostname = hostname;
      this.properties = properties;
    }

    public String getHostname() {
      return hostname;
    }

    public Properties getProperties() {
      return properties;
    }

    public void setHostname(String hostname) {
      this.hostname = hostname;
    }

    public void setProperties(Properties properties) {
      this.properties = properties;
    }

    @Override
    public boolean equals(Object host) {
      if (host instanceof SystemData) {
        return hostname.equals(((SystemData) host).getHostname());
      }
      return false;
    }
}
