package de.keksuccino.fancymenu;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.ExtendedEditBox;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class TextInputScreen extends Screen {

    @NotNull
    protected Consumer<String> callback;

    protected ExtendedEditBox input;
    protected AdvancedButton cancelButton;
    protected AdvancedButton doneButton;

    public TextInputScreen(@NotNull Component title, @Nullable CharacterFilter filter, @NotNull Consumer<String> callback) {

        super(title);
        this.callback = callback;

        this.input = new ExtendedEditBox(Minecraft.getInstance().font, 0, 0, 200, 20, Component.empty(), true);
        this.input.setMaxLength(10000);
        this.input.setCharacterFilter(filter);

        this.cancelButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.guicomponents.cancel"), true, (button) -> {
            this.callback.accept(null);
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

        this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("fancymenu.guicomponents.done"), true, (button) -> {
           this.callback.accept(this.input.getValue());
        });
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.fill(0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        RenderSystem.enableBlend();
        int titleWidth = Minecraft.getInstance().font.width(this.title);
        graphics.drawString(Minecraft.getInstance().font, this.title, (this.width / 2) - (titleWidth / 2), (this.height / 2) - 30, -1, false);

        this.input.x = (this.width / 2) - (this.input.getWidth() / 2);
        this.input.y = (this.height / 2) - (this.input.getHeight() / 2);
        this.input.render(graphics, mouseX, mouseY, partial);

        this.cancelButton.x = (this.width / 2) - 5 - this.cancelButton.getWidth();
        this.cancelButton.y = this.height - 40;
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

        this.doneButton.x = (this.width / 2) + 5;
        this.doneButton.y = this.height - 40;
        this.doneButton.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        // ENTER
        if (button == 257) {
            this.callback.accept(this.input.getValue());
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    @Override
    public void onClose() {
        this.callback.accept(null);
    }

}
