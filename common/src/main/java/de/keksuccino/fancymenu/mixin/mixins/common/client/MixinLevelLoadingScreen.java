package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.rendering.ui.screen.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.RendererWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
import org.spongepowered.asm.mixin.injection.Redirect;
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
                .setWidgetIdentifierFancyMenu("percentage");

        this.addRenderableWidget(new RendererWidget((this.width / 2) - 50, (this.height / 2) + 30 - 50, 100, 100, (graphics, mouseX, mouseY, partial, x, y, width1, height1, renderer) -> {
            this.renderChunkBoxFancyMenu(graphics, x + 50, y + 50);
        })).setWidgetIdentifierFancyMenu("chunks");

    }

    @Inject(method = "render", at = @At("HEAD"))
    private void beforeRenderFancyMenu(GuiGraphics $$0, int $$1, int $$2, float $$3, CallbackInfo ci) {
        this.percentageTextFancyMenu.setMessage(Component.literal(this.getFormattedProgress()));
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void afterRenderFancyMenu(GuiGraphics $$0, int $$1, int $$2, float $$3, CallbackInfo ci) {
        super.render($$0, $$1, $$2, $$3);
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;renderChunks(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/server/level/progress/StoringChunkProgressListener;IIII)V"))
    private boolean wrapRenderChunksFancyMenu(GuiGraphics $$0, StoringChunkProgressListener $$1, int $$2, int $$3, int $$4, int $$5) {
        return !this.isCustomizableFancyMenu();
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private boolean wrapRenderPercentStringFancyMenu(GuiGraphics instance, Font $$0, String $$1, int $$2, int $$3, int $$4) {
        return !this.isCustomizableFancyMenu();
    }

    @Unique
    private void renderChunkBoxFancyMenu(@NotNull GuiGraphics graphics, int xCenter, int yCenter) {
        LevelLoadingScreen.renderChunks(graphics, this.progressListener, xCenter, yCenter, 2, 0);
    }

    @Unique
    private boolean isCustomizableFancyMenu() {
        return ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

    @Shadow protected abstract String getFormattedProgress();

}
