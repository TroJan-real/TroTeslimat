package com.trojan.troteslimat.command;

import com.trojan.troteslimat.Troteslimat;
import com.trojan.troteslimat.manager.DeliveryManager;
import com.trojan.troteslimat.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /troteadmin — Troteslimat yönetici komutları.
 *
 * <p>Alt komutlar:
 * <ul>
 *   <li>{@code setitem} — Haftalık eşya seçici GUI'sini açar</li>
 *   <li>{@code reset}   — Haftayı sıfırlar ve ödülleri dağıtır</li>
 *   <li>{@code top}     — Mevcut sıralamayı chat'e yazar</li>
 *   <li>{@code reload}  — config.yml'i yeniden yükler</li>
 * </ul>
 *
 * @author TroJan_real
 */
public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("setitem", "reset", "top", "reload");
    private static final String[] MEDALS = {"§6🥇", "§7🥈", "§c🥉", "§f▫", "§f▫"};

    private final Troteslimat    plugin;
    private final DeliveryManager manager;

    public AdminCommand(Troteslimat plugin, DeliveryManager manager) {
        this.plugin  = plugin;
        this.manager = manager;
    }

    // ─── Komut İşleyici ──────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String label, String[] args) {
        if (!sender.hasPermission("troteslimat.admin")) {
            MessageUtil.send(sender, plugin.getConfig(), "no-permission");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setitem" -> handleSetItem(sender);
            case "reset"   -> handleReset(sender);
            case "top"     -> handleTop(sender);
            case "reload"  -> handleReload(sender);
            default        -> sendHelp(sender);
        }
        return true;
    }

    // ─── Alt Komutlar ────────────────────────────────────────────────────────

    private void handleSetItem(CommandSender sender) {
        if (!sender.hasPermission("troteslimat.admin.setitem")) {
            MessageUtil.send(sender, plugin.getConfig(), "no-permission-sub");
            return;
        }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfig(), "players-only");
            return;
        }
        plugin.getItemSelectorGUI().open(player);
    }

    private void handleReset(CommandSender sender) {
        if (!sender.hasPermission("troteslimat.admin.reset")) {
            MessageUtil.send(sender, plugin.getConfig(), "no-permission-sub");
            return;
        }
        manager.giveWeeklyRewards();
        manager.resetWeekFull();
        MessageUtil.send(sender, plugin.getConfig(), "admin-reset-done");
    }

    private void handleTop(CommandSender sender) {
        if (!sender.hasPermission("troteslimat.admin.top")) {
            MessageUtil.send(sender, plugin.getConfig(), "no-permission-sub");
            return;
        }
        List<Map.Entry<UUID, Integer>> top = manager.getTop5();
        if (top.isEmpty()) {
            MessageUtil.send(sender, plugin.getConfig(), "admin-top-empty");
            return;
        }

        MessageUtil.send(sender, plugin.getConfig(), "admin-top-header");
        for (int i = 0; i < top.size(); i++) {
            String medal = i < MEDALS.length ? MEDALS[i] : "§f▫";
            String name  = plugin.getServer()
                                 .getOfflinePlayer(top.get(i).getKey())
                                 .getName();
            sender.sendMessage(medal + " §f" + (i + 1) + ". §e"
                    + (name != null ? name : "?")
                    + " §7- §f" + top.get(i).getValue() + " teslimat");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("troteslimat.admin.reload")) {
            MessageUtil.send(sender, plugin.getConfig(), "no-permission-sub");
            return;
        }
        plugin.reloadConfig();
        manager.reloadConfig();          // Manager config değerlerini de yeniler
        MessageUtil.send(sender, plugin.getConfig(), "admin-reload-done");
    }

    // ─── Yardım Mesajı ───────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        MessageUtil.send(sender, plugin.getConfig(), "admin-help-header");
        sender.sendMessage("§e/troteadmin setitem §7→ Haftalık eşyayı değiştir");
        sender.sendMessage("§e/troteadmin reset   §7→ Haftayı sıfırla ve ödülleri dağıt");
        sender.sendMessage("§e/troteadmin top     §7→ Mevcut sıralamayı gör");
        sender.sendMessage("§e/troteadmin reload  §7→ Config dosyasını yeniden yükle");
    }

    // ─── Tab Tamamlama ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!sender.hasPermission("troteslimat.admin")) return List.of();
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
