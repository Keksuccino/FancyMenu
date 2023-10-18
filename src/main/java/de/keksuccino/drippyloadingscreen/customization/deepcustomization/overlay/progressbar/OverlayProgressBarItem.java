package de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.progressbar;

import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.drippyloadingscreen.mixin.mixins.client.IMixinLoadingOverlay;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.awt.*;

public class OverlayProgressBarItem extends DeepCustomizationItem {

    public String hexColorString = "#RRGGBB";
    public Color hexColor = null;
    public boolean useOriginalSizeAndPosCalculation = true;

    public OverlayProgressBarItem(DeepCustomizationElement parentElement, PropertiesSection item) {

        super(parentElement, item);

        String hex = item.getEntryValue("custom_color_hex");
        if ((hex != null) && !hex.toUpperCase().replace(" ", "").equals("#RRGGBB") && !hex.replace(" ", "").equals("")) {
            Color c = RenderUtils.getColorFromHexString(hex);
            if (c != null) {
                this.hexColorString = hex;
                this.hexColor = c;
            }
        }

        String oriPosSizeCalc = item.getEntryValue("original_pos_size_calculation");
        if ((oriPosSizeCalc != null) && oriPosSizeCalc.equals("false")) {
            this.useOriginalSizeAndPosCalculation = false;
        }

    }

    @Override
    public void render(GuiGraphics graphics, Screen menu) {

        Minecraft mc = Minecraft.getInstance();
        int i = mc.getWindow().getGuiScaledWidth();
        double d1 = Math.min((double)mc.getWindow().getGuiScaledWidth() * 0.75D, mc.getWindow().getGuiScaledHeight()) * 0.25D;
        double d0 = d1 * 4.0D;
        int j1 = (int)(d0 * 0.5D);
        int k1 = (int)((double)mc.getWindow().getGuiScaledHeight() * 0.8325D);
        float currentProgress = 0.5F;
        if (!isEditorActive() && (Minecraft.getInstance().getOverlay() != null) && (Minecraft.getInstance().getOverlay() instanceof LoadingOverlay)) {
            currentProgress = ((IMixinLoadingOverlay)Minecraft.getInstance().getOverlay()).getCurrentProgressDrippy();
        }

        int posX = i / 2 - j1;
        int posY = k1 - 5;
        int w = j1 * 2;
        int h = 10;

        if (this.useOriginalSizeAndPosCalculation) {
            this.posX = posX;
            this.posY = posY;
            this.width = w;
            this.height = h;
            this.orientation = "top-left";
        } else {
            posX = this.getPosX(menu);
            posY = this.getPosY(menu);
            w = this.getWidth();
            h = this.getHeight();
        }

        if (this.shouldRender()) {
            this.drawProgressBar(graphics, posX, posY, posX + w, posY + h, this.opacity, currentProgress);
        }

    }

    @Override
    public boolean shouldRender() {
        return super.shouldRender() && !this.hidden;
    }

    private void drawProgressBar(GuiGraphics graphics, int xMin, int yMin, int xMax, int yMax, float opacity, float currentProgress) {
        int i = Mth.ceil((float)(xMax - xMin - 2) * currentProgress);
        int j = Math.round(opacity * 255.0F);
        int k = FastColor.ARGB32.color(j, 255, 255, 255);
        if (this.hexColor != null) {
            k = FastColor.ARGB32.color(j, this.hexColor.getRed(), this.hexColor.getGreen(), this.hexColor.getBlue());
        }
        graphics.fill(xMin + 2, yMin + 2, xMin + i, yMax - 2, k);
        graphics.fill(xMin + 1, yMin, xMax - 1, yMin + 1, k);
        graphics.fill(xMin + 1, yMax, xMax - 1, yMax - 1, k);
        graphics.fill(xMin, yMin, xMin + 1, yMax, k);
        graphics.fill(xMax, yMin, xMax - 1, yMax, k);
    }

}