package uk.ac.ntu.part_b;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

public class Item__Rep {

    // I keep my Cosmos DB connection details as constants so I have a single place to change them.
    // (In my final deployment, I would move these into environment variables or config to avoid hardcoding secrets.)
    //
    // I made these package-visible (not private) so my CosmosClientProvider can reuse them without duplicating values.
    // I did not change the values; I only changed the visibility to support my Singleton client.
    static final String ENDPOINT = "https://myfirstfreedb1.documents.azure.com:443/";
    static final String KEY = "lb9oscLbltkcuDhkkwasQV84awcOKc41A0bdAE9XA2Upk5TrkULCznKKKCgR9XDaz8N8dFJXDCzXACDbKWBLkQ==";

    private static final String DATABASE_ID = "thedb";
    private static final String ITEM_CONTAINER_ID = "item";
    private static final String REQUEST_CONTAINER_ID = "request";

    private CosmosClient cosmosClient;
    private CosmosContainer itemContainer;
    private CosmosContainer requestContainer;

    // I initialise the repository by connecting once to Cosmos and caching container references for reuse.
    public Item__Rep() {
        initializeCosmos();
    }

    private void initializeCosmos() {

        // I DO NOT create a new CosmosClient here anymore because under load that creates too many clients/connections.
        // Instead, I reuse my Singleton Cosmos client, which is the correct approach for stability and performance.
        cosmosClient = CosmosClientProvider.getClient();

        CosmosDatabase database = cosmosClient.getDatabase(DATABASE_ID);
        itemContainer = database.getContainer(ITEM_CONTAINER_ID);
        requestContainer = database.getContainer(REQUEST_CONTAINER_ID);

        System.out.println("★★★ Successfully connected to Azure Cosmos DB! ★★★");
        System.out.println("Database: " + DATABASE_ID);
        System.out.println("Items container: " + ITEM_CONTAINER_ID);
        System.out.println("Requests container: " + REQUEST_CONTAINER_ID + " (partition key: /request_id)");
    }

    // I build a parameterised SQL query so I can apply filters safely without string-injecting raw user input.
    public List<Item> searchItems(String location, Double maxPrice, String category, Boolean available) {
        StringBuilder sql = new StringBuilder("SELECT * FROM c WHERE 1=1");
        List<SqlParameter> params = new ArrayList<>();

        if (location != null && !location.isEmpty()) {
            sql.append(" AND CONTAINS(LOWER(c.location), LOWER(@location))");
            params.add(new SqlParameter("@location", location));
        }
        if (maxPrice != null) {
            sql.append(" AND c.daily_rate <= @maxPrice");
            params.add(new SqlParameter("@maxPrice", maxPrice));
        }
        if (category != null && !category.isEmpty()) {
            sql.append(" AND c.category = @category");
            params.add(new SqlParameter("@category", category));
        }
        if (available != null) {
            sql.append(" AND c.available = @available");
            params.add(new SqlParameter("@available", available));
        }

        SqlQuerySpec querySpec = new SqlQuerySpec();
        querySpec.setQueryText(sql.toString());
        querySpec.setParameters(params);

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();

        return itemContainer.queryItems(querySpec, options, Item.class)
                .stream()
                .collect(Collectors.toList());
    }

    // I query by item_id and return the first match (or null) to keep the calling code simple.
    public Item getItemById(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }

        String queryText = "SELECT * FROM c WHERE c.item_id = @itemId";
        List<SqlParameter> params = List.of(new SqlParameter("@itemId", itemId));

        SqlQuerySpec querySpec = new SqlQuerySpec();
        querySpec.setQueryText(queryText);
        querySpec.setParameters(params);

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();

        return itemContainer.queryItems(querySpec, options, Item.class)
                .stream()
                .findFirst()
                .orElse(null);
    }

    // I create a new rental request, ensuring I include Cosmos's required "id" field and the correct partition key.
    public Rental__Request createRequest(String itemId, String requesterId) {
        Item item = getItemById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }

        String requestId = "req" + System.currentTimeMillis();
        Rental__Request request = new Rental__Request(requestId, itemId, requesterId, "pending");
        request.setRequestDate(new Date().toString());

        Map<String, Object> requestMap = new ObjectMapper().convertValue(request, Map.class);
        requestMap.put("id", requestId);

        requestContainer.createItem(requestMap, new PartitionKey(requestId), new CosmosItemRequestOptions());

        return request;
    }

    // I locate the request by request_id, update status if it is still pending, and then upsert the record back into Cosmos.
    public Rental__Request cancelRequest(String requestId) {
        try {
            String queryText = "SELECT * FROM c WHERE c.request_id = @requestId";
            List<SqlParameter> params = List.of(new SqlParameter("@requestId", requestId));

            SqlQuerySpec querySpec = new SqlQuerySpec();
            querySpec.setQueryText(queryText);
            querySpec.setParameters(params);

            Rental__Request request = requestContainer
                    .queryItems(querySpec, new CosmosQueryRequestOptions(), Rental__Request.class)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (request == null) {
                return null;
            }

            if ("pending".equals(request.getStatus())) {
                request.setStatus("cancelled");

                Map<String, Object> requestMap = new ObjectMapper().convertValue(request, Map.class);
                requestMap.put("id", requestId);

                requestContainer.upsertItem(requestMap, new PartitionKey(requestId), new CosmosItemRequestOptions());
            }

            return request;
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    // I fetch all requests as a simple collection so I can return them to controllers/services without extra paging logic.
    public Collection<Rental__Request> getAllRequests() {
        return requestContainer.queryItems("SELECT * FROM c", new CosmosQueryRequestOptions(), Rental__Request.class)
                .stream()
                .collect(Collectors.toList());
    }

    // I close the Cosmos client when I am done so connections are released cleanly.
    //
    // Because I now use a Singleton CosmosClient shared across requests, I do not close it per repository instance.
    // If I closed it here, another request could fail because the shared client would be closed unexpectedly.
    // I keep this method so I do not remove anything, but I intentionally do not close the shared client.
    public void close() {

    }
}
