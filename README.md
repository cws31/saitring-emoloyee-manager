Saitring Employee Manager
A production-oriented Employee & Workforce Management REST API built with Java 21 and Spring Boot 3.4.2.
The application is designed for an organization owner/administrator to manage employees, daily attendance, employee advances, settlements/payments, monthly account closing (Hisab), owner profile information, and authentication.
It also includes production-focused capabilities such as JWT security, OTP verification, Redis-backed rate limiting, database migrations, API documentation, health/metrics endpoints, distributed tracing, structured logging, and an Elasticsearch + Logstash + Kibana (ELK) observability stack.
Repository scope: The supplied project is a Spring Boot backend. No React/Vite frontend source code is present in this archive.

Table of Contents
- What the Application Does
- Core Features
- Architecture
- Technology Stack
- Project Structure
- Business Modules
- Authentication and Security
- OTP Security
- Database Design
- API Reference
- Observability and Monitoring
- Logging and ELK Stack
- File and Logo Management
- Configuration
- Environment Variables
- Local Development
- Docker
- API Documentation
- Database Migrations
- Testing
- Production Considerations
- Business Calculation Logic
- Important Implementation Notes
What the Application Does
Saitring Employee Manager is a multi-owner workforce management backend.
Each authenticated owner works with their own organization and its employees. Business records are associated with an owner_id, which allows the application to keep employee, attendance, advance, settlement, and monthly accounting data separated between owners.
The main workflow is:
Owner Registration
       │
       ▼
Owner Login
       │
       ▼
Password Verification
       │
       ▼
OTP Verification
       │
       ▼
JWT Token
       │
       ▼
Authenticated API Access
       │
       ├── Employee Management
       ├── Attendance
       ├── Advances
       ├── Settlements
       ├── Monthly Hisab
       ├── Dashboard
       └── Owner Profile
Core Features
1. Owner / Administrator Management
The owner represents the organization administrator.
Supported functionality:
- Owner registration
- Organization information
- Email and mobile number
- Password-based first authentication factor
- OTP-based second authentication factor
- JWT token generation
- Owner profile retrieval
- Owner profile update
- Organization logo upload
- Organization logo retrieval
- Organization logo deletion
Owner profile supports:
- Owner name
- Organization name
- Email
- Mobile number
- Address line 1
- Address line 2
- City
- State
- Country
- Postal code
- Website
- Logo
2. Employee Management
Employees belong to an owner/organization.
Supported operations:
- Add employee
- Update employee
- Delete employee
- Get all employees
- Get employee by ID
- Block/unblock employee
Employee information includes:
- Name
- Mobile number
- Initial rate
- Blocked/active status
- Creation timestamp
- Owner association
The database enforces uniqueness of an employee's mobile number within the same owner:
(owner_id, mobile)
3. Attendance Management
The attendance module records an employee's attendance for a specific date.
Supported operations:
- Mark attendance
- Update an existing attendance record for the same employee/date
- Get attendance for a month
- Get attendance for one employee for a month
Attendance contains:
- Employee
- Owner
- Attendance date
- Attendance status
- Reason
- Creation timestamp
The database prevents duplicate attendance records for the same:
owner + employee + date
This makes attendance marking effectively an update-or-create operation.
4. Employee Advances
The advance module records money paid to employees before final settlement.
Supported operations:
- Record advance
- View monthly advances
- Update advance
- Delete advance
An advance contains:
- Employee
- Owner
- Amount
- Payment date
- Note
Advance data is indexed by owner and payment date for efficient monthly queries.
5. Employee Settlements
Settlements represent money actually paid to an employee.
Supported operations:
- Create settlement
- Update settlement
- Delete settlement
- Get all settlements
- Get settlements for a specific employee
Settlement information includes:
- Employee
- Owner
- Amount paid
- Settlement date
- Note
- Creation timestamp
6. Monthly Hisab / Account Closing
Hisab is the monthly accounting/closing module.
A month closing contains:
- Year
- Month
- Closing timestamp
- Total employees
- Total payable
- Total over-advance
- Owner
- Employee-level closing details
Each employee's monthly detail can contain:
- Employee name
- Total presences
- Rate
- Total earning
- Total advance
- Previous balance
- Extra money
- Net payable
- Amount paid
- Remaining balance
- Hisab completed status
The database allows only one month closing per owner for a given:
owner + year + month
7. Dashboard
The dashboard calculates a monthly financial summary.
It returns:
totalActiveEmployees
totalPayableAmount
totalAdvanceAmount
totalOverAdvanceAmount
The dashboard uses attendance, employee rates, advances, settlements, and previous month balances to calculate the current financial position.
For the current month, calculations are limited through the current date instead of automatically including future dates.
Architecture
The backend follows a layered Spring Boot architecture.
                         CLIENT / FRONTEND
                                │
                                ▼
                       ┌─────────────────┐
                       │ REST Controllers│
                       └────────┬────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │    Services     │
                       │ Business Logic  │
                       └────────┬────────┘
                                │
                ┌───────────────┼────────────────┐
                │               │                │
                ▼               ▼                ▼
        ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
        │ Repositories │ │ External     │ │ Security     │
        │ Spring Data  │ │ Services     │ │ JWT / Redis  │
        │ JPA          │ │ Brevo/Twilio │ │              │
        └──────┬───────┘ └──────────────┘ └──────────────┘
               │
               ▼
        ┌──────────────┐
        │    MySQL     │
        │  Relational  │
        │   Database   │
        └──────────────┘

                 Observability
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
   Actuator      Micrometer      Tracing
        │             │             │
        └─────────────┼─────────────┘
                      ▼
             Structured Logging
                      │
                      ▼
                Logstash
                      │
                      ▼
                Elasticsearch
                      │
                      ▼
                  Kibana
