// DOM Elements
const encryptForm = document.getElementById('encryptForm');
const decryptForm = document.getElementById('decryptForm');
const encConsole = document.getElementById('encConsole');
const decConsole = document.getElementById('decConsole');

// Encrypt inputs
const encMethodToggle = document.getElementById('encMethodToggle');
const encPasswordGroup = document.getElementById('encPasswordGroup');
const encKeyGroup = document.getElementById('encKeyGroup');
const encPassword = document.getElementById('encPassword');
const encDropzone = document.getElementById('encDropzone');
const encFileInput = document.getElementById('encFileInput');
const encFileInfo = document.getElementById('encFileInfo');
const generateKeyBtn = document.getElementById('generateKeyBtn');
const loadedKeyInfo = document.getElementById('loadedKeyInfo');
const saveToCloudCheckbox = document.getElementById('saveToCloudCheckbox');

// Decrypt inputs
const decMethodToggle = document.getElementById('decMethodToggle');
const decPasswordGroup = document.getElementById('decPasswordGroup');
const decKeyGroup = document.getElementById('decKeyGroup');
const decPassword = document.getElementById('decPassword');
const decDropzone = document.getElementById('decDropzone');
const decFileInput = document.getElementById('decFileInput');
const decFileInfo = document.getElementById('decFileInfo');
const uploadKeyInput = document.getElementById('uploadKeyInput');
const uploadKeyBtn = document.getElementById('uploadKeyBtn');
const decKeyFileInfo = document.getElementById('decKeyFileInfo');

// Cloud Elements
const cloudPortal = document.getElementById('cloudPortal');
const cloudDashboard = document.getElementById('cloudDashboard');
const currentUserDisplay = document.getElementById('currentUserDisplay');
const logoutBtn = document.getElementById('logoutBtn');
const authForm = document.getElementById('authForm');
const authUsername = document.getElementById('authUsername');
const authPassword = document.getElementById('authPassword');
const authSubmitBtn = document.getElementById('authSubmitBtn');
const authToggle = document.getElementById('authToggle');
const authToggleLogin = document.getElementById('authToggleLogin');
const authToggleRegister = document.getElementById('authToggleRegister');
const metadataList = document.getElementById('metadataList');

// State variables
let encMethod = 'password'; // 'password' or 'key'
let decMethod = 'password'; // 'password' or 'key'
let selectedEncFile = null;
let selectedDecFile = null;
let generatedKeyBytes = null; // Uint8Array for key-based encrypt
let uploadedDecKeyBytes = null; // Uint8Array for key-based decrypt

// Cloud State variables
const BACKEND_URL = 'http://YOUR_EC2_PUBLIC_IP:8080';
let currentToken = localStorage.getItem('jwtToken') || null;
let currentUsername = localStorage.getItem('jwtUsername') || null;
let authMode = 'login'; // 'login' or 'register'
let cloudDecryptedBytesRef = null; // Uint8Array of downloaded cloud file
let cloudDecryptedMetadataRef = null; // metadata object for cloud file

// Custom SCRYPT Format Constants
const MAGIC_BYTES = new Uint8Array([0x53, 0x43, 0x52, 0x59, 0x50, 0x54]); // "SCRYPT"
const VERSION_PASSWORD_BASED = 0x01;
const VERSION_KEY_BASED = 0x02;
const SALT_LENGTH = 16;
const IV_LENGTH = 12;
const PBKDF2_ITERATIONS = 65536;

// Hex Helper Functions
function bytesToHex(bytes) {
    return Array.from(bytes).map(b => b.toString(16).padStart(2, '0').toUpperCase()).join('');
}

function hexToBytes(hex) {
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < hex.length; i += 2) {
        bytes[i / 2] = parseInt(hex.substring(i, i + 2), 16);
    }
    return bytes;
}

// GZIP Compression / Decompression Helpers using Web Streams API
async function compressBytes(bytes) {
    const stream = new Response(bytes).body.pipeThrough(new CompressionStream('gzip'));
    const compressedResponse = new Response(stream);
    const compressedBuffer = await compressedResponse.arrayBuffer();
    return new Uint8Array(compressedBuffer);
}

async function decompressBytes(bytes) {
    const stream = new Response(bytes).body.pipeThrough(new DecompressionStream('gzip'));
    const decompressedResponse = new Response(stream);
    const decompressedBuffer = await decompressedResponse.arrayBuffer();
    return new Uint8Array(decompressedBuffer);
}

