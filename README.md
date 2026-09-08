# Sonu Saitring Management System

A full-stack **Worker Management System** designed to manage employees, attendance, advances, settlements, monthly accounts, and secure administrator access.

The backend is built with **Spring Boot + MySQL**, with JWT-based authentication and email OTP verification for administrator login.

> **Project status:** Backend development is active. Cloud deployment and the React/Vite frontend deployment are planned.

---

## Overview

The Sonu Saitring Management System provides a centralized platform for managing day-to-day worker and payroll-related operations.

The application is designed around the following core areas:

* Employee management
* Employee attendance
* Employee advances
* Employee settlements
* Monthly account closing (`Hisab`)
* Administrator authentication
* Email OTP verification
* JWT-based API authentication

The project is being prepared for deployment using a cloud-based architecture with a managed MySQL database.

---

## Features

### 🔐 Administrator Authentication

* Username/password authentication
* Email OTP verification
* JWT token generation
* Stateless authentication
* BCrypt password hashing
* Protected API endpoints using Spring Security

Authentication flow:

```text
Admin Login
    │
    ▼
Username + Password
    │
    ▼
Credentials Verified
    │
    ▼
OTP Sent to Registered Email
    │
    ▼
OTP Verification
    │
    ▼
JWT Token
    │
    ▼
Authenticated API Access
```

---

### 👨‍💼 Employee Management

Manage employee records through REST APIs.

Supported operations:

* Create employee
* View all employees
* View employee by ID
* Update employee
* Delete employee
* Block/unblock employee

Base endpoint:

```text
/api/employees
```

---

### 📅 Attendance Management

Manage employee attendance and retrieve attendance information by month or employee.

Supported operations:

* Mark attendance
* View monthly attendance
* View an employee's monthly attendance

Base endpoint:

```text
/api/attendance
```

---

### 💰 Employee Advances

Track advances given to employees.

Supported operations:

* Record advance
* Update advance
* Delete advance
* View monthly advances

Base endpoint:

```text
/api/advances
```

---

### 🧾 Employee Settlements

Manage employee settlement records.

Supported operations:

* Create settlement
* Update settlement
* Delete settlement
* View all settlements
* View settlements for a specific employee

Base endpoint:

```text
/api/settlements
```

---

### 📊 Monthly Accounts / Hisab

Manage monthly closing/accounting information.

Supported operations include:

* View all month closings
* View month closing by ID
* Search by year and month
* Access closing by year/month path
* Mark employee `Hisab` details as completed

Base endpoint:

```text
/api/month-closings
```

---

## Technology Stack

### Backend

| Technology        | Purpose                        |
| ----------------- | ------------------------------ |
| Java 21           | Programming language           |
| Spring Boot 3.4.2 | Backend framework              |
| Spring Web        | REST APIs                      |
| Spring Data JPA   | Database access                |
| Hibernate         | ORM                            |
| Spring Security   | Authentication & authorization |
| JWT               | Stateless authentication       |
| Spring Mail       | Email OTP                      |
| Bean Validation   | Request validation             |
| Lombok            | Boilerplate reduction          |
| SpringDoc OpenAPI | API documentation              |
| Maven             | Build & dependency management  |

### Database

```text
MySQL
```

The application uses MySQL through the MySQL Connector/J driver and Spring Data JPA.

### Planned Frontend

```text
React
Vite
```

The frontend will consume the Spring Boot REST APIs.

---

## Project Structure

```text
sonuSaitringManagement/
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
│   │   │               ├── admin/
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
│   │   │               │   ├── service/
│   │   │               │   └── ...
│   │   │
│   │   │               ├── security/
│   │   │               │   ├── EmailService.java
│   │   │               │   ├── EmailServiceImpl.java
│   │   │               │   ├── JwtAuthFilter.java
│   │   │               │   ├── JwtUtils.java
│   │   │               │   └── SecurityConfig.java
│   │   │
│   │   │               └── common/
│   │   │                   └── config/
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
└── README.md
```

---

## API Overview

### Authentication

```http
POST /api/auth/login
POST /api/auth/verify-otp
```

### Employees

```http
GET    /api/employees
GET    /api/employees/{id}
POST   /api/employees
PUT    /api/employees/{id}
DELETE /api/employees/{id}
PATCH  /api/employees/{id}/toggle-block
```

### Attendance

```http
POST /api/attendance
GET  /api/attendance/month?year={year}&month={month}
GET  /api/attendance/employee/{employeeId}?year={year}&month={month}
```

### Advances

```http
POST   /api/advances
GET    /api/advances/monthly?year={year}&month={month}
PUT    /api/advances/{id}
DELETE /api/advances/{id}
```

### Settlements

```http
GET    /api/settlements
GET    /api/settlements/employee/{employeeId}
POST   /api/settlements
PUT    /api/settlements/{id}
DELETE /api/settlements/{id}
```

### Monthly Accounts

```http
GET    /api/month-closings
GET    /api/month-closings/{id}
GET    /api/month-closings/search?year={year}&month={month}
GET    /api/month-closings/year/{year}/month/{month}
POST   /api/month-closings/detail/{detailId}/complete?completed={true|false}
```

---

## Authentication & Security

The backend uses Spring Security with JWT authentication.

Protected endpoints require an authenticated request after successful login and OTP verification.

The application uses:

* JWT authentication
* BCrypt password hashing
* Stateless sessions
* Email-based OTP verification
* CORS configuration
* Request validation

### Important Security Rule

**Never commit credentials or secrets to GitHub.**

Do not commit:

```text
Database passwords
Database usernames
JWT secrets
Gmail passwords
Gmail app passwords
API keys
Cloud credentials
Private keys
Environment files containing secrets
```

Production configuration should use environment variables or a secure secret-management mechanism.

