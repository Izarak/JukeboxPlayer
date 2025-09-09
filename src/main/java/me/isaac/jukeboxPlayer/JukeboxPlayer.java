package me.isaac.jukeboxPlayer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class JukeboxPlayer extends JavaPlugin implements Listener {

    Set<JukeboxLocation> jukeboxLocations = new HashSet<>();

    final Material[] discs = new Material[] {
            Material.MUSIC_DISC_RELIC,
            Material.MUSIC_DISC_CREATOR,
            Material.MUSIC_DISC_TEARS,
//            Material.MUSIC_DISC_5,
            Material.MUSIC_DISC_OTHERSIDE,
            Material.MUSIC_DISC_BLOCKS,
            Material.MUSIC_DISC_CHIRP,
            Material.MUSIC_DISC_FAR,
            Material.MUSIC_DISC_MELLOHI,
            Material.MUSIC_DISC_STAL,
            Material.MUSIC_DISC_STRAD,
            Material.MUSIC_DISC_CREATOR_MUSIC_BOX,
            Material.MUSIC_DISC_WAIT
    };

    private File file;
    private YamlConfiguration yaml;

    @Override
    public void onEnable() {
        file = new File(getDataFolder() + File.separator + "locations.yml");

        if (!file.exists()) {
            try {
                getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        yaml = YamlConfiguration.loadConfiguration(file);

        if (yaml.isList("Locations"))
            for (Object locations : yaml.getList("Locations")) {
                if (!(locations instanceof Location location))
                    continue;
                jukeboxLocations.add(new JukeboxLocation(location));
            }

        initRunnable();

        getCommand("jukebox").setExecutor(this);
        getCommand("jukebox").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);

    }

    @Override
    public void onDisable() {

        List<Location> list = new ArrayList<>();

        for (JukeboxLocation jukeboxLocation : jukeboxLocations) {
            list.add(jukeboxLocation.getLocation());
        }

        yaml.set("Locations", list);

        try {
            yaml.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player))
            return true;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("skip")) {
                disc = discs[ThreadLocalRandom.current().nextInt(discs.length)];
            } else {
                try {
                    disc = Material.valueOf(args[0]);
                } catch (IllegalArgumentException exception) {
                    player.sendMessage(ChatColor.RED + "Unknown disc.");
                    return true;
                }
                player.sendMessage(ChatColor.GREEN + "Playing disc: " + args[0]);
            }
            lastPlayed = 0;
            playDisc(disc);
            return true;
        }

        Block block = player.getTargetBlock(null, 5);

        block.setType(Material.JUKEBOX);
        jukeboxLocations.add(new JukeboxLocation(block.getLocation()));
        player.sendMessage(ChatColor.GREEN + "Jukebox location set. break this jukebox to remove it from the saved list.");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            list.add("skip");

            for (Material material : discs) {
                list.add(material.name());
            }

        }

        return list;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null)
            return;
        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (e.getPlayer().hasPermission("jukebox.admin") && e.getPlayer().getGameMode().equals(GameMode.CREATIVE))
                return;
        }
        e.setCancelled(isJukebox(e.getClickedBlock().getLocation()));
    }

    public boolean isJukebox(Location location) {
        for (JukeboxLocation jukeboxLocation : jukeboxLocations) {
            if (jukeboxLocation.getLocation().equals(location))
                return true;
        }
        return false;
    }

    private int lastPlayed = 0;
    Material disc;

    private void initRunnable() {

        disc = discs[ThreadLocalRandom.current().nextInt(discs.length)];
        playDisc(disc);

        new BukkitRunnable() {
            @Override
            public void run() {
                lastPlayed += 5;

                if (lastPlayed >= 20 * 60 * 6) {
                    disc = discs[ThreadLocalRandom.current().nextInt(discs.length)];
                    playDisc(disc);
                }
                playParticle();

            }
        }.runTaskTimer(this, 0, 5);

    }

    public void playDisc(Material disc) {
        for (int i = 0; i < jukeboxLocations.size(); i++) {
            JukeboxLocation jbl = jukeboxLocations.toArray(new JukeboxLocation[]{})[i];
            if (!jbl.playDisc(disc))
                jukeboxLocations.remove(jbl);
        }
        lastPlayed = 0;
    }

    public void playParticle() {
        for (JukeboxLocation jukeboxLocation : jukeboxLocations) {
            if (!jukeboxLocation.isPlaying())
                continue;
            jukeboxLocation.getLocation().getWorld().spawnParticle(Particle.NOTE, jukeboxLocation.getLocation().clone().add(.5, 1, .5), 5, 1, .5, 1);
        }
    }

    public Set<JukeboxLocation> getJukeboxLocations() {
        return jukeboxLocations;
    }

    @Override
    public File getFile() {
        return file;
    }

    public YamlConfiguration getYaml() {
        return yaml;
    }
}
