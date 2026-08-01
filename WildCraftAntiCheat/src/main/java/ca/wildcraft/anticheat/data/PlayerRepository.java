package ca.wildcraft.anticheat.data;

import ca.wildcraft.anticheat.WildCraftAntiCheat;
import ca.wildcraft.anticheat.model.CaseStatus;
import ca.wildcraft.anticheat.model.PlayerRisk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerRepository {
    private final WildCraftAntiCheat plugin;
    private final File file;
    private final Map<UUID, PlayerRisk> risks = new LinkedHashMap<>();

    public PlayerRepository(WildCraftAntiCheat plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public PlayerRisk getOrCreate(UUID uuid, String name) {
        PlayerRisk risk = risks.computeIfAbsent(uuid, id -> new PlayerRisk(id, name));
        risk.name(name);
        return risk;
    }

    public PlayerRisk get(UUID uuid) { return risks.get(uuid); }

    public PlayerRisk findByName(String name) {
        return risks.values().stream().filter(r -> r.name().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public List<PlayerRisk> sorted() {
        return risks.values().stream().filter(r -> r.score() > 0 || r.status() != CaseStatus.CLEARED)
            .sorted(Comparator.comparingDouble(PlayerRisk::score).reversed()).toList();
    }

    public void load() {
        if (!file.isFile()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("players");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String base = "players." + key + ".";
                PlayerRisk risk = new PlayerRisk(uuid, yml.getString(base + "name", key));
                risk.score(yml.getDouble(base + "score"));
                risk.veins(yml.getInt(base + "veins"));
                risk.lastAlert(yml.getLong(base + "last-alert"));
                risk.lastUpdated(yml.getLong(base + "last-updated", System.currentTimeMillis()));
                try { risk.status(CaseStatus.valueOf(yml.getString(base + "status", "OPEN"))); }
                catch (IllegalArgumentException ignored) { risk.status(CaseStatus.OPEN); }
                ConfigurationSection ores = yml.getConfigurationSection(base + "ores");
                if (ores != null) for (String ore : ores.getKeys(false)) risk.ores().put(ore, ores.getInt(ore));
                risk.locations().addAll(yml.getStringList(base + "locations"));
                risk.timeline().addAll(yml.getStringList(base + "timeline"));
                risks.put(uuid, risk);
            } catch (Exception ex) {
                plugin.getLogger().warning("Skipping invalid player record " + key + ": " + ex.getMessage());
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (PlayerRisk risk : risks.values()) {
            String base = "players." + risk.uuid() + ".";
            yml.set(base + "name", risk.name());
            yml.set(base + "score", risk.score());
            yml.set(base + "veins", risk.veins());
            yml.set(base + "last-alert", risk.lastAlert());
            yml.set(base + "last-updated", risk.lastUpdated());
            yml.set(base + "status", risk.status().name());
            yml.set(base + "ores", risk.ores());
            yml.set(base + "locations", new ArrayList<>(risk.locations()));
            yml.set(base + "timeline", new ArrayList<>(risk.timeline()));
        }
        try { yml.save(file); }
        catch (IOException ex) { plugin.getLogger().severe("Could not save players.yml: " + ex.getMessage()); }
    }
}
