package de.keksuccino.fancymenu.customization.element.elements.button.vanillawidget;

import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.HideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custombutton.ButtonElement;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class VanillaWidgetElement extends ButtonElement implements HideableElement {

    private static final Logger LOGGER = LogManager.getLogger();

    public WidgetMeta widgetMeta;
    public boolean vanillaButtonHidden = false;
    public int automatedButtonClicks = 0;
    protected boolean automatedButtonClicksDone = false;

    public VanillaWidgetElement(ElementBuilder<ButtonElement, ButtonEditorElement> builder) {
        super(builder);
        this.supportsRotation = false;
        this.supportsTilting = false;
    }

    @Override
    public void tick() {

        //Auto-click the vanilla button on menu load
        if (!isEditor() && !this.automatedButtonClicksDone && (this.automatedButtonClicks > 0)) {
            for (int i = 0; i < this.automatedButtonClicks; i++) {
                if (this.getWidget() != null) this.getWidget().onClick(this.getWidget().getX() + 1, this.getWidget().getY() + 1);
            }
            this.automatedButtonClicksDone = true;
        }

        super.tick();

    }

    @SuppressWarnings("all")
    @Override
    protected void renderElementWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        if (isEditor()) {
            //Only render button in editor
            super.renderElementWidget(graphics, mouseX, mouseY, partial);
        }
        if (this.anchorPoint == ElementAnchorPoints.VANILLA) {
            this.resetVanillaWidgetSizeAndPosition();
            this.mirrorVanillaWidgetSizeAndPosition();
        }
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return null;
    }

    @Override
    public void updateWidgetNavigatable() {
        //do nothing
    }

    @Override
    public void updateWidgetVisibility() {
        //Note: A patch in TitleScreen.class is needed to make the screen not permanently update the widget's alpha
        super.updateWidgetVisibility();
        if (this.getWidget() instanceof CustomizableWidget w) {
            boolean forceVisible = isEditor() || this.isCopyrightButton();
            if (this.vanillaButtonHidden) w.setHiddenFancyMenu(true);
            if (forceVisible) w.setHiddenFancyMenu(false);
        }
    }

    @Override
    public void updateWidgetPosition() {
        if (this.getWidget() instanceof CustomizableWidget w) {
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
    public void updateWidgetSize() {
        if (this.getWidget() instanceof CustomizableWidget w) {
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
    public void updateWidgetLabels() {
        if (this.getWidget() == null) return;
        ((CustomizableWidget)this.getWidget()).setCustomLabelFancyMenu((this.getLabel() != null) ? buildComponent(this.getLabel()) : null);
        ((CustomizableWidget)this.getWidget()).setHoverLabelFancyMenu((this.getHoverLabel() != null) ? buildComponent(this.getHoverLabel()) : null);
    }

    @Override
    public void updateWidgetActiveState() {
        if (this.activeStateSupplier.isEmpty()) return;
        super.updateWidgetActiveState();
    }

    @Override
    public @NotNull String getInstanceIdentifier() {
        if (this.widgetMeta != null) {
            return this.widgetMeta.getIdentifier().replace("vanillabtn:", "").replace("button_compatibility_id:", "");
        }
        return super.getInstanceIdentifier().replace("vanillabtn:", "").replace("button_compatibility_id:", "");
    }

    public void setVanillaWidget(WidgetMeta data, boolean mirrorWidgetSizeAndPos) {
        this.widgetMeta = data;
        this.setWidget(data.getWidget());
        if (this.baseWidth <= 0) this.baseWidth = data.width;
        if (this.baseHeight <= 0) this.baseHeight = data.height;
        if (mirrorWidgetSizeAndPos) this.mirrorVanillaWidgetSizeAndPosition();
    }

    public void mirrorVanillaWidgetSizeAndPosition() {
        this.mirrorVanillaWidgetSize();
        this.mirrorVanillaWidgetPosition();
    }

    public void mirrorVanillaWidgetSize() {
        if (this.getWidget() == null) return;
        this.baseWidth = this.getWidget().getWidth();
        this.baseHeight = this.getWidget().getHeight();
    }

    public void mirrorVanillaWidgetPosition() {
        if (this.getWidget() == null) return;
        this.posOffsetX = this.getWidget().getX();
        this.posOffsetY = this.getWidget().getY();
    }

    public void resetVanillaWidgetSizeAndPosition() {
        if (this.getWidget() instanceof CustomizableWidget w) {
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

    @Override
    public float getBaseOpacity() {
        float o = super.getBaseOpacity();
        if (this.isCopyrightButton() && (o < 0.4F)) o = 0.4F;
        return o;
    }

    public boolean isCopyrightButton() {
        if (this.widgetMeta == null) return false;
        String compId = ((UniqueWidget)this.widgetMeta.getWidget()).getWidgetIdentifierFancyMenu();
        return ((compId != null) && compId.equals("title_screen_copyright_button"));
    }

}
