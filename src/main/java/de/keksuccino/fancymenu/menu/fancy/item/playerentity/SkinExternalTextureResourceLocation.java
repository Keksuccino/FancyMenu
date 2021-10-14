package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class SkinExternalTextureResourceLocation extends ExternalTextureResourceLocation {

    protected boolean loaded = false;
    protected String path;
    protected InputStream in;
    protected int width = 0;
    protected int height = 0;
    protected ResourceLocation location;

    public SkinExternalTextureResourceLocation(String path) {
        super(path);
        this.path = path;
    }

    @Override
    public void loadTexture() {
        if (!this.loaded) {
            try {
                if (Minecraft.getMinecraft().getTextureManager() == null) {
                    System.out.println("################################ WARNING ################################");
                    System.out.println("Can't load texture '" + this.path + "'! Minecraft TextureManager instance not ready yet!");
                    return;
                }

                File f = new File(this.path);
                this.in = new FileInputStream(f);

                BufferedImage i = ImageIO.read(this.in);
                this.width = i.getWidth();
                this.height = i.getHeight();
                //Converting old 1.7 skins to new 1.8+ skin format
                if (this.height < 64) {
                    BufferedImage skinNew = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

                    //Copy old skin texture to new skin
                    copyPixelAreaToNewImage(i, skinNew, 0, 0, 0, 0, 64, 32, false);

                    int xOffsetLeg = 16;
                    int yOffsetLeg = 32;
                    //Clone small leg part 1
                    cloneSkinPart(skinNew, 4, 16, 4, 4, xOffsetLeg, yOffsetLeg, true);
                    //Clone small leg part 2
                    cloneSkinPart(skinNew, 8, 16, 4, 4, xOffsetLeg, yOffsetLeg, true);
                    //Clone big leg part 1
                    cloneSkinPart(skinNew, 0, 20, 4, 12, xOffsetLeg + 8, yOffsetLeg, true);
                    //Clone big leg part 2
                    cloneSkinPart(skinNew, 4, 20, 4, 12, xOffsetLeg, yOffsetLeg, true);
                    //Clone big leg part 3
                    cloneSkinPart(skinNew, 8, 20, 4, 12, xOffsetLeg - 8, yOffsetLeg, true);
                    //Clone big leg part 4
                    cloneSkinPart(skinNew, 12, 20, 4, 12, xOffsetLeg, yOffsetLeg, true);

                    int xOffsetArm = -8;
                    int yOffsetArm = 32;
                    //Clone small arm part 1
                    cloneSkinPart(skinNew, 44, 16, 4, 4, xOffsetArm, yOffsetArm, true);
                    //Clone small arm part 2
                    cloneSkinPart(skinNew, 48, 16, 4, 4, xOffsetArm, yOffsetArm, true);
                    //Clone big arm part 1
                    cloneSkinPart(skinNew, 40, 20, 4, 12, xOffsetArm + 8, yOffsetArm, true);
                    //Clone big arm part 2
                    cloneSkinPart(skinNew, 44, 20, 4, 12, xOffsetArm, yOffsetArm, true);
                    //Clone big arm part 3
                    cloneSkinPart(skinNew, 48, 20, 4, 12, xOffsetArm - 8, yOffsetArm, true);
                    //Clone big arm part 4
                    cloneSkinPart(skinNew, 52, 20, 4, 12, xOffsetArm, yOffsetArm, true);

                    i = skinNew;
                }
                this.location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("externaltexture", new SelfcleaningDynamicTexture(i));
                this.in.close();
                this.loaded = true;
            } catch (Exception var2) {
                var2.printStackTrace();
            }

        }
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public ResourceLocation getResourceLocation() {
        return this.location;
    }

    @Override
    public boolean isReady() {
        return this.loaded;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    /** First X/Y pixel is 0 **/
    protected static void copyPixelAreaToNewImage(BufferedImage in, BufferedImage out, int xFrom, int yFrom, int xTo, int yTo, int width, int height, boolean mirrorX) {
        int vertOffset = 0;
        int vertical = yTo;
        while (vertical < yTo + height) {
            int horiOffset = 0;
            if (mirrorX) {
                horiOffset = width - 1;
            }
            int horizontal = xTo;
            while(horizontal < xTo + width) {
                int pixel = in.getRGB(xFrom + horiOffset, yFrom + vertOffset);
                out.setRGB(horizontal, vertical, pixel);
                horizontal++;
                if (mirrorX) {
                    horiOffset--;
                } else {
                    horiOffset++;
                }
            }
            vertical++;
            vertOffset++;
        }
    }

    protected static void cloneSkinPart(BufferedImage in, int xStart, int yStart, int width, int height, int xOffset, int yOffset, boolean mirrorX) {
        copyPixelAreaToNewImage(in, in, xStart, yStart, xStart + xOffset, yStart + yOffset, width, height, mirrorX);
    }

}
