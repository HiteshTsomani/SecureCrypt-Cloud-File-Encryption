package com.securecrypt;

import com.securecrypt.crypto.SecureCryptCrypto;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Cross-Compatibility verification test helper (Java side).
 */
public class CompatTest {

    public static void main(String[] args) {
        System.out.println("--- Starting Java Compatibility Harness ---");
        try {
            // Part 1: Encrypt files for JavaScript to decrypt
            String javaMessage = "Hello from Java Cryptography Architecture!";
            byte[] javaBytes = javaMessage.getBytes(StandardCharsets.UTF_8);

            // V1 Password-based
            char[] password = "CompatPassword123!".toCharArray();
            byte[] v1Payload = SecureCryptCrypto.encryptPasswordBased(javaBytes, password);
            Files.write(Paths.get("java_to_js.password.scf"), v1Payload);
            System.out.println("[JAVA] Generated java_to_js.password.scf");

            // V2 Key-based
            byte[] rawKey = SecureCryptCrypto.generateRandomKey();
            Files.write(Paths.get("java_to_js.sck"), rawKey);
            byte[] v2Payload = SecureCryptCrypto.encryptKeyBased(javaBytes, rawKey);
            Files.write(Paths.get("java_to_js.key.scf"), v2Payload);
            System.out.println("[JAVA] Generated java_to_js.key.scf and key java_to_js.sck");

            System.out.println("[JAVA] Awaiting JavaScript compilation execution...");

            // Part 2: Wait/Trigger check. We can read JS outputs if they exist
            // (We will run this Java process, then the JS process, then another Java run to verify JS outputs)
            if (Files.exists(Paths.get("js_to_java.password.scf"))) {
                System.out.println("\n[JAVA] Verifying JavaScript outputs...");
                
                // Decrypt V1
                byte[] jsV1Payload = Files.readAllBytes(Paths.get("js_to_java.password.scf"));
                byte[] decV1 = SecureCryptCrypto.decryptPasswordBased(jsV1Payload, password);
                String decV1Str = new String(decV1, StandardCharsets.UTF_8);
                System.out.println("[JAVA] Decrypted JS V1 text: " + decV1Str);
                
                // Decrypt V2
                byte[] jsV2Payload = Files.readAllBytes(Paths.get("js_to_java.key.scf"));
                byte[] jsKeyBytes = Files.readAllBytes(Paths.get("js_to_java.sck"));
                byte[] decV2 = SecureCryptCrypto.decryptKeyBased(jsV2Payload, jsKeyBytes);
                String decV2Str = new String(decV2, StandardCharsets.UTF_8);
                System.out.println("[JAVA] Decrypted JS V2 text: " + decV2Str);
                
                System.out.println("[JAVA] Compatibility check completed successfully!");
            }

        } catch (Exception e) {
            System.err.println("[JAVA ERROR] Compatibility check failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
