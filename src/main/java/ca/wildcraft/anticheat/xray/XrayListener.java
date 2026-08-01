package ca.wildcraft.anticheat.xray;

import ca.wildcraft.anticheat.WildCraftAntiCheat;
import ca.wildcraft.anticheat.model.CaseStatus;
import ca.wildcraft.anticheat.model.PlayerRisk;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.EnumMap;
import java.util.Map;

public final class XrayListener implements Listener {
    private static final BlockFace[] FACES = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final WildCraftAntiCheat plugin;
    private final Map<Material, Double> weights = new EnumMap<>(Material.class);

    public XrayListener(WildCraftAntiCheat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        weights.clear();
        var section = plugin.getConfig().getConfigurationSection("detection.ores");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material != null) weights.put(material, section.getDouble(key));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("wildcraftanticheat.bypass")) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (plugin.getConfig().getStringList("ignored-worlds").stream().anyMatch(w -> w.equalsIgnoreCase(player.getWorld().getName()))) return;
        Block block = event.getBlock();
        Double weight = weights.get(block.getType());
        if (weight == null) return;

        boolean hidden = hidden(block);
        double multiplier = hidden ? 1.0 : plugin.getConfig().getDouble("detection.exposed-ore-multiplier", 0.35);
        double points = weight * multiplier + (hidden ? plugin.getConfig().getDouble("detection.hidden-ore-bonus", 2.0) : 0.0);
        PlayerRisk risk = plugin.repository().getOrCreate(player.getUniqueId(), player.getName());
        risk.decay(plugin.getConfig().getDouble("detection.score-decay-per-hour", 2.0));
        risk.addOre(block.getType().name(), points, hidden, block.getLocation());

        if (plugin.dashboard().isWatched(player.getUniqueId())) {
            plugin.sendStaff("<dark_gray>[<gold>Xray Watch</gold>] <yellow>" + player.getName() + "</yellow> mined <white>" + block.getType().name()
                + "</white> | hidden=" + hidden + " | score=" + String.format("%.2f", risk.score()));
        }

        long now = System.currentTimeMillis();
        double threshold = plugin.getConfig().getDouble("detection.score-threshold", 15.0);
        int veins = plugin.getConfig().getInt("detection.minimum-veins", 3);
        long cooldown = plugin.getConfig().getLong("alerts.cooldown-seconds", 30) * 1000L;
        if (risk.score() >= threshold && risk.veins() >= veins && now - risk.lastAlert() >= cooldown) {
            risk.lastAlert(now);
            if (risk.status() == CaseStatus.CLEARED) risk.status(CaseStatus.OPEN);
            risk.addTimeline("Automatic alert sent");
            plugin.alert(player, risk, block.getLocation(), hidden ? "Multiple hidden valuable ores" : "Suspicious valuable ore rate");
        }
        plugin.repository().save();
    }

    private boolean hidden(Block block) {
        for (BlockFace face : FACES) {
            Material material = block.getRelative(face).getType();
            if (material.isAir() || material == Material.WATER || material == Material.LAVA) return false;
        }
        return true;
    }
}
