package de.keksuccino.fancymenu.customization.action.actions.level;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.screen.ScreenInstanceFactory;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.ScreenUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DisconnectAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");

    public DisconnectAction() {
        super("disconnect_server_or_world");
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
        if (value != null) {
            Minecraft mc = Minecraft.getInstance();
            try {
                Screen current = Minecraft.getInstance().screen;
                if (current == null) current = new TitleScreen();
                mc.getReportingContext().draftReportHandled(mc, current, () -> {
                    if ((mc.level != null) && (mc.player != null)) {
                        boolean isSinglePlayer = mc.isLocalServer();
                        Screen openAfter;
                        if (CustomGuiHandler.guiExists(value)) {
                            openAfter = CustomGuiHandler.constructInstance(value, null, null);
                        } else {
                            openAfter = ScreenInstanceFactory.tryConstruct(ScreenIdentifierHandler.tryFixInvalidIdentifierWithNonUniversal(value));
                        }
                        if (openAfter == null) {
                            openAfter = new TitleScreen();
                        }
                        mc.level.disconnect();
                        if (isSinglePlayer) {
                            mc.disconnect(new GenericMessageScreen(SAVING_LEVEL));
                        } else {
                            mc.disconnect();
                        }
                        ScreenUtils.setScreen(openAfter);
                    }
                }, true);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to execute Disconnect action!", ex);
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.disconnect");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.disconnect.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.disconnect.desc.value");
    }

    @Override
    public String getValuePreset() {
        return "example.menu.identifier";
    }

}