Example:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

jwt.secret=${JWT_SECRET}
```

---

## Local Development

### Requirements

Install:

* Java 21
* Maven 3.9+ (optional because Maven Wrapper is included)
* MySQL 8+
* Git

Verify Java:

```bash
java -version
```

Verify Git:

```bash
git --version
```

---

## Database Setup

Create the application database in MySQL:

```sql
CREATE DATABASE worker_management_db;
```

Then configure the database connection using environment variables.

Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/worker_management_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

> Never use production credentials in the repository.

---

## Running the Backend

### Windows

Using Maven Wrapper:

```bash
mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

---

## Build the Application

Windows:

```bash
mvnw.cmd clean package
```

Linux / macOS:

```bash
./mvnw clean package
```

The executable JAR will be generated under:

```text
target/
```

Run the packaged application:

```bash
java -jar target/sonuSaitringManagement-0.0.1-SNAPSHOT.jar
```

---

## API Documentation

The project includes SpringDoc OpenAPI.

When the application is running, Swagger UI is available at:

```text
/swagger-ui/index.html
```

OpenAPI JSON:

```text
/v3/api-docs
```

---

## CORS

During local development, the backend currently supports frontend development origins such as:

```text
http://localhost:5173
http://localhost:3000
http://localhost:5174
```

For production deployment, the allowed origins should be restricted to the actual production frontend domain.

---

## Production Deployment

The planned production architecture is:

```text
                    INTERNET
                       │
                       ▼
                 ┌───────────┐
                 │   Nginx   │
                 │ HTTPS/SSL │
                 └─────┬─────┘
                       │
                       ▼
                 ┌───────────┐
                 │ Spring    │
                 │ Boot API  │
                 │ Java 21   │
                 └─────┬─────┘
                       │
                       │ Private connection
                       ▼
              ┌──────────────────┐
              │ Oracle MySQL     │
              │ Managed Database │
              └──────────────────┘
                       ▲
                       │
                  Backup Layer
                       │
              ┌──────────────────┐
              │ Future Backup    │
              │ Infrastructure   │
              └──────────────────┘
```

The planned cloud environment uses:

* Oracle Cloud Always Free compute
* Oracle MySQL Always Free
* Linux
* Java 21
* Spring Boot
* Nginx
* HTTPS
* Private database connectivity

The React/Vite frontend will be deployed separately and served through the web server.

---

## Production Configuration

Production secrets should be supplied through environment variables.

Example:

```bash
export DB_URL="jdbc:mysql://<MYSQL_HOST>:3306/worker_management_db"
export DB_USERNAME="<DB_USERNAME>"
export DB_PASSWORD="<DB_PASSWORD>"

export MAIL_USERNAME="<EMAIL>"
export MAIL_PASSWORD="<EMAIL_APP_PASSWORD>"

export JWT_SECRET="<LONG_RANDOM_SECRET>"
```

Never place the real values inside `README.md`, source code, or GitHub.

---

## Database Migrations

The project contains Flyway configuration for database migration support.

Current configuration enables:

```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

Before production deployment, database migration strategy should be reviewed and versioned carefully.

For production data, avoid relying on destructive schema changes.

---

## Deployment & Backup Strategy

The application is intended to use multiple layers of protection.

### Primary database

Managed cloud MySQL.

### Cloud database protection

Oracle-managed database backups.

### Additional backups

A future backup process will create independent database dumps such as:

```text
mysqldump
```

These backups can later be stored on:

* Local computer
* Cloud Object Storage
* Another independent storage location

The final backup architecture should follow the principle:

```text
Production Database
       │
       ├── Cloud Backup
       │
       ├── Independent Database Dump
       │
       └── Local / Off-site Backup
```

Backups should also be periodically tested by performing a restore.

---

## Environment Separation

The application should eventually maintain separate configurations for:

```text
Development
Testing
Production
```

Production credentials must never be reused in development repositories or shared configuration files.

---

## Git Workflow

Recommended workflow:

```bash
git status
git add .
git commit -m "describe the change"
git push origin main
```

Before every push, verify that no secrets are included:

```bash
git status
```

and review changed files:

```bash
git diff --cached
```

---

## Project Roadmap

### Backend

* [x] Employee management
* [x] Attendance management
* [x] Employee advances
* [x] Employee settlements
* [x] Monthly `Hisab`
* [x] JWT authentication
* [x] Email OTP authentication
* [x] Spring Security
* [x] REST APIs
* [x] OpenAPI / Swagger

### Frontend

* [x] React/Vite production deployment
* [x] Production API configuration
* [x] Production CORS configuration
* [x] HTTPS integration
* [x] Domain configuration

### Cloud

* [ ] Oracle Cloud account
* [ ] Oracle Always Free VM
* [ ] Oracle MySQL database
* [ ] Production environment configuration
* [ ] Spring Boot deployment
* [ ] Nginx reverse proxy
* [ ] HTTPS
* [ ] Monitoring

### Backup & Reliability

* [ ] Automated database dumps
* [ ] Local backup
* [ ] Off-site backup
* [ ] Restore testing
* [ ] Uploaded-file backup
* [ ] Disaster recovery procedure

---

## Development Notes

This project is being developed as a real-world business management application.

Production deployment should prioritize:

1. Data security
2. Credential security
3. Database reliability
4. Regular backups
5. Restore testing
6. HTTPS
7. Restricted network access
8. Least-privilege database access
9. Secure secret management
10. Monitoring and logging

---

## License

This project is currently maintained as a private/personal business application.

If the repository is made public in the future, an explicit open-source license should be added here.

---

## Author

**Sonu Saitring Management**

Built with:

```text
Java
Spring Boot
Spring Security
JWT
MySQL
React
Vite
```
