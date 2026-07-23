# SecureCrypt Frontend (Web Cryptography SPA)

This directory contains the user interface and local cryptographic routines for SecureCrypt.

## Features
- **Zero-Knowledge Design**: Key derivation (PBKDF2), file compression (GZIP), encryption and decryption (AES-GCM) run locally in browser memory.
- **Form-Data Upload**: Packages local GZIP/AES-GCM blocks into multipart form structures and posts them directly to the API storage.
- **Dynamic Views**: High-performance Single Page Application (SPA) dashboard built using vanilla HTML, CSS, and modern JavaScript.

## Setup & Run
1. Open `app.js` and set the `BACKEND_URL` to your active backend IP or port.
2. Serve this directory using any web server. For local testing, you can run the helper script in the root directory:
   ```bash
   node scratch/server.js
   ```
   and navigate to `http://localhost:3000`.