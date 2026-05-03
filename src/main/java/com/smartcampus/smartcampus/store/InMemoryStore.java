package com.smartcampus.smartcampus.store;

import com.smartcampus.smartcampus.model.Room;
import com.smartcampus.smartcampus.model.Sensor;
import com.smartcampus.smartcampus.model.SensorReading;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory data store (Singleton).
 *
 * JAX-RS creates a new resource class instance per request by default, so all
 * shared state must live outside the resource classes. ConcurrentHashMap is
 * used
 * so that concurrent requests do not corrupt the collections.
 *
 * This class is initialised once with seed data so the API can be tested
 * immediately.
 */
public final class InMemoryStore {

    // Volatile + double-checked locking for safe lazy initialisation.
    private static volatile InMemoryStore instance;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    public static final Map<String, Room> ROOMS = getInstance().getRooms();
    public static final Map<String, Sensor> SENSORS = getInstance().getSensors();
    public static final Map<String, List<SensorReading>> READINGS = getInstance().getSensorReadings();

    private InMemoryStore() {
        seedData();
    }

    public static InMemoryStore getInstance() {
        if (instance == null) {
            synchronized (InMemoryStore.class) {
                if (instance == null) {
                    instance = new InMemoryStore();
                }
            }
        }
        return instance;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Map<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }

    public static List<SensorReading> readingsFor(String sensorId) {
        return getInstance().getSensorReadings().computeIfAbsent(sensorId, key -> new ArrayList<>());
    }

    private void seedData() {
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Lab 1", 30);
        Room r3 = new Room("HALL-01", "Main Hall", 200);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 29.7, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 430.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LAB-101");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        r1.getSensorIds().add(s1.getId());
        r1.getSensorIds().add(s2.getId());
        r2.getSensorIds().add(s3.getId());

        sensorReadings.put(s1.getId(), new ArrayList<>());
        sensorReadings.put(s2.getId(), new ArrayList<>());
        sensorReadings.put(s3.getId(), new ArrayList<>());
    }
}
