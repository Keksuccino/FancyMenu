package de.keksuccino.fancymenu.customization.element.elements.cursor;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CursorElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public final Property.IntegerProperty hotspotX = putProperty(Property.integerProperty("hotspot_x", 0, "fancymenu.elements.cursor.hotspot_x"));
    public final Property.IntegerProperty hotspotY = putProperty(Property.integerProperty("hotspot_y", 0, "fancymenu.elements.cursor.hotspot_y"));
    public boolean editorPreviewMode = false;
    @Nullable
    public ResourceSupplier<ITexture> textureSupplier;
    protected boolean cursorReady = false;
    private final CursorRebuildTracker cursorRebuildTracker = new CursorRebuildTracker();
    @Nullable
    private CursorHandler.CustomCursor registeredCursor;

    public CursorElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.updateCursor();

            if (isEditor()) {
                if ((this.textureSupplier != null) && !this.editorPreviewMode) {
                    ITexture t = this.textureSupplier.get();
                    if (t != null) {
                        Identifier loc = t.getResourceLocation();
                        if (loc != null) {
                            int[] size = t.getAspectRatio().getAspectRatioSizeByMaximumSize(this.getAbsoluteWidth(), this.getAbsoluteHeight());
                            RenderingUtils.resetShaderColor(graphics);
                            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, loc, this.getAbsoluteX(), this.getAbsoluteY(), 0.0F, 0.0F, size[0], size[1], size[0], size[1]);
                            RenderingUtils.resetShaderColor(graphics);
                        }
                    }
                } else {
                    RenderingUtils.resetShaderColor(graphics);
                    graphics.fill(this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteX() + (this.getAbsoluteWidth() / 2), this.getAbsoluteY() + this.getAbsoluteHeight(), DrawableColor.WHITE.getColorInt());
                    graphics.fill(this.getAbsoluteX() + (this.getAbsoluteWidth() / 2), this.getAbsoluteY(), this.getAbsoluteX() + this.getAbsoluteWidth(), this.getAbsoluteY() + this.getAbsoluteHeight(), DrawableColor.BLACK.getColorInt());
                    RenderingUtils.resetShaderColor(graphics);
                }
            }

            if (this.cursorReady && (!isEditor() || (this.editorPreviewMode && UIBase.isXYInArea(mouseX, mouseY, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight())))) {
                CursorHandler.setClientTickCursor(this.getCursorName());
            }

        }

    }

    public void updateCursor() {
        if (this.textureSupplier == null) {
            this.resetCursorState();
            return;
        }
        ITexture texture = this.textureSupplier.get();
        if (!(texture instanceof PngTexture pngTexture)) {
            this.resetCursorState();
            return;
        }
        Identifier location = texture.getResourceLocation();
        int resolvedHotspotX = this.hotspotX.getInteger();
        int resolvedHotspotY = this.hotspotY.getInteger();
        boolean canUseCustomCursor = location != null && (!isEditor() || this.editorPreviewMode);
        CursorHandler.CustomCursor registered = this.registeredCursor;
        boolean registrationStillCurrent = registered != null && CursorHandler.getCustomCursor(this.getCursorName()) == registered && CursorConfigurationMatcher.matches(registered.texture, registered.hotspotX, registered.hotspotY, pngTexture, resolvedHotspotX, resolvedHotspotY);
        boolean shouldRebuild = this.cursorRebuildTracker.shouldAttempt(pngTexture, location, resolvedHotspotX, resolvedHotspotY, canUseCustomCursor, registrationStillCurrent);

        if (!canUseCustomCursor) {
            this.releaseRegisteredCursor();
            this.cursorReady = false;
        } else if (shouldRebuild) {
            this.cursorRebuildTracker.recordResult(this.rebuildCursor(pngTexture, resolvedHotspotX, resolvedHotspotY));
        }
    }

    public void forceRebuildCursor() {
        this.releaseRegisteredCursor();
        this.cursorReady = false;
        this.cursorRebuildTracker.reset();
        this.updateCursor();
    }

    @Override
    public void onDestroyElement() {
        super.onDestroyElement();
        this.resetCursorState();
    }

    @NotNull
    public String getCursorName() {
        return "fm_cursor_element_" + this.getInstanceIdentifier();
    }

    private boolean rebuildCursor(@NotNull PngTexture texture, int resolvedHotspotX, int resolvedHotspotY) {
        this.cursorReady = false;
        CursorHandler.CustomCursor cursor = CursorHandler.getCustomCursor(this.getCursorName());
        if (cursor == null || !CursorConfigurationMatcher.matches(cursor.texture, cursor.hotspotX, cursor.hotspotY, texture, resolvedHotspotX, resolvedHotspotY)) {
            CursorHandler.CustomCursor obsoleteCursor = cursor;
            cursor = CursorHandler.CustomCursor.create(texture, resolvedHotspotX, resolvedHotspotY, this.textureSupplier.getSourceWithPrefix());
            if (cursor != null) CursorHandler.registerCustomCursor(this.getCursorName(), cursor);
            if (cursor == null && obsoleteCursor != null) CursorHandler.unregisterCustomCursor(this.getCursorName(), obsoleteCursor);
        }
        if (cursor != null && CursorHandler.getCustomCursor(this.getCursorName()) == cursor) {
            if (this.registeredCursor != null && this.registeredCursor != cursor) this.registeredCursor.destroy();
            this.registeredCursor = cursor;
            this.cursorReady = true;
            return true;
        } else {
            this.releaseRegisteredCursor();
            return false;
        }
    }

    private void resetCursorState() {
        this.releaseRegisteredCursor();
        this.cursorReady = false;
        this.cursorRebuildTracker.reset();
    }

    private void releaseRegisteredCursor() {
        CursorHandler.CustomCursor cursor = this.registeredCursor;
        this.registeredCursor = null;
        if (cursor != null) CursorHandler.unregisterCustomCursor(this.getCursorName(), cursor);
    }

}
