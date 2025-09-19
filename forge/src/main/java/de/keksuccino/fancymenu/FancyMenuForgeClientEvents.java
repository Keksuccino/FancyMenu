package de.keksuccino.fancymenu;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import de.keksuccino.fancymenu.customization.gameintro.GameIntroOverlay;
import de.keksuccino.fancymenu.events.screen.ScreenKeyPressedEvent;
import de.keksuccino.fancymenu.events.screen.ScreenKeyReleasedEvent;
import de.keksuccino.fancymenu.networking.PacketHandler;
import de.keksuccino.fancymenu.util.event.acara.EventHandler;
import de.keksuccino.fancymenu.util.rendering.GuiBlurRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class FancyMenuForgeClientEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void registerAll() {
        MinecraftForge.EVENT_BUS.register(new FancyMenuForgeClientEvents());
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(), "fancymenu_gui_blur_composite", DefaultVertexFormat.POSITION_TEX);
            event.registerShader(shader, loaded -> GuiBlurRenderer.getInstance().setCompositeShader(loaded, true));
        } catch (IOException exception) {
            LOGGER.error("Failed to register FancyMenu GUI blur shader", exception);
        }
    }

    @SubscribeEvent
    public void afterScreenKeyPress(ScreenEvent.KeyPressed.Post e) {
        ScreenKeyPressedEvent event = new ScreenKeyPressedEvent(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventHandler.INSTANCE.postEvent(event);

        if (Minecraft.getInstance().getOverlay() instanceof GameIntroOverlay o) {
            o.keyPressed(e.getKeyCode(), e.getScanCode(), e.getModifiers());
        }
    }

    @SubscribeEvent
    public void afterScreenKeyRelease(ScreenEvent.KeyReleased.Post e) {
        ScreenKeyReleasedEvent event = new ScreenKeyReleasedEvent(e.getScreen(), e.getKeyCode(), e.getScanCode(), e.getModifiers());
        EventHandler.INSTANCE.postEvent(event);
    }

    @SubscribeEvent
    public void onClientLoggedIn(ClientPlayerNetworkEvent.LoggingIn e) {
        Minecraft.getInstance().execute(PacketHandler::sendHandshakeToServer);
    }
}
