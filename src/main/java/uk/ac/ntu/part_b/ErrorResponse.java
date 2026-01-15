package uk.ac.ntu.part_b;

public class ErrorResponse {
    // I keep these fields private to enforce encapsulation, so other classes can only read/write them through my getters/setters.
    // This makes it easier for me to validate or format values later without changing the public API.
    private String code;
    private String message;
    private String details;

    // I include a no-args constructor because many frameworks (e.g., JSON serializers/deserializers) expect it
    // when they instantiate objects reflectively.
    public ErrorResponse() {}

    // I provide a minimal constructor for the common case where I only need a code and a user-facing message.
    // I leave "details" unset so it can remain null unless I explicitly need extra diagnostics.
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    // I provide a full constructor for cases where I want both a client-friendly message and deeper context,
    // such as validation failures, downstream service errors, or exception summaries.
    public ErrorResponse(String code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    // I expose the error code via a getter so I can standardize how errors are identified across my API.
    // Example codes could be things like "VALIDATION_ERROR", "NOT_FOUND", or "INTERNAL_ERROR".
    public String getCode() { return code; }

    // I keep the setter so I can still build the object step-by-step (useful when mapping from exceptions).
    public void setCode(String code) { this.code = code; }

    // I use "message" for the human-readable explanation that I expect clients to show to users or log at a high level.
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    // I treat "details" as optional supporting information (stack hints, field-level validation notes, correlation IDs, etc.).
    // I keep it separate from "message" so I can decide what is safe to expose externally.
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
