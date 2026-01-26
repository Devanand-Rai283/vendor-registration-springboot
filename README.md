📌 Project Overview

The Vendor Registration System is a backend web application developed using Spring Boot.
It provides REST APIs to register, view, update, and delete vendor details.

The project was initially developed and tested locally using MySQL / H2, then later migrated to PostgreSQL and deployed on Render Cloud using Docker.

This project demonstrates:

Clean layered architecture

Database migration (MySQL → PostgreSQL)

Cloud deployment using Docker

Environment-based configuration (industry standard)

🏗️ Tech Stack
Backend

Java 17

Spring Boot

Spring Data JPA

Hibernate

Database

MySQL (local development – initial phase)

PostgreSQL (cloud database on Render)

Tools & Platforms

IntelliJ IDEA – IDE

Maven – build tool

Docker – containerization

Render – cloud deployment

GitHub – version control

Postman – API testing

📂 Project Structure
vendor-registration-system
│
├── Dockerfile
├── pom.xml
├── README.md
│
├── src/main/java/com/example/vendor
│   ├── VendorRegistrationApplication.java
│   │
│   ├── controller
│   │   └── VendorController.java
│   │
│   ├── service
│   │   ├── VendorService.java
│   │   └── impl
│   │       └── VendorServiceImpl.java
│   │
│   ├── repository
│   │   └── VendorRepository.java
│   │
│   ├── entity
│   │   └── Vendor.java
│   │
│   └── exception
│       ├── VendorNotFoundException.java
│       └── GlobalExceptionHandler.java
│
└── src/main/resources
    └── application.properties

🧠 Architecture Explanation

The project follows a layered architecture:

Controller Layer
Handles HTTP requests and responses (REST APIs).

Service Layer
Contains business logic and validation.

Repository Layer
Communicates with the database using Spring Data JPA.

Entity Layer
Maps Java objects to database tables using JPA annotations.

Exception Layer
Centralized exception handling using @RestControllerAdvice.
