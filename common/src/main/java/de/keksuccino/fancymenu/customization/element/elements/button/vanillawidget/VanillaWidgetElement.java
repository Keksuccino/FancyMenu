package de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IHideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class VanillaWidgetElement extends ButtonElement implements IHideableElement {

    //TODO FIXEN: Vanilla Widgets in modern screens (create world, etc.) broken
    //TODO FIXEN: Vanilla Widgets in modern screens (create world, etc.) broken
    //TODO FIXEN: Vanilla Widgets in modern screens (create world, etc.) broken
    //TODO FIXEN: Vanilla Widgets in modern screens (create world, etc.) broken

    private static final Logger LOGGER = LogManager.getLogger();

    public WidgetMeta widgetMeta;
    public boolean vanillaButtonHidden = false;
    public int automatedButtonClicks = 0;
    protected boolean automatedButtonClicksDone = false;

    public VanillaWidgetElement(ElementBuilder<ButtonElement, ButtonEditorElement> builder) {
        super(builder);
    }

    @SuppressWarnings("all")
    @Override
    protected void renderElementWidget(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        ((CustomizableWidget)this.button).setHiddenFancyMenu((isEditor() || this.isCopyrightButton()) ? false : this.vanillaButtonHidden);
        super.renderElementWidget(pose, mouseX, mouseY, partial);
        if (this.anchorPoint == ElementAnchorPoints.VANILLA) {
            this.resetVanillaWidgetSizeAndPosition();
            this.mirrorVanillaButtonSizeAndPosition();
        }
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return null;
    }

    @Override
    protected void updateWidgetPosition() {
        if (this.getButton() instanceof CustomizableWidget w) {
            if (this.anchorPoint != ElementAnchorPoints.VANILLA) {
                w.setCustomXFancyMenu(this.getAbsoluteX());
                w.setCustomYFancyMenu(this.getAbsoluteY());
            } else {
                w.setCustomXFancyMenu(null);
                w.setCustomYFancyMenu(null);
            }
        }
    }

    @Override
    protected void updateWidgetSize() {
        if (this.getButton() instanceof CustomizableWidget w) {
            if (this.anchorPoint != ElementAnchorPoints.VANILLA) {
                w.setCustomWidthFancyMenu(this.getAbsoluteWidth());
                w.setCustomHeightFancyMenu(this.getAbsoluteHeight());
            } else {
                w.setCustomWidthFancyMenu(null);
                w.setCustomHeightFancyMenu(null);
            }
        }
    }

    @Override
    protected void renderTick() {

        super.renderTick();

        if (this.button == null) return;

        //Auto-click the vanilla button on menu load
        if (!isEditor() && !this.automatedButtonClicksDone && (this.automatedButtonClicks > 0)) {
            for (int i = 0; i < this.automatedButtonClicks; i++) {
                this.button.onClick(this.button.getX() + 1, this.button.getY() + 1);
            }
            this.automatedButtonClicksDone = true;
        }

    }

    @Override
    protected void updateLabels() {
        if (this.button == null) return;
        ((CustomizableWidget)this.button).setCustomLabelFancyMenu((this.label != null) ? buildComponent(this.label) : null);
        ((CustomizableWidget)this.button).setHoverLabelFancyMenu((this.hoverLabel != null) ? buildComponent(this.hoverLabel) : null);
    }

    @Override
    public @NotNull String getInstanceIdentifier() {
        if (this.widgetMeta != null) {
            return "vanillabtn:" + this.widgetMeta.getIdentifier();
        }
        return super.getInstanceIdentifier();
    }

    public void setVanillaButton(WidgetMeta data, boolean mirrorButtonSizeAndPos) {
        this.widgetMeta = data;
        this.button = data.getWidget();
        if (mirrorButtonSizeAndPos) this.mirrorVanillaButtonSizeAndPosition();
    }

    public void mirrorVanillaButtonSizeAndPosition() {
        this.mirrorVanillaButtonSize();
        this.mirrorVanillaButtonPosition();
    }

    public void mirrorVanillaButtonSize() {
        if (this.getButton() == null) return;
        this.baseWidth = this.getButton().getWidth();
        this.baseHeight = this.getButton().getHeight();
    }

    public void mirrorVanillaButtonPosition() {
        if (this.getButton() == null) return;
        this.posOffsetX = this.getButton().getX();
        this.posOffsetY = this.getButton().getY();
    }

    public void resetVanillaWidgetSizeAndPosition() {
        if (this.getButton() instanceof CustomizableWidget w) {
            w.resetWidgetSizeAndPositionFancyMenu();
        }
    }

    @Override
    public boolean isHidden() {
        if (this.isCopyrightButton()) return false;
        return this.vanillaButtonHidden;
    }

    @Override
    public void setHidden(boolean hidden) {
        if (this.isCopyrightButton()) hidden = false;
        this.vanillaButtonHidden = hidden;
    }

    public boolean isCopyrightButton() {
        if (this.widgetMeta == null) return false;
        String compId = this.widgetMeta.getUniversalIdentifier();
        return ((compId != null) && compId.equals("mc_titlescreen_copyright_button"));
    }

}
