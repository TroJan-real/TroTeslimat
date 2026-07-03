package com.trojan.troteslimat.command;

import com.trojan.troteslimat.Troteslimat;
import com.trojan.troteslimat.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /topteslimat — Haftalık sıralama GUI'sini açar.
 *
 * @author TroJan_real
 */
public class TopDeliveryCommand implements CommandExecutor {

    private final Troteslimat plugin;

    public TopDeliveryCommand(Troteslimat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfig(), "players-only");
            return true;
        }
        if (!player.hasPermission("troteslimat.use")) {
            MessageUtil.send(player, plugin.getConfig(), "no-permission");
            return true;
        }
        plugin.getTopDeliveryGUI().open(player);
        return true;
    }
}
