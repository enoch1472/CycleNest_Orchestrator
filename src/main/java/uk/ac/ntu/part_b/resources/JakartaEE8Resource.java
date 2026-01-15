package uk.ac.ntu.part_b.resources;

import javax.ws.rs.GET;              // ✅ CHANGED
import javax.ws.rs.Path;             // ✅ CHANGED
import javax.ws.rs.core.Response;    // ✅ CHANGED

@Path("rest")
public class JakartaEE8Resource {

    @GET
    public Response ping() {
        return Response.ok("ping").build();
    }
}
