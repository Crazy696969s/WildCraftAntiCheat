package ca.wildcraft.anticheat.gui;

import ca.wildcraft.anticheat.WildCraftAntiCheat;
import ca.wildcraft.anticheat.model.CaseStatus;
import ca.wildcraft.anticheat.model.PlayerRisk;
import ca.wildcraft.anticheat.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Dashboard implements Listener {
    private final WildCraftAntiCheat plugin;
    private final NamespacedKey targetKey;
    private final Set<UUID> watched = new HashSet<>();

    public Dashboard(WildCraftAntiCheat plugin) {
        this.plugin = plugin;
        this.targetKey = new NamespacedKey(plugin, "target_uuid");
    }

    public boolean isWatched(UUID uuid) { return watched.contains(uuid); }

    public void open(Player viewer) {
        MainHolder holder = new MainHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, Items.text("<dark_gray>WildCraft AntiCheat"));
        holder.inventory = inv;
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, Items.of(Material.NETHER_STAR, "<red><bold>WILDCRAFT ANTICHEAT", "<gray>Click a player to investigate.", "", "<red>High <gray>• <gold>Medium <gray>• <green>Low"));
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
        List<PlayerRisk> risks = plugin.repository().sorted();
        for (int i = 0; i < Math.min(slots.length, risks.size()); i++) inv.setItem(slots[i], riskItem(risks.get(i)));
        inv.setItem(45, Items.of(Material.ARROW, "<yellow><bold>REFRESH"));
        inv.setItem(49, Items.of(Material.BOOK, "<aqua><bold>STATUS", "<gray>Tracked players: <white>" + risks.size(), "<gray>Discord: " + (plugin.discord().configured() ? "<green>READY" : "<red>MISSING")));
        inv.setItem(53, Items.of(Material.BARRIER, "<red><bold>CLOSE"));
        viewer.openInventory(inv);
    }

    private void openDetail(Player viewer, PlayerRisk risk) {
        DetailHolder holder = new DetailHolder(risk.uuid());
        Inventory inv = Bukkit.createInventory(holder, 45, Items.text("<dark_gray>Review • <yellow>" + risk.name()));
        holder.inventory = inv;
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(4, riskItem(risk));
        inv.setItem(10, Items.of(Material.ENDER_PEARL, "<aqua><bold>TELEPORT", "<gray>Teleport to the player."));
        inv.setItem(12, Items.of(watched.contains(risk.uuid()) ? Material.LIME_DYE : Material.YELLOW_DYE,
            watched.contains(risk.uuid()) ? "<green><bold>WATCHING" : "<yellow><bold>WATCH PLAYER", "<gray>Toggle live mining notices."));
        inv.setItem(14, Items.of(Material.SPYGLASS, "<light_purple><bold>SPECTATE", "<gray>Enter spectator mode and teleport."));
        inv.setItem(16, Items.of(Material.RED_DYE, "<red><bold>RESET DATA", "<gray>Clear score, veins and evidence."));
        inv.setItem(28, Items.of(Material.RED_WOOL, "<red><bold>CASE: OPEN"));
        inv.setItem(29, Items.of(Material.YELLOW_WOOL, "<yellow><bold>CASE: WATCHING"));
        inv.setItem(30, Items.of(Material.LIME_WOOL, "<green><bold>CASE: CLEARED"));
        inv.setItem(31, Items.of(Material.NETHERITE_SWORD, "<dark_red><bold>CASE: CONFIRMED"));
        inv.setItem(33, Items.of(Material.GOLDEN_SHOVEL, "<gold><bold>COREPROTECT", "<gray>Run configured lookup command."));
        inv.setItem(34, Items.of(Material.PAPER, "<red><bold>LITEBANS HISTORY", "<gray>Open punishment history."));
        inv.setItem(36, Items.of(Material.ARROW, "<yellow><bold>BACK"));
        inv.setItem(40, Items.of(Material.COMPASS, "<aqua><bold>RECENT EVIDENCE", evidenceLore(risk)));
        inv.setItem(44, Items.of(Material.BARRIER, "<red><bold>CLOSE"));
        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(holder instanceof MainHolder) && !(holder instanceof DetailHolder)) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        if (holder instanceof MainHolder) {
            if (event.getRawSlot() == 45) open(player);
            else if (event.getRawSlot() == 53) player.closeInventory();
            else {
                ItemMeta meta = clicked.getItemMeta();
                String target = meta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
                if (target != null) {
                    PlayerRisk risk = plugin.repository().get(UUID.fromString(target));
                    if (risk != null) openDetail(player, risk);
                }
            }
            return;
        }

        DetailHolder detail = (DetailHolder) holder;
        PlayerRisk risk = plugin.repository().get(detail.target);
        if (risk == null) { open(player); return; }
        switch (event.getRawSlot()) {
            case 10 -> teleport(player, risk, false);
            case 12 -> { if (!watched.remove(risk.uuid())) watched.add(risk.uuid()); openDetail(player, risk); }
            case 14 -> teleport(player, risk, true);
            case 16 -> { risk.reset(); plugin.repository().save(); player.sendMessage(Items.text("<green>Data reset for <white>" + risk.name())); open(player); }
            case 28 -> setStatus(player, risk, CaseStatus.OPEN);
            case 29 -> { watched.add(risk.uuid()); setStatus(player, risk, CaseStatus.WATCHING); }
            case 30 -> setStatus(player, risk, CaseStatus.CLEARED);
            case 31 -> setStatus(player, risk, CaseStatus.CONFIRMED);
            case 33 -> integration(player, "CoreProtect", "integrations.coreprotect.command", "co lookup user:{player} time:30d action:-block", risk.name());
            case 34 -> integration(player, "LiteBans", "integrations.litebans.command", "history {player}", risk.name());
            case 36 -> open(player);
            case 44 -> player.closeInventory();
            default -> { }
        }
    }

    private void teleport(Player staff, PlayerRisk risk, boolean spectate) {
        Player target = Bukkit.getPlayer(risk.uuid());
        if (target == null) { staff.sendMessage(Items.text("<red>That player is offline.")); return; }
        if (spectate) staff.setGameMode(GameMode.SPECTATOR);
        staff.teleportAsync(target.getLocation());
        staff.closeInventory();
    }

    private void setStatus(Player staff, PlayerRisk risk, CaseStatus status) {
        risk.status(status);
        risk.addTimeline(staff.getName() + " changed case to " + status.display());
        plugin.repository().save();
        openDetail(staff, risk);
    }

    private void integration(Player staff, String pluginName, String path, String fallback, String target) {
        if (!Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            staff.sendMessage(Items.text("<red>" + pluginName + " is not installed or enabled."));
            return;
        }
        String command = plugin.getConfig().getString(path, fallback).replace("{player}", target);
        staff.closeInventory();
        staff.performCommand(command);
    }

    private ItemStack riskItem(PlayerRisk risk) {
        Material material = risk.score() >= 40 ? Material.RED_WOOL : risk.score() >= 20 ? Material.YELLOW_WOOL : Material.LIME_WOOL;
        ItemStack stack = Items.of(material, "<yellow><bold>" + risk.name(), "<gray>Risk: " + riskColor(risk) + riskLevel(risk),
            "<gray>Score: <white>" + String.format("%.2f", risk.score()), "<gray>Veins: <white>" + risk.veins(), "<gray>Case: <white>" + risk.status().display(), "", "<yellow>Click to review.");
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, risk.uuid().toString());
        stack.setItemMeta(meta);
        return stack;
    }

    private String[] evidenceLore(PlayerRisk risk) {
        if (risk.locations().isEmpty()) return new String[]{"<gray>No recent evidence."};
        return risk.locations().stream().limit(8).map(v -> "<dark_gray>• <white>" + v).toArray(String[]::new);
    }

    private String riskLevel(PlayerRisk risk) { return risk.score() >= 40 ? "HIGH" : risk.score() >= 20 ? "MEDIUM" : "LOW"; }
    private String riskColor(PlayerRisk risk) { return risk.score() >= 40 ? "<red>" : risk.score() >= 20 ? "<gold>" : "<green>"; }
    private void fill(Inventory inv, Material material) { ItemStack pane = Items.of(material, " "); for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane); }

    private static final class MainHolder implements InventoryHolder {
        private Inventory inventory;
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }
    private static final class DetailHolder implements InventoryHolder {
        private final UUID target;
        private Inventory inventory;
        private DetailHolder(UUID target) { this.target = target; }
        @Override public @NotNull Inventory getInventory() { return inventory; }
    }
}
