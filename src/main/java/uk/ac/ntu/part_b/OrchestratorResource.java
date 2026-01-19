package uk.ac.ntu.part_b;

import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * I use this resource as my single API entry-point for CycleNest, coordinating Cosmos DB access
 * and calling OSRM directly when I need routing/proximity results.
 */
@Path("orchestrator")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class OrchestratorResource {

    private Item__Rep repository;
    private static final ObjectMapper mapper = new ObjectMapper();

    // I keep OSRM config here because my orchestrator is the component responsible for consuming the external service.
    private static final String OSRM_BASE = "http://router.project-osrm.org";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    public OrchestratorResource() {
        System.out.println("★★★ OrchestratorResource instantiated! ★★★");
        try {
            this.repository = new Item__Rep();

            List<Item> testItems = repository.searchItems(null, null, null, true);
            System.out.println("★★★ Connected to Azure Cosmos DB – "
                + testItems.size() + " available items found ★★★");

        } catch (Exception e) {
            System.err.println("★★★ FAILED to connect to Azure Cosmos DB: " + e.getMessage() + " ★★★");
            e.printStackTrace();
            this.repository = null;
        }
    }

    @GET
    @Path("proximity")
    public Response getProximity(
            @QueryParam("originLat") double originLat,
            @QueryParam("originLng") double originLng,
            @QueryParam("destLat") double destLat,
            @QueryParam("destLng") double destLng
    ) {
        // I validate coordinate ranges early so I do not call OSRM with obviously invalid inputs.
        if (originLat < -90 || originLat > 90 || destLat < -90 || destLat > 90) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("invalid_coords", "Latitude must be between -90 and 90"))
                    .build();
        }
        if (originLng < -180 || originLng > 180 || destLng < -180 || destLng > 180) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("invalid_coords", "Longitude must be between -180 and 180"))
                    .build();
        }

        HttpURLConnection conn = null;
        InputStream is = null;

        try {
            // I build the OSRM URL in (lon,lat) order because that is what OSRM expects.
            String coords = String.format("%f,%f;%f,%f", originLng, originLat, destLng, destLat);
            String fullUrl = OSRM_BASE + "/route/v1/driving/" + coords + "?overview=false";

            System.out.println("OSRM Request URL (from Orchestrator): " + fullUrl);

            conn = (HttpURLConnection) new URL(fullUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

            if (is == null) {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity(new ErrorResponse("no_stream", "OSRM provided no response body."))
                        .build();
            }

            String body = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            if (!body.trim().startsWith("{")) {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity(new ErrorResponse("invalid_response", "OSRM returned a non-JSON response", body))
                        .build();
            }

            OSRMResponse osrm = mapper.readValue(body, OSRMResponse.class);

            if (!"Ok".equalsIgnoreCase(osrm.getCode())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("osrm_error", "OSRM reported: " + osrm.getCode(), osrm.getMessage()))
                        .build();
            }

            if (osrm.getRoutes() == null || osrm.getRoutes().isEmpty()) {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity(new ErrorResponse("no_route", "OSRM returned no routes"))
                        .build();
            }

            OSRMRoute route = osrm.getRoutes().get(0);

            // I return both raw units and user-friendly conversions so my frontend can display values immediately.
            ProximityResult result = new ProximityResult();
            result.setDistanceMeters(route.getDistance());
            result.setDistanceKm(route.getDistance() / 1000.0);
            result.setDurationSeconds(route.getDuration());
            result.setDurationMinutes(route.getDuration() / 60.0);
            result.setOsrmStatus(osrm.getCode());

            return Response.ok(result).build();

        } catch (UnknownHostException uhe) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse("dns_error",
                            "Cannot resolve hostname: " + OSRM_BASE,
                            "Please check your internet connection and DNS settings."))
                    .build();

        } catch (ConnectException ce) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse("connection_error",
                            "Cannot connect to OSRM service at " + OSRM_BASE,
                            "The service may be temporarily unavailable."))
                    .build();

        } catch (SocketTimeoutException te) {
            return Response.status(Response.Status.GATEWAY_TIMEOUT)
                    .entity(new ErrorResponse("timeout",
                            "Connection to OSRM service timed out",
                            "The service took too long to respond."))
                    .build();

        } catch (JsonProcessingException jpe) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse("json_error",
                            "Failed to parse OSRM response",
                            jpe.getMessage()))
                    .build();

        } catch (IOException ioe) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse("io_error",
                            "Error communicating with OSRM service",
                            ioe.getMessage()))
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("internal_error",
                            "Unexpected error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                            "Exception type: " + e.getClass().getName()))
                    .build();

        } finally {
            // I close the stream and disconnect to avoid leaking resources across requests.
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @GET
    @Path("items")
    public Response searchItems(
            @QueryParam("location") String location,
            @QueryParam("maxPrice") Double maxPrice,
            @QueryParam("category") String category,
            @QueryParam("available") Boolean available
    ) {
        if (repository == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("db_error", "Database connection failed"))
                    .build();
        }

        try {
            List<Item> items = repository.searchItems(location, maxPrice, category, available);
            return Response.ok(mapper.writeValueAsString(items)).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("json_error", "Failed to serialize items"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("db_error", "Query failed"))
                    .build();
        }
    }

    @POST
    @Path("requests")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestItem(String content) {
        if (repository == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("db_error", "Database connection failed"))
                    .build();
        }

        try {
            RequestItemRequest requestBody = mapper.readValue(content, RequestItemRequest.class);

            // I validate required inputs here so I can return clear 400 responses instead of failing later.
            if (requestBody.getItemId() == null || requestBody.getItemId().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("validation_error", "item_id is required"))
                        .build();
            }
            if (requestBody.getRequesterId() == null || requestBody.getRequesterId().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("validation_error", "requester_id is required"))
                        .build();
            }

            Item item = repository.getItemById(requestBody.getItemId());
            if (item == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("not_found", "Item not found"))
                        .build();
            }

            Rental__Request request = repository.createRequest(requestBody.getItemId(), requestBody.getRequesterId());
            return Response.status(Response.Status.CREATED).entity(mapper.writeValueAsString(request)).build();

        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("json_error", "Invalid JSON format"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("db_error", "Failed to create request"))
                    .build();
        }
    }

    @PUT
    @Path("requests/{requestId}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelRequest(@PathParam("requestId") String requestId) {
        if (repository == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("db_error", "Database connection failed"))
                    .build();
        }

        try {
            Rental__Request request = repository.cancelRequest(requestId);
            if (request == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("not_found", "Request not found"))
                        .build();
            }
            return Response.ok(mapper.writeValueAsString(request)).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("json_error", "Serialization error"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("db_error", "Failed to cancel request"))
                    .build();
        }
    }

    private static class RequestItemRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("item_id")
        private String itemId;

        @com.fasterxml.jackson.annotation.JsonProperty("requester_id")
        private String requesterId;

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getRequesterId() { return requesterId; }
        public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    }
}
