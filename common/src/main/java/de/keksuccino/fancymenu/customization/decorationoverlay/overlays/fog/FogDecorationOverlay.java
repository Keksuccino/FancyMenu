package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fog;

import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Three layers of mist that drift at different speeds in front of the elements: two bands of long wisps across the middle and ground fog
 * that thickens toward the bottom edge. Banks are flat translucent shapes with soft stepped edges whose top edges catch the light,
 * thin enough that labels stay readable. The fog parts around the pointer, which is an option. Everything is front content.<br><br>
 *
 * Every layer has its own canvas. A layer only changes when its noise textures slid on by a whole pixel (a few times per second),
 * and then at most one layer gets painted completely per frame, or when the pointer moved through it, and then only the area
 * around the pointer gets painted again.
 */
public class FogDecorationOverlay extends PixelDecorationOverlay<FogDecorationOverlay> {

    public final Property<Boolean> clearAroundPointer = putProperty(Property.booleanProperty("clear_around_pointer", true, "fancymenu.decoration_overlays.fog.clear_around_pointer"));

    private final FogLayerState[] layers = new FogLayerState[FogLayer.LAYERS.length];
    /** The layer that gets the first chance to be painted completely in the next frame, so no layer waits for long. */
    private int firstLayer = 0;

    public FogDecorationOverlay() {
        for (int i = 0; i < this.layers.length; i++) this.layers[i] = new FogLayerState(FogLayer.LAYERS[i], i);
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.clearAroundPointer.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.HIGHLIGHT_MOUSE_CURSOR)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fog.clear_around_pointer.desc")));

    }

    @Override
    protected void onResize(int width, int height) {
        for (FogLayerState layer : this.layers) layer.resize(width, height);
    }

    @Override
    protected void update(float deltaSeconds) {
        boolean clearing = this.clearAroundPointer.tryGetNonNullElse(true) && this.pointer.isInside();
        float clearX = clearing ? this.pointer.x() : Float.NaN;
        float clearY = clearing ? this.pointer.y() : Float.NaN;
        float time = this.getTime();
        boolean mayPaintAll = true;
        int count = this.layers.length;
        for (int n = 0; n < count; n++) {
            FogLayerState layer = this.layers[(this.firstLayer + n) % count];
            if (layer.update(time, clearX, clearY, mayPaintAll)) mayPaintAll = false;
        }
        this.firstLayer = (this.firstLayer + 1) % count;
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        for (FogLayerState layer : this.layers) graphics.canvas(layer.canvas, 0, layer.top());
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        for (FogLayerState layer : this.layers) layer.canvas.releaseTexture();
    }

}
