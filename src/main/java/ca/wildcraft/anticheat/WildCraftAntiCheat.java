package ca.wildcraft.anticheat;

import ca.wildcraft.anticheat.command.AntiCheatCommand;
import ca.wildcraft.anticheat.data.PlayerRepository;
import ca.wildcraft.anticheat.discord.DiscordWebhook;
import ca.wildcraft.anticheat.gui.Dashboard;
import ca.wildcraft.anticheat.model.PlayerRisk;
import ca.wildcraft.anticheat.util.Items;
import ca.wildcraft.anticheat.xray.XrayListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class WildCraftAntiCheat extends JavaPlugin {
    private PlayerRepository repository;
    private DiscordWebhook discord;
    private Dashboard dashboard;
    private XrayListener xray;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        repository = new PlayerRepository(this);
        repository.load();
        discord = new DiscordWebhook(this);
        dashboard = new Dashboard(this);
        xray = new XrayListener(this);
        getServer().getPluginManager().registerEvents(dashboard, this);
        getServer().getPluginManager().registerEvents(xray, this);
        var command = getCommand("wcax");
        if (command == null) throw new IllegalStateException("wcax missing from plugin.yml");
        command.setExecutor(new AntiCheatCommand(this));
        getLogger().info("WildCraftAntiCheat 3.0.0 enabled.");
    }

    @Override
    public void onDisable() {
        if (repository != null) repository.save();
    }

    public void reloadPlugin() {
        reloadConfig();
        xray.reload();
    }

    public void alert(Player player, PlayerRisk risk, Location location, String reason) {
        if (getConfig().getBoolean("alerts.in-game", true)) {
            sendStaff("<dark_gray>[<red>Xray Alert</red>] <yellow>" + player.getName() + "</yellow> score=<white>"
                + String.format("%.2f", risk.score()) + "</white> veins=<white>" + risk.veins() + "</white> | <yellow>/wcax</yellow>");
        }
        if (getConfig().getBoolean("alerts.discord", true)) discord.send(player, risk, location, reason);
    }

    public void sendStaff(String message) {
        for (Player staff : getServer().getOnlinePlayers()) {
            if (staff.hasPermission("wildcraftanticheat.alerts")) staff.sendMessage(Items.text(message));
        }
        getLogger().warning(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(Items.text(message)));
    }

    public PlayerRepository repository() { return repository; }
    public DiscordWebhook discord() { return discord; }
    public Dashboard dashboard() { return dashboard; }
}
