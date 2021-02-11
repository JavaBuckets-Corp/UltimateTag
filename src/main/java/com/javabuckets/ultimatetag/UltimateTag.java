package com.javabuckets.ultimatetag;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public final class UltimateTag extends JavaPlugin {

    public static boolean isRunning = false;

    public static boolean randomPosition = true;
    public static int borderSize = 64;
    public static int defaultTimer = 2 * 60;
    public static int timer = 2 * 60; // minutes * seconds_converter

    public static ArrayList<ItemStack> taggerItemsDefault = new ArrayList<ItemStack>();
    public static ArrayList<ItemStack> playerItemsDefault = new ArrayList<ItemStack>();
    public static ArrayList<ItemStack> taggerItems = new ArrayList<ItemStack>();
    public static ArrayList<ItemStack> playerItems = new ArrayList<ItemStack>();

    public static ArrayList<Player> contestants = new ArrayList<Player>();
    public static HashMap<Player, Role> roles = new HashMap<Player, Role>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.getCommand("ultimatetag").setExecutor(new CommandUltimateTag(this));

        getServer().getPluginManager().registerEvents(new PlayerEventListener(), this);

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // Timer runs out, it's a loss for the taggers
                    if (timer < 0) {
                        contestants.forEach(contestant -> contestant.sendMessage("Timer went out! Taggers lose!"));
                        deinitialize();
                    }
                    if (timer == 60) {
                        contestants.forEach(contestant -> contestant.sendMessage("There is 1 minute left!"));
                    }
                    if (timer == 10) {
                        contestants.forEach(contestant -> contestant.sendMessage("There are 10 seconds left!"));
                    }

                    // There are no more players, taggers win
                    if (!roles.containsValue(Role.PLAYER)) {
                        contestants.forEach(contestant -> contestant.sendMessage("Everyone has been tagged! Taggers win!"));
                        deinitialize();
                    }

                    // This will still run if someone lose, so we have to wrap the rest of the checks in another if statement and check if isRunning is still true
                    if (isRunning) {
                        // Last thing to do is to decrease timer
                        timer--;
                    }
                }
            }
        }, 0, 20); // Should be every second
        //Set default items
        ItemStack pickaxe = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        pickaxeMeta.setUnbreakable(true);
        pickaxe.setItemMeta(pickaxeMeta);
        playerItemsDefault.add(pickaxe);

        ItemStack shovel = new ItemStack(Material.IRON_SHOVEL);
        ItemMeta shovelMeta = shovel.getItemMeta();
        shovelMeta.setUnbreakable(true);
        shovel.setItemMeta(shovelMeta);
        playerItemsDefault.add(shovel);

        ItemStack axe = new ItemStack(Material.IRON_AXE);
        ItemMeta axeMeta = axe.getItemMeta();
        axeMeta.setUnbreakable(true);
        axe.setItemMeta(axeMeta);
        playerItemsDefault.add(axe);

        ItemStack hoe = new ItemStack(Material.IRON_HOE);
        ItemMeta hoeMeta = hoe.getItemMeta();
        hoeMeta.setUnbreakable(true);
        hoe.setItemMeta(hoeMeta);
        playerItemsDefault.add(hoe);
        playerItemsDefault.add(new ItemStack(Material.COBBLESTONE, 16));
        playerItemsDefault.add(new ItemStack(Material.WATER_BUCKET));

        playerItems = (ArrayList) playerItemsDefault.clone();

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.setUnbreakable(true);
        bowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        bowMeta.addEnchant(Enchantment.ARROW_KNOCKBACK, 2, true);
        bow.setItemMeta(bowMeta);
        taggerItemsDefault.add(bow);

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta rodMeta = rod.getItemMeta();
        rodMeta.setUnbreakable(true);
        rod.setItemMeta(rodMeta);
        taggerItemsDefault.add(rod);
        taggerItemsDefault.add(new ItemStack(Material.ARROW, 1));
        taggerItems = (ArrayList) taggerItemsDefault.clone();
    }

    public static void initialize(JavaPlugin plugin) {
        timer = defaultTimer;
        // Decide on a tagger
        Random random = new Random();
        int taggerIndex = random.nextInt(contestants.size());
        Player tagger = contestants.get(taggerIndex);
        tagger.setDisplayName(ChatColor.AQUA + tagger.getName());
        tagger.setPlayerListName(ChatColor.AQUA + tagger.getName());

        // Assign roles to all contestants
        for (Player contestant : contestants) {
            if (contestant == tagger) {
                roles.put(contestant, Role.TAGGER);
                contestant.sendTitle(ChatColor.RED + "You are the tagger!", "Try to tag everyone else!");
            } else {
                roles.put(contestant, Role.PLAYER);
                contestant.sendTitle(ChatColor.RED + tagger.getDisplayName() + " is the tagger!", "Try not to be tagged!");
            }
        }

        // Decide on a random area for the game
        World gameWorld = getGameWorld(tagger);

        if (gameWorld == null) {
            return;
        }

        Location center = tagger.getLocation();
        if (randomPosition) {
            center = findSuitableCenter(gameWorld);
        }
        gameWorld.getChunkAt(center).load();
        gameWorld.setTime(0);

        for (Player contestant : contestants) {
            int playerRandomX = center.getBlockX() + random.nextInt(borderSize) - borderSize/2;
            int playerRandomZ = center.getBlockZ() + random.nextInt(borderSize) - borderSize/2;
            int playerRandomY = gameWorld.getHighestBlockYAt(playerRandomX, playerRandomZ);

            Location location = new Location(gameWorld, playerRandomX, playerRandomY + 1, playerRandomZ);

            // Potion effects
            contestant.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 60 * 2, 3));

            if (contestant != tagger) {
                contestant.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,140, 1));

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        contestant.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 60 * 2, 1));
                    }
                }, 140);
            }

            // Teleport the contestants
            contestant.teleport(location);

            // Reset player stats and inventory
            resetContestant(contestant);

            // Give players starter items
            for (int i = 0; i < playerItems.size(); i ++) {
                contestant.getInventory().addItem(playerItems.get(i));
            }

            // Reset their possible bed location to their teleport location
            contestant.setBedSpawnLocation(location);
        }

        // Give tagger some additional items
        for (int i = 0; i < taggerItems.size(); i ++) {
            tagger.getInventory().addItem(taggerItems.get(i));
        }

        // Set the world border
        WorldBorder border = gameWorld.getWorldBorder();

        border.setCenter(center);
        border.setSize(borderSize);

        isRunning = true;
    }

    public static void deinitialize() {
        isRunning = false;
        timer = defaultTimer;
        contestants.forEach(contestant -> resetContestant(contestant));
        contestants.clear();
        roles.clear();

        for (World world : Bukkit.getWorlds()) {
            world.getWorldBorder().reset();
        }
    }

    public static World getGameWorld(Player tagger) {
        for (World world : Bukkit.getWorlds()) {
            if (world.getPlayers().contains(tagger)) {
                return world;
            }
        }
        return null;
    }

    public static void resetContestant(Player contestant) {
        contestant.getInventory().clear();
        contestant.setFoodLevel(20);
        contestant.setHealth(20);
        contestant.setExhaustion(0);
        contestant.setDisplayName(contestant.getName());
        contestant.setPlayerListName(contestant.getName());
        contestant.setGameMode(GameMode.SURVIVAL);
    }

    public static void makeTagger(Player contestant) {
        UltimateTag.roles.put(contestant, Role.TAGGER);

        contestant.setDisplayName(ChatColor.AQUA + contestant.getName());
        contestant.setPlayerListName(ChatColor.AQUA + contestant.getName());

        // Target is now a tagger, so give them tagger items
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.setUnbreakable(true);
        bowMeta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        bowMeta.addEnchant(Enchantment.ARROW_KNOCKBACK, 2, true);
        bow.setItemMeta(bowMeta);

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta rodMeta = rod.getItemMeta();
        rodMeta.setUnbreakable(true);
        rod.setItemMeta(rodMeta);

        contestant.getInventory().addItem(
                bow,
                rod,
                new ItemStack(Material.ARROW, 1)
        );

        contestant.removePotionEffect(PotionEffectType.GLOWING);
    }

    public static Location findSuitableCenter(World world) {
        Random random = new Random();

        int randomX = random.nextInt(10000);
        int randomZ = random.nextInt(10000);
        int randomY = world.getHighestBlockYAt(randomX, randomZ);

        Location location = new Location(world, randomX, randomY, randomZ);

        while (location.getBlock().isLiquid()) {
            int newX = random.nextInt(10000);
            int newZ = random.nextInt(10000);
            int newY = world.getHighestBlockYAt(newX, newZ);
            location = new Location(world, newX, newY, newZ);
        }

        return location;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        deinitialize();
    }
}
