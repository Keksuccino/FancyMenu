package de.keksuccino.fancymenu.customization.action.actions.level;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinServerList;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableNotificationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableScreenHandler;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

public class JoinLastWorldServerAction extends Action {

    private static long lastJoinErrorTrigger = -1;

    public JoinLastWorldServerAction() {
        super("join_last_world");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
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
        if (!LastWorldHandler.getLastWorld().isEmpty() && (Minecraft.getInstance().screen != null)) {
            if (!LastWorldHandler.isLastWorldServer()) { // CASE: SINGLEPLAYER WORLD
                File f = new File(LastWorldHandler.getLastWorld());
                if (Minecraft.getInstance().getLevelSource().levelExists(f.getName())) {
                    Minecraft.getInstance().forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
                    Minecraft.getInstance().createWorldOpenFlows().loadLevel(Minecraft.getInstance().screen, f.getName());
                }
            } else {
                String ipRaw = LastWorldHandler.getLastWorld().replace(" ", "");
                String ip = ipRaw;
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
                for (ServerData data : ((IMixinServerList)l).getServerListFancyMenu()) {
                    if (data.ip.equals(ipRaw)) {
                        d = data;
                        break;
                    }
                }
                if (d == null) {
                    boolean isLan = false;
                    d = new ServerData(ipRaw, ipRaw, isLan);
                    l.add(d, false);
                    l.save();
                }
                boolean isQuickPlay = false;
                ConnectScreen.startConnecting(Minecraft.getInstance().screen, Minecraft.getInstance(), new ServerAddress(ip, port), d, isQuickPlay);
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.join_last_world");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.join_last_world.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return null;
    }

    @Override
    public String getValueExample() {
        return null;
    }

}
