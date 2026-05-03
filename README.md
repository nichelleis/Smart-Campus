# Smart Campus – Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)  
**University of Westminster – School of Computer Science and Engineering**

---

## API Design Overview

The Smart Campus API is a RESTful web service built with JAX-RS (Jersey) that models the physical structure of a university campus. Rooms contain sensors, and sensors hold a history of readings — this structure is reflected directly in the URL hierarchy.

```
/api/v1                                  ← Discovery root
/api/v1/rooms                            ← Room collection
/api/v1/rooms/{roomId}                   ← Individual room
/api/v1/sensors                          ← Sensor collection (filter with ?type=)
/api/v1/sensors/{sensorId}              ← Individual sensor
/api/v1/sensors/{sensorId}/readings     ← Reading history (sub-resource)
```

**Key design decisions:**

- `@ApplicationPath("/api/v1")` in `JakartaRestConfiguration` sets the versioned base path
- A HATEOAS discovery endpoint at `GET /api/v1` returns links to all resource collections so clients never need to hard-code URLs
- The sub-resource locator pattern in `SensorResource` delegates reading management to a dedicated `SensorReadingResource` class
- All exceptions are caught by `@Provider` mappers and returned as consistent JSON — raw stack traces never reach the client
- All shared data lives in `InMemoryStore` as `static` `ConcurrentHashMap` fields, making it safe across JAX-RS's per-request resource instantiation and concurrent threads
- Referential integrity is enforced at write time: a sensor cannot be created with a non-existent `roomId`, and a room cannot be deleted while sensors are assigned to it

---

## Table of Contents