// SPA Routing Controller
function navigateToView(targetViewId) {
    // Hide all views
    document.querySelectorAll('.page-view').forEach(view => {
        view.classList.add('hidden');
    });

    // Show target view
    const targetView = document.getElementById(targetViewId);
    if (targetView) {
        targetView.classList.remove('hidden');
    }

    // Update active state in header navigation links
    document.querySelectorAll('.nav-link').forEach(link => {
        if (link.getAttribute('data-target') === targetViewId) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}

// Setup navigation click handlers
document.querySelectorAll('.nav-link, [data-target]').forEach(element => {
    element.addEventListener('click', (e) => {
        e.preventDefault();
        const targetViewId = element.getAttribute('data-target');
        if (targetViewId) {
            navigateToView(targetViewId);
            if (targetViewId === 'cloudView') {
                updateCloudUI();
            }
        }
    });
});

// Visual Log Console Helpers
function logConsole(consoleEl, message, type = '') {
    const timestamp = new Date().toLocaleTimeString();
    const entry = document.createElement('div');
    entry.className = `log-entry ${type}`;
    entry.textContent = `[${timestamp}] ${message}`;
    consoleEl.appendChild(entry);
    consoleEl.scrollTop = consoleEl.scrollHeight;
}

function clearConsole(consoleEl) {
    consoleEl.innerHTML = '';
}

// Bind Clear Console Events
document.querySelectorAll('.console-clear').forEach(btn => {
    btn.addEventListener('click', (e) => {
        const consoleId = e.target.getAttribute('data-clear');
        clearConsole(document.getElementById(consoleId));
    });
});

// Setup Toggle Buttons
encMethodToggle.addEventListener('click', (e) => {
    if (!e.target.classList.contains('toggle-btn')) return;
    document.querySelectorAll('#encMethodToggle .toggle-btn').forEach(btn => btn.classList.remove('active'));
    e.target.classList.add('active');

    encMethod = e.target.getAttribute('data-method');
    if (encMethod === 'password') {
        encPasswordGroup.classList.remove('hidden');
        encKeyGroup.classList.add('hidden');
    } else {
        encPasswordGroup.classList.add('hidden');
        encKeyGroup.classList.remove('hidden');
    }
    logConsole(encConsole, `Switched encryption method to: ${encMethod.toUpperCase()}`, 'info');
});

decMethodToggle.addEventListener('click', (e) => {
    if (!e.target.classList.contains('toggle-btn')) return;
    document.querySelectorAll('#decMethodToggle .toggle-btn').forEach(btn => btn.classList.remove('active'));
    e.target.classList.add('active');

    decMethod = e.target.getAttribute('data-method');
    if (decMethod === 'password') {
        decPasswordGroup.classList.remove('hidden');
        decKeyGroup.classList.add('hidden');
    } else {
        decPasswordGroup.classList.add('hidden');
        decKeyGroup.classList.remove('hidden');
    }
    logConsole(decConsole, `Switched decryption method to: ${decMethod.toUpperCase()}`, 'info');
});

// Setup Drag & Drop
function setupDragAndDrop(dropzone, fileInput, fileInfoEl, isEncrypt) {
    const consoleEl = isEncrypt ? encConsole : decConsole;

    dropzone.addEventListener('click', () => fileInput.click());

    dropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropzone.style.borderColor = isEncrypt ? 'var(--accent-purple)' : 'var(--accent-cyan)';
    });

    dropzone.addEventListener('dragleave', () => {
        dropzone.style.borderColor = 'rgba(255, 255, 255, 0.15)';
    });

    dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropzone.style.borderColor = 'rgba(255, 255, 255, 0.15)';
        if (e.dataTransfer.files.length > 0) {
            handleFileSelection(e.dataTransfer.files[0], fileInput, fileInfoEl, isEncrypt, consoleEl);
        }
    });

    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleFileSelection(e.target.files[0], fileInput, fileInfoEl, isEncrypt, consoleEl);
        }
    });
}

