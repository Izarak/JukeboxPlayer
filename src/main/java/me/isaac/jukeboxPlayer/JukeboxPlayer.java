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

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class JukeboxPlayer extends JavaPlugin implements Listener {

    Set<JukeboxLocation> jukeboxLocations = new HashSet<>();

    final DiscMaterial[] discs = new DiscMaterial[] {
            new DiscMaterial(Material.MUSIC_DISC_RELIC, 20),
            new DiscMaterial(Material.MUSIC_DISC_CREATOR, 10),
            new DiscMaterial(Material.MUSIC_DISC_TEARS, 5),
            new DiscMaterial(Material.MUSIC_DISC_OTHERSIDE, 10),
            new DiscMaterial(Material.MUSIC_DISC_BLOCKS, 20),
            new DiscMaterial(Material.MUSIC_DISC_CHIRP, 5),
            new DiscMaterial(Material.MUSIC_DISC_FAR, 25),
            new DiscMaterial(Material.MUSIC_DISC_MELLOHI, 15),
            new DiscMaterial(Material.MUSIC_DISC_STAL, 30),
            new DiscMaterial(Material.MUSIC_DISC_STRAD, 10),
            new DiscMaterial(Material.MUSIC_DISC_CREATOR_MUSIC_BOX, 12),
            new DiscMaterial(Material.MUSIC_DISC_WAIT, 5)
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

    @Nullable
    public DiscMaterial getDiscMaterial(Material material) {
        for (DiscMaterial discMaterial : discs) {
            if (discMaterial.disc.equals(material))
                return discMaterial;
        }
        return null;
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
                    disc = getDiscMaterial(Material.valueOf(args[0]));
                } catch (IllegalArgumentException exception) {
                    player.sendMessage(ChatColor.RED + "Unknown disc.");
                    return true;
                }
                player.sendMessage(ChatColor.GREEN + "Playing disc: " + args[0]);
            }
            lastPlayedDisc = 0;
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

            if ("skip".contains(args[0].toLowerCase()))
                list.add("skip");

            for (DiscMaterial material : discs) {
                if (material.disc.name().toLowerCase().contains(args[0].toLowerCase()))
                    list.add(material.disc.name());
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

    private int lastPlayedDisc = 0, lastRunParticle = 0;
    DiscMaterial disc;

    private void initRunnable() {

        disc = discs[ThreadLocalRandom.current().nextInt(discs.length)];
        playDisc(disc);

        final int period = 2;
        new BukkitRunnable() {
            @Override
            public void run() {
                lastPlayedDisc += 5;

                if (lastPlayedDisc >= 20 * 60 * 6) {
                    disc = discs[ThreadLocalRandom.current().nextInt(discs.length)];
                    playDisc(disc);
                }

                lastRunParticle += period;
                if (lastRunParticle >= disc.ticks) {
                    playParticle();
                    lastRunParticle = 0;
                }

            }
        }.runTaskTimer(this, 0, period);

    }

    public void playDisc(DiscMaterial disc) {
        for (int i = 0; i < jukeboxLocations.size(); i++) {
            JukeboxLocation jbl = jukeboxLocations.toArray(new JukeboxLocation[]{})[i];
            if (!jbl.playDisc(disc.disc))
                jukeboxLocations.remove(jbl);
        }
        lastPlayedDisc = 0;
        lastRunParticle = 0;
    }

    public void playParticle() {
        for (JukeboxLocation jukeboxLocation : jukeboxLocations) {
            if (!jukeboxLocation.isPlaying())
                continue;
            if (jukeboxLocation.getLocation().getWorld() == null)
                return;
            jukeboxLocation.getLocation().getWorld().spawnParticle(Particle.NOTE, jukeboxLocation.getLocation().clone().add(.5, 1, .5), 7, .75, .3, .75);
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

    record DiscMaterial(Material disc, long ticks) {

    }

}
