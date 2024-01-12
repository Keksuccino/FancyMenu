package de.keksuccino.fancymenu.customization.action.actions.level;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinServerList;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JoinServerAction extends Action {

    public JoinServerAction() {
        super("joinserver");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
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
            for (ServerData data : ((IMixinServerList)l).getServerListFancyMenu()) {
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
            Screen current = Minecraft.getInstance().screen;
            if (current == null) current = new TitleScreen();
            boolean isQuickPlay = false;
            ConnectScreen.startConnecting(current, Minecraft.getInstance(), new ServerAddress(ip, port), d, isQuickPlay);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.joinserver");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.joinserver.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.joinserver.desc.value");
    }

    @Override
    public String getValueExample() {
        return "exampleserver.com:25565";
    }

}
