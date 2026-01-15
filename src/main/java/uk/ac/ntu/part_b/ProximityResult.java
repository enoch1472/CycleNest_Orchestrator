package uk.ac.ntu.part_b;

public class ProximityResult {

    private double distanceMeters;
    // I keep both meters and km so my API returns a raw value plus a ready-to-display conversion for the UI.
    private double distanceKm;

    private double durationSeconds;
    private double durationMinutes;
    private String osrmStatus;

    public double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(double durationSeconds) { this.durationSeconds = durationSeconds; }

    public double getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(double durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getOsrmStatus() { return osrmStatus; }
    public void setOsrmStatus(String osrmStatus) { this.osrmStatus = osrmStatus; }
}