function handleFileSelection(file, fileInput, fileInfoEl, isEncrypt, consoleEl) {
    if (isEncrypt && saveToCloudCheckbox.checked && file.size > 20 * 1024 * 1024) {
        alert(`Files saved to the cloud cannot exceed 20 MB. "${file.name}" is ${formatBytes(file.size)}. Please uncheck 'Save encrypted file metadata to cloud database' to encrypt locally, or select a smaller file.`);
        logConsole(consoleEl, `Error: Selected file is ${formatBytes(file.size)}, which exceeds the 20 MB limit for cloud storage.`, 'error');
        fileInput.value = '';
        fileInfoEl.classList.remove('active');
        selectedEncFile = null;
        return;
    }

    if (isEncrypt) {
        selectedEncFile = file;
    } else {
        selectedDecFile = file;
        // Clear cloud cache since local file was loaded
        cloudDecryptedBytesRef = null;
        cloudDecryptedMetadataRef = null;
    }

    fileInfoEl.querySelector('.filename').textContent = `${file.name} (${formatBytes(file.size)})`;
    fileInfoEl.classList.add('active');
    logConsole(consoleEl, `Selected file: ${file.name} (${file.size} bytes)`, 'success');
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

setupDragAndDrop(encDropzone, encFileInput, encFileInfo, true);
setupDragAndDrop(decDropzone, decFileInput, decFileInfo, false);

// -------------------------------------------------------------
// Cryptography Functions (Web Crypto API)
// -------------------------------------------------------------

// PBKDF2 key derivation
async function deriveKeyFromPassword(passwordStr, saltBytes) {
    const encoder = new TextEncoder();
    const passwordBytes = encoder.encode(passwordStr);

    // Import password as a raw key
    const baseKey = await window.crypto.subtle.importKey(
        "raw",
        passwordBytes,
        { name: "PBKDF2" },
        false,
        ["deriveKey"]
    );

    // Derive AES-GCM 256 key
    return await window.crypto.subtle.deriveKey(
        {
            name: "PBKDF2",
            salt: saltBytes,
            iterations: PBKDF2_ITERATIONS,
            hash: "SHA-256"
        },
        baseKey,
        { name: "AES-GCM", length: 256 },
        false,
        ["encrypt", "decrypt"]
    );
}

// Generate & Download V2 Key File (.sck)
generateKeyBtn.addEventListener('click', async () => {
    try {
        logConsole(encConsole, "Generating 256-bit AES key...", "info");

        // Generate cryptographic key
        const key = await window.crypto.subtle.generateKey(
            { name: "AES-GCM", length: 256 },
            true,
            ["encrypt", "decrypt"]
        );

        // Export to raw bytes
        const rawKeyBuffer = await window.crypto.subtle.exportKey("raw", key);
        generatedKeyBytes = new Uint8Array(rawKeyBuffer);

        logConsole(encConsole, `Key generated: ${bytesToHex(generatedKeyBytes)}`, "success");
        loadedKeyInfo.classList.remove('hidden');

        // Offer download
        const blob = new Blob([generatedKeyBytes], { type: 'application/octet-stream' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'keyfile.sck';
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
            if (document.body.contains(a)) document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }, 1000);

        logConsole(encConsole, "Downloaded key file: keyfile.sck", "success");
    } catch (e) {
        logConsole(encConsole, `Key generation failed: ${e.message}`, "error");
        console.error(e);
    }
});

// Upload Decryption Key File (.sck)
uploadKeyBtn.addEventListener('click', () => uploadKeyInput.click());
uploadKeyInput.addEventListener('change', async (e) => {
    if (e.target.files.length === 0) return;
    const file = e.target.files[0];

    try {
        const fileBuffer = await file.arrayBuffer();
        uploadedDecKeyBytes = new Uint8Array(fileBuffer);

        if (uploadedDecKeyBytes.length !== 32) {
            throw new Error(`Key file size is ${uploadedDecKeyBytes.length} bytes, but AES-256 requires exactly 32 bytes.`);
        }

        logConsole(decConsole, `Key file loaded: ${file.name} (${bytesToHex(uploadedDecKeyBytes).substring(0, 16)}...)`, "success");
        decKeyFileInfo.querySelector('.key-filename').textContent = file.name;
        decKeyFileInfo.classList.remove('hidden');
    } catch (err) {
        logConsole(decConsole, `Error loading key file: ${err.message}`, "error");
        uploadedDecKeyBytes = null;
        decKeyFileInfo.classList.add('hidden');
    }
});

saveToCloudCheckbox.addEventListener('change', () => {
    if (saveToCloudCheckbox.checked && selectedEncFile && selectedEncFile.size > 20 * 1024 * 1024) {
        alert(`Files saved to the cloud cannot exceed 20 MB. "${selectedEncFile.name}" is ${formatBytes(selectedEncFile.size)}. Please select a smaller file, or encrypt locally without saving to the cloud.`);
        saveToCloudCheckbox.checked = false;
    }
});

// -------------------------------------------------------------
// Encryption Action
// -------------------------------------------------------------
encryptForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!selectedEncFile) {
        logConsole(encConsole, "Error: No file selected to encrypt.", "error");
        alert("Please select a file to encrypt first.");
        return;
    }

    if (saveToCloudCheckbox.checked && selectedEncFile.size > 20 * 1024 * 1024) {
        logConsole(encConsole, `Error: Selected file is ${formatBytes(selectedEncFile.size)}, which exceeds the 20 MB limit for cloud storage.`, "error");
        alert("Files saved to the cloud cannot exceed 20 MB. Please uncheck 'Save encrypted file metadata to cloud database' to encrypt locally, or select a smaller file.");
        return;
    }

    try {
        logConsole(encConsole, `Beginning encryption of ${selectedEncFile.name}...`, "info");
        const fileData = await selectedEncFile.arrayBuffer();
        const fileBytes = new Uint8Array(fileData);

        logConsole(encConsole, "Compressing plaintext file using GZIP...", "info");
        const compressedBytes = await compressBytes(fileBytes);
        logConsole(encConsole, `Compression complete: ${fileBytes.length} bytes -> ${compressedBytes.length} bytes`, "info");

        let payloadBytes;

        if (encMethod === 'password') {
            const passwordStr = encPassword.value;
            if (!passwordStr) {
                throw new Error("Password is required for password-based encryption.");
            }

            logConsole(encConsole, "Generating secure salt and IV...", "info");
            const salt = window.crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
            const iv = window.crypto.getRandomValues(new Uint8Array(IV_LENGTH));

            logConsole(encConsole, `Salt: ${bytesToHex(salt)}`, "info");
            logConsole(encConsole, `IV: ${bytesToHex(iv)}`, "info");
            logConsole(encConsole, "Deriving key using PBKDF2 (65,536 iterations)...", "info");

            const aesKey = await deriveKeyFromPassword(passwordStr, salt);
            logConsole(encConsole, "Key derived. Encrypting compressed payload...", "info");

            const ciphertextBuffer = await window.crypto.subtle.encrypt(
                { name: "AES-GCM", iv: iv, tagLength: 128 },
                aesKey,
                compressedBytes
            );
            const ciphertextBytes = new Uint8Array(ciphertextBuffer);

            logConsole(encConsole, "Encryption complete. Packaging binary V1 layout...", "info");

            // Assemble SCRYPT V1 format
            payloadBytes = new Uint8Array(MAGIC_BYTES.length + 1 + SALT_LENGTH + IV_LENGTH + ciphertextBytes.length);
            payloadBytes.set(MAGIC_BYTES, 0);
            payloadBytes[MAGIC_BYTES.length] = VERSION_PASSWORD_BASED;
            payloadBytes.set(salt, MAGIC_BYTES.length + 1);
            payloadBytes.set(iv, MAGIC_BYTES.length + 1 + SALT_LENGTH);
            payloadBytes.set(ciphertextBytes, MAGIC_BYTES.length + 1 + SALT_LENGTH + IV_LENGTH);

        } else {
            // Key based
            if (!generatedKeyBytes) {
                throw new Error("Please generate or download a key file first.");
            }

            logConsole(encConsole, "Generating secure IV...", "info");
            const iv = window.crypto.getRandomValues(new Uint8Array(IV_LENGTH));
            logConsole(encConsole, `IV: ${bytesToHex(iv)}`, "info");

            logConsole(encConsole, "Importing key...", "info");
            const aesKey = await window.crypto.subtle.importKey(
                "raw",
                generatedKeyBytes,
                { name: "AES-GCM" },
                false,
                ["encrypt"]
            );

            logConsole(encConsole, "Encrypting compressed payload...", "info");
            const ciphertextBuffer = await window.crypto.subtle.encrypt(
                { name: "AES-GCM", iv: iv, tagLength: 128 },
                aesKey,
                compressedBytes
            );
            const ciphertextBytes = new Uint8Array(ciphertextBuffer);

            logConsole(encConsole, "Encryption complete. Packaging binary V2 layout...", "info");

            // Assemble SCRYPT V2 format
            payloadBytes = new Uint8Array(MAGIC_BYTES.length + 1 + IV_LENGTH + ciphertextBytes.length);
            payloadBytes.set(MAGIC_BYTES, 0);
            payloadBytes[MAGIC_BYTES.length] = VERSION_KEY_BASED;
            payloadBytes.set(iv, MAGIC_BYTES.length + 1);
            payloadBytes.set(ciphertextBytes, MAGIC_BYTES.length + 1 + IV_LENGTH);
        }

        const isCloudSave = saveToCloudCheckbox && saveToCloudCheckbox.checked;

        if (isCloudSave) {
            // Cloud Metadata upload stage
            if (!currentToken) {
                logConsole(encConsole, "Cloud Sync Warning: You must be logged in to save metadata. Redirecting to Cloud Files portal...", "warning");
                alert("Please log in or register in the Cloud Files portal first to save encrypted file metadata to the cloud database.");
                navigateToView('cloudView');
                updateCloudUI();
                return;
            }

            logConsole(encConsole, "Uploading encrypted binary and metadata to cloud database...", "info");

            const formData = new FormData();
            const encryptedFile = new File([payloadBytes], selectedEncFile.name + '.scf', { type: 'application/octet-stream' });

            formData.append('file', encryptedFile);
            formData.append('originalFilename', selectedEncFile.name);
            formData.append('algorithm', 'AES-256-GCM');

            const response = await fetch(`${BACKEND_URL}/api/files/upload`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${currentToken}`
                },
                body: formData
            });

            if (response.ok) {
                const resData = await response.json();
                logConsole(encConsole, `Cloud Upload Success! Saved to cloud database (Record ID: ${resData.id})`, "success");
                alert(`File "${selectedEncFile.name}" successfully encrypted and saved to cloud database!`);
                await fetchMetadataList();
                navigateToView('cloudView');
                updateCloudUI();
            } else {
                const resErr = await response.json().catch(() => ({ error: 'Unknown server error' }));
                logConsole(encConsole, `Cloud Upload Failure: ${resErr.error || 'Server error'}`, "error");
                alert(`Cloud Save Failed: ${resErr.error || 'Server error'}`);
            }
        } else {
            // Local Browser Download Stage
            logConsole(encConsole, "Downloading encrypted file (.scf)...", "info");
            const blob = new Blob([payloadBytes], { type: 'application/x-securecrypt' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            const finalFileName = selectedEncFile.name + '.scf';
            a.setAttribute('download', finalFileName);
            a.download = finalFileName;
            document.body.appendChild(a);
            a.click();
            setTimeout(() => {
                if (document.body.contains(a)) document.body.removeChild(a);
                URL.revokeObjectURL(url);
            }, 5000);

            logConsole(encConsole, "Encrypted file downloaded successfully!", "success");
        }
    } catch (err) {
        logConsole(encConsole, `Encryption failed: ${err.message}`, "error");
        console.error(err);
    }
});

// -------------------------------------------------------------
// Decryption Action
// -------------------------------------------------------------
decryptForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!selectedDecFile && !cloudDecryptedBytesRef) {
        logConsole(decConsole, "Error: No file loaded to decrypt. Please select a local .scf file or load a file from the Cloud dashboard.", "error");
        alert("Please load or select an encrypted file first.");
        return;
    }

    try {
        let fileBytes;
        let originalName;

        if (cloudDecryptedBytesRef) {
            logConsole(decConsole, `Using encrypted binary downloaded from Cloud Storage (${cloudDecryptedMetadataRef.originalFilename})...`, "info");
            fileBytes = cloudDecryptedBytesRef;
            originalName = cloudDecryptedMetadataRef.originalFilename;
        } else {
            logConsole(decConsole, `Beginning parsing of local file ${selectedDecFile.name}...`, "info");
            const fileData = await selectedDecFile.arrayBuffer();
            fileBytes = new Uint8Array(fileData);
            originalName = selectedDecFile.name;
        }

        // 1. Verify Minimum length
        const minLength = MAGIC_BYTES.length + 1 + IV_LENGTH + 16; // GCM auth tag is 16 bytes
        if (fileBytes.length < minLength) {
            throw new Error("File is too short to be a valid SCRYPT format.");
        }

        // 2. Verify Magic bytes
        for (let i = 0; i < MAGIC_BYTES.length; i++) {
            if (fileBytes[i] !== MAGIC_BYTES[i]) {
                throw new Error("Invalid file format. Magic bytes 'SCRYPT' not found.");
            }
        }
        logConsole(decConsole, "Magic bytes 'SCRYPT' verified successfully.", "info");

        // 3. Read Version/Mode
        const version = fileBytes[MAGIC_BYTES.length];
        logConsole(decConsole, `Detected encryption format version: 0x0${version}`, "info");

        let decryptedBuffer;

        if (version === VERSION_PASSWORD_BASED) {
            // V1 Password-based
            if (decMethod !== 'password') {
                throw new Error("This file is password-encrypted (V1). Please switch to 'Password' decryption mode.");
            }

            const passwordStr = decPassword.value;
            if (!passwordStr) {
                throw new Error("Password is required to decrypt this file.");
            }

            // Extract Salt, IV, and Ciphertext
            let offset = MAGIC_BYTES.length + 1;
            const salt = fileBytes.subarray(offset, offset + SALT_LENGTH);
            offset += SALT_LENGTH;
            const iv = fileBytes.subarray(offset, offset + IV_LENGTH);
            offset += IV_LENGTH;
            const ciphertext = fileBytes.subarray(offset);

            logConsole(decConsole, `Extracted Salt: ${bytesToHex(salt)}`, "info");
            logConsole(decConsole, `Extracted IV: ${bytesToHex(iv)}`, "info");
            logConsole(decConsole, "Deriving key using PBKDF2...", "info");

            const aesKey = await deriveKeyFromPassword(passwordStr, salt);
            logConsole(decConsole, "Key derived. Starting decryption...", "info");

            decryptedBuffer = await window.crypto.subtle.decrypt(
                { name: "AES-GCM", iv: iv, tagLength: 128 },
                aesKey,
                ciphertext
            );

        } else if (version === VERSION_KEY_BASED) {
            // V2 Key-based
            if (decMethod !== 'key') {
                throw new Error("This file is key-encrypted (V2). Please switch to 'Key File' decryption mode.");
            }

            if (!uploadedDecKeyBytes) {
                throw new Error("Please upload the corresponding .sck key file first.");
            }

            // Extract IV and Ciphertext
            let offset = MAGIC_BYTES.length + 1;
            const iv = fileBytes.subarray(offset, offset + IV_LENGTH);
            offset += IV_LENGTH;
            const ciphertext = fileBytes.subarray(offset);

            logConsole(decConsole, `Extracted IV: ${bytesToHex(iv)}`, "info");
            logConsole(decConsole, "Importing raw AES-256 key...", "info");

            const aesKey = await window.crypto.subtle.importKey(
                "raw",
                uploadedDecKeyBytes,
                { name: "AES-GCM" },
                false,
                ["decrypt"]
            );

            logConsole(decConsole, "Key imported. Starting decryption...", "info");
            decryptedBuffer = await window.crypto.subtle.decrypt(
                { name: "AES-GCM", iv: iv, tagLength: 128 },
                aesKey,
                ciphertext
            );
        } else {
            throw new Error(`Unsupported version/mode flag: 0x0${version}`);
        }

        // 4. Download Decrypted Output File
        logConsole(decConsole, "Decryption successful! Decompressing GZIP payload...", "success");
        const decompressedBytes = await decompressBytes(new Uint8Array(decryptedBuffer));
        logConsole(decConsole, `Decompression complete: ${decryptedBuffer.byteLength} bytes -> ${decompressedBytes.length} bytes`, "success");

        let outputName;
        if (cloudDecryptedBytesRef) {
            outputName = originalName; // Preserves the exact original filename from S3/Database metadata
        } else {
            outputName = selectedDecFile.name;
            
            // 1. Strip trailing .download or .scf extensions (case-insensitive)
            outputName = outputName.replace(/(\.scf|\.download)+$/i, '');
            
            // 2. Strip duplicate counters like " (1)" if any
            outputName = outputName.replace(/\s\(\d+\)$/i, '');

            // 3. If no extension remains, prefix with decrypted_
            if (!outputName.includes('.')) {
                outputName = 'decrypted_' + outputName;
            }
        }

        const blob = new Blob([decompressedBytes], { type: 'application/octet-stream' });
        const url = URL.createObjectURL(blob);

        const a = document.createElement('a');
        a.href = url;
        a.download = outputName;
        document.body.appendChild(a);
        a.click();
        setTimeout(() => {
            if (document.body.contains(a)) document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }, 1000);

        logConsole(decConsole, `File downloaded: ${outputName} (${formatBytes(decompressedBytes.length)})`, "success");

    } catch (err) {
        logConsole(decConsole, `Decryption failed: ${err.message}`, "error");
        console.error(err);
        alert("Decryption failed. Please make sure the password or key file is correct.");
    }
});

// -------------------------------------------------------------
// Cloud Files REST Integration
// -------------------------------------------------------------

// Toggle Auth mode (Sign In vs Register)
if (authToggle) {
    authToggle.addEventListener('click', (e) => {
        const mode = e.target.getAttribute('data-auth');
        if (mode) {
            authMode = mode;
            document.querySelectorAll('#authToggle .toggle-btn').forEach(btn => {
                btn.classList.remove('active');
            });
            e.target.classList.add('active');

            if (authMode === 'login') {
                authSubmitBtn.textContent = 'Sign In';
            } else {
                authSubmitBtn.textContent = 'Register';
            }
        }
    });
}

// Handle Register / Login Form submission
if (authForm) {
    authForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const username = authUsername.value.trim();
        const password = authPassword.value;

        if (!username || !password) {
            alert("Username and password are required.");
            return;
        }

        authSubmitBtn.disabled = true;
        authSubmitBtn.textContent = authMode === 'login' ? 'Signing In...' : 'Registering...';

        try {
            if (authMode === 'register') {
                // Register request
                const response = await fetch(`${BACKEND_URL}/api/auth/register`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                const data = await response.json().catch(() => ({}));

                if (response.ok) {
                    alert("Registration successful! You can now sign in.");
                    // Toggle to login mode automatically
                    authMode = 'login';
                    authToggleRegister.classList.remove('active');
                    authToggleLogin.classList.add('active');
                    authSubmitBtn.textContent = 'Sign In';
                    authPassword.value = '';
                } else {
                    alert(data.error || "Registration failed. Try a different username.");
                }
            } else {
                // Login request
                const response = await fetch(`${BACKEND_URL}/api/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                const data = await response.json().catch(() => ({}));

                if (response.ok && data.token) {
                    currentToken = data.token;
                    currentUsername = data.username;
                    localStorage.setItem('jwtToken', currentToken);
                    localStorage.setItem('jwtUsername', currentUsername);

                    authUsername.value = '';
                    authPassword.value = '';

                    updateCloudUI();
                } else {
                    alert(data.error || "Invalid username or password.");
                }
            }
        } catch (err) {
            console.error(err);
            alert("Unable to connect to the backend server. Please verify it is running on port 8080.");
        } finally {
            authSubmitBtn.disabled = false;
            if (authSubmitBtn.textContent.includes('...')) {
                authSubmitBtn.textContent = authMode === 'login' ? 'Sign In' : 'Register';
            }
        }
    });
}

