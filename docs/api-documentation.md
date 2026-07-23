# SecureCrypt API Specification

This document details the REST endpoints exposed by the SecureCrypt backend API.

## Base URL
- Local: `http://localhost:8080`
- Production: `http://<EC2_PUBLIC_IP>:8080`

---

## 1. Authentication Endpoints

### 1.1 Register User
- **Endpoint**: `POST /api/auth/register`
- **Headers**:
  - `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "username": "tsomani",
    "password": "mySecurePassword"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "message": "User registered successfully!"
  }
  ```

### 1.2 Login User
- **Endpoint**: `POST /api/auth/login`
- **Headers**:
  - `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "username": "tsomani",
    "password": "mySecurePassword"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsIn..."
  }
  ```

---

## 2. File and Storage Endpoints

### 2.1 Upload Encrypted File
- **Endpoint**: `POST /api/files/upload`
- **Headers**:
  - `Authorization: Bearer <JWT_TOKEN>`
- **Request Body (Multipart Form-Data)**:
  - `file`: Binary file (encrypted `.scf` block)
  - `originalFilename`: Name of the original file (e.g. `report.pdf`)
  - `algorithm`: The algorithm used (`AES-256-GCM`)
- **Response (200 OK)**:
  ```json
  {
    "id": 1,
    "storageKey": "11d80eb6-9fbb-40e0-b416-5fdb5c8fc2fa.scf",
    "message": "File uploaded and stored successfully!"
  }
  ```

### 2.2 List File Metadata
- **Endpoint**: `GET /api/files/list-metadata`
- **Headers**:
  - `Authorization: Bearer <JWT_TOKEN>`
- **Response (200 OK)**:
  ```json
  [
    {
      "id": 1,
      "originalFilename": "report.pdf",
      "encryptedFilename": "11d80eb6-9fbb-40e0-b416-5fdb5c8fc2fa.scf",
      "fileSize": 102450,
      "algorithm": "AES-256-GCM",
      "uploadDate": "2026-07-22T04:20:00Z"
    }
  ]
  ```

### 2.3 Download Encrypted File
- **Endpoint**: `GET /api/files/download/{id}`
- **Headers**:
  - `Authorization: Bearer <JWT_TOKEN>`
- **Response (200 OK)**:
  - Streams back the encrypted binary payload from S3/Storage.

### 2.4 Delete File
- **Endpoint**: `DELETE /api/files/delete/{id}`
- **Headers**:
  - `Authorization: Bearer <JWT_TOKEN>`
- **Response (200 OK)**:
  ```json
  {
    "message": "File permanently deleted from cloud storage and database."
  }
  ```