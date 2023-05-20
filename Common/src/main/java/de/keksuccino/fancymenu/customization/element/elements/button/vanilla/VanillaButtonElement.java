package de.keksuccino.fancymenu.customization.element.elements.button.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IHideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonElement;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class VanillaButtonElement extends ButtonElement implements IHideableElement {

    private static final Logger LOGGER = LogManager.getLogger();

    // IMPORTANT:
    // When adding new fields to this class or its superclasses, don't forget to add them to the stackElements() method in this class!

    public WidgetMeta widgetMeta;
    public Component originalLabel;
    public int originalX;
    public int originalY;
    public int originalWidth;
    public int originalHeight;

    public boolean vanillaButtonHidden = false;
    public int automatedButtonClicks = 0;

    protected boolean automatedButtonClicksDone = false;

    public VanillaButtonElement(ElementBuilder<ButtonElement, ButtonEditorElement> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        if (this.isButtonVisible() || isEditor()) {
            super.render(pose, mouseX, mouseY, partial);
        }
    }

    @Override
    protected void tick() {

        super.tick();

        if (this.button == null) return;

        //Restore original label if custom label is null
        if ((this.label == null) && (this.originalLabel != null) && ((this.hoverLabel == null) || !this.button.isHoveredOrFocused())) {
            this.button.setMessage(this.originalLabel);
        }

        //Auto-click the vanilla button on menu load
        if (!isEditor() && !this.automatedButtonClicksDone && (this.automatedButtonClicks > 0)) {
            for (int i = 0; i < this.automatedButtonClicks; i++) {
                this.button.onClick(this.button.getX() + 1, this.button.getY() + 1);
            }
            this.automatedButtonClicksDone = true;
        }

    }

    @Override
    public int getX() {
        if ((this.button != null) && (this.anchorPoint == ElementAnchorPoints.VANILLA)) {
            int bX = this.baseX;
            this.baseX = this.originalX;
            int x = super.getX();
            this.baseX = bX;
            return x;
        }
        return super.getX();
    }

    @Override
    public int getY() {
        if ((this.button != null) && (this.anchorPoint == ElementAnchorPoints.VANILLA)) {
            int bY = this.baseY;
            this.baseY = this.originalY;
            int y = super.getY();
            this.baseY = bY;
            return y;
        }
        return super.getY();
    }

    @Override
    public int getWidth() {
        if ((this.button != null) && ((this.anchorPoint == ElementAnchorPoints.VANILLA) || (this.width == 0))) {
            this.width = this.originalWidth;
            int w = super.getWidth();
            this.width = 0;
            return w;
        }
        return super.getWidth();
    }

    @Override
    public int getHeight() {
        if ((this.button != null) && ((this.anchorPoint == ElementAnchorPoints.VANILLA) || (this.height == 0))) {
            this.height = this.originalHeight;
            int h = super.getHeight();
            this.height = 0;
            return h;
        }
        return super.getHeight();
    }

    @Override
    public String getInstanceIdentifier() {
        if (this.widgetMeta != null) {
            if (this.widgetMeta.getCompatibilityIdentifier() != null) {
                return "vanillabtn:" + this.widgetMeta.getCompatibilityIdentifier();
            } else {
                return "vanillabtn:" + this.widgetMeta.getLongIdentifier();
            }
        }
        return super.getInstanceIdentifier();
    }

    public boolean isButtonVisible() {
        if (!this.loadingRequirementsMet()) {
            return false;
        }
        if (this.vanillaButtonHidden) {
            return false;
        }
        if (!this.visible) {
            return false;
        }
        if (this.button != null) {
            return this.button.visible;
        }
        return true;
    }

    public void setVanillaButton(WidgetMeta data) {
        this.widgetMeta = data;
        this.button = data.getWidget();
        this.originalLabel = this.button.getMessage();
        this.originalX = this.button.x;
        this.originalY = this.button.y;
        this.originalWidth = this.button.getWidth();
        this.originalHeight = this.button.getHeight();
    }

    @Override
    public boolean isHidden() {
        return this.vanillaButtonHidden;
    }

    @Override
    public void setHidden(boolean hidden) {
        this.vanillaButtonHidden = hidden;
    }

}
