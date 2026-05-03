package com.smartcampus.smartcampus.resources;

import com.smartcampus.smartcampus.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.smartcampus.exceptions.ResourceNotFoundException;
import com.smartcampus.smartcampus.model.Room;
import com.smartcampus.smartcampus.model.Sensor;
import com.smartcampus.smartcampus.store.InMemoryStore;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public List<Sensor> listSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(InMemoryStore.getInstance().getSensors().values());
        if (type == null || type.trim().isEmpty()) {
            return sensors;
        }

        return sensors.stream()
                .filter(sensor -> type.equalsIgnoreCase(sensor.getType()))
                .collect(Collectors.toList());
    }

    @GET
    @Path("{sensorId}")
    public Sensor getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = InMemoryStore.getInstance().getSensors().get(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor not found: " + sensorId);
        }
        return sensor;
    }

    @POST
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        validateSensorPayload(sensor);

        Room linkedRoom = InMemoryStore.getInstance().getRooms().get(sensor.getRoomId());
        if (linkedRoom == null) {
            throw new LinkedResourceNotFoundException("The supplied roomId does not exist: " + sensor.getRoomId());
        }

        if (isBlank(sensor.getId())) {
            sensor.setId("SNS-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (isBlank(sensor.getStatus())) {
            sensor.setStatus("ACTIVE");
        }

        InMemoryStore.getInstance().getSensors().put(sensor.getId(), sensor);

        if (linkedRoom.getSensorIds() == null) {
            linkedRoom.setSensorIds(new CopyOnWriteArrayList<>());
        }
        if (!linkedRoom.getSensorIds().contains(sensor.getId())) {
            linkedRoom.getSensorIds().add(sensor.getId());
        }

        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    @Path("{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        Sensor sensor = InMemoryStore.getInstance().getSensors().get(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor not found for readings path: " + sensorId);
        }
        return new SensorReadingResource(sensorId);
    }

    private void validateSensorPayload(Sensor sensor) {
        if (sensor == null || isBlank(sensor.getType()) || isBlank(sensor.getRoomId())) {
            throw new IllegalArgumentException("Sensor payload must include type and roomId.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
