package de.keksuccino.fancymenu.customization.action.actions.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiHandler;
import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.customization.screen.ScreenInstanceFactory;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenScreenAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    private static long lastErrorTriggered = -1;

    public OpenScreenAction() {
        super("opengui");
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
            if (RenderSystem.isOnRenderThread()) {
                value = ScreenIdentifierHandler.tryFixInvalidIdentifierWithNonUniversal(value);
                if (value.equals(CreateWorldScreen.class.getName())) {
                    CreateWorldScreen.openFresh(Minecraft.getInstance(), Minecraft.getInstance().screen);
                } else {
                        if (CustomGuiHandler.guiExists(value)) {
                            Screen custom = CustomGuiHandler.constructInstance(value, Minecraft.getInstance().screen, null);
                        if (custom != null) ScreenUtils.setScreen(custom);
                    } else {
                        Screen s = ScreenInstanceFactory.tryConstruct(value);
                        if (s != null) {
                            ScreenUtils.setScreen(s);
                        } else {
                            LOGGER.error("[FANCYMENU] Unable to construct screen instance for '" + value + "'!", new Exception());
                            Dialogs.openMessage(Component.translatable("fancymenu.actions.open_screen.error"), MessageDialogStyle.ERROR);
                        }
                    }
                }
            } else {
                long now = System.currentTimeMillis();
                if ((lastErrorTriggered + 60000) < now) {
                    lastErrorTriggered = now;
                    MainThreadTaskExecutor.executeInMainThread(
                            () -> ScreenUtils.setScreen(new GenericMessageScreen(Component.translatable("fancymenu.actions.generic.async_error", this.getActionDisplayName()))),
                            MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
                }
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.opengui");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.opengui.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.opengui.desc.value");
    }

    @Override
    public String getValueExample() {
        return "example.menu.identifier";
    }

}
