package de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.progressbar;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class OverlayProgressBarLayoutElement extends DeepCustomizationLayoutEditorElement {

    public OverlayProgressBarLayoutElement(@NotNull DeepCustomizationElement parentDeepCustomizationElement, @NotNull DeepCustomizationItem customizationItemInstance, @NotNull LayoutEditorScreen handler) {
        super(parentDeepCustomizationElement, customizationItemInstance, true, handler);
    }

    @Override
    public void init() {

        super.init();

        AdvancedButton toggleOriSizePosCalcButton = new AdvancedButton(0, 0, 0, 0, "", true, (button) -> {
            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
            if (this.getProgressBarItem().useOriginalSizeAndPosCalculation) {
                this.getProgressBarItem().useOriginalSizeAndPosCalculation = false;
                this.getProgressBarItem().orientation = "top-left";
                this.getProgressBarItem().posX = 30;
                this.getProgressBarItem().posY = 30;
            } else {
                this.getProgressBarItem().useOriginalSizeAndPosCalculation = true;
            }
            this.rightclickMenu.closeMenu();
            this.init();
        }) {
            @Override
            public void render(GuiGraphics p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (getProgressBarItem().useOriginalSizeAndPosCalculation) {
                    this.setMessage(I18n.get("drippyloadingscreen.deepcustomization.overlay.progress_bar.original_size_pos_calc.on"));
                } else {
                    this.setMessage(I18n.get("drippyloadingscreen.deepcustomization.overlay.progress_bar.original_size_pos_calc.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        toggleOriSizePosCalcButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.deepcustomization.overlay.progress_bar.original_size_pos_calc.desc"), "\n"));
        this.rightclickMenu.addContent(toggleOriSizePosCalcButton);

        this.rightclickMenu.addSeparator();

        AdvancedButton setColorButton = new AdvancedButton(0, 0, 0, 0, I18n.get("drippyloadingscreen.deepcustomization.overlay.progress_bar.set_color"), true, (press) -> {
            FMTextInputPopup p = new FMTextInputPopup(new Color(0,0,0,0), I18n.get("drippyloadingscreen.deepcustomization.overlay.progress_bar.set_color"), null, 240, (call) -> {
                if (call != null) {
                    if (call.replace(" ", "").equals("") || call.replace(" ", "").equalsIgnoreCase("#RRGGBB")) {
                        if (this.getProgressBarItem().hexColor != null) {
                            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                        }
                        this.getProgressBarItem().hexColor = null;
                        this.getProgressBarItem().hexColorString = "#RRGGBB";
                    } else {
                        if (!call.equalsIgnoreCase(this.getProgressBarItem().hexColorString)) {
                            Color c = RenderUtils.getColorFromHexString(call);
                            if (c != null) {
                                this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
                                this.getProgressBarItem().hexColorString = call;
                                this.getProgressBarItem().hexColor = c;
                            }
                        }
                    }
                }
            });
            if (this.getProgressBarItem().hexColorString != null) {
                p.setText(this.getProgressBarItem().hexColorString);
            }
            PopupHandler.displayPopup(p);
        });
        setColorButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.deepcustomization.overlay.progress_bar.set_color.desc"), "\n"));
        this.rightclickMenu.addContent(setColorButton);

    }

    @Override
    public void deepCustomizationPreInit() {
        super.deepCustomizationPreInit();
        if (!this.getProgressBarItem().useOriginalSizeAndPosCalculation) {
            this.stretchable = true;
            this.resizeable = true;
            this.orientationCanBeChanged = true;
            this.supportsAdvancedPositioning = true;
            this.supportsAdvancedSizing = true;
            this.dragable = true;
        }
        this.enableVisibilityRequirements = true;
    }

    public OverlayProgressBarItem getProgressBarItem() {
        return (OverlayProgressBarItem) this.object;
    }

    @Override
    public SimplePropertiesSection serializeItem() {

        OverlayProgressBarItem i = (OverlayProgressBarItem) this.object;
        SimplePropertiesSection sec = new SimplePropertiesSection();

        if (i.hexColor != null) {
            sec.addEntry("custom_color_hex", i.hexColorString);
        }

        sec.addEntry("original_pos_size_calculation", "" + this.getProgressBarItem().useOriginalSizeAndPosCalculation);

        return sec;

    }

}