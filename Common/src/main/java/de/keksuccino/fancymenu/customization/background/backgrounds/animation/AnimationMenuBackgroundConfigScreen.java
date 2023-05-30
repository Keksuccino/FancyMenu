package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.layout.editor.ChooseAnimationScreen;
import de.keksuccino.fancymenu.rendering.ui.UIBase;
import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
import de.keksuccino.fancymenu.rendering.ui.tooltip.TooltipHandler;
import de.keksuccino.fancymenu.rendering.ui.widget.Button;
import de.keksuccino.fancymenu.utils.LocalizationUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class AnimationMenuBackgroundConfigScreen extends Screen {

    protected Screen parent;
    protected AnimationMenuBackground background;
    protected Consumer<AnimationMenuBackground> callback;

    protected Button chooseAnimationButton;
    protected Button toggleRestartOnLoadButton;
    protected Button cancelButton;
    protected Button doneButton;

    protected AnimationMenuBackgroundConfigScreen(@Nullable Screen parent, @NotNull AnimationMenuBackground background, @NotNull Consumer<AnimationMenuBackground> callback) {

        super(Component.translatable("fancymenu.background.animation.configure"));

        this.parent = parent;
        this.background = background;
        this.callback = callback;

        this.chooseAnimationButton = new Button(0, 0, 300, 20, Component.translatable("fancymenu.background.animation.configure.choose_animation"), true, (press) -> {
            ChooseAnimationScreen s = new ChooseAnimationScreen(this.background.animationName, (call) -> {
                if (call != null) {
                    this.background.animationName = call;
                }
                Minecraft.getInstance().setScreen(this);
            });
            Minecraft.getInstance().setScreen(s);
        });
        UIBase.applyDefaultButtonSkinTo(this.chooseAnimationButton);

        this.toggleRestartOnLoadButton = new Button(0, 0, 300, 20, Component.literal(""), true, (press) -> {
            this.background.restartOnMenuLoad = !this.background.restartOnMenuLoad;
        }) {
            @Override
            public void render(@NotNull PoseStack $$0, int $$1, int $$2, float $$3) {
                if (!background.restartOnMenuLoad) {
                    this.setMessage(Component.translatable("fancymenu.background.animation.configure.restart_on_load.off"));
                } else {
                    this.setMessage(Component.translatable("fancymenu.background.animation.configure.restart_on_load.on"));
                }
                super.render($$0, $$1, $$2, $$3);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.toggleRestartOnLoadButton);

        this.doneButton = new Button(0, 0, 145, 20, Component.translatable("fancymenu.guicomponents.done"), true, (press) -> {
            Minecraft.getInstance().setScreen(this.parent);
            this.callback.accept(this.background);
        }) {
            @Override
            public void render(@NotNull PoseStack $$0, int $$1, int $$2, float $$3) {
                this.active = background.animationName != null;
                if (!this.active) {
                    TooltipHandler.INSTANCE.addWidgetTooltip(this, Tooltip.create(LocalizationUtils.splitLocalizedLines("fancymenu.background.animation.configure.no_animation_chosen")).setDefaultBackgroundColor(), false, true);
                }
                super.render($$0, $$1, $$2, $$3);
            }
        };
        UIBase.applyDefaultButtonSkinTo(this.doneButton);

        this.cancelButton = new Button(0, 0, 145, 20, Component.translatable("fancymenu.guicomponents.cancel"), true, (press) -> {
            this.onClose();
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        RenderSystem.enableBlend();

        fill(pose, 0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        MutableComponent title = this.title.copy().withStyle(ChatFormatting.BOLD);
        int titleWidth = this.font.width(title);
        this.font.draw(pose, title, (float)centerX - ((float)titleWidth / 2F), 20, -1);

        this.chooseAnimationButton.setX(centerX - (this.chooseAnimationButton.getWidth() / 2));
        this.chooseAnimationButton.setY(centerY - 20 - 3);
        this.chooseAnimationButton.render(pose, mouseX, mouseY, partial);

        this.toggleRestartOnLoadButton.setX(centerX - (this.toggleRestartOnLoadButton.getWidth() / 2));
        this.toggleRestartOnLoadButton.setY(centerY + 2);
        this.toggleRestartOnLoadButton.render(pose, mouseX, mouseY, partial);

        this.doneButton.setX((this.width / 2) - this.doneButton.getWidth() - 5);
        this.doneButton.setY(this.height - 40);
        this.doneButton.render(pose, mouseX, mouseY, partial);

        this.cancelButton.setX((this.width / 2) + 5);
        this.cancelButton.setY(this.height - 40);
        this.cancelButton.render(pose, mouseX, mouseY, partial);

    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
        this.callback.accept(null);
    }

}
