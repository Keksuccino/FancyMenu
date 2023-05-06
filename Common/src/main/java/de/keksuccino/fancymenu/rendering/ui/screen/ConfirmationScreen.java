
package de.keksuccino.fancymenu.rendering.ui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.localization.Locals;
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

    protected AdvancedButton confirmButton;
    protected AdvancedButton cancelButton;

    public ConfirmationScreen(@Nullable Screen parentScreen, @NotNull Consumer<Boolean> callback, @NotNull String... text) {

        super(Component.literal(""));
        this.parentScreen = parentScreen;
        this.callback = callback;
        this.text = text;

        this.confirmButton = new AdvancedButton(0, 0, 150, 20, "§a" + Locals.localize("fancymenu.guicomponents.confirm"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(true);
        });
        UIBase.applyDefaultButtonSkinTo(this.confirmButton);

        this.cancelButton = new AdvancedButton(0, 0, 150, 20, "§c" + Locals.localize("fancymenu.guicomponents.cancel"), true, (button) -> {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(false);
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    protected void init() {

        //Reset the GUI scale in case the layout editor changed it
        Minecraft.getInstance().getWindow().setGuiScale(Minecraft.getInstance().getWindow().calculateScale(Minecraft.getInstance().options.guiScale().get(), Minecraft.getInstance().isEnforceUnicode()));
        this.height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        this.width = Minecraft.getInstance().getWindow().getGuiScaledWidth();

        super.init();

    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {

        fill(matrix, 0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        int y = (this.height / 2) - ((this.text.length * 14) / 2);
        for (String s : this.text) {
            if (s.length() > 0) {
                int textWidth = this.font.width(s);
                drawString(matrix, this.font, Component.literal(s), (this.width / 2) - (textWidth / 2), y, -1);
            }
            y += 14;
        }

        this.confirmButton.setX((this.width / 2) - this.confirmButton.getWidth() - 5);
        this.confirmButton.setY(this.height - 40);
        this.confirmButton.render(matrix, mouseX, mouseY, partial);

        this.cancelButton.setX((this.width / 2) + 5);
        this.cancelButton.setY(this.height - 40);
        this.cancelButton.render(matrix, mouseX, mouseY, partial);

        super.render(matrix, mouseX, mouseY, partial);

    }

    @Override
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        //ENTER
        if (button == 257) {
            Minecraft.getInstance().setScreen(this.parentScreen);
            this.callback.accept(true);
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parentScreen);
        this.callback.accept(false);
    }

}
