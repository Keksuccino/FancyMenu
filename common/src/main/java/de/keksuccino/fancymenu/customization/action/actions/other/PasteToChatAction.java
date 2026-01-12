package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.action.ActionInstance;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinChatScreen;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinMinecraft;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.cycle.CommonCycles;
import de.keksuccino.fancymenu.util.rendering.ui.screen.StringBuilderScreen;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.CycleButton;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

public class PasteToChatAction extends Action {

    public PasteToChatAction() {
        super("paste_to_chat");
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
            String msg;
            boolean append = true;
            if (value.toLowerCase().startsWith("true:") || value.toLowerCase().startsWith("false:")) {
                msg = value.split(":", 2)[1];
                if (value.toLowerCase().startsWith("false:")) {
                    append = false;
                }
            } else {
                msg = value;
            }
            msg = StringUtils.convertFormatCodes(msg, "§", "&");
            if (Minecraft.getInstance().level != null) {
                if (Minecraft.getInstance().player != null) {
                    Screen s = Minecraft.getInstance().screen;
                    if (!(s instanceof ChatScreen)) {
                        ((IMixinMinecraft)Minecraft.getInstance()).openChatScreenFancyMenu(msg);
                    } else {
                        if (append) {
                            ((IMixinChatScreen)s).getInputFancyMenu().insertText(msg);
                        } else {
                            ((IMixinChatScreen)s).getInputFancyMenu().setValue(msg);
                        }
                    }
                }
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.paste_to_chat");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.paste_to_chat.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.empty();
    }

    @Override
    public String getValueExample() {
        return "true:Hi my name is Fred.";
    }

    @Override
    public void editValue(@NotNull Screen parentScreen, @NotNull ActionInstance instance) {
        PasteToChatActionValueScreen s = new PasteToChatActionValueScreen(Objects.requireNonNullElse(instance.value, this.getValueExample()), value -> {
            if (value != null) {
                instance.value = value;
            }
            Minecraft.getInstance().setScreen(parentScreen);
        });
        Minecraft.getInstance().setScreen(s);
    }

    public static class PasteToChatActionValueScreen extends StringBuilderScreen {

        protected boolean append = false;
        protected String msg = "";

        protected PasteToChatActionValueScreen(@NotNull String value, @NotNull Consumer<String> callback) {
            super(Component.translatable("fancymenu.actions.generic_edit_value"), callback);
            if (value.contains(":")) {
                this.msg = value.split(":", 2)[1];
                String appendString = value.split(":", 2)[0];
                if (appendString.equalsIgnoreCase("true")) this.append = true;
            }
        }

        @Override
        protected void initCells() {

            this.addSpacerCell(20);

            this.addLabelCell(Component.translatable("fancymenu.actions.paste_to_chat.text"));
            this.addTextInputCell(null, true, true).setEditListener(s -> this.msg = s).setText(this.msg);

            this.addCellGroupEndSpacerCell();

            this.addWidgetCell(new CycleButton<>(0, 0, 20, 20, CommonCycles.cycleEnabledDisabled("fancymenu.actions.paste_to_chat.append", this.append), (value, button) -> {
                this.append = value.getAsBoolean();
            }).setUITooltip(UITooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.actions.paste_to_chat.append.desc"))), true);

            this.addSpacerCell(20);

        }

        @Override
        public @NotNull String buildString() {
            return this.append + ":" + this.msg;
        }

    }

}
