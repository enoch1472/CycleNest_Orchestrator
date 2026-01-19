package uk.ac.ntu.part_b;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.DirectConnectionConfig;

/**
 * I created this provider so my application uses ONE CosmosClient instance (Singleton),
 * instead of creating a new Cosmos client every time a request hits my API.
 *
 * This is important because CosmosClient is designed to be reused and is expensive to create.
 * Under JMeter load, creating many clients can exhaust connections/resources and cause 500s.
 */
public final class CosmosClientProvider {

    // I keep a single shared client instance here so all my requests reuse it.
    private static volatile CosmosClient client;

    // I block construction because I only want static access.
    private CosmosClientProvider() { }

    /**
     * I lazily create my CosmosClient the first time it is needed, then reuse it forever.
     * This pattern prevents connection storms under stress tests.
     */
    public static CosmosClient getClient() {
        if (client == null) {
            synchronized (CosmosClientProvider.class) {
                if (client == null) {

                    // I optionally tune direct mode networking so I do not open excessive connections under load.
                    // These values are conservative defaults I can adjust later if I profile performance.
                    DirectConnectionConfig direct = new DirectConnectionConfig();
                    direct.setMaxConnectionsPerEndpoint(20);
                    direct.setMaxRequestsPerConnection(30);

                    client = new CosmosClientBuilder()
                            .endpoint(Item__Rep.ENDPOINT)
                            .key(Item__Rep.KEY)
                            .consistencyLevel(ConsistencyLevel.SESSION)
                            .directMode(direct)
                            .buildClient();

                    System.out.println("★★★ CosmosClient Singleton initialised (shared across requests) ★★★");
                }
            }
        }
        return client;
    }
}
