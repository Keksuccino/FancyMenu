package de.keksuccino.fancymenu.customization.action.actions.level;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.screeninstancefactory.ScreenInstanceFactory;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DisconnectAction extends Action {

    private static final Component SAVING_LEVEL = Component.translatable("menu.savingLevel");

    public DisconnectAction() {
        super("disconnect_server_or_world");
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
                        Screen s = ScreenInstanceFactory.tryConstruct(ScreenCustomization.findValidMenuIdentifierFor(value));
                        if (s == null) {
                            s = new TitleScreen();
                        }
                        boolean singleplayer = mc.isLocalServer();
                        mc.level.disconnect();
                        if (singleplayer) {
                            mc.clearLevel(new GenericDirtMessageScreen(SAVING_LEVEL));
                        } else {
                            mc.clearLevel();
                        }
                        mc.setScreen(s);
                    }
                }, true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.disconnect");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.disconnect.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.disconnect.desc.value");
    }

    @Override
    public String getValueExample() {
        return "example.menu.identifier";
    }

}
