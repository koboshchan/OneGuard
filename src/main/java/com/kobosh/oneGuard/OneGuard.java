package com.kobosh.oneGuard;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class OneGuard extends JavaPlugin {

    private String publicKey;
    private PublicKey verificationKey;
    private String signatureAlgorithm;
    private NamespacedKey transferCookieKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPublicKey();

        getServer().getPluginManager().registerEvents(new TransferJoinListener(this), this);
        registerCommand("reload", new ReloadCommand(this));

        getLogger().info("OneGuard enabled with publicKey=" + publicKey);
    }

    @Override
    public void onDisable() {
        getLogger().info("OneGuard disabled");
    }

    public void reloadPublicKey() {
        reloadConfig();
        publicKey = getConfig().getString("publicKey", "");
        signatureAlgorithm = getConfig().getString("signature.algorithm", "Ed25519");
        String keyAlgorithm = getConfig().getString("signature.keyAlgorithm", "Ed25519");

        String namespace = getConfig().getString("transferCookie.namespace", "onemcserver");
        String key = getConfig().getString("transferCookie.key", "auth");
        transferCookieKey = new NamespacedKey(namespace, key);
        verificationKey = parsePublicKey(publicKey, keyAlgorithm);

        getLogger().info("Using transfer cookie key " + transferCookieKey);
        getLogger().info("Using signature algorithm " + signatureAlgorithm);
    }

    public String getPublicKey() {
        return publicKey;
    }

    public NamespacedKey getTransferCookieKey() {
        return transferCookieKey;
    }

    public String verifyTransferCookie(String cookie) {
        if (verificationKey == null) {
            throw new IllegalStateException("verification public key is not configured");
        }

        int payloadEnd = cookie.lastIndexOf('}');
        if (payloadEnd < 0) {
            throw new IllegalArgumentException("cookie payload is missing JSON content");
        }

        String payload = cookie.substring(0, payloadEnd + 1);
        String signatureHex = cookie.substring(payloadEnd + 1).trim();
        if (signatureHex.isEmpty()) {
            throw new IllegalArgumentException("cookie signature is missing");
        }

        byte[] signatureBytes = hexToBytes(signatureHex);

        try {
            Signature verifier = Signature.getInstance(signatureAlgorithm);
            verifier.initVerify(verificationKey);
            verifier.update(payload.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(signatureBytes)) {
                throw new IllegalArgumentException("cookie signature did not verify");
            }
            return payload;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("could not verify cookie signature", exception);
        }
    }

    private PublicKey parsePublicKey(String encodedKey, String keyAlgorithm) {
        String normalizedKey = encodedKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "")
                .trim();

        if (normalizedKey.isEmpty() || normalizedKey.equals("replace-with-your-public-key")) {
            getLogger().warning("No verification public key configured; transfer cookie validation will fail closed.");
            return null;
        }

        try {
            byte[] keyBytes = decodePublicKey(normalizedKey, keyAlgorithm);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance(keyAlgorithm).generatePublic(spec);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            getLogger().warning("Failed to parse verification public key: " + exception.getMessage());
            return null;
        }
    }

    private byte[] decodePublicKey(String encodedKey, String keyAlgorithm) {
        if ("Ed25519".equalsIgnoreCase(keyAlgorithm) && encodedKey.matches("[0-9a-fA-F]{64}")) {
            byte[] rawKey = hexToBytes(encodedKey);
            byte[] prefixedKey = new byte[12 + rawKey.length];

            // SubjectPublicKeyInfo wrapper for a raw Ed25519 public key.
            byte[] prefix = new byte[] {
                    0x30, 0x2a,
                    0x30, 0x05,
                    0x06, 0x03, 0x2b, 0x65, 0x70,
                    0x03, 0x21, 0x00
            };
            System.arraycopy(prefix, 0, prefixedKey, 0, prefix.length);
            System.arraycopy(rawKey, 0, prefixedKey, prefix.length, rawKey.length);
            return prefixedKey;
        }

        return Base64.getDecoder().decode(encodedKey);
    }

    private byte[] hexToBytes(String value) {
        if ((value.length() & 1) != 0) {
            throw new IllegalArgumentException("cookie signature hex has odd length");
        }

        byte[] result = new byte[value.length() / 2];
        for (int index = 0; index < value.length(); index += 2) {
            int high = Character.digit(value.charAt(index), 16);
            int low = Character.digit(value.charAt(index + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("cookie signature contains non-hex characters");
            }
            result[index / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }
}
