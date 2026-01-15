package uk.ac.ntu.part_b;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OSRMRoute {

    // I map OSRM's duration directly (seconds) so I can convert it later into minutes/hours for display if needed.
    @JsonProperty("duration")
    private double duration;

    // I map OSRM's distance directly (meters) so I can convert it later into km/miles depending on my UI.
    @JsonProperty("distance")
    private double distance;

    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
}
