package de.keksuccino.fancymenu.mixin.mixins.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.patches.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.RendererWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@WidgetifiedScreen
@Mixin(LevelLoadingScreen.class)
public abstract class MixinLevelLoadingScreen extends Screen {

    @Shadow @Final private StoringChunkProgressListener progressListener;

    @Unique private TextWidget percentageTextFancyMenu;

    protected MixinLevelLoadingScreen(Component $$0) {
        super($$0);
    }

    @Override
    protected void init() {

        this.percentageTextFancyMenu = this.addRenderableWidget(TextWidget.of(this.getFormattedProgress(), 0, (this.height / 2) - 30 - (9 / 2), 200))
                .setTextAlignment(TextWidget.TextAlignment.CENTER)
                .centerWidget(this)
                .setIdentifier("percentage");

        this.addRenderableWidget(new RendererWidget((this.width / 2) - 50, (this.height / 2) + 30 - 50, 100, 100, (pose, mouseX, mouseY, partial, x, y, width1, height1, renderer) -> {
            this.renderChunkBoxFancyMenu(pose, x + 50, y + 50);
        })).setIdentifier("chunks");

    }

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRenderFancyMenu(PoseStack $$0, int $$1, int $$2, float $$3, CallbackInfo info) {
        this.percentageTextFancyMenu.setMessage(Component.literal(this.getFormattedProgress()));
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void afterRenderFancyMenu(PoseStack $$0, int $$1, int $$2, float $$3, CallbackInfo info) {
        super.render($$0, $$1, $$2, $$3);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;renderChunks(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/server/level/progress/StoringChunkProgressListener;IIII)V"))
    private boolean wrapRenderChunksFancyMenu(PoseStack poseStack, StoringChunkProgressListener storingChunkProgressListener, int i, int j, int k, int l) {
        return !this.isCustomizableFancyMenu();
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private boolean wrapRenderPercentStringFancyMenu(PoseStack poseStack, Font font, String s, int i1, int i2, int i3) {
        return !this.isCustomizableFancyMenu();
    }

    @Unique
    private void renderChunkBoxFancyMenu(@NotNull PoseStack pose, int xCenter, int yCenter) {
        LevelLoadingScreen.renderChunks(pose, this.progressListener, xCenter, yCenter, 2, 0);
    }

    @Unique
    private boolean isCustomizableFancyMenu() {
        return ScreenCustomization.isCustomizationEnabledForScreen(this.getScreenFancyMenu());
    }

    @Unique
    @SuppressWarnings("all")
    private LevelLoadingScreen getScreenFancyMenu() {
        return (LevelLoadingScreen) ((Object)this);
    }

    @Shadow protected abstract String getFormattedProgress();

}
