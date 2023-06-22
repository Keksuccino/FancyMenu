
package de.keksuccino.fancymenu.util.rendering.ui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.input.InputConstants;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.ExtendedButton;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ConfirmationScreen extends Screen {

    protected Screen parentScreen;
    protected String[] text;
    protected Consumer<Boolean> callback;
    protected boolean openParentScreen = true;

    protected ExtendedButton confirmButton;
    protected ExtendedButton cancelButton;

    /**
     *  Confirmation screen that does NOT open its parent screen on close.
     */
    public ConfirmationScreen(@NotNull Consumer<Boolean> callback, @NotNull String... text) {
        this(null, callback, text);
        this.openParentScreen = false;
    }

    /**
     * Confirmation screen that opens its parent screen on close.
     */
    public ConfirmationScreen(@Nullable Screen parentScreen, @NotNull Consumer<Boolean> callback, @NotNull String... text) {
        super(Component.literal(""));
        this.parentScreen = parentScreen;
        this.callback = callback;
        this.text = text;
    }

    @Override
    protected void init() {

        //Reset the GUI scale in case the layout editor changed it
        Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
        this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        super.init();

        this.confirmButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.guicomponents.confirm"), (button) -> {
            if (this.openParentScreen) Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(true);
        });
        this.addWidget(this.confirmButton);
        UIBase.applyDefaultButtonSkinTo(this.confirmButton);

        this.cancelButton = new ExtendedButton(0, 0, 150, 20, I18n.get("fancymenu.guicomponents.cancel"), (button) -> {
            if (this.openParentScreen) Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(false);
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        fill(pose, 0, 0, this.width, this.height, UIBase.getUIColorScheme().screen_background_color_darker.getColorInt());

        int y = (this.height / 2) - ((this.text.length * 14) / 2);
        for (String s : this.text) {
            if (s.length() > 0) {
                int textWidth = this.font.width(s);
                drawString(pose, this.font, Component.literal(s), (this.width / 2) - (textWidth / 2), y, UIBase.getUIColorScheme().generic_text_base_color.getColorInt());
            }
            y += 14;
        }

        this.confirmButton.setX((this.width / 2) - this.confirmButton.getWidth() - 5);
        this.confirmButton.setY(this.height - 40);
        this.confirmButton.render(pose, mouseX, mouseY, partial);

        this.cancelButton.setX((this.width / 2) + 5);
        this.cancelButton.setY(this.height - 40);
        this.cancelButton.render(pose, mouseX, mouseY, partial);

        super.render(pose, mouseX, mouseY, partial);

    }

    @Override
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        //ENTER
        if (button == InputConstants.KEY_ENTER) {
            if (this.openParentScreen) Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(true);
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    @Override
    public void onClose() {
        if (this.openParentScreen) Minecraft.getInstance().setScreen(this.parentScreen);
        this.callback.accept(false);
    }

}
