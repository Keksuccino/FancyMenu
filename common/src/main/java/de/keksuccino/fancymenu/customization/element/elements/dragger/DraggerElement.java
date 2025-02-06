package de.keksuccino.fancymenu.customization.element.elements.dragger;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.List;

public class DraggerElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public final DraggerWidget widget;
    public int userDragOffsetX = 0;
    public int userDragOffsetY = 0;
    public boolean saveDragOffset = true;
    protected boolean leftMouseDownOnElement = false;
    protected int mouseDownX = 0;
    protected int mouseDownY = 0;
    protected int mouseDownOffsetX = 0;
    protected int mouseDownOffsetY = 0;
    protected boolean firstTick = true;

    public DraggerElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.widget = new DraggerWidget(0,0,0,0, this::onDraggerElementDragged, this::onDraggerElementClickedOrReleased);
    }

    protected void onDraggerElementDragged(double mouseX, double mouseY, double dragX, double dragY) {
        if (!isEditor() && this.leftMouseDownOnElement) {
            int draggingDiffX = (int) (mouseX - this.mouseDownX);
            int draggingDiffY = (int) (mouseY - this.mouseDownY);
            if ((draggingDiffX != 0) || (draggingDiffY != 0)) {
                int xCached = this.userDragOffsetX;
                int yCached = this.userDragOffsetY;
                this.userDragOffsetX = this.mouseDownOffsetX + draggingDiffX;
                this.userDragOffsetY = this.mouseDownOffsetY + draggingDiffY;
                if (this.stayOnScreen) {
                    if (!this.checkIsValidStayOnScreenX(this._getAbsoluteX())) {
                        this.userDragOffsetX = xCached;
                    }
                    if (!this.checkIsValidStayOnScreenY(this._getAbsoluteY())) {
                        this.userDragOffsetY = yCached;
                    }
                }
            }
        }
    }

    protected void onDraggerElementClickedOrReleased(double mouseX, double mouseY, boolean released) {
        if (!isEditor() && ((IMixinAbstractWidget)this.widget).getIsHoveredFancyMenu()) {
            this.leftMouseDownOnElement = !released;
            this.mouseDownX = (int) mouseX;
            this.mouseDownY = (int) mouseY;
            this.mouseDownOffsetX = this.userDragOffsetX;
            this.mouseDownOffsetY = this.userDragOffsetY;
        } else {
            this.leftMouseDownOnElement = false;
        }
        if (released) {
            if (this.saveDragOffset) {
                DraggerElementHandler.putMeta(this.getInstanceIdentifier(), this.userDragOffsetX, this.userDragOffsetY);
            } else {
                DraggerElementHandler.putMeta(this.getInstanceIdentifier(), 0, 0);
            }
        }
    }

    @Override
    public void tick() {

        super.tick();

        if (this.firstTick) {
            this.firstTick = false;
            if (this.saveDragOffset) {
                DraggerElementHandler.DraggerMeta meta = DraggerElementHandler.getMeta(this.getInstanceIdentifier());
                if (meta != null) {
                    this.userDragOffsetX = meta.offsetX;
                    this.userDragOffsetY = meta.offsetY;
                }
            }
        }

    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        this.widget.visible = this.shouldRender();

        if (this.shouldRender()) {

            int x = this.getAbsoluteX();
            int y = this.getAbsoluteY();
            int w = this.getAbsoluteWidth();
            int h = this.getAbsoluteHeight();

            this.widget.x = (x);
            this.widget.y = (y);
            this.widget.setWidth(w);
            ((IMixinAbstractWidget)this.widget).setHeightFancyMenu(h);
            this.widget.render(graphics.pose(), mouseX, mouseY, partial);

            if (isEditor()) {
                RenderSystem.enableBlend();
                graphics.fill(x, y, x + w, y + h, this.inEditorColor.getColorInt());
                graphics.enableScissor(x, y, x + w, y + h);
                graphics.drawCenteredString(Minecraft.getInstance().font, this.getDisplayName(), x + (w / 2), y + (h / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
                graphics.disableScissor();
                RenderingUtils.resetShaderColor(graphics);
            }

        } else {
            this.leftMouseDownOnElement = false;
        }

    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return List.of(this.widget);
    }

    @Override
    public int getAbsoluteX() {
        int i = this._getAbsoluteX();
        if (this.stayOnScreen) {
            if (i < STAY_ON_SCREEN_EDGE_ZONE_SIZE) {
                i = STAY_ON_SCREEN_EDGE_ZONE_SIZE;
            }
            if (i > (getScreenWidth() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteWidth())) {
                i = getScreenWidth() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteWidth();
            }
        }
        return i;
    }

    protected int _getAbsoluteX() {
        return super.getAbsoluteX() + ((!isEditor()) ? this.userDragOffsetX : 0);
    }

    @Override
    public int getAbsoluteY() {
        int i = this._getAbsoluteY();
        if (this.stayOnScreen) {
            if (i < STAY_ON_SCREEN_EDGE_ZONE_SIZE) {
                i = STAY_ON_SCREEN_EDGE_ZONE_SIZE;
            }
            if (i > (getScreenHeight() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteHeight())) {
                i = getScreenHeight() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteHeight();
            }
        }
        return i;
    }

    protected int _getAbsoluteY() {
        return super.getAbsoluteY() + ((!isEditor()) ? this.userDragOffsetY : 0);
    }

    public boolean checkIsValidStayOnScreenX(int x) {
        if (this.stayOnScreen) {
            if (x < STAY_ON_SCREEN_EDGE_ZONE_SIZE) {
                return false;
            }
            if (x > (getScreenWidth() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteWidth())) {
                return false;
            }
        }
        return true;
    }

    public boolean checkIsValidStayOnScreenY(int y) {
        if (this.stayOnScreen) {
            if (y < STAY_ON_SCREEN_EDGE_ZONE_SIZE) {
                return false;
            }
            if (y > (getScreenHeight() - STAY_ON_SCREEN_EDGE_ZONE_SIZE - this.getAbsoluteHeight())) {
                return false;
            }
        }
        return true;
    }

}
