package de.keksuccino.fancymenu.mixin.mixins.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProgressScreen.class)
public class MixinProgressScreen extends Screen {

    @Shadow private @Nullable Component header;
    @Shadow private @Nullable Component stage;
    @Shadow private int progress;

    @Unique private StringWidget headerText;
    @Unique private StringWidget stageText;

    protected MixinProgressScreen(Component $$0) {
        super($$0);
    }

    @Override
    protected void init() {

        if (this.isCustomizable()) {

            this.headerText = this.addRenderableWidget(new StringWidget(Component.empty(), this.font));

            this.stageText = this.addRenderableWidget(new StringWidget(Component.empty(), this.font));

            this.updateText();

        }

    }
    @Unique
    private void updateText() {

        if (this.headerText != null) {
            if (this.header != null) {
                this.headerText.setMessage(this.header);
                this.headerText.setWidth(this.font.width(this.headerText.getMessage().getVisualOrderText()));
                this.headerText.x = (this.width / 2) - (this.headerText.getWidth() / 2);
                this.headerText.y = 70;
            } else {
                this.headerText.setMessage(Component.empty());
            }
        }

        if (this.stageText != null) {
            if ((this.stage != null) && (this.progress != 0)) {
                this.stageText.setMessage(Component.empty().append(this.stage).append(" " + this.progress + "%"));
                this.stageText.setWidth(this.font.width(this.stageText.getMessage().getVisualOrderText()));
                this.stageText.x = (this.width / 2) - (this.stageText.getWidth() / 2);
                this.stageText.y = 90;
            } else {
                this.stageText.setMessage(Component.empty());
            }
        }

    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/ProgressScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean wrapDrawCenteredStringInRenderFancyMenu(PoseStack poseStack, Font font, Component component, int i1, int i2, int i3) {
        return !this.isCustomizable();
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
    private boolean isCustomizable() {
        return ScreenCustomization.isCustomizationEnabledForScreen(this.getScreen());
    }

    @Unique
    @SuppressWarnings("all")
    private ProgressScreen getScreen() {
        return (ProgressScreen)((Object)this);
    }

}
