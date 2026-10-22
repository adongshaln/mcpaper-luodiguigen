package com.pjminecraft.luodiguigen;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;

final class Settings {
    private final boolean enabled;
    private final int minimumLeaves;
    private final boolean effects;
    private final Set<String> worlds;

    private Settings(boolean enabled, int minimumLeaves, boolean effects, Set<String> worlds) {
        this.enabled = enabled;
        this.minimumLeaves = minimumLeaves;
        this.effects = effects;
        this.worlds = worlds;
    }

    boolean enabled() {
        return enabled;
    }

    int minimumLeaves() {
        return minimumLeaves;
    }

    boolean effects() {
        return effects;
    }

    boolean allows(World world) {
        return worlds.isEmpty() || worlds.contains(world.getName());
    }

    static Settings load(FileConfiguration config) {
        int minimum = config.getInt("minimum-leaves", 6);
        if (minimum < 1) {
            minimum = 1;
        }
        return new Settings(
                config.getBoolean("enabled", true),
                minimum,
                config.getBoolean("effects", true),
                Set.copyOf(new HashSet<>(config.getStringList("worlds"))));
    }
}
