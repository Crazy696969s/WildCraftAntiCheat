package ca.wildcraft.anticheat.discord;

import ca.wildcraft.anticheat.WildCraftAntiCheat;
import ca.wildcraft.anticheat.model.PlayerRisk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class DiscordWebhook {
    private final WildCraftAntiCheat plugin;

    public DiscordWebhook(WildCraftAntiCheat plugin) { this.plugin = plugin; }

    public boolean configured() {
        String url = plugin.getConfig().getString("discord.webhook-url", "");
        return url.startsWith("https://") && !url.contains("PASTE_");
    }

    public void send(Player player, PlayerRisk risk, Location location, String reason) {
        if (!configured()) {
            plugin.getLogger().warning("Discord webhook is not configured.");
            return;
        }
        String role = plugin.getConfig().getString("discord.staff-role-id", "");
        String mention = role == null || role.isBlank() ? "" : "<@&" + role + ">";
        String body = "{" +
            "\"content\":\"" + escape(mention) + "\"," +
            "\"allowed_mentions\":{\"parse\":[\"roles\"]}," +
            "\"embeds\":[{" +
            "\"title\":\"⚠ Possible X-Ray Activity\"," +
            "\"description\":\"Alert only — review before punishing.\"," +
            "\"color\":15158332," +
            "\"fields\":[" +
            field("Player", player.getName(), true) + "," +
            field("Risk", risk.score() >= 40 ? "HIGH" : risk.score() >= 20 ? "MEDIUM" : "LOW", true) + "," +
            field("Score", String.format("%.2f", risk.score()), true) + "," +
            field("Veins", String.valueOf(risk.veins()), true) + "," +
            field("Reason", reason, false) + "," +
            field("Location", location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(), false) +
            "],\"timestamp\":\"" + Instant.now() + "\"}]}";
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> post(body));
    }

    private void post(String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(plugin.getConfig().getString("discord.webhook-url")).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "WildCraftAntiCheat/3.0.0");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
            int response = connection.getResponseCode();
            if (response < 200 || response >= 300) plugin.getLogger().warning("Discord returned HTTP " + response);
        } catch (Exception ex) {
            plugin.getLogger().warning("Discord webhook failed: " + ex.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String field(String name, String value, boolean inline) {
        return "{\"name\":\"" + escape(name) + "\",\"value\":\"" + escape(value) + "\",\"inline\":" + inline + "}";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