// Handle Logout
if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
        currentToken = null;
        currentUsername = null;
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('jwtUsername');
        updateCloudUI();
    });
}

// Update the Cloud dashboard UI state
function updateCloudUI() {
    if (currentToken) {
        cloudPortal.classList.add('hidden');
        cloudDashboard.classList.remove('hidden');
        currentUserDisplay.textContent = currentUsername;
        fetchMetadataList();
    } else {
        cloudPortal.classList.remove('hidden');
        cloudDashboard.classList.add('hidden');
        currentUserDisplay.textContent = '-';
        metadataList.innerHTML = '';
    }
}

// Fetch file metadata records from the backend
async function fetchMetadataList() {
    if (!currentToken) return;

    metadataList.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-secondary); padding: 24px;">Loading metadata list...</td></tr>`;

    try {
        const response = await fetch(`${BACKEND_URL}/api/files/list-metadata`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        if (response.ok) {
            const list = await response.json();
            metadataList.innerHTML = '';

            if (list.length === 0) {
                metadataList.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-secondary); padding: 24px;">No files saved on the cloud yet.</td></tr>`;
                return;
            }

            list.forEach(item => {
                const tr = document.createElement('tr');
                tr.style.borderBottom = '1px solid rgba(255, 255, 255, 0.02)';

                // Format upload date
                let dateStr = 'Unknown';
                if (item.uploadDate) {
                    try {
                        dateStr = new Date(item.uploadDate).toLocaleString();
                    } catch (e) { }
                }

                tr.innerHTML = `
                    <td style="padding: 12px 10px; font-weight: 500; color: var(--text-primary); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">📄 ${escapeHtml(item.originalFilename)}</td>
                    <td style="padding: 12px 10px; color: var(--text-secondary);">${formatBytes(item.fileSize)}</td>
                    <td style="padding: 12px 10px;"><code style="color: var(--accent-cyan); font-size: 12px;">${escapeHtml(item.algorithm)}</code></td>
                    <td style="padding: 12px 10px; color: var(--text-muted); font-size: 13px;">${dateStr}</td>
                    <td style="padding: 12px 10px; text-align: right; display: flex; justify-content: flex-end; gap: 8px;">
                        <button type="button" class="btn-table-action" 
                                onclick="loadCloudDecryption(${item.id}, '${escapeJs(item.originalFilename)}', ${item.fileSize}, '${escapeJs(item.algorithm)}')">
                            🔓 Load to Decrypt
                        </button>
                        <button type="button" class="btn-table-action" style="background: rgba(224, 86, 122, 0.15); color: var(--accent-rose); border-color: rgba(224, 86, 122, 0.3);"
                                onclick="deleteCloudFile(${item.id}, '${escapeJs(item.originalFilename)}')">
                            🗑️ Delete
                        </button>
                    </td>
                `;
                metadataList.appendChild(tr);
            });
        } else {
            metadataList.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--accent-rose); padding: 24px;">Session expired or error loading files. Please log out and back in.</td></tr>`;
        }
    } catch (err) {
        console.error(err);
        metadataList.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--accent-rose); padding: 24px;">Error connecting to API. Please make sure the backend is active.</td></tr>`;
    }
}

