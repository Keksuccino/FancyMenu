package de.keksuccino.fancymenu.customization.element.elements.button.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.widget.WidgetMeta;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IHideableElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonElement;
import de.keksuccino.fancymenu.mixin.mixins.client.IMixinScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class VanillaButtonElement extends ButtonElement implements IHideableElement {

    private static final Logger LOGGER = LogManager.getLogger();

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
        if (this.isHidden()) {
            Screen s = getScreen();
            if ((s != null) && (this.getButton() != null)) {
                ((IMixinScreen)s).invokeRemoveWidgetFancyMenu(this.getButton());
            }
        }
        if (this.isButtonVisible() || isEditor()) {
            super.render(pose, mouseX, mouseY, partial);
        }
    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return null;
    }

    @Override
    protected void renderTick() {

        super.renderTick();

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
    public int getAbsoluteX() {
        if ((this.button != null) && (this.anchorPoint == ElementAnchorPoints.VANILLA)) {
            int bX = this.posOffsetX;
            this.posOffsetX = this.originalX;
            int x = super.getAbsoluteX();
            this.posOffsetX = bX;
            return x;
        }
        return super.getAbsoluteX();
    }

    @Override
    public int getAbsoluteY() {
        if ((this.button != null) && (this.anchorPoint == ElementAnchorPoints.VANILLA)) {
            int bY = this.posOffsetY;
            this.posOffsetY = this.originalY;
            int y = super.getAbsoluteY();
            this.posOffsetY = bY;
            return y;
        }
        return super.getAbsoluteY();
    }

    @Override
    public int getAbsoluteWidth() {
        if ((this.button != null) && ((this.anchorPoint == ElementAnchorPoints.VANILLA) || (this.baseWidth == 0))) {
            this.baseWidth = this.originalWidth;
            int w = super.getAbsoluteWidth();
            this.baseWidth = 0;
            return w;
        }
        return super.getAbsoluteWidth();
    }

    @Override
    public int getAbsoluteHeight() {
        if ((this.button != null) && ((this.anchorPoint == ElementAnchorPoints.VANILLA) || (this.baseHeight == 0))) {
            this.baseHeight = this.originalHeight;
            int h = super.getAbsoluteHeight();
            this.baseHeight = 0;
            return h;
        }
        return super.getAbsoluteHeight();
    }

    @Override
    public @NotNull String getInstanceIdentifier() {
        if (this.widgetMeta != null) {
            return "vanillabtn:" + this.widgetMeta.getIdentifier();
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

    //TODO remove debug
    public static VanillaButtonElement singleplayerButtonInstance = null;

    public void setVanillaButton(WidgetMeta data) {
        //TODO remove debug
        if (this == singleplayerButtonInstance) {
            LOGGER.info("??????????????????? META OF SP BUTTON INSTANCE SET AGAIN!!!");
        }
        if (data.getIdentifier().contains("singleplayer_button")) {
//            LOGGER.info("!!!!!!!!!!!!!!!!!!!!!! META SET! SINGLEPLAYER VANILLA BUTTON INSTANCE IS: " + this + " | ANCHOR: " + this.anchorPoint.getName());
            if (this.anchorPoint == ElementAnchorPoints.TOP_LEFT) {
                LOGGER.info("??????????????????????? FINAL SP BUTTON INSTANCE FOUND: " + this);
                singleplayerButtonInstance = this;
            }
        }
        //----------------
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
