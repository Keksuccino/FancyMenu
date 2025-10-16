package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.util.rendering.ui.screen.WidgetifiedScreen;
import de.keksuccino.fancymenu.util.rendering.ui.widget.RendererWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.TextWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@WidgetifiedScreen
@Mixin(LevelLoadingScreen.class)
public abstract class MixinLevelLoadingScreen extends Screen {

    @Shadow @Final private static Component DOWNLOADING_TERRAIN_TEXT;
    @Shadow private LevelLoadTracker loadTracker;
    @Shadow private float smoothedProgress;

    @Unique private TextWidget downloadingTerrainText_FancyMenu;
    @Unique private RendererWidget chunkRenderer_FancyMenu;
    @Unique private RendererWidget progressBar_FancyMenu;

    protected MixinLevelLoadingScreen(Component component) {
        super(component);
    }

    @Shadow
    protected abstract void drawProgressBar(GuiGraphics p_433901_, int p_433815_, int p_434324_, int p_433974_, int p_433800_, float p_433827_);

    @Override
    protected void init() {

        if (this.isCustomizableFancyMenu()) {

            this.chunkRenderer_FancyMenu = this.addRenderableWidget(new RendererWidget((this.width / 2) - 50, (this.height / 2) + 30 - 50, 100, 100,
                    (graphics, mouseX, mouseY, partial, x, y, width1, height1, renderer) -> {
                        this.renderChunkBox_FancyMenu(graphics, x + 50, y + 50);
                    }
            )).setWidgetIdentifierFancyMenu("chunks");

            int progressScreenCenterX = this.width / 2;
            int progressScreenCenterY = this.height / 2;
            ChunkLoadStatusView statusView = this.loadTracker.statusView();
            int progressDefaultY;
            if (statusView != null) {
                int i = progressScreenCenterY - statusView.radius() * 2;
                progressDefaultY = i - 9 * 3;
            } else {
                progressDefaultY = progressScreenCenterY - 50;
            }
            int progressDefaultX = progressScreenCenterX - 100;

            this.progressBar_FancyMenu = this.addRenderableWidget(new RendererWidget(progressDefaultX, progressDefaultY + 9 + 3, 200, 2, (graphics, mouseX, mouseY, partial, x, y, width1, height1, renderer) -> {
                if (this.loadTracker.hasProgress()) {
                    this.drawProgressBar(graphics, x, y, width1, height1, this.smoothedProgress);
                }
            })).setWidgetIdentifierFancyMenu("progress_bar");

            this.downloadingTerrainText_FancyMenu = this.addRenderableWidget(TextWidget.of(DOWNLOADING_TERRAIN_TEXT, 0, progressDefaultY, 400))
                    .setTextAlignment(TextWidget.TextAlignment.CENTER)
                    .centerWidget(this)
                    .setWidgetIdentifierFancyMenu("downloading_terrain_text");

        } else {
            this.chunkRenderer_FancyMenu = null;
            this.downloadingTerrainText_FancyMenu = null;
            this.progressBar_FancyMenu = null;
        }

    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;renderChunks(Lnet/minecraft/client/gui/GuiGraphics;IIIILnet/minecraft/server/level/progress/ChunkLoadStatusView;)V"))
    private boolean wrap_renderChunks_in_render_FancyMenu(GuiGraphics j1, int chunkstatus, int l1, int i2, int k1, ChunkLoadStatusView j2) {
        return !this.isCustomizableFancyMenu();
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private boolean wrap_drawCenteredString_in_render_FancyMenu(GuiGraphics instance, Font p_282901_, Component p_282456_, int p_283083_, int p_282276_, int p_281457_) {
        return !this.isCustomizableFancyMenu();
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"))
    private boolean wrap_drawProgressBar_in_render_FancyMenu(LevelLoadingScreen instance, GuiGraphics p_433901_, int p_433815_, int p_434324_, int p_433974_, int p_433800_, float p_433827_) {
        return !this.isCustomizableFancyMenu();
    }

    @Unique
    private void renderChunkBox_FancyMenu(@NotNull GuiGraphics graphics, int xCenter, int yCenter) {
        if ((this.loadTracker == null) || (this.loadTracker.statusView() == null)) return;
        LevelLoadingScreen.renderChunks(graphics, xCenter, yCenter, 2, 0, this.loadTracker.statusView());
    }

    @Unique
    private boolean isCustomizableFancyMenu() {
        return ScreenCustomization.isCustomizationEnabledForScreen(this);
    }

}
