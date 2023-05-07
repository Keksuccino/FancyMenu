package de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.splash;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationElement;
import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationItem;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

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
    public int getPosX(Screen screen) {
        return this.lastSplashPosX - (this.getWidth() / 2);
    }

    //Only used in editor
    @Override
    public int getPosY(Screen screen) {
        return this.lastSplashPosY - (this.getHeight() / 2);
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        this.width = 60;
        this.height = 30;

        if (!this.hidden) {
            RenderSystem.enableBlend();
            this.renderSplash(matrix, Minecraft.getInstance().font, menu);
        }

    }

    protected void renderSplash(PoseStack matrix, Font font, Screen s) {

        float finalPosX = (s.width / 2 + 90);
        float finalPosY = 70.0F;

        int originX = 0;
        int originY = 0;

        if (anchorPoint.equalsIgnoreCase("original")) {
            originX = (int) finalPosX;
            originY = (int) finalPosY;
        } else if (anchorPoint.equalsIgnoreCase("top-left")) {
            ;
        } else if (anchorPoint.equalsIgnoreCase("mid-left")) {
            originY = s.height / 2;
        } else if (anchorPoint.equalsIgnoreCase("bottom-left")) {
            originY = s.height;
        } else if (anchorPoint.equalsIgnoreCase("top-centered")) {
            originX = s.width / 2;
        } else if (anchorPoint.equalsIgnoreCase("mid-centered")) {
            originX = s.width / 2;
            originY = s.height / 2;
        } else if (anchorPoint.equalsIgnoreCase("bottom-centered")) {
            originX = s.width / 2;
            originY = s.height;
        } else if (anchorPoint.equalsIgnoreCase("top-right")) {
            originX = s.width;
        } else if (anchorPoint.equalsIgnoreCase("mid-right")) {
            originX = s.width;
            originY = s.height / 2;
        } else if (anchorPoint.equalsIgnoreCase("bottom-right")) {
            originX = s.width;
            originY = s.height;
        }

        finalPosX = originX + baseX;
        finalPosY = originY + baseY;

        this.lastSplashPosX = (int) finalPosX;
        this.lastSplashPosY = (int) finalPosY;

        if (cachedSplashText == null) {
            this.cachedSplashText = getRandomSplashText();
        }
        if (this.cachedSplashText == null) {
            this.cachedSplashText = "§c< ERROR! UNABLE TO GET SPLASH TEXT! >";
        }

        matrix.pushPose();
        matrix.translate(finalPosX, finalPosY, 0.0F);
        matrix.mulPose(Axis.ZP.rotationDegrees(this.splashRotation));
        float f = 1.8F - Mth.abs(Mth.sin((float) (System.currentTimeMillis() % 1000L) / 1000.0F * ((float) Math.PI * 2F)) * 0.1F);
        f = f * 100.0F / (float) (font.width(this.cachedSplashText) + 32);
        matrix.scale(f, f, f);

        drawCenteredString(matrix, font, Component.literal(this.cachedSplashText), 0, -8, this.splashColor.getRGB());

        matrix.popPose();

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
        return Minecraft.getInstance().getSplashManager().getSplash();
    }

}