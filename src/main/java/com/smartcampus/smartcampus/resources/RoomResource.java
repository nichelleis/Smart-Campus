package com.smartcampus.smartcampus.resources;

import com.smartcampus.smartcampus.exceptions.ResourceNotFoundException;
import com.smartcampus.smartcampus.exceptions.RoomNotEmptyException;
import com.smartcampus.smartcampus.model.Room;
import com.smartcampus.smartcampus.store.InMemoryStore;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public List<Room> listRooms() {
        return new ArrayList<>(InMemoryStore.getInstance().getRooms().values());
    }

    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        validateRoomPayload(room);

        if (isBlank(room.getId())) {
            room.setId("ROOM-" + UUID.randomUUID().toString().substring(0, 8));
        }

        InMemoryStore.getInstance().getRooms().put(room.getId(), room);

        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("{roomId}")
    public Room getRoom(@PathParam("roomId") String roomId) {
        Room room = InMemoryStore.getInstance().getRooms().get(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }
        return room;
    }

    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = InMemoryStore.getInstance().getRooms().get(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room " + roomId + " cannot be deleted while sensors are still assigned.");
        }

        InMemoryStore.getInstance().getRooms().remove(roomId);
        return Response.noContent().build();
    }

    private void validateRoomPayload(Room room) {
        if (room == null || isBlank(room.getName())) {
            throw new IllegalArgumentException("Room payload must include at least a non-empty name.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
