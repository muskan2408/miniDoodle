# Mini Doodle - Meeting Scheduling Platform

A high-performance meeting scheduling platform built with Spring Boot, designed to handle hundreds of users managing thousands of time slots efficiently.

## Features

- **User Management**: Create and manage user profiles
- **Calendar Management**: Personal calendars for each user with timezone support
- **Time Slot Management**:
  - Create, update, and delete available time slots
  - Mark slots as FREE, BUSY, or BOOKED
  - Validate slots to prevent overlaps
  - Configurable duration (15 minutes to 8 hours)
- **Meeting Scheduling**:
  - Convert available slots into meetings
  - Add multiple participants
  - Meeting details (title, description)
  - Manage participants dynamically
- **Availability Queries**: Query free and busy slots within time ranges
- **Concurrent Booking Protection**: Optimistic locking to handle race conditions
- **REST API**: Comprehensive RESTful endpoints
- **API Documentation**: Swagger/OpenAPI documentation
- **Metrics**: Prometheus metrics for monitoring
- **Production Ready**: Docker containerization with PostgreSQL

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Data JPA** with Hibernate
- **PostgreSQL** (Production)
- **H2** (Testing)
- **Docker & Docker Compose**
- **Swagger/OpenAPI** for API documentation
- **Prometheus** for metrics
- **Lombok** for reducing boilerplate
- **Gradle** for build management

## Architecture & Design Decisions

### Domain Model
- **User**: Platform users with unique email addresses
- **Calendar**: One-to-one relationship with User, manages time zones
- **TimeSlot**: Time slots with status (FREE, BUSY, BOOKED) and overlap prevention
- **Meeting**: Scheduled meetings linked to time slots with multiple participants

### Performance Optimizations
1. **Database Indexing**: Strategic indexes on frequently queried fields
   - User email
   - Calendar userId
   - TimeSlot calendar+time range
   - Meeting participants
2. **Pessimistic Locking**: Prevents concurrent booking conflicts
3. **Optimistic Locking**: Version control on TimeSlot for updates
4. **Lazy Loading**: Optimized entity relationships to reduce database queries
5. **Query Optimization**: Custom JPQL queries with proper JOIN strategies

### Scalability Considerations
- Connection pooling with HikariCP (20 max connections)
- Stateless REST API design
- Dockerized deployment for horizontal scaling
- Database indexes for query performance
- Metrics endpoint for monitoring bottlenecks

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- (Optional) Java 17+ and Gradle 8+ for local development

## Quick Start

### Running with Docker Compose (Recommended)

1. **Clone the repository**:
```bash
git clone <repository-url>
cd MiniDoodle
```

2. **Start all services**:
```bash
docker-compose up --build
```

This will start:
- PostgreSQL database on port 5432
- Mini Doodle application on port 8080
- Prometheus on port 9090

3. **Access the application**:
- API Base URL: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs
- Health Check: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Prometheus: http://localhost:9090

### Local Development

1. **Start PostgreSQL** (or use H2 by setting `spring.profiles.active=test`):
```bash
docker run -d \
  --name postgres \
  -e POSTGRES_DB=minidoodle \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

2. **Run the application**:
```bash
./gradlew bootRun
```

## API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

### Endpoints Overview

#### User Management
- `POST /users` - Create a new user
- `GET /users` - Get all users
- `GET /users/{id}` - Get user by ID
- `GET /users/email/{email}` - Get user by email
- `PUT /users/{id}` - Update user
- `DELETE /users/{id}` - Delete user

#### Time Slot Management
- `POST /timeslots/users/{userId}` - Create time slot for user
- `GET /timeslots/{id}` - Get time slot by ID
- `PUT /timeslots/{id}` - Update time slot
- `DELETE /timeslots/{id}` - Delete time slot
- `PATCH /timeslots/{id}/status?status={FREE|BUSY|BOOKED}` - Update slot status
- `PATCH /timeslots/{id}/mark-busy` - Mark slot as busy
- `PATCH /timeslots/{id}/mark-free` - Mark slot as free
- `GET /timeslots/users/{userId}?startTime={ISO8601}&endTime={ISO8601}` - Get slots in time range
- `GET /timeslots/users/{userId}/availability?startTime={ISO8601}&endTime={ISO8601}` - Get availability

#### Meeting Management
- `POST /meetings` - Create meeting from time slot
- `GET /meetings/{id}` - Get meeting by ID
- `PUT /meetings/{id}` - Update meeting
- `DELETE /meetings/{id}` - Cancel meeting
- `GET /meetings/users/{userId}?startTime={ISO8601}&endTime={ISO8601}` - Get user's meetings
- `GET /meetings/users/{userId}/owned?startTime={ISO8601}&endTime={ISO8601}` - Get meetings owned by user
- `POST /meetings/{meetingId}/participants/{userId}` - Add participant
- `DELETE /meetings/{meetingId}/participants/{userId}` - Remove participant

### Example Requests

#### 1. Create a User
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com"
  }'
```

