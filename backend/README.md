# SecureCrypt Backend (Spring Boot API)

This directory contains the Spring Boot REST API for SecureCrypt.

## Features
- **Spring Security & JWT**: Protects all API endpoints using token validation.
- **RESTful Endpoints**: Uploads binary blocks and retrieves list-metadata/download/delete requests.
- **Storage Strategy Bean Selection**: Auto-switches storage between local simulated directory and AWS S3 dynamically.

## Requirements
- Java 21
- Maven 3

## Setup & Run
1. Configure credentials inside `src/main/resources/application.properties` (or pass them via environment variables).
2. Clean and package the application:
   ```bash
   mvn clean package
   ```
3. Run the executable jar:
   ```bash
   java -jar target/securecrypt-platform-1.0-SNAPSHOT.jar
   ```