// Redirects cloud files selection to local decryption tab with visual notices
window.loadCloudDecryption = async function (id, originalName, size, algorithm) {
    navigateToView('decryptView');

    // Reset Decrypt console and file selection
    decConsole.innerHTML = '';
    selectedDecFile = null;
    decFileInfo.classList.remove('active');

    // Clear and reset local file dropzone to show S3 download state
    decFileInfo.querySelector('.filename').textContent = `Downloading from cloud: ${originalName}...`;
    decFileInfo.querySelector('.filename').style.color = 'var(--accent-purple)';
    decFileInfo.classList.add('active');

    logConsole(decConsole, `[CLOUD] Initiating binary retrieval for: ${originalName} (Record ID: ${id})`, 'info');
    logConsole(decConsole, `[CLOUD] Fetching encrypted payload from storage endpoint...`, 'info');

    try {
        const response = await fetch(`${BACKEND_URL}/api/files/download/${id}`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        if (response.ok) {
            const buffer = await response.arrayBuffer();
            cloudDecryptedBytesRef = new Uint8Array(buffer);
            cloudDecryptedMetadataRef = {
                originalFilename: originalName,
                fileSize: size,
                algorithm: algorithm
            };

            decFileInfo.querySelector('.filename').textContent = `Cloud S3 Source: ${originalName} (${formatBytes(size)})`;
            decFileInfo.querySelector('.filename').style.color = 'var(--accent-cyan)';

            logConsole(decConsole, `[CLOUD] Encrypted binary successfully downloaded from S3/storage!`, 'success');
            logConsole(decConsole, `[CLOUD] Please type the encryption password or upload your key file, and click 'Decrypt File' below.`, 'success');
        } else {
            const errData = await response.json().catch(() => ({ error: 'Storage retrieval error' }));
            throw new Error(errData.error || 'Failed to download binary from S3/sim');
        }
    } catch (err) {
        logConsole(decConsole, `[CLOUD] Storage Download Error: ${err.message}`, 'error');
        decFileInfo.querySelector('.filename').textContent = `Download Failed: ${originalName}`;
        decFileInfo.querySelector('.filename').style.color = 'var(--accent-rose)';
        cloudDecryptedBytesRef = null;
        cloudDecryptedMetadataRef = null;
        alert(`Failed to retrieve binary from S3 storage: ${err.message}`);
    }
};

window.deleteCloudFile = async function (id, originalFilename) {
    if (!confirm(`Are you sure you want to permanently delete "${originalFilename}" from cloud storage and database?`)) {
        return;
    }

    try {
        const response = await fetch(`${BACKEND_URL}/api/files/delete/${id}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${currentToken}`
            }
        });

        if (response.ok) {
            alert(`"${originalFilename}" has been permanently deleted.`);

            // Clear cloud cache if we just deleted the loaded file
            if (cloudDecryptedMetadataRef && cloudDecryptedMetadataRef.originalFilename === originalFilename) {
                cloudDecryptedBytesRef = null;
                cloudDecryptedMetadataRef = null;
                decFileInfo.querySelector('.filename').textContent = 'No file selected';
                decFileInfo.querySelector('.filename').style.color = 'var(--text-secondary)';
                decFileInfo.classList.remove('active');
            }

            // Reload list
            fetchMetadataList();
        } else {
            const errData = await response.json().catch(() => ({ error: 'Delete request failed' }));
            alert(`Failed to delete file: ${errData.error || 'Server error'}`);
        }
    } catch (err) {
        console.error(err);
        alert(`Failed to connect to API to delete file: ${err.message}`);
    }
};

// HTML/JS Escaping Utilities
function escapeHtml(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function escapeJs(unsafe) {
    return unsafe
        .replace(/\\/g, '\\\\')
        .replace(/'/g, "\\'")
        .replace(/"/g, '\\"');
}