Response:
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T10:00:00"
}
```

#### 2. Create a Time Slot
```bash
curl -X POST http://localhost:8080/api/v1/timeslots/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2025-01-20T10:00:00",
    "endTime": "2025-01-20T11:00:00"
  }'
```

Or with duration:
```bash
curl -X POST http://localhost:8080/api/v1/timeslots/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2025-01-20T10:00:00",
    "durationMinutes": 60
  }'
```

#### 3. Get User Availability
```bash
curl "http://localhost:8080/api/v1/timeslots/users/1/availability?startTime=2025-01-20T00:00:00&endTime=2025-01-21T00:00:00"
```

Response:
```json
{
  "freeSlots": [
    {
      "id": 1,
      "calendarId": 1,
      "startTime": "2025-01-20T10:00:00",
      "endTime": "2025-01-20T11:00:00",
      "status": "FREE",
      "durationMinutes": 60,
      "createdAt": "2025-01-15T10:00:00",
      "updatedAt": "2025-01-15T10:00:00"
    }
  ],
  "busySlots": [],
  "totalFreeSlots": 1,
  "totalBusySlots": 0
}
```

#### 4. Create a Meeting
```bash
curl -X POST http://localhost:8080/api/v1/meetings \
  -H "Content-Type: application/json" \
  -d '{
    "timeSlotId": 1,
    "title": "Project Kickoff",
    "description": "Initial project planning meeting",
    "participantIds": [2, 3]
  }'
```

#### 5. Get User's Meetings
```bash
curl "http://localhost:8080/api/v1/meetings/users/1?startTime=2025-01-20T00:00:00&endTime=2025-01-21T00:00:00"
```

## Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run with Coverage
```bash
./gradlew test jacocoTestReport
```

## Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
# All metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Prometheus format
curl http://localhost:8080/actuator/prometheus
```

### Prometheus Dashboard
Access Prometheus at http://localhost:9090 to view:
- Request rates
- Response times
- JVM metrics
- Database connection pool stats

## Database Schema

```sql
-- Users table
users (id, name, email, created_at, updated_at)

-- Calendars table
calendars (id, user_id, timezone, created_at, updated_at)

-- Time slots table
time_slots (id, calendar_id, start_time, end_time, status, version, created_at, updated_at)

-- Meetings table
meetings (id, time_slot_id, title, description, created_at, updated_at)

-- Meeting participants (many-to-many)
meeting_participants (meeting_id, user_id)
```

## Configuration

### Environment Variables
- `DB_HOST`: Database host (default: localhost)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name (default: minidoodle)
- `DB_USER`: Database user (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)
- `SPRING_PROFILES_ACTIVE`: Active profile (default, test)

### Application Properties
See `src/main/resources/application.yml` for configuration options.

## Future Enhancements

1. **Authentication & Authorization**: Add Spring Security with JWT
2. **Recurring Meetings**: Support for recurring time slots
3. **Email Notifications**: Send meeting invites and reminders
4. **Time Zone Handling**: Better timezone conversion for participants
5. **Conflict Resolution**: Advanced scheduling algorithms
6. **GraphQL API**: Alternative to REST for complex queries
7. **Caching**: Redis integration for frequently accessed data
8. **Event Sourcing**: Track all state changes for audit trail
9. **WebSocket**: Real-time updates for calendar changes
10. **Rate Limiting**: API rate limiting for fair usage

## License

Apache License 2.0

## Support

For issues and questions, please open an issue in the repository.
