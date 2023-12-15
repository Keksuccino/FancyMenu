package de.keksuccino.fancymenu.customization.action.actions.level;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinServerList;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
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

    public JoinLastWorldServerAction() {
        super("join_last_world");
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public void execute(@Nullable String value) {
        if (!LastWorldHandler.getLastWorld().equals("") && (Minecraft.getInstance().screen != null)) {
            if (!LastWorldHandler.isLastWorldServer()) {
                File f = new File(LastWorldHandler.getLastWorld());
                if (Minecraft.getInstance().getLevelSource().levelExists(f.getName())) {
                    Minecraft.getInstance().forceSetScreen(new GenericDirtMessageScreen(Components.translatable("selectWorld.data_read")));
                    Minecraft.getInstance().loadLevel(f.getName());
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
                    d = new ServerData(ipRaw, ipRaw, false);
                    l.add(d);
                    l.save();
                }
                ConnectScreen.startConnecting(Minecraft.getInstance().screen, Minecraft.getInstance(), new ServerAddress(ip, port), d);
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Components.translatable("fancymenu.editor.custombutton.config.actiontype.join_last_world");
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
