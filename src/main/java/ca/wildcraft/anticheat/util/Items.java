package ca.wildcraft.anticheat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class Items {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private Items() {}

    public static ItemStack of(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(MM.deserialize(name));
        meta.lore(Arrays.stream(lore).map(MM::deserialize).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    public static Component text(String input) { return MM.deserialize(input); }
}
