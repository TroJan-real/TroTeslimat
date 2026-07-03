package com.trojan.troteslimat;

import com.trojan.troteslimat.command.AdminCommand;
import com.trojan.troteslimat.command.DeliverCommand;
import com.trojan.troteslimat.command.TopDeliveryCommand;
import com.trojan.troteslimat.discord.DiscordBot;
import com.trojan.troteslimat.gui.DeliveryGUI;
import com.trojan.troteslimat.gui.ItemSelectorGUI;
import com.trojan.troteslimat.gui.TopDeliveryGUI;
import com.trojan.troteslimat.manager.DataManager;
import com.trojan.troteslimat.manager.DeliveryManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * Troteslimat — Haftalık teslimat yarışı plugini.
 *
 * <p>Ana plugin sınıfı; diğer tüm bileşenlerin başlatılmasından
 * ve yaşam döngüsü yönetiminden sorumludur.
 *
 * @author TroJan_real
 */
public class Troteslimat extends JavaPlugin {

    // ─── Singleton ───────────────────────────────────────────────────────────

    private static Troteslimat instance;

    /** Singleton erişimi — sadece statik bağlamda (ör. DiscordBot) kullanın. */
    public static Troteslimat getInstance() {
        return instance;
    }

    // ─── Bileşenler ──────────────────────────────────────────────────────────

    private DataManager     dataManager;
    private DeliveryManager deliveryManager;
    private DiscordBot      discordBot;

    private DeliveryGUI     deliveryGUI;
    private ItemSelectorGUI itemSelectorGUI;
    private TopDeliveryGUI  topDeliveryGUI;

    // ─── Yaşam Döngüsü ───────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        validateConfigVersion();

        // Manager'lar
        dataManager     = new DataManager(this);
        deliveryManager = new DeliveryManager(this);

        // GUI'lar
        deliveryGUI     = new DeliveryGUI(this);
        itemSelectorGUI = new ItemSelectorGUI(this);
        topDeliveryGUI  = new TopDeliveryGUI(this);

        // Komutlar
        registerCommands();

        // Listener'lar
        Bukkit.getPluginManager().registerEvents(deliveryGUI,     this);
        Bukkit.getPluginManager().registerEvents(itemSelectorGUI, this);
        Bukkit.getPluginManager().registerEvents(topDeliveryGUI,  this);

        // Discord (opsiyonel)
        initDiscordBot();

        // Haftalık sıfırlama kontrolü
        setupWeeklyResetTask();

