package com.javabuckets.ultimatetag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerEventListener implements Listener {
    @EventHandler
    public void onPlayerTookDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player target = (Player) event.getEntity();

            if (UltimateTag.isRunning) {
                if (event.getDamager() instanceof Player) {
                    Player damager = (Player) event.getDamager();

                    if (UltimateTag.roles.get(damager) == Role.TAGGER && UltimateTag.roles.get(target) == Role.PLAYER) {
                        UltimateTag.makeTagger(target);

                        Bukkit.broadcastMessage(target.getDisplayName() + " has been tagged!");
                        target.sendMessage(ChatColor.RED + "You've been tagged!");
                        damager.sendMessage("You tagged " + target.getDisplayName() + "!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player target = (Player) event.getEntity();

            if (UltimateTag.isRunning) {
                if (UltimateTag.roles.get(target) == Role.PLAYER) {
                    if (target.getHealth() - event.getDamage() < 1) {
                        UltimateTag.makeTagger(target);

                        Bukkit.broadcastMessage(target.getDisplayName() + " died and is now also a tagger!");
                        target.sendMessage(ChatColor.RED + "Because you died you are a now a tagger!");

                        target.setHealth(20);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
