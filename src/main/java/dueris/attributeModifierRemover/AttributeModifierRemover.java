package dueris.attributeModifierRemover;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
 

@SuppressWarnings("deprecation")
public final class AttributeModifierRemover extends JavaPlugin {

    @Override
    public void onEnable() {
        // nothing to do on enable for command registration; onCommand is overridden
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("attributeremove")) return false;

        if (args.length < 3) {
            sender.sendMessage("Usage: /attributeremove <player> <attribute> <id|ALL>");
            return true;
        }

        String targetName = args[0];
        String attributeArg = args[1];
        String idArg = args[2];

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("Player not found or not online: " + targetName);
            return true;
        }

        Attribute attribute = resolveAttribute(attributeArg);
        if (attribute == null) {
            sender.sendMessage("Unknown attribute: " + attributeArg + " (example: minecraft:attack_damage)");
            return true;
        }

        AttributeInstance instance = target.getAttribute(attribute);
        if (instance == null) {
            sender.sendMessage("Player does not have attribute instance: " + attributeArg);
            return true;
        }

        // Handle removing all modifiers except the special base attack damage modifier
        if (idArg.equalsIgnoreCase("all")) {
            Collection<AttributeModifier> mods = new ArrayList<>(instance.getModifiers());
            int removed = 0;
            for (AttributeModifier mod : mods) {
                String name = mod.getName();
                // Keep modifiers named minecraft:base_attack_damage or base_attack_damage
                if (name != null && (name.equalsIgnoreCase("minecraft:base_attack_damage") || name.equalsIgnoreCase("base_attack_damage"))) {
                    continue;
                }
                try {
                    instance.removeModifier(mod);
                    removed++;
                } catch (Exception ignored) {
                }
            }
            sender.sendMessage("Removed " + removed + " modifiers from " + target.getName() + " for attribute " + attributeArg);
            return true;
        }

        // Single modifier removal by namespaced key or raw id. Accept forms like minecraft:uuid or uuid
        NamespacedKey modKey = NamespacedKey.fromString(idArg);
        if (modKey == null && idArg.contains("-")) {
            // maybe a raw UUID was provided without namespace; try minecraft namespace
            modKey = NamespacedKey.minecraft(idArg);
        }

        boolean found = false;
        Collection<AttributeModifier> current = new ArrayList<>(instance.getModifiers());
        for (AttributeModifier mod : current) {
            try {
                NamespacedKey k = mod.getKey();
                if (k != null && modKey != null && k.equals(modKey)) {
                    instance.removeModifier(mod);
                    found = true;
                }
            } catch (Exception ignored) {
            }
        }

        if (found) {
            sender.sendMessage("Removed modifier " + idArg + " from " + target.getName());
        } else {
            sender.sendMessage("Modifier with id " + idArg + " not found on " + target.getName());
        }

        return true;
    }

    private Attribute resolveAttribute(String input) {
        if (input == null) return null;
        // Expect namespaced attribute like minecraft:attack_damage
        NamespacedKey key = NamespacedKey.fromString(input);
        if (key == null) return null;
        try {
            return Bukkit.getRegistry(Attribute.class).get(key);
        } catch (Exception ignored) {
            return null;
        }
    }
}
