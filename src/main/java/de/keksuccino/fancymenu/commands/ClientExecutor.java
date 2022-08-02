package de.keksuccino.fancymenu.commands;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class ClientExecutor {

    protected static List<Runnable> commandQueue = new ArrayList<>();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(ClientExecutor.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {

        List<Runnable> queue = new ArrayList<>();
        queue.addAll(commandQueue);
        for (Runnable r : queue) {
            r.run();
            commandQueue.remove(r);
        }

    }

    public static void execute(Runnable command) {
        commandQueue.add(command);
    }

}
