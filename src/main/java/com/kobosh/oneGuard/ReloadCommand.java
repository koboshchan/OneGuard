package com.kobosh.oneGuard;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public final class ReloadCommand implements BasicCommand {

    private static final String PERMISSION = "oneguard.reload";

    private final OneGuard plugin;

    public ReloadCommand(OneGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        plugin.reloadPublicKey();
        sender.sendMessage(ChatColor.GREEN + "OneGuard config reloaded.");
        sender.sendMessage(ChatColor.YELLOW + "publicKey: " + plugin.getPublicKey());
    }

    @Override
    public String permission() {
        return PERMISSION;
    }
}

