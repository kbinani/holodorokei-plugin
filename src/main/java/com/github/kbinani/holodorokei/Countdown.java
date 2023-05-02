package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class Countdown {

    private Countdown() {
    }

    public static void Then(World world, BoundingBox[] boxes, JavaPlugin plugin, Predicate<Integer> countdown, Supplier<Boolean> task, long delay) {
        Server server = plugin.getServer();
        BukkitScheduler scheduler = server.getScheduler();

        if (!countdown.test(3)) {
            return;
        }
        Players.Within(world, boxes, player -> {
            player.playNote(player.getLocation(), Instrument.BIT, new Note(12));
            player.showTitle(Title.title(Component.text("3"), Component.text("")));
        });

        scheduler.runTaskLater(plugin, () -> {
            if (!countdown.test(2)) {
                return;
            }
            Players.Within(world, boxes, player -> {
                player.playNote(player.getLocation(), Instrument.BIT, new Note(12));
                player.showTitle(Title.title(Component.text("2"), Component.text("")));
            });

            scheduler.runTaskLater(plugin, () -> {
                if (!countdown.test(1)) {
                    return;
                }
                Players.Within(world, boxes, player -> {
                    player.playNote(player.getLocation(), Instrument.BIT, new Note(12));
                    player.showTitle(Title.title(Component.text("1"), Component.text("")));
                });
                scheduler.runTaskLater(plugin, () -> {
                    if (!task.get()) {
                        return;
                    }
                    Players.Within(world, boxes, player -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
                        player.showTitle(Title.title(Component.text("START!!!"), Component.text("")));
                    });
                }, delay);
            }, delay);
        }, delay);
    }
}