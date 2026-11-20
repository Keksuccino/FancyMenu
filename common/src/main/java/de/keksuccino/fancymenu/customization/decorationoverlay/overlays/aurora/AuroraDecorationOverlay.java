package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.aurora;

import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Northern lights behind the elements: three curtains hang across the upper sky and wave slowly, each with a bright lower edge and rays
 * that rise from it. The edge and tip colors are options. Everything is back content.<br><br>
 *
 * Every curtain lives in its own canvas, so the GPU blends them. The curtains change slowly, so each frame repaints only the curtain
 * that waited longest, which keeps every curtain at about 20 repaints per second.
 */
public class AuroraDecorationOverlay extends PixelDecorationOverlay<AuroraDecorationOverlay> {

    private static final float STAGE_HEIGHT = 270.0F;
    /** A curtain gets repainted when it is at least this old. Below 60 fps all curtains take turns, one per frame. */
    private static final float REPAINT_INTERVAL = 1.0F / 20.0F;
    private static final AuroraCurtain[] CURTAINS = {
            new AuroraCurtain(56.0F, 8.0F, 0.015F, -0.13F, 46.0F, 2.1F, -1, 0.8F),
            new AuroraCurtain(79.0F, 10.0F, 0.011F, 0.17F, 72.0F, 0.0F, 1, 1.0F),
            new AuroraCurtain(70.0F, 7.0F, 0.021F, 0.22F, 40.0F, 4.4F, 1, 0.65F)
    };

    public final Property.ColorProperty edgeColor = putProperty(Property.hexColorProperty("edge_color", "#58F2A2", true, "fancymenu.decoration_overlays.aurora.edge_color"));
    public final Property.ColorProperty tipColor = putProperty(Property.hexColorProperty("tip_color", "#AA52BC", true, "fancymenu.decoration_overlays.aurora.tip_color"));

    private final PixelCanvas[] canvases = new PixelCanvas[CURTAINS.length];
    /** Seconds since each curtain got repainted, {@link Float#MAX_VALUE} when it has to be repainted right away. */
    private final float[] curtainAge = new float[CURTAINS.length];
    @NotNull
    private AuroraPalette palette = AuroraPalette.NATURAL;
    @Nullable
    private String lastEdgeHex = null;
    @Nullable
    private String lastTipHex = null;

    public AuroraDecorationOverlay() {
        for (int i = 0; i < CURTAINS.length; i++) this.canvases[i] = new PixelCanvas("aurora_" + i);
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.edgeColor.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.aurora.edge_color.desc")));

        this.tipColor.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.GRADIENT)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.aurora.tip_color.desc")));

    }

    @Override
    protected void onResize(int width, int height) {
        float scaleY = height / STAGE_HEIGHT;
        float lowest = 0.0F;
        for (AuroraCurtain curtain : CURTAINS) lowest = Math.max(lowest, curtain.lowestStageRow());
        // The edge rounds to the next row and two glow pixels hang below it
        int rows = Math.min(height, (int)Math.ceil(lowest * scaleY) + 3);
        for (PixelCanvas canvas : this.canvases) canvas.resize(width, rows);
        this.repaintAllNow();
    }

    @Override
    protected void update(float deltaSeconds) {
        this.syncPalette();
        float scaleY = this.getHeight() / STAGE_HEIGHT;
        int oldest = -1;
        for (int i = 0; i < CURTAINS.length; i++) {
            if (this.curtainAge[i] == Float.MAX_VALUE) {
                this.repaint(i, scaleY);
                continue;
            }
            this.curtainAge[i] += deltaSeconds;
            if ((oldest < 0) || (this.curtainAge[i] > this.curtainAge[oldest])) oldest = i;
        }
        if ((oldest >= 0) && (this.curtainAge[oldest] >= REPAINT_INTERVAL)) this.repaint(oldest, scaleY);
    }

    private void repaint(int index, float scaleY) {
        PixelCanvas canvas = this.canvases[index];
        if ((canvas.width() <= 0) || (canvas.height() <= 0)) return;
        CURTAINS[index].render(canvas.pixels(), canvas.width(), canvas.height(), this.getTime(), scaleY, this.palette);
        canvas.markAllDirty();
        this.curtainAge[index] = 0.0F;
    }

    private void repaintAllNow() {
        Arrays.fill(this.curtainAge, Float.MAX_VALUE);
    }

    /**
     * Rebuilds the palette when a color option changed, then all curtains repaint in their new colors right away.
     */
    private void syncPalette() {
        String edgeHex = this.edgeColor.getHex();
        String tipHex = this.tipColor.getHex();
        if (Objects.equals(edgeHex, this.lastEdgeHex) && Objects.equals(tipHex, this.lastTipHex)) return;
        this.lastEdgeHex = edgeHex;
        this.lastTipHex = tipHex;
        AuroraPalette updated = AuroraPalette.of(this.edgeColor.getDrawable().getColorInt(), this.tipColor.getDrawable().getColorInt());
        if (updated == this.palette) return;
        this.palette = updated;
        this.repaintAllNow();
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        for (PixelCanvas canvas : this.canvases) graphics.canvas(canvas, 0, 0);
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        for (PixelCanvas canvas : this.canvases) canvas.releaseTexture();
    }

}
