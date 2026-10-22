package com.pjminecraft.luodiguigen;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

public final class LuoDiGuiGenPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private Settings settings;
    private FellListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.load(getConfig());
        listener = new FellListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        var command = getCommand("luodiguigen");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
        getServer().getScheduler().runTaskTimer(this, () -> listener.prune(), 600L, 600L);
        getLogger().info("落地归根已启用");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    Settings settings() {
        return settings;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("luodiguigen.reload")) {
            sender.sendMessage("你没有权限这么做");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            settings = Settings.load(getConfig());
            sender.sendMessage("落地归根配置已重载");
            return true;
        }
        sender.sendMessage("用法: /luodiguigen reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("luodiguigen.reload")
                && "reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("reload");
        }
        return List.of();
    }
}
