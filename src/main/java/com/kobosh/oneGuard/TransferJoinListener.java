package com.kobosh.oneGuard;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

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
                plugin.getLogger().info("Transfer accepted for " + player.getName() + " with verified payload: " + payload);
                player.sendMessage("Transfer verified.");
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
}


