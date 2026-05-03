package com.smartcampus.smartcampus.resources;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Map<String, Object> discovery() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "Smart Campus Sensor & Room Management API");
        response.put("version", "v1");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("team", "Smart Campus Backend");
        contact.put("email", "smartcampus-admin@westminster.ac.uk");
        response.put("contact", contact);

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        resources.put("sensorReadings", "/api/v1/sensors/{sensorId}/readings");
        response.put("resources", resources);

        return response;
    }
}
