package iudx.rs.proxy.authenticator.authorization;

import java.util.stream.Stream;

public enum Api {
  ENTITIES("/ngsi-ld/v1/entities"),
  CONSUMER_AUDIT("/ngsi-ld/v1/consumer/audit"),

  PROVIDER_AUDIT("/ngsi-ld/v1/provider/audit"),
  TEMPORAL("/ngsi-ld/v1/temporal/entities");

 private final String endpoint;

  Api(String endpoint) {
    this.endpoint = endpoint;
  }

  public static Api fromEndpoint(final String endpoint) {
    return Stream.of(values()).filter(v -> v.endpoint.equalsIgnoreCase(endpoint)).findAny()
        .orElse(null);
  }

  public String getApiEndpoint() {
    return this.endpoint;
  }
}
