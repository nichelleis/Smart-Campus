# Smart Campus – Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)  
**University of Westminster – School of Computer Science**

---
	
Name	Nichelle Jeewandara

UOW ID	w2153578

IIT ID	20240293



## Video Demonstration & Conceptual Report

[View Video Demonstration ](https://drive.google.com/file/d/1Gc7dvw0O9BZYmv5UjTxHm-RkxGbszdbb/view?usp=drive_link)

[Conceptual Report](https://drive.google.com/file/d/1bmtPHiQ2au8zfKHtg3F63i9PV7S1uceW/view?usp=drive_link)




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
- All exceptions are caught by `@Provider` mappers and returned as consistent JSON raw stack traces never reach the client
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

Expected response: `409 Conflict` room cannot be deleted while sensors are assigned.

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

- `name` is required returns `400 Bad Request` if missing
- `id` is optional auto-generated as `ROOM-{uuid}` if not provided
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

- `type` and `roomId` are required returns `400 Bad Request` if missing
- `roomId` must reference an existing room returns `422 Unprocessable Entity` if not found
- `id` is optional auto-generated as `SNS-{uuid}` if not provided
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





# Part 1 - Service Architecture & Setup
## Part 1.1 JAX-RS Resource Lifecycle & In-Memory Data Management
JAX-RS creates a new instance of a resource class for each HTTP request. For example, every request for GET /api/v1/rooms creates a new instance of the class RoomResource, handles the request, and is discarded. The drawback to this approach is that class instance fields are empty with each new HTTP request, and nothing persists.
To mitigate this, all data in this project is encapsulated in a separate class, InMemoryStore, where data is stored in static fields. Static fields belong to the class, and persist as long as the server is running, therefore data is kept regardless of the number of times resource instances are created or discarded.
The other consideration is regarding concurrent data access. Since multiple HTTP requests are handled in separate threads, data may become corrupt if concurrent access is not handled. For this reason, each of the three stores (ROOMS, SENSORS, READINGS) is implemented using ConcurrentHashMap with CopyOnWriteArrayList for the sensor ID and reading lists. The data structures used are safe for concurrent data access and therefore handle the requirement of concurrent data access.

## Part 1.2 HATEOAS and Hypermedia in RESTful Design
HATEOAS stands for Hypermedia as the Engine of Application State. The idea is that responses from the API carry links that tell the client what it can do next, rather than the client needing to already know every URL. 
The DiscoveryResource in this project implements a version of this at GET /api/v1, returning the names and paths of all main resource collections. A client that hits this endpoint first can then navigate the rest of the API purely from the links it receives, without consulting any external documentation.
This matters for client developers because it removes tight coupling between the client and server. If a URL changes on the server side, a client that reads its paths from responses can adapt without a code change. Static documentation does not offer that it goes out of date and any change breaks clients that relied on hard-coded paths.

# Part 2 - Room Management
## Part 2.1 Returning IDs vs. Full Room Objects
There are two ways to handle GET /api/v1/rooms, return just the room IDs, or return the complete room objects. Returning only IDs keeps the payload small but pushes the problem onto the client, which then has to make a separate request for every ID to get any useful information. 
This implementation returns full room objects in one go, which means the client has everything it needs name, capacity, sensor IDs from a single request. For a campus management dashboard that needs to display this information immediately, that is the right trade-off. The only scenario where returning IDs makes more sense is when the collection is extremely large, and in that case the better solution is proper pagination, not stripping out the data.

## Part 2.2 Idempotency of DELETE
Idempotency means the server ends up in the same state whether you send a request once or ten times. HTTP requires DELETE to behave this way.
In this implementation, the first DELETE on a valid empty room removes it from the store and returns 204 No Content. If the same request is sent again, the room is already gone so a ResourceNotFoundException is thrown and 404 Not Found comes back. The response code is different, but that does not break idempotency 
There is one edge case worth noting. If DELETE is attempted on a room that still has sensors assigned, a RoomNotEmptyException is thrown and 409 Conflict is returned. This will keep happening consistently until the sensors are removed, so even this case behaves predictably.

# Part 3 - Sensor Operations & Linking
## Part 3.1 “@Consumes(MediaType.APPLICATION_JSON)” and Format Mismatches
The @Consumes(MediaType.APPLICATION_JSON) annotation on createSensor tells JAX-RS that this method will only deal with application/json request bodies. If a client sends something with Content-Type: text/plain or Content-Type: application/xml, JAX-RS checks the header before it even touches the method. Finding no matching reader, it immediately sends back HTTP 415 Unsupported Media Type and the method never runs at all.
This is useful because it means the framework itself acts as the first line of defence against bad input. No partial parsing happens, no null objects get passed into the method, and no extra validation code is needed to handle wrong content types. The annotation makes the contract explicit and the enforcement automatic.

## Part 3.2 “@QueryParam” vs. Path Segment for Filtering
To filter the sensors by type, get /api/v1/sensors?type=CO2 is called by using @QueryParam. Another way of expressing this filter would be /api/v1/sensors/type/CO2, where the actual type filter is integrated into the path.

For a few reasons, the query parameter/facilitating approach is the best expressive type filter. When it comes to the semantics of expressive type filter, path segments are supposed to be used as type filter identifiers. However, the expressive type filter, with query parameters, is also a means of filter addition. A path-based approach would, rather, be expressing a filter. filter addition would quickly be inhibited.

# Part 4 - Deep Nesting with Sub-Resources
## Part 4.1 Architectural Benefits of the Sub-Resource Locator Pattern
The reading endpoints are not hardcoded in SensorResource. A locator method provides a SensorReadingResource instance, and JAX-RS operates from there. The locator validates that a sensor exists before delegation, so it catches invalid sensor IDs at an early stage.
The main advantage of the current structure is that it simplifies class responsibilities. SensorResource is responsible for sensor creation and retrieval. SensorReadingResource is in charge of reading history. The classes provide separation of concern, as neither class needs to know how the other operates. They also remain concise as they do not attempt to handle multiple responsibilities at once.
Additionally, the current structure simplifies testing. SensorReadingResource needs only a sensorId to be instantiated and can then be tested without the routing stack. SensorId is a constructor argument, so it is self-contained concerning context, and the code requirements are already met. For future requirements concerning sub-resources, for example, alerts and calibration logs, new classes and locators can be implemented, while the current structure is left untouched.

# Part 5 - Advanced Error Handling, Exception Mapping & Logging
## Part 5.2 HTTP 422 vs. HTTP 404 for Missing Referenced Resources
When a sensor is created with a roomId that is undefined, a 404 Not Found error should not be expected to be returned. A 404 error means that the requested URL is undefined. A 404 error would make the client think that they typed the wrong URL, but that is not the case at all.
In this case, a 422 Unprocessable Entity is the best response. A 422 Unprocessable Entity means that the server is currently unable to process the request because the server understands the request and the syntax of the request is correct, but it cannot, for instance, process the request because the roomId is that undefined request. This means a 422 Unprocessable Entity response should be sent back to the client.

## Part 5.4 Security Risks of Exposing Java Stack Traces
Returning raw stack traces in API responses is a well-known security mistake. The most immediate risk is that it reveals exactly what technology the server is running framework names, version numbers, package paths which an attacker can use to look up known vulnerabilities and target them directly. 
Beyond that, stack traces expose the internal structure of the code. Package names like com.smartcampus.smartcampus.store.InMemoryStore show how the application is organised. Method names and call sequences reveal business logic and the order that validation steps run in, which could help someone craft a request designed to slip through a gap. Even a simple NullPointerException with a line number tells an attacker exactly where something broke and gives them a starting point to probe further.
The GlobalExceptionMapper prevents all of this by catching any unhandled Throwable, logging the full details privately on the server, and returning nothing more than a generic 500 message to the client. There is nothing an attacker can use in that response.

## Part 5.5 JAX-RS Filters for Cross-Cutting Concerns vs. Inline Logging
The ApiLoggingFilter automatically captures every incoming request and every outgoing response, whether or not resources know they exist. Alternatively, developers may need to include Logger.info() calls in every resource method, which comes with multiple challenges as the project expands.

The biggest challenge comes from the added and mandated boilerplate. Since each resource method must include the same boilerplate to include request/response logging, anyone adding a new method could potentially forget to include the logging, creating an unlogged endpoint. The ApiLoggingFilter provides an implicit guarantee to ensure that the outcome of each endpoint method is logged, as it automatically captures logging for every request, including anything that prevents a request from reaching a method, such as a 415 response.

Furthermore, it provides developers with a cleaner separation of responsibilities. The resource method’s focus should be on the main action it is performing. Anything extraneous to the core action of the resource method only serves to make the method harder to understand and the code more challenging to test. Having the logging statements isolated to the ApiLoggingFilter serves the purpose of making logging one singular responsibility. If changes to the log format are needed, they only need to be made in one place, and with the logging being isolated, nothing will be missed if the API is in the process of being expanded.



>

