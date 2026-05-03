package com.smartcampus.smartcampus;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Configures Jakarta RESTful Web Services for the application.
 */
@ApplicationPath("/api/v1")
public class JakartaRestConfiguration extends ResourceConfig {

	public JakartaRestConfiguration() {
		packages("com.smartcampus.smartcampus");
		register(JacksonFeature.class);
	}
}
