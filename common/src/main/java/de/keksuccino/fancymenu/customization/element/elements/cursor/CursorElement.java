package de.keksuccino.fancymenu.customization.element.elements.cursor;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.SimpleTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CursorElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public int hotspotX = 0;
    public int hotspotY = 0;
    public boolean editorPreviewMode = false;
    @Nullable
    public ResourceSupplier<ITexture> textureSupplier;
    protected boolean cursorReady = false;
    @Nullable
    protected ResourceLocation lastLocation;
    protected int lastHotspotX;
    protected int lastHotspotY;

    public CursorElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.updateCursor();

            if (isEditor()) {
                if ((this.textureSupplier != null) && !this.editorPreviewMode) {
                    ITexture t = this.textureSupplier.get();
                    if (t != null) {
                        ResourceLocation loc = t.getResourceLocation();
                        if (loc != null) {
                            int[] size = t.getAspectRatio().getAspectRatioSizeByMaximumSize(this.getAbsoluteWidth(), this.getAbsoluteHeight());
                            RenderingUtils.resetShaderColor(graphics);
                            graphics.blit(loc, this.getAbsoluteX(), this.getAbsoluteY(), 0.0F, 0.0F, size[0], size[1], size[0], size[1]);
                            RenderingUtils.resetShaderColor(graphics);
                        }
                    }
                } else {
                    RenderingUtils.resetShaderColor(graphics);
                    RenderSystem.enableBlend();
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

        if (this.textureSupplier != null) {
            ITexture t = this.textureSupplier.get();
            if (t instanceof SimpleTexture s) {
                ResourceLocation loc = t.getResourceLocation();
                if ((loc != this.lastLocation) || (this.lastHotspotX != this.hotspotX) || (this.lastHotspotY != this.hotspotY)) {
                    if (loc != null) {
                        this.cursorReady = false;
                        if (!isEditor() || this.editorPreviewMode) {
                            CursorHandler.CustomCursor cursor = CursorHandler.getCustomCursor(this.getCursorName());
                            if ((cursor == null) || (cursor.texture != s) || (cursor.hotspotX != this.hotspotX) || (cursor.hotspotY != this.hotspotY)) {
                                cursor = CursorHandler.CustomCursor.create(s, this.hotspotX, this.hotspotY, this.textureSupplier.getSourceWithPrefix());
                                if (cursor != null) {
                                    CursorHandler.registerCustomCursor(this.getCursorName(), cursor);
                                    this.cursorReady = true;
                                }
                            } else {
                                this.cursorReady = true;
                            }
                        }
                    }
                }
                this.lastLocation = loc;
                this.lastHotspotX = this.hotspotX;
                this.lastHotspotY = this.hotspotY;
            }
        } else {
            this.lastLocation = null;
            this.lastHotspotX = 0;
            this.lastHotspotY = 0;
            this.cursorReady = false;
        }

    }

    public void forceRebuildCursor() {
        this.cursorReady = false;
        this.lastLocation = null;
        this.updateCursor();
    }

    @NotNull
    public String getCursorName() {
        return "fm_cursor_element_" + this.getInstanceIdentifier();
    }

}
