package uk.ac.ntu.part_b;

import javax.ws.rs.core.Application;
import java.util.Set;
import java.util.HashSet;

// I rely on web.xml for the base JAX-RS mapping, so I do not use @ApplicationPath here.
public class JakartaRestConfiguration extends Application {

    // I log startup so I can confirm the JAX-RS application is being discovered and loaded at runtime.
    public JakartaRestConfiguration() {
        System.out.println("★★★ JAX-RS Configuration LOADED! ★★★");
    }

    @Override
    public Set<Class<?>> getClasses() {
        // I return an explicit class set so I control exactly which resources get registered.
        Set<Class<?>> classes = new HashSet<>();

        // I register only the OrchestratorResource because it centralises the external OSRM consumption in my design.
        classes.add(OrchestratorResource.class);
        System.out.println("★★★ Registering OrchestratorResource ★★★");

        return classes;
    }
}
