package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Consumer;

//TODO übernehmen (annotation)
@Deprecated(forRemoval = true)
public class AnimationMenuBackgroundConfigScreen extends Screen {

    protected Screen parent;
    protected AnimationMenuBackground background;
    protected Consumer<AnimationMenuBackground> callback;

    protected ExtendedButton chooseAnimationButton;
    protected ExtendedButton toggleRestartOnLoadButton;
    protected ExtendedButton cancelButton;
    protected ExtendedButton doneButton;

    protected AnimationMenuBackgroundConfigScreen(@Nullable Screen parent, @NotNull AnimationMenuBackground background, @NotNull Consumer<AnimationMenuBackground> callback) {

        super(Component.translatable("fancymenu.background.animation.configure"));

        this.parent = parent;
        this.background = background;
        this.callback = callback;

    }

    @Override
    protected void init() {

        super.init();

        this.chooseAnimationButton = new ExtendedButton(0, 0, 300, 20, Component.translatable("fancymenu.background.animation.configure.choose_animation"), (press) -> {
            ChooseAnimationScreen s = new ChooseAnimationScreen(this.background.animationName, (call) -> {
                if (call != null) {
                    this.background.animationName = call;
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });
        this.addWidget(this.chooseAnimationButton);
        UIBase.applyDefaultWidgetSkinTo(this.chooseAnimationButton);

        this.toggleRestartOnLoadButton = new ExtendedButton(0, 0, 300, 20, Component.literal(""), (press) -> {
            this.background.restartOnMenuLoad = !this.background.restartOnMenuLoad;
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                if (!AnimationMenuBackgroundConfigScreen.this.background.restartOnMenuLoad) {
                    this.setMessage(Component.translatable("fancymenu.background.animation.configure.restart_on_load.off"));
                } else {
                    this.setMessage(Component.translatable("fancymenu.background.animation.configure.restart_on_load.on"));
                }
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.toggleRestartOnLoadButton);
        UIBase.applyDefaultWidgetSkinTo(this.toggleRestartOnLoadButton);

        this.doneButton = new ExtendedButton(0, 0, 145, 20, Component.translatable("fancymenu.guicomponents.done"), (press) -> {
            Minecraft.getInstance().setScreen(this.parent);
            this.callback.accept(this.background);
        }) {
            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
                this.active = AnimationMenuBackgroundConfigScreen.this.background.animationName != null;
                if (!this.active) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.of(LocalizationUtils.splitLocalizedLines("fancymenu.background.animation.configure.no_animation_chosen")).setDefaultStyle(), false, true);
                }
                super.render(graphics, mouseX, mouseY, partial);
            }
        };
        this.addWidget(this.doneButton);
        UIBase.applyDefaultWidgetSkinTo(this.doneButton);

        this.cancelButton = new ExtendedButton(0, 0, 145, 20, Component.translatable("fancymenu.guicomponents.cancel"), (press) -> {
            this.onClose();
        });
        this.addWidget(this.cancelButton);
        UIBase.applyDefaultWidgetSkinTo(this.cancelButton);

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        graphics.fill(RenderType.guiOverlay(), 0, 0, this.width, this.height, UIBase.getUIColorTheme().screen_background_color.getColorInt());

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        MutableComponent title = this.title.copy().withStyle(ChatFormatting.BOLD);
        int titleWidth = this.font.width(title);
        graphics.drawString(this.font, title, (int)(centerX - (titleWidth / 2F)), 20, UIBase.getUIColorTheme().generic_text_base_color.getColorInt(), false);

        this.chooseAnimationButton.setX(centerX - (this.chooseAnimationButton.getWidth() / 2));
        this.chooseAnimationButton.setY(centerY - 20 - 3);
        this.chooseAnimationButton.render(graphics, mouseX, mouseY, partial);

        this.toggleRestartOnLoadButton.setX(centerX - (this.toggleRestartOnLoadButton.getWidth() / 2));
        this.toggleRestartOnLoadButton.setY(centerY + 2);
        this.toggleRestartOnLoadButton.render(graphics, mouseX, mouseY, partial);

        this.doneButton.setX((this.width / 2) - this.doneButton.getWidth() - 5);
        this.doneButton.setY(this.height - 40);
        this.doneButton.render(graphics, mouseX, mouseY, partial);

        this.cancelButton.setX((this.width / 2) + 5);
        this.cancelButton.setY(this.height - 40);
        this.cancelButton.render(graphics, mouseX, mouseY, partial);

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
        this.callback.accept(null);
    }

}
