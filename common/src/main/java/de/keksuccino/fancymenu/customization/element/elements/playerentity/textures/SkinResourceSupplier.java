package de.keksuccino.fancymenu.customization.element.elements.playerentity.textures;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.resources.ResourceHandlers;
import de.keksuccino.fancymenu.util.resources.ResourceSourceType;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.SimpleTexture;
import de.keksuccino.konkrete.resources.SelfcleaningDynamicTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;

public class SkinResourceSupplier extends ResourceSupplier<ITexture> {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final SimpleTexture DEFAULT_SKIN = SimpleTexture.location(new ResourceLocation("textures/entity/player/wide/zuri.png"));

    protected boolean sourceIsPlayerName;

    public SkinResourceSupplier(@NotNull String source, boolean sourceIsPlayerName) {
        super(ITexture.class, FileMediaType.IMAGE, source);
        this.sourceIsPlayerName = sourceIsPlayerName;
    }

    //TODO Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(gameProfile)

    @Override
    public @Nullable ITexture get() {
        String getterSource = PlaceholderParser.replacePlaceholders(this.source, false);
        if (!getterSource.equals(this.lastGetterSource)) {
            this.current = null;
        }
        this.lastGetterSource = getterSource;
        if (this.current == null) {
            try {
                ResourceSourceType sourceType = ResourceSourceType.getSourceTypeOf(getterSource);
                String withoutPrefix = ResourceSourceType.getWithoutSourcePrefix(getterSource);
                getterSource = sourceType.getSourcePrefix() + withoutPrefix;
                if (FileTypes.PNG_IMAGE.isFileType(getterSource)) {
                    ITexture t = ResourceHandlers.getImageHandler().get(getterSource);
                    if (t != null) {
                        //TODO return default skin here if t#getLocation() returns NULL
                    } else {
                        LOGGER.error("[FANCYMENU] Failed to ");
                    }
                } else {
                    this.current = ResourceHandlers.getImageHandler().get(getterSource);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get skin resource: " + getterSource + " (" + this.source + ")", ex);
            }
        }
        return this.current;
    }

    @Override
    public void setSource(@NotNull String source) {
        super.setSource(source);
    }

    protected static ITexture modernizePngSkinTexture(@NotNull String pngSource) {
        try {

            NativeImage

            ITexture t = ResourceHandlers.getImageHandler().get(source);
            if (t != null) {
                t.waitForReady(5000);
                ResourceLocation loc = t.getResourceLocation();
                if (loc != null) {
                    if (t.getHeight() >= 64) {
                        this.width = t.getWidth();
                        this.height = t.getHeight();
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
            this.location = Minecraft.getInstance().getTextureManager().register("fancymenu_modernized_skin_texture", new SelfcleaningDynamicTexture(i));
            this.loaded = true;
        } catch (Exception var2) {
            var2.printStackTrace();
        }
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
