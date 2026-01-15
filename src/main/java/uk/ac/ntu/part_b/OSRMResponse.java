package uk.ac.ntu.part_b;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OSRMResponse {

    // I map this directly to OSRM's "code" so I can quickly tell whether the request succeeded (e.g., "Ok") or failed.
    @JsonProperty("code")
    private String code;

    // I store the routes list so I can read distance/duration from the first route when OSRM returns results.
    @JsonProperty("routes")
    private List<OSRMRoute> routes;

    // I include this because OSRM can return a "message" field when it fails, and I want to surface that cleanly.
    @JsonProperty("message")
    private String message;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public List<OSRMRoute> getRoutes() { return routes; }
    public void setRoutes(List<OSRMRoute> routes) { this.routes = routes; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