        getLogger().info("[Troteslimat] v" + getDescription().getVersion()
                + " başarıyla yüklendi! — by TroJan_real");
    }

    @Override
    public void onDisable() {
        if (deliveryManager != null) deliveryManager.saveToData();
        if (discordBot != null)      discordBot.shutdown();
        getLogger().info("[Troteslimat] Plugin kapatıldı.");
    }

    // ─── Komut Kaydı ─────────────────────────────────────────────────────────

    private void registerCommands() {
        Objects.requireNonNull(getCommand("teslimat"))
               .setExecutor(new DeliverCommand(this));
        Objects.requireNonNull(getCommand("topteslimat"))
               .setExecutor(new TopDeliveryCommand(this));

        AdminCommand adminCmd = new AdminCommand(this, deliveryManager);
        Objects.requireNonNull(getCommand("troteadmin"))
               .setExecutor(adminCmd);
        Objects.requireNonNull(getCommand("troteadmin"))
               .setTabCompleter(adminCmd);
    }

    // ─── Discord ─────────────────────────────────────────────────────────────

    private void initDiscordBot() {
        if (!getConfig().getBoolean("discord.enabled", false)) return;

        String token     = getConfig().getString("discord.token", "");
        String channelId = getConfig().getString("discord.channel-id", "");
        String guildId   = getConfig().getString("discord.guild-id", "");

        if (token.isBlank() || token.equals("BOT_TOKENINI_BURAYA_YAZ")) {
            getLogger().warning("[Troteslimat] Discord etkin ama token ayarlanmamış!"
                    + " config.yml içindeki discord.token değerini gir.");
            return;
        }

        discordBot = new DiscordBot(this);
        final String finalToken     = token;
        final String finalChannelId = channelId;
        final String finalGuildId   = guildId;

        Bukkit.getScheduler().runTaskAsynchronously(this,
                () -> discordBot.startBot(finalToken, finalChannelId, finalGuildId));
    }

    // ─── Haftalık Sıfırlama ──────────────────────────────────────────────────

    /**
     * Sunucu yeniden başlatmalarında sıfırlama kaçmasın diye her dakika
     * kontrol eden bir görev başlatır. Spigot tick'lerinden bağımsız
     * çalıştığından System.currentTimeMillis() kullanılır.
     */
    private void setupWeeklyResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now            = System.currentTimeMillis();
                long lastReset      = dataManager.getLastResetTimestamp();
                long nextResetEpoch = getNextResetEpoch();

                if (now >= nextResetEpoch && lastReset < nextResetEpoch) {
                    getLogger().info("[Troteslimat] Haftalık sıfırlama başlatılıyor...");
                    deliveryManager.giveWeeklyRewards();
                    deliveryManager.resetWeek();
                    getLogger().info("[Troteslimat] Haftalık sıfırlama tamamlandı.");
                }
            }
        }.runTaskTimer(this, 100L, 1200L);   // 5 saniyede bir başla, her dakika kontrol et
    }

    /**
     * Config'deki gün/saat/dakikaya göre bir sonraki sıfırlama epoch'unu hesaplar.
     * java.time kullanılır; Calendar (eski API) kullanılmaz.
     *
     * @return Bir sonraki sıfırlama anı (epoch ms)
     */
    private long getNextResetEpoch() {
        String resetDayStr = getConfig().getString("reset.day", "SUNDAY").toUpperCase();
        int hour   = getConfig().getInt("reset.hour",   23);
        int minute = getConfig().getInt("reset.minute", 59);

        DayOfWeek targetDay;
        try {
            targetDay = DayOfWeek.valueOf(resetDayStr);
        } catch (IllegalArgumentException ex) {
            getLogger().warning("[Troteslimat] Geçersiz reset.day: '" + resetDayStr
                    + "'. SUNDAY kullanılıyor.");
            targetDay = DayOfWeek.SUNDAY;
        }

        ZonedDateTime now    = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime target = now
                .with(TemporalAdjusters.nextOrSame(targetDay))
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);

        long lastReset = dataManager.getLastResetTimestamp();
        if (target.toInstant().toEpochMilli() <= lastReset) {
            target = target.plusWeeks(1);
        }

        return target.toInstant().toEpochMilli();
    }

    // ─── Getter'lar ──────────────────────────────────────────────────────────

    public DataManager     getDataManager()     { return dataManager;     }
    public DeliveryManager getDeliveryManager() { return deliveryManager; }
    public DiscordBot      getDiscordBot()       { return discordBot;      }
    public DeliveryGUI     getDeliveryGUI()      { return deliveryGUI;     }
    public ItemSelectorGUI getItemSelectorGUI()  { return itemSelectorGUI; }
    public TopDeliveryGUI  getTopDeliveryGUI()   { return topDeliveryGUI;  }

    // ─── Config Doğrulama ────────────────────────────────────────────────────

    private void validateConfigVersion() {
        int expectedVersion = 2;
        int currentVersion  = getConfig().getInt("config-version", 1);
        if (currentVersion < expectedVersion) {
            getLogger().warning("[Troteslimat] Config versiyonu eski ("
                    + currentVersion + " < " + expectedVersion + ")."
                    + " Yeni özellikleri kullanmak için config.yml'i güncelle.");
        }
    }
}
