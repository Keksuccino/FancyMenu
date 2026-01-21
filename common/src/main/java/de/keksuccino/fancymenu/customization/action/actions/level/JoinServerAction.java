package de.keksuccino.fancymenu.customization.action.actions.level;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinServerList;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableNotificationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableScreenHandler;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JoinServerAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    private static long lastJoinErrorTrigger = -1;

    public JoinServerAction() {
        super("joinserver");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (Minecraft.getInstance().level != null) {
            long now = System.currentTimeMillis();
            if ((lastJoinErrorTrigger + 20000) < now) {
                lastJoinErrorTrigger = now;
                QueueableScreenHandler.addToQueue(new QueueableNotificationScreen(Component.translatable("fancymenu.actions.errors.cannot_join_world_while_in_world")));
            }
            return;
        }
        if (value != null) {
            if (Minecraft.getInstance().screen instanceof DisconnectedScreen) {
                ScreenUtils.setScreen(new TitleScreen());
            }
            if (!(Minecraft.getInstance().screen instanceof JoinServerBridgeScreen) && !(Minecraft.getInstance().screen instanceof ConnectScreen)) {
                try {

                    Screen current = Minecraft.getInstance().screen;

                    ScreenUtils.setScreen(new JoinServerBridgeScreen());

                    String ip = value.replace(" ", "");
                    int port = 25565;
                    if (ip.contains(":")) {
                        String portString = ip.split(":", 2)[1];
                        ip = ip.split(":", 2)[0];
                        if (MathUtils.isInteger(portString)) {
                            port = Integer.parseInt(portString);
                        }
                    }
                    ServerData d = null;
                    ServerList l = new ServerList(Minecraft.getInstance());
                    l.load();
                    for (ServerData data : ((IMixinServerList) l).getServerListFancyMenu()) {
                        if (data.ip.equals(value.replace(" ", ""))) {
                            d = data;
                            break;
                        }
                    }
                    if (d == null) {
                        d = new ServerData(value.replace(" ", ""), value.replace(" ", ""), ServerData.Type.OTHER);
                        l.add(d, false);
                        l.save();
                    }
                    if (current == null) current = new TitleScreen();
                    boolean isQuickPlay = false;

                    ConnectScreen.startConnecting(current, Minecraft.getInstance(), new ServerAddress(ip, port), d, isQuickPlay, null);

                } catch (Exception ex) {
                    LOGGER.error("[FANCYMENU] Failed to execute the 'Join Server' action!", ex);
                }
            }
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.joinserver");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.joinserver.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.joinserver.desc.value");
    }

    @Override
    public String getValuePreset() {
        return "exampleserver.com:25565";
    }

    private static class JoinServerBridgeScreen extends GenericMessageScreen {

        public JoinServerBridgeScreen() {
            super(Component.empty());
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

    }

}
