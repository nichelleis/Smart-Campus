package com.smartcampus.smartcampus.resources;

import com.smartcampus.smartcampus.exceptions.ResourceNotFoundException;
import com.smartcampus.smartcampus.exceptions.SensorUnavailableException;
import com.smartcampus.smartcampus.model.Sensor;
import com.smartcampus.smartcampus.model.SensorReading;
import com.smartcampus.smartcampus.store.InMemoryStore;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public List<SensorReading> getReadings() {
        return InMemoryStore.readingsFor(sensorId);
    }

    @POST
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        Sensor sensor = InMemoryStore.getInstance().getSensors().get(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor not found: " + sensorId);
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                    "Sensor " + sensorId + " is in MAINTENANCE and cannot accept readings.");
        }

        if (reading == null) {
            throw new IllegalArgumentException("Reading payload is required.");
        }
        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }

        InMemoryStore.readingsFor(sensorId).add(reading);

        // Keep parent sensor currentValue synchronized with latest reading.
        sensor.setCurrentValue(reading.getValue());
        InMemoryStore.getInstance().getSensors().put(sensorId, sensor);

        URI location = uriInfo.getAbsolutePathBuilder().path(reading.getId()).build();
        return Response.created(location).entity(reading).build();
    }
}
