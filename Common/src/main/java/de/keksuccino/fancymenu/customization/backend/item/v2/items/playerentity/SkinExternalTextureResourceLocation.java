package de.keksuccino.fancymenu.customization.backend.item.v2.items.playerentity;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class SkinExternalTextureResourceLocation extends ExternalTextureResourceLocation {

    private static final Logger LOGGER = LogManager.getLogger();

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

                if (Minecraft.getInstance().getTextureManager() == null) {
                    LOGGER.error("[FANCYMENU] Can't load texture '" + this.path + "'! Minecraft TextureManager instance not ready yet!");
                    return;
                }

                ExternalTextureResourceLocation exRL = ExternalTextureHandler.INSTANCE.getTexture(this.path);
                if (exRL != null) {
                    ResourceLocation loc = exRL.getResourceLocation();
                    if (loc != null) {
                        if (exRL.getHeight() >= 64) {
                            this.width = exRL.getWidth();
                            this.height = exRL.getHeight();
                            this.location = loc;
                            this.loaded = true;
                            return;
                        }
                    }
                }

                File f = new File(this.path);
                this.in = new FileInputStream(f);

                NativeImage i = NativeImage.read(this.in);
                this.width = i.getWidth();
                this.height = i.getHeight();
                //Converting old 1.7 skins to new 1.8+ skin format
                if (this.height < 64) {
                    NativeImage skinNew = new NativeImage(64, 64, true);

                    //Copy old skin texture to new skin
                    skinNew.copyFrom(i);

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
                this.location = Minecraft.getInstance().getTextureManager().register("externaltexture", new SelfcleaningDynamicTexture(i));
                this.loaded = true;
            } catch (Exception var2) {
                var2.printStackTrace();
            }

            if (this.in != null) {
                IOUtils.closeQuietly(this.in);
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
    protected static void copyPixelArea(NativeImage in, int xFrom, int yFrom, int xTo, int yTo, int width, int height, boolean mirrorX) {
        int vertOffset = 0;
        int vertical = yTo;
        while (vertical < yTo + height) {
            int horiOffset = 0;
            if (mirrorX) {
                horiOffset = width - 1;
            }
            int horizontal = xTo;
            while(horizontal < xTo + width) {
                int pixel = in.getPixelRGBA(xFrom + horiOffset, yFrom + vertOffset);
                in.setPixelRGBA(horizontal, vertical, pixel);
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

    protected static void cloneSkinPart(NativeImage in, int xStart, int yStart, int width, int height, int xOffset, int yOffset, boolean mirrorX) {
        copyPixelArea(in, xStart, yStart, xStart + xOffset, yStart + yOffset, width, height, mirrorX);
    }

}
