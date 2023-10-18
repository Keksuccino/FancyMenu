package de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.logo;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class OverlayLogoItem extends DeepCustomizationItem {

    private static final ResourceLocation MOJANG_STUDIOS_LOGO_LOCATION = new ResourceLocation("textures/gui/title/mojangstudios.png");

    public boolean useOriginalSizeAndPosCalculation = true;

    public OverlayLogoItem(DeepCustomizationElement parentElement, PropertiesSection item) {

        super(parentElement, item);

        String oriPosSizeCalc = item.getEntryValue("original_pos_size_calculation");
        if ((oriPosSizeCalc != null) && oriPosSizeCalc.equals("false")) {
            this.useOriginalSizeAndPosCalculation = false;
        }

    }

    @Override
    public void render(GuiGraphics graphics, Screen menu) {

        Minecraft mc = Minecraft.getInstance();
        int centerX = (int) ((double) mc.getWindow().getGuiScaledWidth() * 0.5D);
        int centerY = (int) ((double) mc.getWindow().getGuiScaledHeight() * 0.5D);
        double logoHeight = Math.min((double) mc.getWindow().getGuiScaledWidth() * 0.75D, mc.getWindow().getGuiScaledHeight()) * 0.25D;
        int logoHeightHalf = (int) (logoHeight * 0.5D);
        double logoWidth = logoHeight * 4.0D;
        int logoWidthHalf = (int) (logoWidth * 0.5D);
        int posX = centerX - logoWidthHalf;
        int posY = centerY - logoHeightHalf;

        if (this.useOriginalSizeAndPosCalculation) {
            this.posX = posX;
            this.posY = posY;
            this.width = logoWidthHalf * 2;
            this.height = logoHeightHalf * 2;
            this.orientation = "top-left";
        } else {
            logoHeight = this.getHeight();
            logoWidthHalf = this.getWidth() / 2;
            posX = this.getPosX(menu);
            posY = this.getPosY(menu);
        }

        if (this.shouldRender()) {

//            RenderSystem.setShaderTexture(0, MOJANG_STUDIOS_LOGO_LOCATION);
//            RenderSystem.enableBlend();
//            RenderSystem.blendEquation(32774);
//            RenderSystem.blendFunc(770, 1);
//            RenderSystem.setShader(GameRenderer::getPositionTexShader);
//            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
//
//            blit(graphics, posX, posY, logoWidthHalf, (int) logoHeight, -0.0625F, 0.0F, 120, 60, 120, 120);
//            blit(graphics, posX + logoWidthHalf, posY, logoWidthHalf, (int) logoHeight, 0.0625F, 60.0F, 120, 60, 120, 120);
//
//            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//            RenderSystem.defaultBlendFunc();
//            RenderSystem.disableBlend();

            RenderSystem.enableBlend();
            RenderSystem.blendEquation(32774);
            RenderSystem.blendFunc(770, 1);
            graphics.setColor(1.0F, 1.0F, 1.0F, this.opacity);
            graphics.blit(MOJANG_STUDIOS_LOGO_LOCATION, posX, posY, logoWidthHalf, (int) logoHeight, -0.0625F, 0.0F, 120, 60, 120, 120);
            graphics.blit(MOJANG_STUDIOS_LOGO_LOCATION, posX + logoWidthHalf, posY, logoWidthHalf, (int) logoHeight, 0.0625F, 60.0F, 120, 60, 120, 120);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();

        }

    }

    @Override
    public boolean shouldRender() {
        return super.shouldRender() && !this.hidden;
    }

}