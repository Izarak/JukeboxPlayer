package me.isaac.jukeboxPlayer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Jukebox;

public class JukeboxLocation {

    final Location location;

    public JukeboxLocation(Location location) {
        this.location = location;
    }

    public boolean playDisc(Material disc) {
        if (!(location.getBlock().getState() instanceof Jukebox box))
            return false;
        box.setPlaying(disc);
        box.update();
        return true;
    }

    public Location getLocation() {
        return location;
    }
}
