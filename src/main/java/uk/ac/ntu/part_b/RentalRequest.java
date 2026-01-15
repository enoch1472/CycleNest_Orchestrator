package uk.ac.ntu.part_b;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * I use this model to represent a rental request in my CycleNest system,
 * and I map each field to the JSON keys my API and Cosmos documents use.
 */
public class RentalRequest {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("requester_id")
    private String requesterId;

    // I store status as a string so my API can easily exchange values like "pending", "cancelled", "approved", "rejected".
    @JsonProperty("status")
    private String status;

    @JsonProperty("request_date")
    private String requestDate;

    // I keep a no-args constructor for Jackson deserialization.
    public RentalRequest() {}

    // I use this constructor when I create new requests in my repository/service layer.
    public RentalRequest(String requestId, String itemId, String requesterId, String status) {
        this.requestId = requestId;
        this.itemId = itemId;
        this.requesterId = requesterId;
        this.status = status;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestDate() { return requestDate; }
    public void setRequestDate(String requestDate) { this.requestDate = requestDate; }
}
