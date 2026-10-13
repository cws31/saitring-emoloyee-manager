<div align="center">

# Saitring Employee Manager

### Employee & Workforce Management REST API

A modular, security-focused workforce management backend built with **Java 21** and **Spring Boot 3.4.2**.

<p>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3.4.2">
  <img src="https://img.shields.io/badge/MySQL-Database-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-Rate%20Limiting-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
</p>

[Features](#-core-features) · [Architecture](#-architecture) · [API Reference](#-api-reference) · [Setup](#-local-development) · [Configuration](#-environment-variables)

</div>

---

## Overview

**Saitring Employee Manager** is a multi-owner workforce management backend. Each authenticated owner manages their own organization and its business records, including employees, attendance, advances, settlements, and monthly account closing (*Hisab*).

> **Repository scope:** This repository contains the Spring Boot backend and supporting infrastructure. It does **not** include a React/Vite frontend.

### At a glance

| Area | Capabilities |
|---|---|
| Workforce | Employee records, status management, attendance |
| Finance | Advances, settlements, monthly Hisab, dashboard totals |
| Identity | Owner registration, password verification, OTP, JWT |
| Data | MySQL, Spring Data JPA, Flyway migrations |
| Operations | Actuator, Micrometer, Prometheus, tracing, structured logs |
| Observability | Logstash, Elasticsearch, Kibana |
| Delivery | Maven Wrapper, Docker, Docker Compose, Swagger UI |

## Contents

- [Core Features](#-core-features)
- [Architecture](#-architecture)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Authentication and Security](#-authentication-and-security)
- [OTP Security](#-otp-security)
- [Database Design](#-database-design)
- [API Reference](#-api-reference)
- [Observability and Logging](#-observability-and-logging)
- [Configuration](#-configuration)
- [Local Development](#-local-development)
- [Docker](#-docker)
- [Database Migrations](#-database-migrations)
- [Testing](#-testing)
- [Business Calculation Logic](#-business-calculation-logic)
- [Validation and Error Handling](#-validation-and-error-handling)
- [Data Isolation](#-data-isolation)
- [Production Checklist](#-production-checklist)
- [Implementation Notes](#-implementation-notes)

---

## ✨ Core Features

### Owner and organization management
- Register an owner and organization.
- Authenticate using email or mobile number and password.
- Complete OTP verification and receive a JWT.
- View and update owner profile information.
- Upload, retrieve, and delete an organization logo.

Profile fields include owner name, organization name, email, mobile, address, city, state, country, postal code, website, and logo.

### Employee management
- Create, retrieve, update, and delete employees.
- Block or unblock an employee.
- Store name, mobile number, initial rate, status, creation time, and owner association.
- Enforce mobile-number uniqueness per owner using `(owner_id, mobile)`.

### Attendance
- Mark attendance for an employee and date.
- Update an existing record for the same employee/date.
- Retrieve monthly attendance for an organization or an individual employee.
- Prevent duplicate records using `(owner_id, employee_id, attendance_date)`.

### Employee advances
- Record, view, update, and delete advances.
- Store employee, amount, payment date, and note.
- Support efficient monthly lookups with owner/date indexing.

### Settlements
- Create, retrieve, update, and delete employee settlements.
- Filter settlements by employee.
- Store amount paid, settlement date, note, and creation timestamp.

### Monthly Hisab
Monthly account closing includes year, month, closing timestamp, employee totals, payable totals, over-advance totals, and employee-level closing details.

Employee closing details may include:
- Total presence and rate
- Total earnings and advances
- Previous balance and extra money
- Net payable, amount paid, and remaining balance
- Hisab completion status

A unique constraint allows one monthly closing per owner/year/month.

### Financial dashboard
The dashboard returns:
- `totalActiveEmployees`
- `totalPayableAmount`
- `totalAdvanceAmount`
- `totalOverAdvanceAmount`

It combines attendance, employee rates, advances, settlements, and previous balances. For the current month, calculations stop at the current date. Blocked employees are excluded from active-employee and dashboard financial calculations.

---

## 🏗️ Architecture

The application follows a layered Spring Boot architecture.

```text
Client / Frontend
       |
       v
REST Controllers
       |
       v
Services and Business Logic
       |
       +-------------------+-------------------+
       |                   |                   |
       v                   v                   v
Spring Data JPA      External Services     Security
Repositories         Brevo / Twilio        JWT / Redis
       |
       v
     MySQL

Observability:
Actuator + Micrometer + Tracing
                |
                v
       Structured JSON Logs
                |
                v
             Logstash
                |
                v
          Elasticsearch
                |
                v
              Kibana
```

### Main workflow

```text
Owner Registration
        |
        v
Owner Login -> Password Verification
        |
        v
OTP Verification
        |
        v
JWT Issued
        |
        v
Authenticated API Access
        |
        +--> Employees
        +--> Attendance
        +--> Advances and Settlements
        +--> Monthly Hisab
        +--> Dashboard
        +--> Owner Profile
```

---

## 🧰 Technology Stack

| Category | Technologies |
|---|---|
| Language and framework | Java 21, Spring Boot 3.4.2 |
| API | Spring Web, REST, SpringDoc OpenAPI, Swagger UI |
| Persistence | Spring Data JPA, Hibernate, MySQL, MySQL Connector/J |
| Schema migrations | Flyway |
| Security | Spring Security, JJWT, BCrypt |
| OTP and rate limiting | Redis, Brevo email, Twilio SMS |
| Validation | Jakarta Bean Validation, Spring Validation |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus registry |
| Tracing and logging | Micrometer Tracing, Brave, Logstash Logback Encoder |
| Log analytics | Logstash, Elasticsearch, Kibana |
| Build and containers | Maven Wrapper, Docker, Docker Compose, Eclipse Temurin 21 |
| Utilities | Lombok, Apache Tika |

Hibernate validates the schema rather than generating it automatically:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Schema changes are managed through Flyway migrations.

---

## 📁 Project Structure

```text
saitring-emoloyee-manager/
├── src/
│   ├── main/
│   │   ├── java/com/sonuSaitring/sonuSaitringManagement/
│   │   │   ├── Attendance/
│   │   │   ├── Hisab/
│   │   │   ├── advanceMoney/
│   │   │   ├── employee/
│   │   │   ├── sattlement/
│   │   │   ├── owner/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── otp/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   ├── dashboard/
│   │   │   ├── security/
│   │   │   ├── common/
│   │   │   └── observability/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback-spring.xml
│   │       └── db/migration/
│   └── test/
├── logstash/pipeline/logstash.conf
├── uploads/organization-logos/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

Each business module generally separates controllers, DTOs, entities, repositories, and services.

---

## 🔐 Authentication and Security

Authentication uses two steps.

### 1. Login

```http
POST /api/owners/login
Content-Type: application/json
```

```json
{
  "identifier": "owner@example.com",
  "password": "your-password"
}
```

The `identifier` accepts the owner's email or mobile number. The password is verified using BCrypt. If valid, an OTP is generated and delivered.

### 2. Verify OTP

```http
POST /api/owners/verify-otp
Content-Type: application/json
```

```json
{
  "identifier": "owner@example.com",
  "otp": "123456"
}
```

After successful verification, the API returns a JWT. Send it with protected requests:

```http
Authorization: Bearer <JWT_TOKEN>
```

### Security controls
- Stateless Spring Security configuration
- JWT authentication filter and owner identification
- BCrypt password hashing
- Request validation and centralized exception handling
- CORS configuration
- Sanitized error responses
- CSRF disabled for the stateless API

### Public endpoints

```text
POST /api/owners/register
POST /api/owners/login
POST /api/owners/verify-otp
GET  /uploads/**
GET  /actuator/health
GET  /actuator/health/**
GET  /swagger-ui.html
GET  /swagger-ui/**
GET  /v3/api-docs/**
```

Other application endpoints require authentication.

## OTP Security

Redis stores temporary OTP state, while BCrypt is used to hash OTP values.

| Setting | Value |
|---|---:|
| OTP expiry | 5 minutes |
| Maximum verification attempts | 5 |
| Resend cooldown | 60 seconds |
| Rate-limit window | 15 minutes |
| Maximum OTP requests | 5 per window |
| Verification lock | 10 seconds |

The OTP service generates a secure six-digit code, stores its hash with an expiry, enforces cooldown and rate limits, limits verification attempts, and removes temporary state if delivery fails.

**Delivery providers**
- Email: Brevo
- SMS: Twilio

---

## 🗄️ Database Design

### Main tables

```text
owners
employees
attendances
employee_advances
employee_settlements
month_closings
month_closing_detail
owner_login_otps
owner_logos
```

### Relationship overview

```text
owners
 ├── employees
 │    ├── attendances
 │    ├── employee_advances
 │    ├── employee_settlements
 │    └── month_closing_detail
 ├── month_closings
 │    └── month_closing_detail
 ├── owner_login_otps
 └── owner_logos
```

### Important constraints

| Entity | Constraint |
|---|---|
| Owner | Unique email and mobile number |
| Employee | Unique `(owner_id, mobile)` |
| Attendance | Unique `(owner_id, employee_id, attendance_date)` |
| Monthly closing | Unique `(owner_id, year, month)` |
| Owner logo | One logo record per owner (`owner_id` unique) |

---

## 🔌 API Reference

All paths below are relative to the backend base URL, `http://localhost:8080`.

### Owner and authentication

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/owners/register` | Register owner; multipart form with request and optional logo |
| `POST` | `/api/owners/login` | Start login |
| `POST` | `/api/owners/verify-otp` | Verify OTP and obtain JWT |
| `GET` | `/api/owners/profile` | Retrieve owner profile |
| `PUT` | `/api/owners/profile` | Update profile; multipart form with request and optional logo |
| `DELETE` | `/api/owners/profile/logo` | Delete owner logo |
| `GET` | `/api/owners/{ownerId}/logo` | Retrieve owner logo |

### Employees — `/api/employees`

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/employees` | Add employee |
| `GET` | `/api/employees` | List employees |
| `GET` | `/api/employees/{id}` | Get employee |
| `PUT` | `/api/employees/{id}` | Update employee |
| `DELETE` | `/api/employees/{id}` | Delete employee |
| `PATCH` | `/api/employees/{id}/toggle-block` | Block or unblock employee |

### Attendance — `/api/attendance`

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/attendance` | Mark or update attendance |
| `GET` | `/api/attendance/month?year=2026&month=9` | Get monthly attendance |
| `GET` | `/api/attendance/employee/{employeeId}?year=2026&month=9` | Get an employee's monthly attendance |

Example request:

```json
{
  "employeeId": 1,
  "attendanceDate": "2026-09-18",
  "status": "PRESENT",
  "reason": null
}
```

### Advances — `/api/advances`

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/advances` | Record advance |
| `GET` | `/api/advances/monthly?year=2026&month=9` | List monthly advances |
| `PUT` | `/api/advances/{id}` | Update advance |
| `DELETE` | `/api/advances/{id}` | Delete advance |

Example request:

```json
{
  "employeeId": 1,
  "amount": 5000,
  "paymentDate": "2026-09-10",
  "note": "Advance payment"
}
```

### Settlements — `/api/settlements`

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/settlements` | Create settlement |
| `GET` | `/api/settlements` | List settlements |
| `GET` | `/api/settlements/employee/{employeeId}` | List settlements for an employee |
| `PUT` | `/api/settlements/{id}` | Update settlement |
| `DELETE` | `/api/settlements/{id}` | Delete settlement |

### Dashboard

```http
GET /api/dashboard?year=2026&month=9
Authorization: Bearer <JWT_TOKEN>
```

Example response:

```json
{
  "totalActiveEmployees": 10,
  "totalPayableAmount": 85000.00,
  "totalAdvanceAmount": 25000.00,
  "totalOverAdvanceAmount": 5000.00
}
```

### Monthly Hisab — `/api/month-closings`

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/month-closings` | List month closings |
| `GET` | `/api/month-closings/{id}` | Get closing by ID |
| `GET` | `/api/month-closings/search?year=2026&month=9` | Find closing by year and month |
| `GET` | `/api/month-closings/year/{year}/month/{month}` | Find closing using path parameters |
| `POST` / `PUT` / `PATCH` | `/api/month-closings/detail/{detailId}/complete?completed=true` | Toggle employee Hisab completion |

The service layer contains monthly report generation logic. The supplied controller exposes retrieval and completion operations; it does not expose a dedicated public report-generation endpoint.

---

## 📈 Observability and Logging

### Actuator and metrics

Exposed endpoints include:

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
```

Health probes support liveness and readiness. Micrometer metrics cover HTTP requests, JVM/system/process information, JDBC connections, dashboard calculations, attendance, OTP operations, and application error categories.

### Distributed tracing and request logging

The project includes Micrometer Tracing and the Brave bridge. `GlobalObservabilityFilter` records:
- HTTP method and request URI
- Sanitized query information
- Request ID and trace ID
- Response status and request duration
- Exception type for failures
- Request content length

Query strings are not logged directly; a query is represented as `[present]`.

### ELK stack

Docker Compose includes Elasticsearch, Logstash, and Kibana.

```text
Spring Boot --TCP JSON logs--> Logstash :5000
                                      |
                                      v
                              Elasticsearch :9200
                                      |
                                      v
                                 Kibana :5601
```

Logstash writes daily indices using the pattern:

```text
sonu-saitring-logs-YYYY.MM.dd
```

---

## ⚙️ Configuration

Important application properties include:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.flyway.enabled=true
spring.flyway.clean-disabled=true
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
server.compression.enabled=true
spring.jackson.time-zone=UTC

server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-stacktrace=never
server.error.include-exception=false
```

### Environment variables

Create a local environment configuration and keep secrets out of source control.

<details>
<summary><strong>Database and connection pool</strong></summary>

```env
DB_URL=jdbc:mysql://localhost:3306/worker_management_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your-password

DB_MAX_POOL_SIZE=
DB_MIN_IDLE=
DB_CONNECTION_TIMEOUT=
DB_IDLE_TIMEOUT=
DB_MAX_LIFETIME=
DB_KEEPALIVE_TIME=
DB_VALIDATION_TIMEOUT=
```
</details>

<details>
<summary><strong>Frontend and JWT</strong></summary>

```env
FRONTEND_URL=http://localhost:5173
JWT_SECRET=replace-with-a-strong-random-secret
JWT_EXPIRATION=86400000
```

`FRONTEND_URL` is used by CORS configuration. Only the configured frontend origin is allowed by the current setup.
</details>

<details>
<summary><strong>Brevo email OTP</strong></summary>

```env
BREVO_API_KEY=
BREVO_SENDER_EMAIL=
BREVO_SENDER_NAME=
BREVO_OTP_TEMPLATE_ID=
```
</details>

<details>
<summary><strong>Redis</strong></summary>

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL_ENABLED=false
REDIS_CONNECT_TIMEOUT=
REDIS_TIMEOUT=
```
</details>

<details>
<summary><strong>Twilio SMS OTP</strong></summary>

```env
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
TWILIO_PHONE_NUMBER=
```
</details>

<details>
<summary><strong>Logging and runtime environment</strong></summary>

```env
LOGGING_LOGSTASH_HOST=localhost
ENVIRONMENT=local
```

Inside Docker Compose, the Logstash host is configured as `logstash:5000`.
</details>

> **Security:** Never commit real API keys, database passwords, JWT secrets, or provider credentials to Git.

---

## 🚀 Local Development

### Prerequisites

- Java 21
- MySQL 8+
- Redis
- Git

Maven does not need to be installed globally; the repository includes the Maven Wrapper.

### 1. Clone the repository

```bash
git clone <repository-url>
cd saitring-emoloyee-manager
```

### 2. Create the database

```sql
CREATE DATABASE worker_management_db;
```

### 3. Configure environment variables

Set the database, JWT, Redis, Brevo, and Twilio values required by your local configuration.

### 4. Run the application

**Windows**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS**

```bash
./mvnw spring-boot:run
```

The default base URL is `http://localhost:8080`.

### 5. Open API documentation

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

In Swagger UI, select **Authorize** and provide the JWT token. Depending on the prompt, enter `Bearer <JWT_TOKEN>`.

### 6. Build and run the JAR

**Windows**

```powershell
.\mvnw.cmd clean package
```

**Linux / macOS**

```bash
./mvnw clean package
```

The JAR is generated under `target/`. The expected artifact name in the supplied configuration is:

```bash
java -jar target/sonuSaitringManagement-0.0.1-SNAPSHOT.jar
```

---

## 🐳 Docker

The multi-stage Dockerfile uses Eclipse Temurin 21 JDK to build the application and a JRE image to run the packaged JAR.

### Build the image

```bash
docker build -t sonu-saitring-management:latest .
```

### Run the application container

```bash
docker run -p 8080:8080 sonu-saitring-management:latest
```

### Docker Compose services

| Service | Port |
|---|---:|
| Spring Boot application | `8080` |
| Elasticsearch | `9200` |
| Logstash | `5000` |
| Kibana | `5601` |

Start the stack:

```bash
docker compose up --build
```

Stop the stack:

```bash
docker compose down
```

Elasticsearch data is persisted using the `elasticsearch_data` volume.

> **Infrastructure note:** The supplied Compose file does not define MySQL or Redis. Run them separately and configure the application through environment variables.

---

## 🧱 Database Migrations

Flyway migrations are stored in `src/main/resources/db/migration/`.

```text
V1__initial_schema.sql
V2__move_owner_logos_to_database.sql
V3__add_owner_profile_fields.sql
V4__create_owner_logos.sql
V5__remove_owner_username.sql
```

Flyway is enabled, `baseline-on-migrate` is disabled, and `clean-disabled` is enabled. Hibernate validates the schema with `ddl-auto=validate`; database structure should be changed through versioned migrations.

---

## 🧪 Testing

The supplied project includes a Spring Boot application context test under `src/test/java/`.

Run tests:

**Windows**

```powershell
.\mvnw.cmd test
```

**Linux / macOS**

```bash
./mvnw test
```

The Docker build packages the application with tests skipped. For CI/CD, run tests in a separate validation stage before creating the production image.

---

## 🧮 Business Calculation Logic

The dashboard's conceptual calculation is:

```text
Payable = Earnings - Advances + Previous Balance
Due     = Payable - Amount Paid
```

Balances are categorized as follows:

| Condition | Meaning |
|---|---|
| `due > 0` | Remaining payable amount |
| `due < 0` | Over-advance amount |

Attendance values include:

| Status | Attendance value |
|---|---:|
| `PRESENT` | `1.0` |
| `HALF_DAY` | `0.5` |
| Other statuses | Determined by service calculation rules |

The primary calculation services are `DashboardServiceImpl` and `HisabServiceImpl`. The exact calculation behavior should be verified in those implementations when changing business rules.

---

## 🛡️ Validation and Error Handling

Jakarta Bean Validation is used for request validation.

- **Owner registration:** required owner and organization names, valid email, international-format mobile number, and password constraints.
- **Employee:** required name, valid mobile format, and non-negative initial rate.
- **OTP:** required six-digit numeric code.

`GlobalExceptionHandler`, implemented with `@RestControllerAdvice`, handles validation failures, malformed requests, missing parameters, type mismatches, invalid dates, not-found errors, conflicts, unauthorized requests, rate limits, external-service failures, database errors, multipart upload errors, and unexpected exceptions.

Micrometer also records error counters such as `application.errors`, categorized with tags including `bad_request`, `unauthorized`, `not_found`, `conflict`, `too_many_requests`, `external_service`, `database`, and `unexpected`.

---

## 🧩 Data Isolation

Business records are associated with an owner, and services use the authenticated owner's identity to scope operations. This is the foundation of the multi-owner data isolation model for employees, attendance, advances, settlements, and Hisab records.

Always verify owner scoping in service and repository queries when introducing new endpoints or changing data access.

---

## 📦 File and Logo Management

Organization logos are stored in MySQL through the `owner_logos` table, which includes `logo_data`, `content_type`, timestamps, and `owner_id`.

The project also contains `uploads/organization-logos/` and a resource mapping for `/uploads/**`. The current logo service uses the database-backed `owner_logos` entity.

| Upload setting | Limit |
|---|---:|
| Maximum file size | 5 MB |
| Maximum request size | 6 MB |

Review file type validation, storage, and access policies before accepting untrusted uploads in production. The older `logo_url` field remains represented in the `Owner` entity for migration compatibility.

---

## ✅ Production Checklist

Before deploying:

- [ ] Store credentials in a secret manager or secure environment configuration.
- [ ] Use a strong, randomly generated JWT secret.
- [ ] Keep MySQL and Redis credentials outside Git.
- [ ] Restrict CORS to the real frontend domain.
- [ ] Use HTTPS.
- [ ] Protect Elasticsearch and Kibana behind a private network or authentication.
- [ ] Avoid exposing sensitive Actuator endpoints publicly.
- [ ] Configure MySQL backups and test recovery.
- [ ] Configure Redis for the production OTP workload.
- [ ] Run automated tests before deployment.
- [ ] Manage schema changes through Flyway migrations.
- [ ] Configure Elasticsearch log retention.
- [ ] Monitor `/actuator/health` and `/actuator/prometheus`.
- [ ] Set production JVM memory limits.
- [ ] Ensure credentials and OTP values are not written to logs.
- [ ] Consider a production-grade reverse proxy or load balancer.
- [ ] Review file-upload validation and storage policies.

---

## 📝 Implementation Notes

1. **Backend only:** The supplied archive contains the Spring Boot API, not a React/Vite frontend. It can be consumed by a web frontend, mobile app, or another REST client.
2. **External dependencies:** MySQL, Redis, Brevo, and Twilio must be configured separately. Compose currently includes the application and ELK services.
3. **Package naming:** Existing names such as `sattlement`, `Hisab`, `Attendance`, and `advanceMoney` are retained from the source tree.
4. **Current-month dashboard:** Calculations use the current date as the upper boundary so future dates are not counted as completed.
5. **Monthly report generation:** Report-generation logic exists in the service layer, but the supplied controller does not expose a dedicated public generation endpoint.
6. **Production readiness:** The repository includes production-oriented features, but a deployment should still be reviewed, configured, tested, and secured for its actual environment.

---

<div align="center">

### Built for practical workforce and financial management

**Saitring Employee Manager** · Java 21 · Spring Boot 3.4.2

</div>
