package de.keksuccino.fancymenu.customization.element.elements.button.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.button.ButtonData;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.element.IActionExecutorElement;
import de.keksuccino.fancymenu.customization.element.anchor.ElementAnchorPoints;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonEditorElement;
import de.keksuccino.fancymenu.customization.element.elements.button.custom.ButtonElement;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class VanillaButtonElement extends ButtonElement implements IActionExecutorElement {

    private static final Logger LOGGER = LogManager.getLogger();

    // IMPORTANT:
    // When adding new fields to this class or its superclasses, don't forget to add them to the stackElements() method in this class!

    public String vanillaButtonIdentifier;
    public ButtonData buttonData;
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
        if (this.buttonData != null) {
            if (this.buttonData.getCompatibilityId() != null) {
                return "vanillabtn:" + this.buttonData.getCompatibilityId();
            } else {
                return "vanillabtn:" + this.buttonData.getId();
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

    public void setVanillaButton(ButtonData data) {
        this.buttonData = data;
        this.button = data.getButton();
        this.originalLabel = this.button.getMessage();
        this.originalX = this.button.x;
        this.originalY = this.button.y;
        this.originalWidth = this.button.getWidth();
        this.originalHeight = this.button.getHeight();
    }

    @SuppressWarnings("all")
    @NotNull
    public static VanillaButtonElement stackElements(@NotNull VanillaButtonElement... elements) {

        VanillaButtonElement stack = VanillaButtonElementBuilder.INSTANCE.buildDefaultInstance();

        for (VanillaButtonElement e : elements) {

            //Don't stack cached button stuff, just the plain customization part + identifier

            //VanillaButtonElement stuff
            if (e.vanillaButtonIdentifier != null) {
                stack.vanillaButtonIdentifier = e.vanillaButtonIdentifier;
            }
            if (e.vanillaButtonHidden) {
                stack.vanillaButtonHidden = true;
            }
            if (e.automatedButtonClicks != 0) {
                stack.automatedButtonClicks = e.automatedButtonClicks;
            }

            //ButtonElement stuff
            if (e.clickSound != null) {
                stack.clickSound = e.clickSound;
            }
            if (e.hoverSound != null) {
                stack.hoverSound = e.hoverSound;
            }
            if (e.label != null) {
                stack.label = e.label;
            }
            if (e.hoverLabel != null) {
                stack.hoverLabel = e.hoverLabel;
            }
            if (e.tooltip != null) {
                stack.tooltip = e.tooltip;
            }
            if (e.backgroundTextureNormal != null) {
                stack.backgroundTextureNormal = e.backgroundTextureNormal;
            }
            if (e.backgroundTextureHover != null) {
                stack.backgroundTextureHover = e.backgroundTextureHover;
            }
            if (e.backgroundAnimationNormal != null) {
                stack.backgroundAnimationNormal = e.backgroundAnimationNormal;
            }
            if (e.backgroundAnimationHover != null) {
                stack.backgroundAnimationHover = e.backgroundAnimationHover;
            }
            if (!e.loopBackgroundAnimations) {
                stack.loopBackgroundAnimations = false;
            }
            if (!e.restartBackgroundAnimationsOnHover) {
                stack.restartBackgroundAnimationsOnHover = false;
            }

            //AbstractElement stuff
            if (e.anchorPoint != ElementAnchorPoints.VANILLA) {
                stack.anchorPoint = e.anchorPoint;
            }
            if (e.anchorPointElementIdentifier != null) {
                stack.anchorPointElementIdentifier = e.anchorPointElementIdentifier;
            }
            if (e.baseX != 0) {
                stack.baseX = e.baseX;
            }
            if (e.baseY != 0) {
                stack.baseY = e.baseY;
            }
            if (e.width != 0) {
                stack.width = e.width;
            }
            if (e.height != 0) {
                stack.height = e.height;
            }
            if (e.advancedX != null) {
                stack.advancedX = e.advancedX;
            }
            if (e.advancedY != null) {
                stack.advancedY = e.advancedY;
            }
            if (e.advancedWidth != null) {
                stack.advancedWidth = e.advancedWidth;
            }
            if (e.advancedHeight != null) {
                stack.advancedHeight = e.advancedHeight;
            }
            if (e.stretchX) {
                stack.stretchX = true;
            }
            if (e.stretchY) {
                stack.stretchY = true;
            }
            if (e.appearanceDelay != AppearanceDelay.NO_DELAY) {
                stack.appearanceDelay = e.appearanceDelay;
            }
            if (e.appearanceDelayInSeconds != 1.0F) {
                stack.appearanceDelayInSeconds = e.appearanceDelayInSeconds;
            }
            if (e.fadeIn) {
                stack.fadeIn = true;
            }
            if (e.fadeInSpeed != 1.0F) {
                stack.fadeInSpeed = e.fadeInSpeed;
            }

        }

        return stack;

    }

}