Technology Stack
Backend
Technology	Version / Role
Java	21
Spring Boot	3.4.2
Spring Web	REST API development
Spring Data JPA	Repository/data-access layer
Hibernate	ORM
Spring Security	Authentication/security
Spring Validation	Request validation
Spring Actuator	Health and operational endpoints
Spring Data Redis	Redis integration
Maven	Build and dependency management
Lombok	Boilerplate reduction


Security
Technology	Purpose
Spring Security	HTTP security and authentication
JJWT	JWT creation and validation
BCrypt	Password and OTP hashing
Redis	OTP storage, cooldowns, rate limiting and verification locks


JJWT modules used:
jjwt-api
jjwt-impl
jjwt-jackson
Database
MySQL
Database access uses:
MySQL Connector/J
Spring Data JPA
Hibernate
Flyway
JPA schema validation is enabled:
spring.jpa.hibernate.ddl-auto=validate
The application therefore expects the database schema to be managed by migrations rather than Hibernate automatically creating tables.
API Documentation
SpringDoc OpenAPI
Swagger UI
OpenAPI 3
Messaging / OTP Providers
Email
Brevo
The application uses Brevo for email OTP delivery.
SMS
Twilio
The application contains a TwilioSmsOtpSender implementation for SMS OTP delivery.
Caching / Security State
Redis
Redis is used by the OTP service for:
- OTP storage
- OTP expiration
- OTP resend cooldown
- OTP request rate limiting
- Verification locking
Observability
Spring Boot Actuator
Micrometer
Prometheus registry
Micrometer Tracing
Brave tracing bridge
Logstash Logback Encoder
Elasticsearch
Logstash
Kibana
Containerization
Docker
Docker Compose
Eclipse Temurin JDK 21
Eclipse Temurin JRE 21
Utility Libraries
Apache Tika
Apache Tika is included as a dependency for content/type handling.
Project Structure
saitring-emoloyee-manager/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── sonuSaitring/
│   │   │           └── sonuSaitringManagement/
│   │   │
│   │   │               ├── Attendance/
│   │   │               │   ├── controller/
│   │   │               │   ├── dto/
│   │   │               │   ├── entity/
│   │   │               │   ├── repository/
│   │   │               │   └── service/
│   │   │
│   │   │               ├── Hisab/
│   │   │               │   ├── controller/
│   │   │               │   ├── dto/
│   │   │               │   ├── entity/
│   │   │               │   ├── repository/
│   │   │               │   └── service/
│   │   │
│   │   │               ├── advanceMoney/
│   │   │               │   ├── controller/
│   │   │               │   ├── dto/
│   │   │               │   ├── entity/
│   │   │               │   ├── repository/
│   │   │               │   └── service/
│   │   │
│   │   │               ├── employee/
│   │   │               │   ├── controller/
│   │   │               │   ├── dto/
│   │   │               │   ├── entity/
│   │   │               │   ├── repository/
│   │   │               │   └── service/
│   │   │
│   │   │               ├── sattlement/
│   │   │               │   ├── controller/
│   │   │               │   ├── dto/
│   │   │               │   ├── entity/
│   │   │               │   ├── repository/
│   │   │               │   └── service/
│   │   │
│   │   │               ├── owner/
│   │   │               │   ├── controller/
│   │   │               │   ├── dto/
│   │   │               │   ├── entity/
│   │   │               │   ├── otp/
│   │   │               │   ├── repository/
│   │   │               │   └── service/
│   │   │
│   │   │               ├── dashboard/
│   │   │               │   ├── controller/
│   │   │               │   ├── dto/
│   │   │               │   └── service/
│   │   │
│   │   │               ├── security/
│   │   │               │   ├── CurrentOwnerService.java
│   │   │               │   ├── JwtAuthenticationFilter.java
│   │   │               │   ├── JwtService.java
│   │   │               │   └── SecurityConfig.java
│   │   │
│   │   │               ├── common/
│   │   │               │   ├── config/
│   │   │               │   └── exception/
│   │   │
│   │   │               └── observability/
│   │   │                   ├── GlobalObservabilityFilter.java
│   │   │                   └── ObservabilityConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback-spring.xml
│   │       └── db/
│   │           └── migration/
│   │
│   └── test/
│       └── java/
│
├── logstash/
│   └── pipeline/
│       └── logstash.conf
│
├── uploads/
│   └── organization-logos/
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .dockerignore
├── .gitignore
└── README.md
Business Modules
Module Relationship
Owner
 │
 ├── Employees
 │     │
 │     ├── Attendance
 │     ├── Advances
 │     ├── Settlements
 │     └── Monthly Hisab Details
 │
 ├── Dashboard
 │
 ├── Owner Profile
 │     └── Owner Logo
 │
 └── Authentication
       ├── Password
       ├── OTP
       └── JWT
