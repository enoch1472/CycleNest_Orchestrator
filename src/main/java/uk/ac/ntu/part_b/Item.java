package uk.ac.ntu.part_b;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * I use this model to represent an item in my CycleNest system, and I map fields
 * directly to the JSON property names my API returns/accepts.
 */
public class Item {

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("owner_id")
    private String ownerId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("category")
    private String category;

    @JsonProperty("location")
    private String location;

    @JsonProperty("daily_rate")
    private double dailyRate;

    @JsonProperty("available")
    private boolean available;

    @JsonProperty("condition")
    private String condition;

    @JsonProperty("description")
    private String description;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    // I keep a no-args constructor for Jackson (and similar tools) to instantiate the class during deserialization.
    public Item() {}

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
