package me.metumorits.bswt;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

import static me.metumorits.bswt.CommandHandler.coords;
import static me.metumorits.bswt.CommandHandler.randomMob;

public class EventListener implements Listener {
    public static BSWT plugin;
    public EventListener(BSWT plugin) {
        EventListener.plugin = plugin;
    }


    @EventHandler
    public void onMMDeath(MythicMobDeathEvent e){
        if(BSWT.activeMob == null) return;
        if(e.getMob().getName().equals(BSWT.activeMob.getName()) && e.getMob().getMobType().equals(BSWT.activeMob.getMobType())){
            Random rand = new Random();
            coords[0] = rand.nextLong(1300);
            coords[2] = rand.nextLong(1300);
            randomMob = new BukkitAPIHelper().getMythicMob(plugin.getConfig().getStringList("mobs").get((int) (Math.random() * plugin.getConfig().getStringList("mobs").size())));
            BSWT.activeMob = null;
            BSWT.isSpawned = false;
            plugin.getConfig().set("isSpawned", false);
        }
    }
}
