package de.keksuccino.fancymenu.customization.deep;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.rendering.ui.popup.FMNotificationPopup;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.properties.PropertyContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEditorDeepElementOLD extends AbstractEditorElement {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/DeepCustomizationLayoutEditorElement");

    public final DeepElementBuilder parentDeepElementBuilder;

    protected int dragMouseX = -1000;
    protected int dragMouseY = -1000;

    public AbstractEditorDeepElementOLD(@Nonnull DeepElementBuilder parentDeepElementBuilder, @Nonnull AbstractDeepElement customizationItemInstance, boolean destroyable, @Nonnull LayoutEditorScreen handler, boolean doInit) {
        super(customizationItemInstance, destroyable, handler, doInit);
        this.parentDeepElementBuilder = parentDeepElementBuilder;
    }

    public AbstractEditorDeepElementOLD(@Nonnull DeepElementBuilder parentDeepElementBuilder, @Nonnull AbstractDeepElement customizationItemInstance, boolean destroyable, @Nonnull LayoutEditorScreen handler) {
        super(customizationItemInstance, destroyable, handler);
        this.parentDeepElementBuilder = parentDeepElementBuilder;
    }

    public void deepCustomizationPreInit() {
        this.stretchable = false;
        this.orderable = false;
        this.fadeable = false;
        this.copyable = false;
        this.delayable = false;
        this.resizeable = false;
        this.dragable = false;
        this.orientationCanBeChanged = false;
        this.enableVisibilityRequirements = false;
        this.enableElementIdCopyButton = false;
        this.supportsAdvancedPositioning = false;
        this.supportsAdvancedSizing = false;
    }

    @Override
    public void init() {
        this.deepCustomizationPreInit();
        super.init();
    }

    @Override
    public void render(PoseStack matrix, int mouseX, int mouseY) {

        if (!this.getDeepCustomizationItem().deepElementHidden) {
            super.render(matrix, mouseX, mouseY);
        } else {
            if (this.editor.isFocused(this)) {
                this.editor.setObjectFocused(this, false, true);
            }
            this.hovered = false;
            if (this.rightClickContextMenu.isOpen()) {
                this.rightClickContextMenu.closeMenu();
            }
        }

        this.handleMoveWarning();

    }

    protected void handleMoveWarning() {
        if (!this.dragable) {
            if (MouseInput.isLeftMouseDown() && this.editor.isFocused(this) && this.hovered) {
                int mX = MouseInput.getMouseX();
                int mY = MouseInput.getMouseY();
                if ((this.dragMouseX == -1000) && (this.dragMouseY == -1000)) {
                    this.dragMouseX = mX;
                    this.dragMouseY = mY;
                }
                if ((mX != this.dragMouseX) || (mY != this.dragMouseY)) {
                    if (FancyMenu.getConfig().getOrDefault("showvanillamovewarning", true)) {
                        FMNotificationPopup p;
                        if (this.orientationCanBeChanged) {
                            p = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.element.vanilla.orientation_needed"), "%n%"));
                        } else {
                            p = new FMNotificationPopup(300, new Color(0, 0, 0, 0), 240, null, StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.element.moving_not_allowed"), "%n%"));
                        }
                        PopupHandler.displayPopup(p);
                    }
                }
                this.dragMouseX = mX;
                this.dragMouseY = mY;
            } else {
                this.dragMouseX = -1000;
                this.dragMouseY = -1000;
            }
        }
    }

    @Override
    public boolean isHovered() {
        if (this.getDeepCustomizationItem().deepElementHidden) {
            return false;
        }
        return super.isHovered();
    }

    @Override
    public boolean isLeftClicked() {
        if (this.getDeepCustomizationItem().deepElementHidden) {
            return false;
        }
        return super.isLeftClicked();
    }

    @Override
    public boolean isRightClicked() {
        if (this.getDeepCustomizationItem().deepElementHidden) {
            return false;
        }
        return super.isRightClicked();
    }

    @Override
    protected void renderBorder(PoseStack matrix, int mouseX, int mouseY) {
        String cachedOri = this.element.anchorPoint;
        if (!this.orientationCanBeChanged) {
            this.element.anchorPoint = "original";
        }
        super.renderBorder(matrix, mouseX, mouseY);
        this.element.anchorPoint = cachedOri;
    }

    @Override
    public void destroyElement() {
        if (this.isDestroyable()) {
            if (FancyMenu.getConfig().getOrDefault("editordeleteconfirmation", true)) {
                FMNotificationPopup pop = new FMNotificationPopup(300, new Color(0,0,0,0), 240, null, StringUtils.splitLines(Locals.localize("fancymenu.helper.editor.element.vanilla.delete.confirm"), "%n%"));
                PopupHandler.displayPopup(pop);
            }
            if (!this.getDeepCustomizationItem().deepElementHidden) {
                this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
            }
            this.getDeepCustomizationItem().deepElementHidden = true;
            this.editor.setObjectFocused(this, false, true);
            this.resetElementStates();
        }
    }

    public AbstractDeepElement getDeepCustomizationItem() {
        return (AbstractDeepElement) this.element;
    }

    public abstract SimplePropertyContainer serializeItem();

    @Override
    public List<PropertyContainer> getProperties() {
        List<PropertyContainer> l = new ArrayList<>();
        PropertyContainer sec = this.serializeItem();
        if (sec == null) {
            sec = new SimplePropertyContainer();
        }
        if (sec.hasProperty("action")) {
            LOGGER.warn("WARN: Entry key 'action' for serialized customization item instances is reserved by the system. Overriding entry!");
            sec.removeProperty("action");
        }
        sec.putProperty("action", "deep_customization_element:" + this.parentDeepElementBuilder.getIdentifier());
        sec.putProperty("actionid", this.element.getInstanceIdentifier());
        if (this.element.delayAppearance) {
            sec.putProperty("delayappearance", "true");
            sec.putProperty("delayappearanceeverytime", "" + this.element.delayAppearanceEverytime);
            sec.putProperty("delayappearanceseconds", "" + this.element.appearanceDelayInSeconds);
            if (this.element.fadeIn) {
                sec.putProperty("fadein", "true");
                sec.putProperty("fadeinspeed", "" + this.element.fadeInSpeed);
            }
        }
        sec.putProperty("x", "" + this.element.baseX);
        sec.putProperty("y", "" + this.element.baseY);
        sec.putProperty("orientation", this.element.anchorPoint);
        if (this.element.anchorPoint.equals("element") && (this.element.anchorPointElementIdentifier != null)) {
            sec.putProperty("orientation_element", this.element.anchorPointElementIdentifier);
        }
        if (this.stretchX) {
            sec.putProperty("x", "0");
            sec.putProperty("width", "%guiwidth%");
        } else {
            sec.putProperty("x", "" + this.element.baseX);
            sec.putProperty("width", "" + this.element.getWidth());
        }
        if (this.stretchY) {
            sec.putProperty("y", "0");
            sec.putProperty("height", "%guiheight%");
        } else {
            sec.putProperty("y", "" + this.element.baseY);
            sec.putProperty("height", "" + this.element.getHeight());
        }
        sec.putProperty("hidden", "" + this.getDeepCustomizationItem().deepElementHidden);
        this.serializeLoadingRequirementsTo(sec);
        l.add(sec);
        return l;
    }

    public class SimplePropertyContainer extends PropertyContainer {

        public SimplePropertyContainer() {
            super("customization");
        }

    }

}
