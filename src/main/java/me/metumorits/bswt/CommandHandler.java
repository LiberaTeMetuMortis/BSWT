package me.metumorits.bswt;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.adapters.AbstractWorld;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static me.metumorits.bswt.BSWT.*;


public class CommandHandler implements CommandExecutor {
    public static boolean loopRunning = false;
    public static boolean stopped = false;
    public static Long loopLastStarted;
    public static Long[] coords = {null, 90L, null};
    public static MythicMob randomMob = null;
    public static boolean skipped = false;
    public static BukkitRunnable mainTask;
    public static BukkitRunnable broadcastTask;
    public static BukkitRunnable spawnerTask;
    public static Long lastSpawn = -1L;
    private BSWT plugin;
    public CommandHandler(BSWT plugin) {
        this.plugin = plugin;
    }
    public static void broadcastMethod(Long lastSpawn, JavaPlugin plugin, MythicMob randomMob, Long[] coords){
        if(stopped) return;
        Long nextSpawn = lastSpawn + plugin.getConfig().getLong("period")*1000*60*(skipped ? 2 : 1);
        Long currentTimeInMilis = System.currentTimeMillis()+1000;
        Long remainingTimeInMilis = nextSpawn - currentTimeInMilis;
        if(remainingTimeInMilis < 0){
            lastSpawn = System.currentTimeMillis();
            nextSpawn = lastSpawn + plugin.getConfig().getLong("period")*1000*60*(skipped ? 2 : 1);
            remainingTimeInMilis = nextSpawn - currentTimeInMilis;
        }
        Long finalRemainingTimeInMilis = remainingTimeInMilis;
        plugin.getConfig().getStringList("broadcastMessages").forEach(s -> {
            Bukkit.broadcastMessage(translateColors(s
                    .replace("%hour%", String.valueOf(Math.round(Math.ceil(finalRemainingTimeInMilis /1000)/60/60)))
                    .replace("%minute%", String.valueOf(Math.round(Math.round(finalRemainingTimeInMilis /1000)/60%60)))
                    .replace("%second%", String.valueOf(Math.round(Math.round(finalRemainingTimeInMilis /1000)%60)))
                    .replace("%selected_boss%", randomMob.getDisplayName() != null ? randomMob.getDisplayName().get().replaceAll("(§[0-9a-f])", "$1§l") : randomMob.getInternalName())
                    .replace("%x%", coords[0].toString())
                    .replace("%y%", coords[1].toString())
                    .replace("%z%", coords[2].toString())
            ));
        });
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0){
            plugin.getConfig().getStringList("helpMessages").forEach(s -> sender.sendMessage(translateColors(s)));
            return true;
        }
        else if(args[0].equalsIgnoreCase("start")){
            if(!sender.hasPermission("bswt.start")){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.noPermission")));
                return true;
            }
            if(plugin.getConfig().getLong("lastSpawn", -1) != -1){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.alreadyStarted")));
                return true;
            }
            if(loopRunning){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.alreadyStarted")));
                return true;
            }
            loopRunning = true;
            stopped = false;
            loopLastStarted = System.currentTimeMillis();
            lastSpawn = plugin.getConfig().getLong("lastSpawn", -1) == -1 ? System.currentTimeMillis()+1000 : plugin.getConfig().getLong("lastSpawn");
            Random rand = new Random();
            coords[0] = coords[0] == null ? rand.nextLong(1300) : coords[0];
            coords[2] = coords[2] == null ? rand.nextLong(1300) : coords[2];
            randomMob = randomMob == null ? new BukkitAPIHelper().getMythicMob(plugin.getConfig().getStringList("mobs").get((int) (Math.random() * plugin.getConfig().getStringList("mobs").size()))) : randomMob;
            mainTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if(!isSpawned){
                        spawnerTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(stopped) return;
                                if(!CommandHandler.skipped){
                                    Bukkit.broadcastMessage(translateColors(plugin.getConfig().getString("messages.spawned").replace("%selected_boss%", CommandHandler.randomMob.getDisplayName() != null ? CommandHandler.randomMob.getDisplayName().get() : CommandHandler.randomMob.getInternalName())));
                                    String world = plugin.getConfig().getString("world");
                                    MythicMob mob = CommandHandler.randomMob;
                                    Location loc = new Location(Bukkit.getWorld(world), coords[0], coords[1], coords[2]);
                                    loc.getChunk().load(true);
                                    ActiveMob activeMob = mob.spawn(new AbstractLocation(BukkitAdapter.adapt(Bukkit.getWorld(world)), coords[0], coords[1], coords[2]), 1);
                                    BSWT.activeMob = activeMob;
                                    isSpawned = true;


                                    plugin.getConfig().set("isSpawned", isSpawned);
                                    plugin.getConfig().set("lastSpawn", System.currentTimeMillis());
                                    lastSpawn = System.currentTimeMillis()+1000;
                                    randomMob = null;
                                }
                                else CommandHandler.skipped = false;
                            }
                        };
                        broadcastTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                CommandHandler.broadcastMethod(lastSpawn, plugin, randomMob, coords);
                            }
                        };
                        Random rand = new Random();
                        coords[0] = coords[0] == null ? rand.nextLong(1300) : coords[0];
                        coords[2] = coords[2] == null ? rand.nextLong(1300) : coords[2];
                        randomMob = randomMob == null ? new BukkitAPIHelper().getMythicMob(plugin.getConfig().getStringList("mobs").get((int) (Math.random() * plugin.getConfig().getStringList("mobs").size()))) : randomMob;
                        broadcastTask.runTask(plugin);
                        broadcastTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                CommandHandler.broadcastMethod(lastSpawn, plugin, randomMob, coords);
                            }
                        };
                        broadcastTask.runTaskLater(plugin, 5*60*20);
                        broadcastTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                CommandHandler.broadcastMethod(lastSpawn, plugin, randomMob, coords);
                            }
                        };
                        broadcastTask.runTaskLater(plugin, 9*60*20);
                        broadcastTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                CommandHandler.broadcastMethod(lastSpawn, plugin, randomMob, coords);
                            }
                        };
                        spawnerTask.runTaskLater(plugin, 10*60*20);

                    }
                }
            };
            mainTask.runTaskTimer(plugin,plugin.getConfig().getLong("period")*20*60-60*10*20, plugin.getConfig().getLong("period")*20*60);
            sender.sendMessage(translateColors(plugin.getConfig().getString("messages.started")));
        }
        else if(args[0].equalsIgnoreCase("stop")){
            if(!sender.hasPermission("bswt.stop")){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.noPermission")));
                return true;
            }
            if(stopped){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.notStarted")));
                return true;
            }
            loopRunning = false;
            stopped = true;
            try{
                if(spawnerTask != null && !spawnerTask.isCancelled()) spawnerTask.cancel();
                if(broadcastTask != null && !broadcastTask.isCancelled()) broadcastTask.cancel();
                if(mainTask != null && !mainTask.isCancelled()) mainTask.cancel();
            }catch (Exception ignored){}
            randomMob = null;
            coords[0] = null;
            coords[2] = null;
            Bukkit.getScheduler().getPendingTasks().stream().filter(task -> task.getOwner().equals(plugin)).forEach(BukkitTask::cancel);
            plugin.getConfig().set("lastSpawn", -1);
            sender.sendMessage(translateColors(plugin.getConfig().getString("messages.stopped")));
        }
        else if(args[0].equalsIgnoreCase("skip")){
            if(!sender.hasPermission("bswt.skip")){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.noPermission")));
                return true;
            }
            if(plugin.getConfig().getLong("lastSpawn") == -1){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.notStarted")));
                return true;
            }
            if(!sender.hasPermission("bswt.skip")){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.noPermission")));
                return true;
            }
            skipped = true;
            sender.sendMessage(translateColors(plugin.getConfig().getString("messages.skipped")));
        }
        else if(args[0].equalsIgnoreCase("reload")){
            if(!sender.hasPermission("bswt.reload")){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.noPermission")));
                return true;
            }
            long latestLastSpawn = plugin.getConfig().getLong("lastSpawn");
            boolean latestIsSpawned = plugin.getConfig().getBoolean("isSpawned");
            plugin.saveDefaultConfig();
            plugin.reloadConfig();
            plugin.getConfig().set("lastSpawn", latestLastSpawn);
            plugin.getConfig().set("isSpawned", latestIsSpawned);
            plugin.reloadConfig();
            sender.sendMessage(translateColors(plugin.getConfig().getString("messages.reloaded")));
        }
        else if(args[0].equalsIgnoreCase("help")){
            if(!sender.hasPermission("bswt.help")){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.noPermission")));
                return true;
            }
        }
        else if(args[0].equalsIgnoreCase("time")){
            Long nextSpawn = null;
            if(!sender.hasPermission("bswt.time")){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.noPermission")));
                return true;
            }
            if(randomMob == null){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.notStarted")));
                return true;
            }
            if(loopLastStarted != null && lastSpawn == null){
                nextSpawn = loopLastStarted + plugin.getConfig().getLong("period")*20*60 - System.currentTimeMillis();
            }
            nextSpawn = nextSpawn == null ? lastSpawn + plugin.getConfig().getLong("period")*1000*60*(skipped ? 2 : 1) : nextSpawn;
            Long currentTimeInMilis = System.currentTimeMillis()+1000;
            Long remainingTimeInMilis = nextSpawn - currentTimeInMilis;
            if(remainingTimeInMilis < 0){
                lastSpawn = System.currentTimeMillis();
                nextSpawn = lastSpawn + plugin.getConfig().getLong("period")*1000*60*(skipped ? 2 : 1);
                remainingTimeInMilis = nextSpawn - currentTimeInMilis;
            }
            Long finalRemainingTimeInMilis = remainingTimeInMilis;

            plugin.getConfig().getStringList("broadcastMessages").forEach(s -> {
                sender.sendMessage(translateColors(s
                        .replace("%hour%", String.valueOf(Math.round(Math.ceil(finalRemainingTimeInMilis /1000)/60/60)))
                        .replace("%minute%", String.valueOf(Math.round(Math.round(finalRemainingTimeInMilis /1000)/60%60)))
                        .replace("%second%", String.valueOf(Math.round(Math.round(finalRemainingTimeInMilis /1000)%60)))
                        .replace("%selected_boss%", randomMob.getDisplayName() != null ? randomMob.getDisplayName().get().replaceAll("(§[0-9a-f])", "$1§l") : randomMob.getInternalName())

                ));
            });
        }else if(args[0].equalsIgnoreCase("location")){
            if(!sender.hasPermission("bswt.location")){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.noPermission")));
                return true;
            }
            if(coords[0] == null || coords[2] == null){
                sender.sendMessage(translateColors(plugin.getConfig().getString("messages.notStarted")));
                return true;
            }
            sender.sendMessage(new ArrayList(Arrays.stream(coords).collect(Collectors.toList())).toString());
        }





        else{
            plugin.getConfig().getStringList("helpMessages").forEach(s -> sender.sendMessage(translateColors(s)));
            return true;
        }
        return true;
    }

}
