package ca.wildcraft.anticheat.command;

import ca.wildcraft.anticheat.WildCraftAntiCheat;
import ca.wildcraft.anticheat.model.PlayerRisk;
import ca.wildcraft.anticheat.util.Items;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AntiCheatCommand implements CommandExecutor {
    private final WildCraftAntiCheat plugin;
    public AntiCheatCommand(WildCraftAntiCheat plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("wildcraftanticheat.admin")) { sender.sendMessage(Items.text("<red>No permission.")); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin(); sender.sendMessage(Items.text("<green>WildCraftAntiCheat reloaded.")); return true;
        }
        if (!(sender instanceof Player player)) { sender.sendMessage("Use this command in game."); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
            PlayerRisk risk = plugin.repository().getOrCreate(player.getUniqueId(), player.getName());
            risk.score(50); risk.veins(5);
            plugin.alert(player, risk, player.getLocation(), "Manual staff test");
            player.sendMessage(Items.text("<green>Test alert queued.")); return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("reset")) {
            PlayerRisk risk = plugin.repository().findByName(args[1]);
            if (risk == null) player.sendMessage(Items.text("<red>No tracked player found."));
            else { risk.reset(); plugin.repository().save(); player.sendMessage(Items.text("<green>Reset <white>" + risk.name())); }
            return true;
        }
        plugin.dashboard().open(player);
        return true;
    }
}
