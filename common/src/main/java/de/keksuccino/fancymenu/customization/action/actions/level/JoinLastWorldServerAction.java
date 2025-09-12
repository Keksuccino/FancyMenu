package de.keksuccino.fancymenu.customization.action.actions.level;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinServerList;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

public class JoinLastWorldServerAction extends Action {

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
        if (!LastWorldHandler.getLastWorld().isEmpty() && (Minecraft.getInstance().screen != null)) {
            if (!LastWorldHandler.isLastWorldServer()) { // CASE: SINGLEPLAYER WORLD
                File f = new File(LastWorldHandler.getLastWorld());
                if (Minecraft.getInstance().getLevelSource().levelExists(f.getName())) {
                    Screen current = (Minecraft.getInstance().screen != null) ? Minecraft.getInstance().screen : new TitleScreen();
                    Minecraft.getInstance().forceSetScreen(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
                    Minecraft.getInstance().createWorldOpenFlows().openWorld(f.getName(), () -> {
                        Minecraft.getInstance().setScreen(current);
                    });
                }
            } else { //CASE: SERVER
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
                    d = new ServerData(ipRaw, ipRaw, ServerData.Type.OTHER);
                    l.add(d, false);
                    l.save();
                }
                boolean isQuickPlay = false;
                ConnectScreen.startConnecting(Minecraft.getInstance().screen, Minecraft.getInstance(), new ServerAddress(ip, port), d, isQuickPlay, null);
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.join_last_world");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.join_last_world.desc");
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
