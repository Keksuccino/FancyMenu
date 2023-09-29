package de.keksuccino.fancymenu.customization.element.elements.cursor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.cursor.CursorHandler;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.LocalTexture;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CursorElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    public String source;
    public int hotspotX = 0;
    public int hotspotY = 0;
    public boolean editorPreviewMode = false;
    @Nullable
    protected LocalTexture texture;
    protected boolean cursorReady = false;
    @Nullable
    protected String lastSource;
    protected int lastHotspotX;
    protected int lastHotspotY;

    public CursorElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.updateCursor();

            if (isEditor()) {
                if ((this.texture != null) && (this.texture.getResourceLocation() != null) && !this.editorPreviewMode) {
                    int[] size = this.texture.getAspectRatio().getAspectRatioSizeByMaximumSize(this.getAbsoluteWidth(), this.getAbsoluteHeight());
                    RenderingUtils.bindTexture(this.texture.getResourceLocation());
                    RenderingUtils.resetShaderColor();
                    blit(pose, this.getAbsoluteX(), this.getAbsoluteY(), 0.0F, 0.0F, size[0], size[1], size[0], size[1]);
                    RenderingUtils.resetShaderColor();
                } else {
                    RenderingUtils.resetShaderColor();
                    RenderSystem.enableBlend();
                    fill(pose, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteX() + (this.getAbsoluteWidth() / 2), this.getAbsoluteY() + this.getAbsoluteHeight(), DrawableColor.WHITE.getColorInt());
                    fill(pose, this.getAbsoluteX() + (this.getAbsoluteWidth() / 2), this.getAbsoluteY(), this.getAbsoluteX() + this.getAbsoluteWidth(), this.getAbsoluteY() + this.getAbsoluteHeight(), DrawableColor.BLACK.getColorInt());
                    RenderingUtils.resetShaderColor();
                }
            }

            if (this.cursorReady && (!isEditor() || (this.editorPreviewMode && UIBase.isXYInArea(mouseX, mouseY, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteWidth(), this.getAbsoluteHeight())))) {
                CursorHandler.setClientTickCursor(this.getCursorName());
            }

        }

    }

    public void updateCursor() {

        if (this.source != null) {
            if (!this.source.equals(this.lastSource) || (this.lastHotspotX != this.hotspotX) || (this.lastHotspotY != this.hotspotY)) {
                ITexture t = TextureHandler.INSTANCE.getTexture(ScreenCustomization.getAbsoluteGameDirectoryPath(this.source));
                if (t instanceof LocalTexture l) {
                    this.texture = l;
                } else {
                    return;
                }
                this.cursorReady = false;
                if (!isEditor() || this.editorPreviewMode) {
                    CursorHandler.CustomCursor cursor = CursorHandler.getCustomCursor(this.getCursorName());
                    if (this.texture != null) {
                        if ((cursor == null) || (cursor.texture != this.texture) || (cursor.hotspotX != this.hotspotX) || (cursor.hotspotY != this.hotspotY)) {
                            cursor = CursorHandler.CustomCursor.create(this.texture, this.hotspotX, this.hotspotY);
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
            this.lastSource = this.source;
            this.lastHotspotX = this.hotspotX;
            this.lastHotspotY = this.hotspotY;
        } else {
            this.texture = null;
            this.lastSource = null;
            this.lastHotspotX = 0;
            this.lastHotspotY = 0;
            this.cursorReady = false;
        }

    }

    public void forceRebuildCursor() {
        this.cursorReady = false;
        this.lastSource = null;
        this.updateCursor();
    }

    @NotNull
    public String getCursorName() {
        return "fm_cursor_element_" + this.getInstanceIdentifier();
    }

}
