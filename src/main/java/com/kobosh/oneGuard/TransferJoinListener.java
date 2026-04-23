package com.kobosh.oneGuard;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class TransferJoinListener implements Listener {

    private final OneGuard plugin;

    public TransferJoinListener(OneGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        CompletableFuture<byte[]> cookieFuture = player.retrieveCookie(plugin.getTransferCookieKey());

        cookieFuture.thenAccept(data -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (data == null || data.length == 0) {
                plugin.getLogger().warning("Missing transfer cookie for " + player.getName()
                        + " using key " + plugin.getTransferCookieKey());
                player.kickPlayer("Connection denied: transfer cookie missing.");
                return;
            }

            String cookie = new String(data, StandardCharsets.UTF_8);
            try {
                String payload = plugin.verifyTransferCookie(cookie);
                applyUuidFromPayload(player, payload);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                plugin.getLogger().warning("Invalid transfer cookie for " + player.getName() + ": " + exception.getMessage());
                player.kickPlayer("Connection denied: transfer signature invalid.");
            }
        })).exceptionally(ex -> {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> player.kickPlayer("Connection denied: transfer validation failed."));
            plugin.getLogger().warning("Failed to read transfer cookie for " + player.getName() + ": " + ex.getMessage());
            return null;
        });
    }

    private void applyUuidFromPayload(Player player, String payload) {
        JsonObject json;
        try {
            json = JsonParser.parseString(payload).getAsJsonObject();
        } catch (Exception e) {
            plugin.getLogger().warning("Could not parse cookie payload JSON for " + player.getName() + ": " + e.getMessage());
            player.kickPlayer("Connection denied: transfer payload malformed.");
            return;
        }

        if (!json.has("uuid") || !json.has("username") || !json.has("cracked") || !json.has("time")) {
            plugin.getLogger().warning("Cookie payload missing required fields for " + player.getName());
            player.kickPlayer("Connection denied: transfer payload malformed.");
            return;
        }

        long cookieTime = json.get("time").getAsLong();
        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - cookieTime) > 10) {
            plugin.getLogger().warning("Cookie expired for " + player.getName()
                    + " (cookie time: " + cookieTime + ", server time: " + now + ")");
            player.kickPlayer("Connection denied: transfer cookie expired.");
            return;
        }

        String cookieUsername = json.get("username").getAsString();
        if (!player.getName().equalsIgnoreCase(cookieUsername)) {
            plugin.getLogger().warning("Username mismatch for " + player.getName()
                    + ": cookie username was '" + cookieUsername + "'");
            player.kickPlayer("Connection denied: username mismatch.");
            return;
        }

        UUID cookieUuid;
        try {
            cookieUuid = UUID.fromString(json.get("uuid").getAsString());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Cookie payload contains invalid UUID for " + player.getName() + ": " + e.getMessage());
            player.kickPlayer("Connection denied: transfer payload UUID invalid.");
            return;
        }

        boolean cracked = json.get("cracked").getAsBoolean();

        if (!cracked && !player.getUniqueId().equals(cookieUuid)) {
            plugin.getLogger().info("Reassigning UUID for " + player.getName()
                    + " from " + player.getUniqueId() + " (offline) to " + cookieUuid + " (premium)");
            PlayerProfile profile = Bukkit.createProfile(cookieUuid, player.getName());
            player.setPlayerProfile(profile);
        }

        plugin.getLogger().info("Transfer accepted for " + player.getName()
                + " (UUID: " + cookieUuid + ", cracked: " + cracked + ")");
    }
}


