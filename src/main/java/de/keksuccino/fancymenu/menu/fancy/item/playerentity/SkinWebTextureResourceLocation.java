package de.keksuccino.fancymenu.menu.fancy.item.playerentity;

import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
import de.keksuccino.konkrete.resources.WebTextureResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SkinWebTextureResourceLocation extends WebTextureResourceLocation {

    protected boolean loaded = false;
    protected int width = 0;
    protected int height = 0;
    protected ResourceLocation location = null;
    protected String url;

    public SkinWebTextureResourceLocation(String url) {
        super(url);
        this.url = url;
    }

    @Override
    public void loadTexture() {
        if (!this.loaded) {
            try {
                if (Minecraft.getMinecraft().getTextureManager() == null) {
                    System.out.println("################################ WARNING ################################");
                    System.out.println("Can't load texture '" + this.url + "'! Minecraft TextureManager instance not ready yet!");
                    return;
                }

                URL u = new URL(this.url);
                HttpURLConnection httpcon = (HttpURLConnection)u.openConnection();
                httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");
                InputStream s = httpcon.getInputStream();
                if (s == null) {
                    return;
                }

                BufferedImage i = ImageIO.read(s);
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
                this.location = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(this.filterUrl(this.url), new SelfcleaningDynamicTexture(i));
                s.close();
                this.loaded = true;
            } catch (Exception var5) {
                System.out.println("######################### ERROR #########################");
                System.out.println("Can't load texture '" + this.url + "'! Invalid URL!");
                System.out.println("#########################################################");
                this.loaded = false;
                var5.printStackTrace();
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
    public String getURL() {
        return this.url;
    }

    @Override
    public boolean isReady() {
        return this.loaded;
    }

    protected String filterUrl(String url) {
        CharacterFilter c = new CharacterFilter();
        c.addAllowedCharacters(new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "."});
        return c.filterForAllowedChars(url.toLowerCase());
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
