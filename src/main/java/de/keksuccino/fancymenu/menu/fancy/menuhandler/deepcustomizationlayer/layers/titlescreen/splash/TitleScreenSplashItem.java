package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.splash;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class TitleScreenSplashItem extends DeepCustomizationItem {

    public static String cachedSplashText;

    public String splashTextFilePath;
    public int splashRotation = -20;
    public Color splashColor = new Color(255, 255, 0);
    public String splashColorHEX = "#ffff00";

    protected int lastSplashPosX = 0;
    protected int lastSplashPosY = 0;

    public TitleScreenSplashItem(DeepCustomizationElement parentElement, PropertiesSection item) {

        super(parentElement, item);

        this.splashTextFilePath = item.getEntryValue("splash_file_path");

        String splashRot = item.getEntryValue("splash_rotation");
        if ((splashRot != null) && MathUtils.isInteger(splashRot)) {
            splashRotation = Integer.parseInt(splashRot);
        }

        String splashCol = item.getEntryValue("splash_color");
        if (splashCol != null) {
            Color c = RenderUtils.getColorFromHexString(splashCol);
            if (c != null) {
                this.splashColor = c;
                this.splashColorHEX = splashCol;
            }
        }

    }

    //Only used in editor
    @Override
    public int getPosX(Screen menu) {
        return this.lastSplashPosX - (this.width / 2);
    }

    //Only used in editor
    @Override
    public int getPosY(Screen menu) {
        return this.lastSplashPosY - (this.height / 2);
    }

    @Override
    public void render(MatrixStack matrix, Screen menu) throws IOException {

        this.width = 60;
        this.height = 30;

        if (!this.hidden) {
            RenderSystem.enableBlend();
            this.renderSplash(matrix, Minecraft.getInstance().fontRenderer, menu);
        }

    }

    protected void renderSplash(MatrixStack matrix, FontRenderer font, Screen s) {

        float finalPosX = (s.width / 2 + 90);
        float finalPosY = 70.0F;

        int originX = 0;
        int originY = 0;

        if (orientation.equalsIgnoreCase("original")) {
            originX = (int) finalPosX;
            originY = (int) finalPosY;
        } else if (orientation.equalsIgnoreCase("top-left")) {
            ;
        } else if (orientation.equalsIgnoreCase("mid-left")) {
            originY = s.height / 2;
        } else if (orientation.equalsIgnoreCase("bottom-left")) {
            originY = s.height;
        } else if (orientation.equalsIgnoreCase("top-centered")) {
            originX = s.width / 2;
        } else if (orientation.equalsIgnoreCase("mid-centered")) {
            originX = s.width / 2;
            originY = s.height / 2;
        } else if (orientation.equalsIgnoreCase("bottom-centered")) {
            originX = s.width / 2;
            originY = s.height;
        } else if (orientation.equalsIgnoreCase("top-right")) {
            originX = s.width;
        } else if (orientation.equalsIgnoreCase("mid-right")) {
            originX = s.width;
            originY = s.height / 2;
        } else if (orientation.equalsIgnoreCase("bottom-right")) {
            originX = s.width;
            originY = s.height;
        }

        finalPosX = originX + posX;
        finalPosY = originY + posY;

        this.lastSplashPosX = (int) finalPosX;
        this.lastSplashPosY = (int) finalPosY;

        if (cachedSplashText == null) {
            this.cachedSplashText = getRandomSplashText();
        }
        if (this.cachedSplashText == null) {
            this.cachedSplashText = "§c< ERROR! UNABLE TO GET SPLASH TEXT! >";
        }

        RenderSystem.pushMatrix();
        RenderSystem.translatef(finalPosX, finalPosY, 0.0F);
        RenderSystem.rotatef((float)this.splashRotation, 0.0F, 0.0F, 1.0F);
        float f = 1.8F - MathHelper.abs(MathHelper.sin((float) (Util.milliTime() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
        f = f * 100.0F / (float) (font.getStringWidth(this.cachedSplashText) + 32);
        RenderSystem.scalef(f, f, f);

        drawCenteredString(matrix, font, new StringTextComponent(this.cachedSplashText), 0, -8, this.splashColor.getRGB());

        RenderSystem.popMatrix();

    }

    public String getRandomSplashText() {
        if ((splashTextFilePath != null) && !splashTextFilePath.replace(" ", "").equals("")) {
            File f = new File(splashTextFilePath);
            if (f.exists() && f.isFile() && f.getPath().toLowerCase().endsWith(".txt")) {
                List<String> l = FileUtils.getFileLines(f);
                if ((l != null) && !l.isEmpty()) {
                    int random = MathUtils.getRandomNumberInRange(0, l.size()-1);
                    return l.get(random);
                }
            }
        }
        return Minecraft.getInstance().getSplashes().getSplashText();
    }

}