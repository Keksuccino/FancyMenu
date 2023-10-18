package de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.logo;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayoutEditorElement;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.StringUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;

public class OverlayLogoLayoutElement extends DeepCustomizationLayoutEditorElement {

    public OverlayLogoLayoutElement(@NotNull DeepCustomizationElement parentDeepCustomizationElement, @NotNull DeepCustomizationItem customizationItemInstance, @NotNull LayoutEditorScreen handler) {
        super(parentDeepCustomizationElement, customizationItemInstance, true, handler);
    }

    @Override
    public void init() {

        super.init();

        AdvancedButton toggleOriSizePosCalcButton = new AdvancedButton(0, 0, 0, 0, "", true, (button) -> {
            this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
            if (this.getLogoItem().useOriginalSizeAndPosCalculation) {
                this.getLogoItem().useOriginalSizeAndPosCalculation = false;
                this.getLogoItem().orientation = "top-left";
                this.getLogoItem().posX = 30;
                this.getLogoItem().posY = 30;
            } else {
                this.getLogoItem().useOriginalSizeAndPosCalculation = true;
            }
            this.rightclickMenu.closeMenu();
            this.init();
        }) {
            @Override
            public void render(GuiGraphics p_93657_, int p_93658_, int p_93659_, float p_93660_) {
                if (getLogoItem().useOriginalSizeAndPosCalculation) {
                    this.setMessage(I18n.get("drippyloadingscreen.deepcustomization.overlay.logo.original_size_pos_calc.on"));
                } else {
                    this.setMessage(I18n.get("drippyloadingscreen.deepcustomization.overlay.logo.original_size_pos_calc.off"));
                }
                super.render(p_93657_, p_93658_, p_93659_, p_93660_);
            }
        };
        toggleOriSizePosCalcButton.setDescription(StringUtils.splitLines(I18n.get("drippyloadingscreen.deepcustomization.overlay.logo.original_size_pos_calc.desc"), "\n"));
        this.rightclickMenu.addContent(toggleOriSizePosCalcButton);

    }

    @Override
    public void deepCustomizationPreInit() {
        super.deepCustomizationPreInit();
        if (!this.getLogoItem().useOriginalSizeAndPosCalculation) {
            this.stretchable = true;
            this.resizeable = true;
            this.orientationCanBeChanged = true;
            this.supportsAdvancedPositioning = true;
            this.supportsAdvancedSizing = true;
            this.dragable = true;
        }
        this.enableVisibilityRequirements = true;
    }

    public OverlayLogoItem getLogoItem() {
        return (OverlayLogoItem) this.object;
    }

    @Override
    public SimplePropertiesSection serializeItem() {

        SimplePropertiesSection sec = new SimplePropertiesSection();

        sec.addEntry("original_pos_size_calculation", "" + this.getLogoItem().useOriginalSizeAndPosCalculation);

        return sec;

    }

}