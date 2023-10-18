package de.keksuccino.drippyloadingscreen.customization.items.bars;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.resources.TextureHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public abstract class AbstractProgressBarCustomizationItem extends CustomizationItem {

    public BarDirection direction = BarDirection.RIGHT;

    public String barColorHex = "#5295FF";
    public Color barColor = new Color(82, 149, 255);
    public String barTexturePath;
    public ResourceLocation barTexture = null;
    public String backgroundColorHex = "#ABC8F7";
    public Color backgroundColor = new Color(171, 200, 247);
    public String backgroundTexturePath;
    public ResourceLocation backgroundTexture = null;
    public boolean useProgressForElementOrientation = false;

    //These fields are for caching the last x, y, width and height of the progress bar (not the element!)
    protected int lastProgressX = 0;
    protected int lastProgressY = 0;
    protected int lastProgressWidth = 0;
    protected int lastProgressHeight = 0;

    public AbstractProgressBarCustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {

        super(parentContainer, item);

        String progressForElementOri = item.getEntryValue("progress_for_element_orientation");
        if ((progressForElementOri != null) && progressForElementOri.equalsIgnoreCase("true")) {
            this.useProgressForElementOrientation = true;
        }

        String barHex = item.getEntryValue("bar_color");
        if ((barHex != null) && !barHex.replace(" ", "").equals("")) {
            Color c = RenderUtils.getColorFromHexString(barHex);
            if (c != null) {
                this.barColorHex = barHex;
                this.barColor = c;
            }
        }

        this.barTexturePath = item.getEntryValue("bar_texture");
        if (this.barTexturePath != null) {
            File f = new File(MenuCustomization.getAbsoluteGameDirectoryPath(this.barTexturePath));
            if (f.exists() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
                this.barTexture = TextureHandler.getResource(f.getAbsolutePath()).getResourceLocation();
            }
        }

        String backgroundHex = item.getEntryValue("background_color");
        if ((backgroundHex != null) && !backgroundHex.replace(" ", "").equals("")) {
            Color c = RenderUtils.getColorFromHexString(backgroundHex);
            if (c != null) {
                this.backgroundColorHex = backgroundHex;
                this.backgroundColor = c;
            }
        }

        this.backgroundTexturePath = item.getEntryValue("background_texture");
        if (this.backgroundTexturePath != null) {
            File f = new File(MenuCustomization.getAbsoluteGameDirectoryPath(this.backgroundTexturePath));
            if (f.exists() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".jpeg") || f.getName().toLowerCase().endsWith(".png"))) {
                this.backgroundTexture = TextureHandler.getResource(f.getAbsolutePath()).getResourceLocation();
            }
        }

        String barDirection = item.getEntryValue("direction");
        if (barDirection != null) {
            this.direction = BarDirection.byName(barDirection);
        }

    }

    @Override
    public void render(GuiGraphics graphics, Screen menu) throws IOException {
        if (this.shouldRender()) {
            RenderSystem.enableBlend();
            this.renderBackground(graphics, menu);
            this.renderProgress(graphics, menu);
        }
    }

    protected void renderProgress(GuiGraphics graphics, Screen menu) {

        float currentProgress = Math.max(0.0F, Math.min(1.0F, this.getCurrentProgress()));
        int progressWidth = this.getWidth();
        int progressHeight = this.getHeight();
        int progressX = this.getPosX(menu);
        int progressY = this.getPosY(menu);
        float offsetX = 0.0F;
        float offsetY = 0.0F;
        if ((this.direction == BarDirection.LEFT) || (this.direction == BarDirection.RIGHT)) {
            progressWidth = (int)((float)this.getWidth() * currentProgress);
        }
        if ((this.direction == BarDirection.UP) || (this.direction == BarDirection.DOWN)) {
            progressHeight = (int)((float)this.getHeight() * currentProgress);
        }
        if (this.direction == BarDirection.LEFT) {
            progressX += this.getWidth() - progressWidth;
            offsetX = this.getWidth() - progressWidth;
        }
        if (this.direction == BarDirection.UP) {
            progressY += this.getHeight() - progressHeight;
            offsetY = this.getHeight() - progressHeight;
        }
        this.lastProgressX = progressX;
        this.lastProgressY = progressY;
        this.lastProgressWidth = progressWidth;
        this.lastProgressHeight = progressHeight;

        if (this.barTexture != null) {
//            RenderUtils.bindTexture(this.barTexture);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
            graphics.blit(this.barTexture, progressX, progressY, offsetX, offsetY, progressWidth, progressHeight, this.getWidth(), this.getHeight());
        } else if (this.barColor != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderUtils.fill(graphics, progressX, progressY, progressX + progressWidth, progressY + progressHeight, this.barColor.getRGB(), this.opacity);
        }

    }

    protected void renderBackground(GuiGraphics graphics, Screen menu) {
        if (this.backgroundTexture != null) {
//            RenderUtils.bindTexture(this.backgroundTexture);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
            graphics.blit(this.backgroundTexture, this.getPosX(menu), this.getPosY(menu), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        } else if (this.barColor != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderUtils.fill(graphics, this.getPosX(menu), this.getPosY(menu), this.getPosX(menu) + this.getWidth(), this.getPosY(menu) + this.getHeight(), this.backgroundColor.getRGB(), this.opacity);
        }
    }

    public abstract float getCurrentProgress();

    public int getProgressX() {
        return this.lastProgressX;
    }

    public int getProgressY() {
        return this.lastProgressY;
    }

    public int getProgressWidth() {
        return this.lastProgressWidth;
    }

    public int getProgressHeight() {
        return this.lastProgressHeight;
    }

    public enum BarDirection {

        LEFT("left"),
        RIGHT("right"),
        UP("up"),
        DOWN("down");

        private String name;

        BarDirection(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static BarDirection byName(String name) {
            for (BarDirection d : BarDirection.values()) {
                if (d.name.equals(name)) {
                    return d;
                }
            }
            return BarDirection.LEFT;
        }

    }

}
