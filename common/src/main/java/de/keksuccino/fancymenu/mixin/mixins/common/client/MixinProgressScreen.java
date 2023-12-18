package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.rendering.ui.screen.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Objects;

@WidgetifiedScreen
@Mixin(ProgressScreen.class)
public class MixinProgressScreen extends Screen {

    @Shadow private @Nullable Component header;
    @Shadow private @Nullable Component stage;
    @Shadow private int progress;

    @Unique private TextWidget headerTextFancyMenu;
    @Unique private TextWidget stageTextFancyMenu;

    protected MixinProgressScreen(Component $$0) {
        super($$0);
    }

    @Override
    protected void init() {

        if (this.isCustomizableFancyMenu()) {

            this.headerTextFancyMenu = this.addRenderableWidget(TextWidget.empty(0, 70, 500))
                    .setTextAlignment(TextWidget.TextAlignment.CENTER)
                    .centerWidget(this)
                    .setWidgetIdentifierFancyMenu("header");

            this.stageTextFancyMenu = this.addRenderableWidget(TextWidget.empty(0, 90, 500))
                    .setTextAlignment(TextWidget.TextAlignment.CENTER)
                    .centerWidget(this)
                    .setWidgetIdentifierFancyMenu("stage");

            this.updateText();

        }

    }
    @Unique
    private void updateText() {

        if (this.headerTextFancyMenu != null) {
            this.headerTextFancyMenu.setMessage(Objects.requireNonNullElse(this.header, Component.empty()));
        }

        if (this.stageTextFancyMenu != null) {
            if ((this.stage != null) && (this.progress != 0)) {
                this.stageTextFancyMenu.setMessage(Component.empty().append(this.stage).append(" " + this.progress + "%"));
            } else {
                this.stageTextFancyMenu.setMessage(Component.empty());
            }
        }

    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean wrapDrawCenteredStringInRenderFancyMenu(GuiGraphics instance, Font $$0, Component $$1, int $$2, int $$3, int $$4) {
        return !this.isCustomizableFancyMenu();
    }

    @Inject(method = "progressStart", at = @At("RETURN"))
    private void onProgressStartFancyMenu(Component component, CallbackInfo info) {
        this.updateText();
    }

    @Inject(method = "progressStage", at = @At("RETURN"))
    private void onProgressStageFancyMenu(Component component, CallbackInfo info) {
        this.updateText();
    }

    @Inject(method = "progressStagePercentage", at = @At("RETURN"))
    private void onProgressStagePercentageFancyMenu(int percentage, CallbackInfo info) {
        this.updateText();
    }

    @Unique
    private boolean isCustomizableFancyMenu() {
        return ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
