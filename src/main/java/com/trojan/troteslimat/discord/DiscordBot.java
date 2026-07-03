package com.trojan.troteslimat.discord;

import com.trojan.troteslimat.Troteslimat;
import com.trojan.troteslimat.manager.DeliveryManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * JDA tabanlı Discord bot entegrasyonu.
 *
 * <p>Özellikler:
 * <ul>
 *   <li>/teslimat slash komutu — anlık sıralama embed'i</li>
 *   <li>Haftalık sonuç embed'i (@everyone opsiyonel)</li>
 * </ul>
 *
 * <p>⚠️ Token config.yml'de düz metin olarak saklanır. Üretim ortamında
 * güvenli bir secrets yöneticisi kullanmayı değerlendirin.
 *
 * @author TroJan_real
 */
public class DiscordBot extends ListenerAdapter {

    private static final String[] MEDALS = {"🥇", "🥈", "🥉", "▫️", "▫️"};

    private final Troteslimat plugin;
    private JDA    jda;
    private String channelId;

    public DiscordBot(Troteslimat plugin) {
        this.plugin = plugin;
    }

    // ─── Başlatma / Kapatma ──────────────────────────────────────────────────

    /**
     * Discord botunu başlatır. Async thread içinde çağrılmalıdır.
     *
     * @param token     Bot token'ı
     * @param channelId Embed'lerin gönderileceği kanal ID'si
     * @param guildId   Slash komutunun kaydedileceği sunucu ID'si
     */
    public void startBot(String token, String channelId, String guildId) {
        this.channelId = channelId;

        try {
            jda = JDABuilder.createDefault(token,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.MESSAGE_CONTENT)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Troteslimat | MC"))
                    .addEventListeners(this)
                    .build()
                    .awaitReady();

            registerSlashCommand(guildId);
            plugin.getLogger().info("[DiscordBot] Bağlantı kuruldu: "
                    + jda.getSelfUser().getAsTag());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            plugin.getLogger().log(Level.SEVERE,
                    "[DiscordBot] Bağlantı kesildi (interrupted)!", ex);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE,
                    "[DiscordBot] Bot başlatılamadı: " + ex.getMessage(), ex);
        }
    }

    private void registerSlashCommand(String guildId) {
        var guild = jda.getGuildById(guildId);
        if (guild == null) {
            plugin.getLogger().warning(
                    "[DiscordBot] Guild bulunamadı! guild-id: " + guildId);
            return;
        }
        guild.updateCommands()
             .addCommands(Commands.slash("teslimat",
                     "Bu haftanın teslimat sıralamasını gösterir"))
             .queue(
                ok  -> plugin.getLogger().info(
                        "[DiscordBot] /teslimat slash komutu kaydedildi."),
                err -> plugin.getLogger().warning(
                        "[DiscordBot] Slash komutu kaydedilemedi: " + err.getMessage())
             );
    }

    public void shutdown() {
        if (jda == null) return;
        try {
            jda.getPresence().setStatus(OnlineStatus.OFFLINE);
            jda.shutdown();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING,
                    "[DiscordBot] Kapatma sırasında hata oluştu.", ex);
        }
    }

    // ─── Slash Komut İşleyici ────────────────────────────────────────────────

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("teslimat")) return;

        event.deferReply().queue();

        DeliveryManager manager = plugin.getDeliveryManager();
        List<Map.Entry<UUID, Integer>> top = manager.getTop5();
        FileConfiguration cfg = plugin.getConfig();

        if (top.isEmpty()) {
            String bosMesaj = cfg.getString(
                    "discord.embed-anlik.bos-mesaj",
                    "📦 Bu hafta henüz teslimat yapılmamış!");
            event.getHook().sendMessage(bosMesaj).queue();
            return;
        }

        String satirSablon = cfg.getString(
                "discord.embed-anlik.satir-sablon",
                "%madalya% **%sira%.** **%oyuncu%** — %sayi% teslimat");

        StringBuilder sb = new StringBuilder();
        int toplamTeslimat = 0;

        for (int i = 0; i < top.size(); i++) {
            OfflinePlayer op  = Bukkit.getOfflinePlayer(top.get(i).getKey());
            String name       = op.getName() != null ? op.getName() : "Bilinmeyen";
            int    count      = top.get(i).getValue();
            toplamTeslimat   += count;
            String medal      = i < MEDALS.length ? MEDALS[i] : "▫️";

            sb.append(satirSablon
                    .replace("%madalya%", medal)
                    .replace("%sira%",    String.valueOf(i + 1))
                    .replace("%oyuncu%",  name)
                    .replace("%sayi%",    String.valueOf(count)))
              .append("\n");
        }

        int toplamOyuncu = manager.getTotalParticipants();

        String baslik      = cfg.getString("discord.embed-anlik.baslik",
                "📦 Bu Haftanın Teslimat Sıralaması");
        int r = cfg.getInt("discord.embed-anlik.renk-r", 114);
        int g = cfg.getInt("discord.embed-anlik.renk-g", 137);
        int b = cfg.getInt("discord.embed-anlik.renk-b", 218);

        String alanToplam = cfg.getString("discord.embed-anlik.alan-toplam-teslimat",
                "Toplam Teslimat");
        String alanYapan  = cfg.getString("discord.embed-anlik.alan-teslimat-yapan",
                "Teslimat Yapan");
        String alanYapanDeger = cfg.getString(
                "discord.embed-anlik.alan-teslimat-yapan-deger", "%sayi% oyuncu")
                .replace("%sayi%", String.valueOf(toplamOyuncu));
        String footer = cfg.getString("discord.embed-anlik.footer",
                "Troteslimat • Anlık Sıralama");

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(baslik)
                .setDescription(sb.toString())
                .setColor(new Color(clamp(r), clamp(g), clamp(b)))
                .addField(alanToplam, String.valueOf(toplamTeslimat), true)
                .addField(alanYapan, alanYapanDeger, true)
                .setTimestamp(Instant.now())
                .setFooter(footer);

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    // ─── Haftalık Sonuç Embed'i ──────────────────────────────────────────────

    /**
     * Haftalık yarışma sonuçlarını Discord kanalına gönderir.
     * Ana thread dışından çağrılabilir.
     *
     * @param top5 Sıralanmış ilk 5 oyuncu listesi
     */
    public void sendWeeklyTopEmbed(List<Map.Entry<UUID, Integer>> top5) {
        if (jda == null || channelId == null || top5.isEmpty()) return;

        FileConfiguration cfg = plugin.getConfig();

        Thread.ofVirtual().name("Troteslimat-DiscordSend").start(() -> {
            try {
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel == null) {
                    plugin.getLogger().warning(
                            "[DiscordBot] Kanal bulunamadı: " + channelId);
                    return;
                }

                String satirSablon = cfg.getString(
                        "discord.embed-haftalik.satir-sablon",
                        "%madalya% **%sira%.** **%oyuncu%** — %sayi% teslimat");

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < top5.size(); i++) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(top5.get(i).getKey());
                    String name  = op.getName() != null ? op.getName() : "Bilinmeyen";
                    String medal = i < MEDALS.length ? MEDALS[i] : "▫️";

                    sb.append(satirSablon
                            .replace("%madalya%", medal)
                            .replace("%sira%",    String.valueOf(i + 1))
                            .replace("%oyuncu%",  name)
                            .replace("%sayi%",    String.valueOf(top5.get(i).getValue())))
                      .append("\n");
                }

                String baslik = cfg.getString("discord.embed-haftalik.baslik",
                        "🏆 Haftalık Teslimat Yarışması Sonuçları");
                int r = cfg.getInt("discord.embed-haftalik.renk-r", 255);
                int g = cfg.getInt("discord.embed-haftalik.renk-g", 140);
                int b = cfg.getInt("discord.embed-haftalik.renk-b", 0);

                String alanKatilimci = cfg.getString(
                        "discord.embed-haftalik.alan-katilimci", "Katılımcı");
                String alanKatilimciDeger = cfg.getString(
                        "discord.embed-haftalik.alan-katilimci-deger", "%sayi% oyuncu")
                        .replace("%sayi%",
                                String.valueOf(plugin.getDeliveryManager().getTotalParticipants()));
                String footerStr = cfg.getString("discord.embed-haftalik.footer",
                        "Troteslimat • %tarih%")
                        .replace("%tarih%", LocalDate.now().toString());

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle(baslik)
                        .setDescription(sb.toString())
                        .setColor(new Color(clamp(r), clamp(g), clamp(b)))
                        .addField(alanKatilimci, alanKatilimciDeger, true)
                        .setTimestamp(Instant.now())
                        .setFooter(footerStr);

                boolean mentionEveryone = cfg.getBoolean("discord.mention-everyone", true);
                MessageCreateBuilder msg = new MessageCreateBuilder()
                        .addEmbeds(embed.build());
                if (mentionEveryone) msg.setContent("@everyone");

                channel.sendMessage(msg.build()).queue();

            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE,
                        "[DiscordBot] Haftalık embed gönderilemedi!", ex);
            }
        });
    }

    // ─── Yardımcı ────────────────────────────────────────────────────────────

    /** RGB değerini 0–255 aralığına sıkıştırır. */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
