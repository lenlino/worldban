package com.lenlino.worldban;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.Buffer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Worldban extends JavaPlugin implements Listener {

    World bansworld;
    Map<UUID,LocalDateTime> banplayers  = new HashMap<>();

    @Override
    public void onEnable() {
        FileConfiguration config = getConfig();
        String banworldname = config.getString("bannedplayerworldname");
        if (banworldname==null) {
            bansworld = new WorldCreator("banworld").createWorld();
        } else {
            bansworld = new WorldCreator(banworldname).createWorld();
        }
        if (config.getConfigurationSection("playerdata")!=null) {
            config.getConfigurationSection("playerdata").getKeys(false).forEach(key -> {
                banplayers.put(UUID.fromString(key), LocalDateTime.parse(config.getString("playerdata."+key+".period","2015-11-01"),DateTimeFormatter.ISO_DATE_TIME));
            });
        }
        Bukkit.getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        FileConfiguration config = getConfig();
        banplayers.forEach((uuid, localDateTime) -> {
            config.set("playerdata."+uuid+".period", localDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        });
        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ban-world")) {
            if (args.length!=2) {
                return false;
            }
            OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
            if (!player.hasPlayedBefore()) {
                return false;
            }
            banplayers.put(player.getUniqueId(),LocalDateTime.now().plusDays(Long.parseLong(args[1])));
            return true;
        }
        return false;
    }

    @EventHandler
    public void onJoinEvent(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        if (isbanned(player)) {
            return;
        }
        if (player.getWorld()==bansworld) {
            return;
        }
        player.teleport(bansworld.getSpawnLocation());
    }

    public boolean isbanned(Player player) {
        if (banplayers.containsKey(player.getUniqueId())) {
            return banplayers.get(player.getUniqueId()).isBefore(LocalDateTime.now());
        }
        return true;
    }
}
