const fs = require('fs');
const path = require('path');
const { crypto } = globalThis; // Node 20 native Web Crypto

const MAGIC_BYTES = new Uint8Array([0x53, 0x43, 0x52, 0x59, 0x50, 0x54]); // "SCRYPT"
const VERSION_PASSWORD_BASED = 0x01;
const VERSION_KEY_BASED = 0x02;
const SALT_LENGTH = 16;
const IV_LENGTH = 12;
const PBKDF2_ITERATIONS = 65536;

function bytesToHex(bytes) {
    return Array.from(bytes).map(b => b.toString(16).padStart(2, '0').toUpperCase()).join('');
}

// GZIP Compression / Decompression Helpers
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

async function deriveKeyFromPassword(passwordStr, saltBytes) {
    const encoder = new TextEncoder();
    const passwordBytes = encoder.encode(passwordStr);
    
    const baseKey = await crypto.subtle.importKey(
        "raw",
        passwordBytes,
        { name: "PBKDF2" },
        false,
        ["deriveKey"]
    );
    
    return await crypto.subtle.deriveKey(
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

async function runJsTests() {
    console.log("--- Starting JavaScript Compatibility Harness ---");
    
    const passwordStr = "CompatPassword123!";
    const password = passwordStr.toCharArray ? passwordStr.toCharArray() : passwordStr;
    const jsMessage = "Hello from Javascript Web Crypto API!";
    const jsBytes = new TextEncoder().encode(jsMessage);

    try {
        // GZIP compress jsBytes first
        const compressedJsBytes = await compressBytes(jsBytes);

        // ==========================================
        // PART 1: Generate Outputs for Java to Decrypt
        // ==========================================
        
        // V1 Password-based
        const salt = crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
        const iv = crypto.getRandomValues(new Uint8Array(IV_LENGTH));
        const aesKey = await deriveKeyFromPassword(passwordStr, salt);
        
        const ciphertextBuffer = await crypto.subtle.encrypt(
            { name: "AES-GCM", iv: iv, tagLength: 128 },
            aesKey,
            compressedJsBytes
        );
        const ciphertextBytes = new Uint8Array(ciphertextBuffer);

        const v1Payload = new Uint8Array(MAGIC_BYTES.length + 1 + SALT_LENGTH + IV_LENGTH + ciphertextBytes.length);
        v1Payload.set(MAGIC_BYTES, 0);
        v1Payload[MAGIC_BYTES.length] = VERSION_PASSWORD_BASED;
        v1Payload.set(salt, MAGIC_BYTES.length + 1);
        v1Payload.set(iv, MAGIC_BYTES.length + 1 + SALT_LENGTH);
        v1Payload.set(ciphertextBytes, MAGIC_BYTES.length + 1 + SALT_LENGTH + IV_LENGTH);
        
        fs.writeFileSync('js_to_java.password.scf', v1Payload);
        console.log("[JS] Generated js_to_java.password.scf");

        // V2 Key-based
        const rawKey = crypto.getRandomValues(new Uint8Array(32)); // 256 bits
        fs.writeFileSync('js_to_java.sck', rawKey);
        
        const iv2 = crypto.getRandomValues(new Uint8Array(IV_LENGTH));
        const aesKey2 = await crypto.subtle.importKey(
            "raw",
            rawKey,
            { name: "AES-GCM" },
            false,
            ["encrypt"]
        );
        
        const ciphertextBuffer2 = await crypto.subtle.encrypt(
            { name: "AES-GCM", iv: iv2, tagLength: 128 },
            aesKey2,
            compressedJsBytes
        );
        const ciphertextBytes2 = new Uint8Array(ciphertextBuffer2);

        const v2Payload = new Uint8Array(MAGIC_BYTES.length + 1 + IV_LENGTH + ciphertextBytes2.length);
        v2Payload.set(MAGIC_BYTES, 0);
        v2Payload[MAGIC_BYTES.length] = VERSION_KEY_BASED;
        v2Payload.set(iv2, MAGIC_BYTES.length + 1);
        v2Payload.set(ciphertextBytes2, MAGIC_BYTES.length + 1 + IV_LENGTH);
        
        fs.writeFileSync('js_to_java.key.scf', v2Payload);
        console.log("[JS] Generated js_to_java.key.scf and key js_to_java.sck");

        // ==========================================
        // PART 2: Decrypt Java Outputs
        // ==========================================
        if (fs.existsSync('java_to_js.password.scf')) {
            console.log("\n[JS] Verifying Java outputs...");
            
            // Decrypt V1
            const javaV1Payload = new Uint8Array(fs.readFileSync('java_to_js.password.scf'));
            let offset = MAGIC_BYTES.length + 1;
            const extractedSalt = javaV1Payload.subarray(offset, offset + SALT_LENGTH);
            offset += SALT_LENGTH;
            const extractedIv = javaV1Payload.subarray(offset, offset + IV_LENGTH);
            offset += IV_LENGTH;
            const extractedCiphertext = javaV1Payload.subarray(offset);

            const decAesKey = await deriveKeyFromPassword(passwordStr, extractedSalt);
            const decryptedV1Buffer = await crypto.subtle.decrypt(
                { name: "AES-GCM", iv: extractedIv, tagLength: 128 },
                decAesKey,
                extractedCiphertext
            );
            const decompressedV1Bytes = await decompressBytes(new Uint8Array(decryptedV1Buffer));
            const decV1Str = new TextDecoder().decode(decompressedV1Bytes);
            console.log("[JS] Decrypted Java V1 text: " + decV1Str);

            // Decrypt V2
            const javaV2Payload = new Uint8Array(fs.readFileSync('java_to_js.key.scf'));
            const javaKeyBytes = new Uint8Array(fs.readFileSync('java_to_js.sck'));
            
            let offset2 = MAGIC_BYTES.length + 1;
            const extractedIv2 = javaV2Payload.subarray(offset2, offset2 + IV_LENGTH);
            offset2 += IV_LENGTH;
            const extractedCiphertext2 = javaV2Payload.subarray(offset2);

            const decAesKey2 = await crypto.subtle.importKey(
                "raw",
                javaKeyBytes,
                { name: "AES-GCM" },
                false,
                ["decrypt"]
            );
            const decryptedV2Buffer = await crypto.subtle.decrypt(
                { name: "AES-GCM", iv: extractedIv2, tagLength: 128 },
                decAesKey2,
                extractedCiphertext2
            );
            const decompressedV2Bytes = await decompressBytes(new Uint8Array(decryptedV2Buffer));
            const decV2Str = new TextDecoder().decode(decompressedV2Bytes);
            console.log("[JS] Decrypted Java V2 text: " + decV2Str);

            if (decV1Str === "Hello from Java Cryptography Architecture!" && 
                decV2Str === "Hello from Java Cryptography Architecture!") {
                console.log("[JS] Compatibility check completed successfully!");
            } else {
                console.log("[JS ERROR] Decrypted strings did not match expected Java output.");
            }
        }
        
    } catch (err) {
        console.error("[JS ERROR] Cryptographic error during compatibility check:", err);
    }
}

runJsTests();