Authentication and Security
The authentication process is designed as a two-step flow.
Step 1: Login
Client sends:
POST /api/owners/login
with:
{
  "identifier": "owner@example.com",
  "password": "your-password"
}
The identifier can represent the owner's email or mobile number.
The server verifies the password using BCrypt.
If the first factor succeeds, an OTP is generated and delivered.
Step 2: OTP Verification
Client sends:
POST /api/owners/verify-otp
with:
{
  "identifier": "owner@example.com",
  "otp": "123456"
}
After successful verification, the server returns a JWT.
Subsequent protected requests should send:
Authorization: Bearer <JWT_TOKEN>
Security Configuration
The application uses:
- Stateless Spring Security sessions
- JWT authentication filter
- BCrypt password encoder
- CSRF disabled for the stateless API
- Authenticated access for application APIs
- CORS configuration
- Request validation
- Global exception handling
- Sanitized error responses
- JWT-based owner identification
Public endpoints include:
POST /api/owners/register
POST /api/owners/login
POST /api/owners/verify-otp

GET /uploads/**

GET /actuator/health
GET /actuator/health/**

GET /swagger-ui.html
GET /swagger-ui/**
GET /v3/api-docs/**
All other endpoints require authentication.
OTP Security
OTP management uses Redis and BCrypt hashing.
The OTP itself is not stored as plain text.
The OTP service:
1. Generates a secure six-digit OTP.
2. Hashes the OTP using the configured password encoder.
3. Stores the OTP hash in Redis.
4. Gives the OTP a 5-minute expiry.
5. Enforces a 60-second resend cooldown.
6. Limits requests to 5 requests within 15 minutes.
7. Limits verification attempts.
8. Uses a short verification lock to reduce concurrent verification abuse.
9. Deletes temporary OTP state when delivery fails.
Current OTP settings in the implementation:
OTP expiry:              5 minutes
Maximum verification attempts: 5
Resend cooldown:         60 seconds
Rate-limit window:       15 minutes
Maximum OTP requests:    5 per window
Verification lock:       10 seconds
Supported channels:
EMAIL
SMS
Email is delivered through Brevo and SMS through Twilio.
Database Design
The database is relational and managed using MySQL + Flyway.
Main Tables
owners
employees
attendances
employee_advances
employee_settlements
month_closings
month_closing_detail
owner_login_otps
owner_logos
Main Relationships
owners
   │
   ├──────────< employees
   │                │
   │                ├──────────< attendances
   │                ├──────────< employee_advances
   │                ├──────────< employee_settlements
   │                └──────────< month_closing_detail
   │
   ├──────────< month_closings
   │                │
   │                └──────────< month_closing_detail
   │
   ├──────────< owner_login_otps
   │
   └──────────1 owner_logos
Important Database Constraints
Employees
Unique:
owner_id + mobile
Attendance
Unique:
owner_id + employee_id + attendance_date
Monthly Closing
Unique:
owner_id + year + month
Owner
Unique:
email
mobile_number
Owner Logo
One logo record per owner:
owner_id UNIQUE
API Reference
All endpoints below are relative to the backend base URL.
Owner / Authentication
Register Owner
POST /api/owners/register
Content-Type: multipart/form-data
Parts:
request = OwnerRegistrationRequest
logo    = image file
Login
POST /api/owners/login
Content-Type: application/json
Example:
{
  "identifier": "owner@example.com",
  "password": "password"
}
Verify OTP
POST /api/owners/verify-otp
Content-Type: application/json
Example:
{
  "identifier": "owner@example.com",
  "otp": "123456"
}
Get Profile
GET /api/owners/profile
Authorization: Bearer <token>
Update Profile
PUT /api/owners/profile
Content-Type: multipart/form-data
Authorization: Bearer <token>
Parts:
request = OwnerProfileUpdateRequest
logo    = optional image
Delete Profile Logo
DELETE /api/owners/profile/logo
Authorization: Bearer <token>
Get Owner Logo
GET /api/owners/{ownerId}/logo
Employee APIs
Base path:
/api/employees
Method	Endpoint	Purpose
POST	/api/employees	Add employee
GET	/api/employees	Get all employees
GET	/api/employees/{id}	Get employee
PUT	/api/employees/{id}	Update employee
DELETE	/api/employees/{id}	Delete employee
PATCH	/api/employees/{id}/toggle-block	Block/unblock employee


Attendance APIs
Base path:
/api/attendance
Method	Endpoint	Purpose
POST	/api/attendance	Mark/update attendance
GET	/api/attendance/month?year=2026&month=9	Get monthly attendance
GET	/api/attendance/employee/{employeeId}?year=2026&month=9	Employee monthly attendance


Example attendance request:
{
  "employeeId": 1,
  "attendanceDate": "2026-09-18",
  "status": "PRESENT",
  "reason": null
}
Advance APIs
Base path:
/api/advances
Method	Endpoint	Purpose
POST	/api/advances	Record advance
GET	/api/advances/monthly?year=2026&month=9	Monthly advances
PUT	/api/advances/{id}	Update advance
DELETE	/api/advances/{id}	Delete advance


Example:
{
  "employeeId": 1,
  "amount": 5000,
  "paymentDate": "2026-09-10",
  "note": "Advance payment"
}
Settlement APIs
Base path:
/api/settlements
Method	Endpoint	Purpose
POST	/api/settlements	Create settlement
GET	/api/settlements	Get all settlements
GET	/api/settlements/employee/{employeeId}	Get employee settlements
PUT	/api/settlements/{id}	Update settlement
DELETE	/api/settlements/{id}	Delete settlement


Dashboard API
GET /api/dashboard?year=2026&month=9
Authorization: Bearer <token>
Response structure:
{
  "totalActiveEmployees": 10,
  "totalPayableAmount": 85000.00,
  "totalAdvanceAmount": 25000.00,
  "totalOverAdvanceAmount": 5000.00
}
Monthly Hisab APIs
Base path:
/api/month-closings
Method	Endpoint	Purpose
GET	/api/month-closings	Get all month closings
GET	/api/month-closings/{id}	Get closing by ID
GET	/api/month-closings/search?year=2026&month=9	Find closing by year/month
GET	/api/month-closings/year/{year}/month/{month}	Find closing using path
POST/PUT/PATCH	/api/month-closings/detail/{detailId}/complete?completed=true	Toggle employee Hisab completion


The service layer contains monthly report generation logic. The supplied controller currently exposes retrieval and completion operations rather than a dedicated public generate-report endpoint.
Observability and Monitoring
The project has a strong operational/observability layer.
Spring Boot Actuator
Exposed endpoints include:
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
Health probes include:
liveness
readiness
Micrometer Metrics
The application records metrics for areas including:
- HTTP/server requests
- JVM
- System
- Process
- JDBC connections
- Dashboard requests
- Dashboard calculation timings
- Attendance operations
- OTP operations
- Error categories
- Other service-specific operations
Prometheus-compatible metrics are exposed through:
/actuator/prometheus
Distributed Tracing
The application includes:
Micrometer Tracing
Brave bridge
Tracing is enabled with a sampling probability configured in application.properties.
Log levels also include trace/span IDs through the logging pattern.
Request Observability
GlobalObservabilityFilter records:
- HTTP method
- Request URI
- Sanitized query information
- Request ID
- Trace ID
- Response status
- Request duration
- Exception type for failures
- Request content length
The query string is intentionally not logged directly; it is represented as [present] when a query exists.
Logging and ELK Stack
Docker Compose includes:
Elasticsearch
Logstash
Kibana
Architecture:
Spring Boot
    │
    │ TCP JSON logs
    ▼
Logstash :5000
    │
    ▼
Elasticsearch :9200
    │
    ▼
Kibana :5601
Logstash receives JSON lines through TCP and stores them in daily Elasticsearch indices:
sonu-saitring-logs-YYYY.MM.dd
Kibana can then be used to search and visualize application logs.
File and Logo Management
Owner organization logos are stored in the database through the owner_logos table.
The table stores:
logo_data
content_type
created_at
updated_at
owner_id
The project also contains an uploads/organization-logos/ directory and a Spring MVC resource mapping for /uploads/**.
The current owner logo service, however, uses the database-backed owner_logos entity introduced by the Flyway migration.
Maximum multipart request configuration:
Maximum file size: 5 MB
Maximum request size: 6 MB
Configuration
The application is configured primarily through environment variables.
Important application settings include:
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false

spring.flyway.enabled=true
spring.flyway.clean-disabled=true

server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s

server.compression.enabled=true

spring.jackson.time-zone=UTC
The application also disables detailed error information in production-style responses:
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-stacktrace=never
server.error.include-exception=false
Environment Variables
Create a local environment configuration outside source control.
Database
DB_URL
DB_USERNAME
DB_PASSWORD

DB_MAX_POOL_SIZE
DB_MIN_IDLE
DB_CONNECTION_TIMEOUT
DB_IDLE_TIMEOUT
DB_MAX_LIFETIME
DB_KEEPALIVE_TIME
DB_VALIDATION_TIMEOUT
Example:
DB_URL=jdbc:mysql://localhost:3306/worker_management_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your-password
Frontend
FRONTEND_URL
This value is used by the CORS configuration.
Example:
FRONTEND_URL=http://localhost:5173
Only the configured frontend origin is allowed by the current CORS configuration.
JWT
JWT_SECRET
JWT_EXPIRATION
Example:
JWT_EXPIRATION=86400000
Use a strong, random secret for JWT_SECRET.
Brevo
BREVO_API_KEY
BREVO_SENDER_EMAIL
BREVO_SENDER_NAME
BREVO_OTP_TEMPLATE_ID
Redis
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
REDIS_SSL_ENABLED
REDIS_CONNECT_TIMEOUT
REDIS_TIMEOUT
Twilio
TWILIO_ACCOUNT_SID
TWILIO_AUTH_TOKEN
TWILIO_PHONE_NUMBER
Logging
LOGGING_LOGSTASH_HOST
In Docker Compose this is configured as:
logstash:5000
Environment
ENVIRONMENT
Local Development
Requirements
Install:
- Java 21
- MySQL 8+
- Redis
- Git
Maven does not need to be installed globally because the project includes the Maven Wrapper.
Verify Java:
java -version
1. Clone the Repository
git clone <repository-url>
cd saitring-emoloyee-manager
2. Create the Database
Example:
CREATE DATABASE worker_management_db;
3. Configure Environment Variables
Configure the required database, JWT, Redis, and OTP provider values.
Do not commit real credentials.
4. Run the Application
Windows
mvnw.cmd spring-boot:run
Linux/macOS
./mvnw spring-boot:run
The application runs on the default Spring Boot port:
8080
Base URL:
http://localhost:8080
Build
Windows
mvnw.cmd clean package
Linux/macOS
./mvnw clean package
The packaged JAR is generated under:
target/
Run the JAR:
java -jar target/sonuSaitringManagement-0.0.1-SNAPSHOT.jar
Docker
The project contains a multi-stage Dockerfile.
Docker Build Stages
Stage 1
Eclipse Temurin 21 JDK
       │
       ├── Maven dependency resolution
       ├── Compile
       └── Package JAR
              │
              ▼
Stage 2
Eclipse Temurin 21 JRE
       │
       └── Run packaged JAR
Build:
docker build -t sonu-saitring-management:latest .
Run:
docker run -p 8080:8080 sonu-saitring-management:latest
Docker Compose
The supplied docker-compose.yml defines:
app
elasticsearch
logstash
kibana
Ports:
Application     8080
Elasticsearch  9200
Logstash       5000
Kibana         5601
Start:
docker compose up --build
Stop:
docker compose down
Elasticsearch data is persisted through:
elasticsearch_data
Redis and MySQL are not defined as Compose services in the supplied project, so they need to be provided separately and configured through environment variables.
API Documentation
When the backend is running:
Swagger UI:
http://localhost:8080/swagger-ui/index.html
OpenAPI JSON:
http://localhost:8080/v3/api-docs
The OpenAPI configuration defines a Bearer JWT security scheme.
In Swagger UI, use:
Authorize
and provide:
Bearer <JWT_TOKEN>
depending on the Swagger UI authentication prompt.
Database Migrations
Flyway migrations are located at:
src/main/resources/db/migration/
Current migration files:
V1__initial_schema.sql
V2__move_owner_logos_to_database.sql
V3__add_owner_profile_fields.sql
V4__create_owner_logos.sql
V5__remove_owner_username.sql
The application uses:
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=false
spring.flyway.clean-disabled=true
Hibernate is configured only to validate the schema:
spring.jpa.hibernate.ddl-auto=validate
This is an important production characteristic because schema changes are intended to be versioned through Flyway rather than generated automatically by Hibernate.
Testing
The supplied project contains a Spring Boot application context test:
src/test/java/
└── com/sonuSaitring/
    └── sonuSaitringManagement/
        └── SonuSaitringManagementApplicationTests.java
Run tests:
./mvnw test
Windows:
mvnw.cmd test
The Docker build intentionally packages the application with tests skipped:
./mvnw clean package -DskipTests
For CI/CD, tests should be executed in a separate validation stage before the production image is built.
Business Calculation Logic
The dashboard calculates monthly employee financial information using:
Earnings
- Advances
+ Previous Balance
- Amount Paid
= Current Due
Conceptually:
payable = earnings - advances + previousBalance

due = payable - amountPaid
The dashboard then separates positive and negative balances:
due > 0
    → payable amount

due < 0
    → over-advance amount
Blocked employees are excluded from active employee and dashboard financial calculations.
For attendance:
PRESENT
    → full attendance value

HALF_DAY
    → 0.5 attendance value

Other statuses
    → handled according to service calculation rules
The exact monthly earnings logic is implemented in the DashboardServiceImpl and HisabServiceImpl service layers.
Error Handling
The application uses a centralized:
GlobalExceptionHandler
implemented using:
@RestControllerAdvice
It handles categories including:
- Validation errors
- Constraint violations
- Invalid request bodies
- Missing parameters
- Parameter type mismatches
- Invalid dates
- Resource not found
- Bad requests
- Conflicts
- Unauthorized requests
- Too many requests
- External service failures
- Database errors
- Multipart upload errors
- Unexpected exceptions
The application also records error counters through Micrometer.
Example metric category:
application.errors
with tags such as:
bad_request
unauthorized
not_found
conflict
too_many_requests
external_service
database
unexpected
Data Isolation
Business entities are associated with an owner.
Important entities contain:
owner
owner_id
Examples:
Employee
Attendance
EmployeeAdvance
EmployeeSettlement
Hisab
Services use the authenticated owner's identity to scope operations.
This is the basis of the application's multi-owner/organization data isolation model.
Validation
The application uses Jakarta Bean Validation.
Examples include:
Owner Registration
- Owner name required
- Organization name required
- Valid email
- International-format mobile number
- Password length constraints
Employee
- Name required
- Mobile number format validation
- Initial rate cannot be negative
OTP
- OTP required
- OTP must contain exactly 6 digits
Validation errors are returned through the global exception handler.
Performance-Oriented Configuration
The project includes several production-oriented optimizations.
Database Connection Pool
HikariCP settings are configurable through environment variables:
maximum pool size
minimum idle
connection timeout
idle timeout
maximum lifetime
keepalive time
validation timeout
JPA
spring.jpa.open-in-view=false
This prevents the Open Session in View pattern from remaining active for web requests.
HTTP Compression
Response compression is enabled for common text/JSON content types.
Graceful Shutdown
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
This gives active requests time to complete during application shutdown.
Production Considerations
Before deploying to production:
1. Store all credentials in a secret manager or secure environment configuration.
2. Use a strong random JWT secret.
3. Keep MySQL and Redis credentials outside Git.
4. Restrict CORS to the real frontend domain.
5. Use HTTPS.
6. Protect Elasticsearch and Kibana if exposed outside a private network.
7. Do not expose sensitive Actuator endpoints publicly.
8. Configure MySQL backups.
9. Configure Redis appropriately for the production OTP workload.
10. Run automated tests before deployment.
11. Use database migrations consistently.
12. Configure log retention for Elasticsearch.
13. Monitor /actuator/health and /actuator/prometheus.
14. Configure production JVM memory limits.
15. Avoid storing sensitive credentials or OTP values in logs.
16. Use a production-grade reverse proxy/load balancer where required.
17. Review file-upload validation and storage policies before accepting untrusted production uploads.
Important Implementation Notes
1. Backend Only
The supplied archive contains the Spring Boot backend and its supporting infrastructure.
There is no React/Vite frontend source directory in the supplied project.
The backend is therefore ready to be consumed by:
React
Angular
Vue
Mobile application
Other REST clients
2. External Services
The application expects external infrastructure for:
MySQL
Redis
Brevo
Twilio
Docker Compose currently provides:
Spring Boot
Elasticsearch
Logstash
Kibana
3. Owner Logo Storage
The project migrated owner logos into MySQL using:
owner_logos
The older logo_url field remains represented in the Owner entity for compatibility with the migration history, while the current logo service works with the database-backed logo record.
4. Naming
Some package names retain their original project naming, including:
sattlement
Hisab
Attendance
advanceMoney
These are functional package names in the current source tree and can be refactored later if a consistent Java naming convention is desired.
5. Current Month Calculations
Dashboard calculations for the current month use the current date as the upper boundary rather than assuming the entire month has already occurred.
This prevents future attendance dates from being treated as already completed when calculating the current monthly dashboard.
Quick Start
For a fast local setup:
# 1. Create MySQL database
CREATE DATABASE worker_management_db;

# 2. Configure DB/JWT/Redis/OTP environment variables

# 3. Start backend
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run

# 4. Open Swagger
http://localhost:8080/swagger-ui/index.html

# 5. Register an owner
POST /api/owners/register

# 6. Login
POST /api/owners/login

# 7. Verify OTP
POST /api/owners/verify-otp

# 8. Use returned JWT
Authorization: Bearer <TOKEN>

# 9. Start using employee/attendance/advance/settlement/dashboard APIs
Project Summary
Saitring Employee Manager is a Java/Spring Boot workforce-management backend that combines normal business CRUD operations with production-oriented security and observability.
Main business capabilities
Owner Management
Employee Management
Attendance Management
Employee Advances
Employee Settlements
Monthly Hisab
Financial Dashboard
Organization Logo Management
Main engineering capabilities
Java 21
Spring Boot 3.4.2
REST APIs
Spring Data JPA
Hibernate
MySQL
Flyway
Spring Security
JWT
BCrypt
Redis
Brevo
Twilio
Swagger/OpenAPI
Spring Actuator
Micrometer
Prometheus
Micrometer Tracing
Brave
Structured Logging
Logstash
Elasticsearch
Kibana
Docker
Docker Compose
Maven
The project is structured as a modular layered backend and is suitable as the API foundation for a web or mobile employee-management application.