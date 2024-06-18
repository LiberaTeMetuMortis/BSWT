package me.metumorits.bswt;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Date;
import java.util.Random;

import static me.metumorits.bswt.CommandHandler.*;

public final class BSWT extends JavaPlugin {

    public static boolean isSpawned = false;
    public static ActiveMob activeMob;
    public static String translateColors(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getCommand("bswt").setExecutor(new CommandHandler(this));
        getServer().getPluginManager().registerEvents(new EventListener(this), this);

        Long lastSpawn = getConfig().getLong("lastSpawn", -1);
        final Long[] lastSpawnFromConfig = {this.getConfig().getLong("lastSpawn", -1) == -1 ? System.currentTimeMillis()+1000 : this.getConfig().getLong("lastSpawn")};
        if(lastSpawn != -1){
            loopRunning = true;
            loopLastStarted = System.currentTimeMillis();
            JavaPlugin plugin = this;
            mainTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if(!isSpawned){
                        spawnerTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(!CommandHandler.skipped){
                                    Bukkit.broadcastMessage(translateColors(plugin.getConfig().getString("messages.spawned").replace("%selected_boss%", randomMob.getDisplayName() != null ? randomMob.getDisplayName().get().replaceAll("(§[0-9a-f])", "$1§l") : randomMob.getInternalName())));
                                    String world = plugin.getConfig().getString("world");
                                    MythicMob mob = randomMob;
                                    Location loc = new Location(Bukkit.getWorld(world), coords[0], coords[1], coords[2]);
                                    loc.getChunk().load(true);
                                    ActiveMob activeMob = mob.spawn(new AbstractLocation(BukkitAdapter.adapt(Bukkit.getWorld(world)), coords[0], coords[1], coords[2]), 1);
                                    BSWT.activeMob = activeMob;
                                    isSpawned = true;

                                    plugin.getConfig().set("isSpawned", isSpawned);
                                    plugin.getConfig().set("lastSpawn", System.currentTimeMillis());
                                    lastSpawnFromConfig[0] = System.currentTimeMillis()+1000;

                                }
                                else CommandHandler.skipped = false;
                            }
                        };
                        broadcastTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                CommandHandler.broadcastMethod(lastSpawnFromConfig[0], plugin, randomMob, coords);
                            }
                        };
                        Random rand = new Random();
                        coords[0] = rand.nextLong(1300);
                        coords[2] = rand.nextLong(1300);
                        randomMob = new BukkitAPIHelper().getMythicMob(plugin.getConfig().getStringList("mobs").get((int) (Math.random() * plugin.getConfig().getStringList("mobs").size())));
                        broadcastTask.runTask(plugin);
                        broadcastTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                CommandHandler.broadcastMethod(lastSpawnFromConfig[0], plugin, randomMob, coords);
                            }
                        };
                        broadcastTask.runTaskLater(plugin, 5*60*20);
                        broadcastTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                CommandHandler.broadcastMethod(lastSpawnFromConfig[0], plugin, randomMob, coords);
                            }
                        };
                        broadcastTask.runTaskLater(plugin, 9*60*20);
                        broadcastTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                CommandHandler.broadcastMethod(lastSpawnFromConfig[0], plugin, randomMob, coords);
                            }
                        };
                        spawnerTask.runTaskLater(plugin, 10*60*20);

                    }
                }
            };

            mainTask.runTaskTimer(this,(this.getConfig().getLong("lastSpawn")+this.getConfig().getLong("period")*20*60-60*10*20)-System.currentTimeMillis(), plugin.getConfig().getLong("period")*20*60);

        }
    }

    @Override
    public void onDisable() {
        long latestLastSpawn = getConfig().getLong("lastSpawn", -1);
        boolean latestIsSpawned = getConfig().getBoolean("isSpawned", false);
        saveDefaultConfig();
        reloadConfig();
        getConfig().set("lastSpawn", latestLastSpawn);
        getConfig().set("isSpawned", latestIsSpawned);
        saveConfig();
    }

    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
