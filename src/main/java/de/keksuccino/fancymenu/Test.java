package de.keksuccino.fancymenu;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class Test {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDrawScreen(ScreenEvent.DrawScreenEvent.Post e) {
        Minecraft.getInstance().font.draw(e.getPoseStack(), Minecraft.getInstance().fpsString, 20, 20, -1);
    }

}
