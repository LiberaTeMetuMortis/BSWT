package me.metumorits.bswt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {

    public static void onChunkLoad(ChunkLoadEvent e) {
        Block b = new Location(Bukkit.getWorld("world"), 5, 10, 15).getBlock();

    }
}