1. [Technology Stack](#1-technology-stack)
2. [Project Structure](#2-project-structure)
3. [How to Build and Run](#3-how-to-build-and-run)
4. [Sample curl Commands](#4-sample-curl-commands)
5. [API Reference](#5-api-reference)
6. [Error Handling](#6-error-handling)

---

## 1. Technology Stack

| Component | Details |
|---|---|
| Language | Java 11 |
| Framework | Jersey (JAX-RS 2.1) |
| Build Tool | Maven |
| JSON Serialisation | Jackson via `JacksonFeature` |
| Data Storage | In-memory `ConcurrentHashMap` — no database |
| API Base Path | `/api/v1` |

---

## 2. Project Structure

```
src/main/java/com/smartcampus/smartcampus/
├── JakartaRestConfiguration.java        # @ApplicationPath, registers Jersey + Jackson
├── exceptions/
│   ├── LinkedResourceNotFoundException.java
│   ├── ResourceNotFoundException.java
│   ├── RoomNotEmptyException.java
│   └── SensorUnavailableException.java
├── filters/
│   └── ApiLoggingFilter.java            # Logs every request and response
├── mappers/
│   ├── GlobalExceptionMapper.java       # Catches all Throwable → 500
│   ├── IllegalArgumentExceptionMapper.java  → 400
│   ├── LinkedResourceNotFoundExceptionMapper.java  → 422
│   ├── ResourceNotFoundExceptionMapper.java  → 404
│   ├── RoomNotEmptyExceptionMapper.java  → 409
│   └── SensorUnavailableExceptionMapper.java  → 403
├── model/
│   ├── ApiError.java
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── resources/
│   ├── DiscoveryResource.java           # GET /api/v1
│   ├── RoomResource.java                # /api/v1/rooms
│   ├── SensorResource.java              # /api/v1/sensors
│   └── SensorReadingResource.java       # /api/v1/sensors/{sensorId}/readings
└── store/
    └── InMemoryStore.java               # Static thread-safe data store
```

---

## 3. How to Build and Run

### Prerequisites

- Java JDK 11 or higher
- Maven 3.6 or higher
- Apache Tomcat 9

### Step 1 — Clone the repository

```bash
git clone https://github.com/your-username/your-repo-name.git
cd your-repo-name
```

### Step 2 — Build the project

```bash
mvn clean package
```

This compiles the project and produces a `.war` file inside the `target/` folder.

### Step 3 — Deploy to Tomcat

Copy the generated WAR file into your Tomcat `webapps` directory:

```bash
cp target/smartcampus.war /path/to/tomcat/webapps/
```

### Step 4 — Start Tomcat

```bash
/path/to/tomcat/bin/startup.sh
```

On Windows:
```bash
\path\to\tomcat\bin\startup.bat
```

### Step 5 — Verify the server is running

Open a browser or send a request to the discovery endpoint:

```
http://localhost:8080/smartcampus/api/v1
```

You should see:

```json
{
  "service": "Smart Campus Sensor & Room Management API",
  "version": "v1",
  "contact": {
    "team": "Smart Campus Backend",
    "email": "smartcampus-admin@westminster.ac.uk"
  },
  "resources": {
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors",
    "sensorReadings": "/api/v1/sensors/{sensorId}/readings"
  }
}
```

---

## 4. Sample curl Commands

All commands below assume the server is running at `http://localhost:8080/smartcampus`.

### 1. Get the API discovery response

```bash
curl -X GET http://localhost:8080/smartcampus/api/v1
```

### 2. Create a new room

```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Library Quiet Study", "capacity": 50}'
```

Expected response: `201 Created` with the created room including its auto-generated ID.

### 3. List all rooms

```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/rooms
```

### 4. Register a sensor in an existing room

Replace `ROOM-xxxxxxxx` with the room ID returned from command 2.

```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "Temperature", "status": "ACTIVE", "currentValue": 21.0, "roomId": "ROOM-xxxxxxxx"}'
```

Expected response: `201 Created` with the created sensor including its auto-generated ID.

### 5. Filter sensors by type

```bash
curl -X GET "http://localhost:8080/smartcampus/api/v1/sensors?type=Temperature"
```

### 6. Post a reading for a sensor

Replace `SNS-xxxxxxxx` with the sensor ID returned from command 4.

```bash
curl -X POST http://localhost:8080/smartcampus/api/v1/sensors/SNS-xxxxxxxx/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 23.5}'
```

Expected response: `201 Created`. Also updates the sensor's `currentValue` to `23.5`.

### 7. Get all readings for a sensor

```bash
curl -X GET http://localhost:8080/smartcampus/api/v1/sensors/SNS-xxxxxxxx/readings
```

### 8. Attempt to delete a room that still has sensors (expect 409)

```bash
curl -X DELETE http://localhost:8080/smartcampus/api/v1/rooms/ROOM-xxxxxxxx
```

Expected response: `409 Conflict` — room cannot be deleted while sensors are assigned.

---

## 5. API Reference

Base URL: `http://localhost:8080/smartcampus/api/v1`  
All endpoints consume and produce `application/json`.

### Discovery

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/v1` | Returns service metadata and resource links | 200 OK |

---

### Rooms

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/v1/rooms` | List all rooms | 200 OK |
| POST | `/api/v1/rooms` | Create a new room | 201 Created |
| GET | `/api/v1/rooms/{roomId}` | Get a room by ID | 200 OK |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (fails if sensors assigned) | 204 No Content |

**POST request body:**
```json
{
  "name": "Seminar Room B",
  "capacity": 40
}
```

- `name` is required — returns `400 Bad Request` if missing
- `id` is optional — auto-generated as `ROOM-{uuid}` if not provided
- DELETE returns `409 Conflict` if sensors are still assigned to the room
- DELETE returns `404 Not Found` if the room does not exist

---

### Sensors

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/v1/sensors` | List all sensors | 200 OK |
| GET | `/api/v1/sensors?type={type}` | Filter sensors by type | 200 OK |
| POST | `/api/v1/sensors` | Register a new sensor | 201 Created |
| GET | `/api/v1/sensors/{sensorId}` | Get a sensor by ID | 200 OK |

**POST request body:**
```json
{
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 410.0,
  "roomId": "ROOM-xxxxxxxx"
}
```

- `type` and `roomId` are required — returns `400 Bad Request` if missing
- `roomId` must reference an existing room — returns `422 Unprocessable Entity` if not found
- `id` is optional — auto-generated as `SNS-{uuid}` if not provided
- `status` defaults to `ACTIVE` if not provided. Valid values: `ACTIVE`, `MAINTENANCE`, `OFFLINE`

---

### Sensor Readings

| Method | Path | Description | Response |
|---|---|---|---|
| GET | `/api/v1/sensors/{sensorId}/readings` | Get all readings for a sensor | 200 OK |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a new reading | 201 Created |

**POST request body:**
```json
{
  "value": 23.5
}
```

- `id` and `timestamp` are auto-generated by the server if not provided
- Posting a reading also updates the parent sensor's `currentValue` field
- Returns `403 Forbidden` if the sensor status is `MAINTENANCE`
- Returns `404 Not Found` if the sensor does not exist

---

## 6. Error Handling

All error responses use a consistent JSON format:

```json
{
  "timestamp": 1714000000000,
  "status": 409,
  "error": "Conflict",
  "message": "Room ROOM-xxxxxxxx cannot be deleted while sensors are still assigned.",
  "path": "rooms/ROOM-xxxxxxxx"
}
```

| Scenario | Exception | Status |
|---|---|---|
| Resource not found | `ResourceNotFoundException` | 404 Not Found |
| Delete room with sensors still assigned | `RoomNotEmptyException` | 409 Conflict |
| Sensor created with non-existent roomId | `LinkedResourceNotFoundException` | 422 Unprocessable Entity |
| POST reading to a sensor in MAINTENANCE | `SensorUnavailableException` | 403 Forbidden |
| Missing or invalid request fields | `IllegalArgumentException` | 400 Bad Request |
| Any unexpected runtime error | `Throwable` (catch-all) | 500 Internal Server Error |

Raw Java stack traces are never returned to clients. All exceptions are intercepted by `@Provider` mappers and full stack traces are written to the server log only.
