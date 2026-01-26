🚀 Vendor Registration System

Spring Boot | PostgreSQL | Docker | Render | REST API

A production-ready backend application built using Spring Boot that provides RESTful APIs for vendor registration and management.
The project demonstrates end-to-end backend development, from local setup to cloud deployment with Docker and PostgreSQL.

📌 Key Highlights

Clean layered architecture (Controller → Service → Repository)

RESTful API design using Spring Boot

Database migration: Local DB → PostgreSQL (Cloud)

Secure configuration using environment variables

Fully Dockerized application

Deployed on Render Cloud

Industry-standard practices followed

🧠 Problem Statement

To design and deploy a backend system that allows vendors to:

Register themselves

View vendor records

Update vendor details

Delete vendor records

The system must be scalable, secure, and cloud-deployable.

🛠️ Tech Stack
Backend

Java 17

Spring Boot

Spring Data JPA

Hibernate ORM

Database

MySQL / H2 (local development – initial phase)

PostgreSQL (production database on Render)

DevOps & Tools

IntelliJ IDEA – Development IDE

Maven – Build & dependency management

Docker – Containerization

Render – Cloud hosting

Git & GitHub – Version control

Postman – API testing

🧱 Project Architecture
Controller  →  Service  →  Repository  →  Database

Layer Responsibilities

Controller – Handles HTTP requests & responses

Service – Business logic & validation

Repository – Database access via JPA

Entity – Database table mapping

Exception Layer – Centralized error handling
