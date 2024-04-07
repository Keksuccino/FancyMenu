package de.keksuccino.fancymenu.customization.element.elements.playerentity.textures;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.minecraftuser.v2.MinecraftUsers;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.PngTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SkinResourceSupplier extends ResourceSupplier<ITexture> {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final ResourceLocation DEFAULT_SKIN_LOCATION = new ResourceLocation("textures/entity/player/wide/zuri.png");
    public static final PngTexture DEFAULT_SKIN = PngTexture.location(DEFAULT_SKIN_LOCATION);
    protected static final Map<String, SkinMetadata> CACHED_SKIN_METADATA = new HashMap<>();

    protected boolean sourceIsPlayerName;
    @Nullable
    protected ITexture currentlyLoadingPngSkinTexture;
    @Nullable
    protected volatile SkinResourceSupplier.SkinMetadata playerNameSkinMeta;
    protected volatile boolean startedDownloadingMetadata = false;
    @Nullable
    protected String lastGetterPlayerName;

    public SkinResourceSupplier(@NotNull String source, boolean sourceIsPlayerName) {
        super(ITexture.class, FileMediaType.IMAGE, source);
        this.sourceIsPlayerName = sourceIsPlayerName;
    }

    @Override
    @NotNull
    public ITexture get() {
        SkinMetadata playerNameSkinMetaFinal = this.playerNameSkinMeta;
        String getterPlayerName = PlaceholderParser.replacePlaceholders(this.source, false);
        if (this.sourceIsPlayerName) {
            //If last player name is different one, reload supplier
            if (!getterPlayerName.equals(this.lastGetterPlayerName)) {
                this.startedDownloadingMetadata = false;
                this.current = null;
                this.playerNameSkinMeta = null;
                playerNameSkinMetaFinal = null;
                this.clearCurrentlyLoadingPngSkinTexture();
            }
            this.lastGetterPlayerName = getterPlayerName;
            if (playerNameSkinMetaFinal == null) {
                if (!this.startedDownloadingMetadata) {
                    if (CACHED_SKIN_METADATA.containsKey(getterPlayerName)) {
                        this.startedDownloadingMetadata = true;
                        this.playerNameSkinMeta = CACHED_SKIN_METADATA.get(getterPlayerName);
                        playerNameSkinMetaFinal = this.playerNameSkinMeta;
                    } else {
                        this.downloadPlayerNameSkinMetadata(getterPlayerName);
                    }
                }
            }
            if (playerNameSkinMetaFinal == null) {
                return DEFAULT_SKIN;
            }
        }
        if ((this.current != null) && this.current.isClosed()) {
            this.current = null;
        }
        String getterSource = PlaceholderParser.replacePlaceholders(this.source, false);
        if (this.sourceIsPlayerName) {
            if ((playerNameSkinMetaFinal == null) || (playerNameSkinMetaFinal.resourceSource() == null)) {
                return DEFAULT_SKIN;
            }
            getterSource = playerNameSkinMetaFinal.resourceSource();
        }
        if (!getterSource.equals(this.lastGetterSource)) {
            this.current = null;
            this.clearCurrentlyLoadingPngSkinTexture();
        }
        this.lastGetterSource = getterSource;
        //If 'currentlyLoadingPngSkinTexture' got closed, NULL it (and close it again, just to be safe)
        if ((this.currentlyLoadingPngSkinTexture != null) && this.currentlyLoadingPngSkinTexture.isClosed()) {
            this.clearCurrentlyLoadingPngSkinTexture();
        }
        if (this.current == null) {
            ResourceSource resourceSource = ResourceSource.of(getterSource);
            String convertedSkinKey = "converted_skin_" + resourceSource.getSourceWithPrefix();
            //Caches the Skin Metadata
            if (this.sourceIsPlayerName && !CACHED_SKIN_METADATA.containsKey(getterPlayerName)) CACHED_SKIN_METADATA.put(getterPlayerName, playerNameSkinMetaFinal);
            //Searches for an already converted version of the given skin
            ITexture cached = ResourceHandlers.getImageHandler().getIfRegistered(convertedSkinKey);
            if (cached != null) {
                this.current = cached;
                return this.current;
            }
            try {
                if (this.currentlyLoadingPngSkinTexture == null) {
                    if (this.sourceIsPlayerName || FileTypes.PNG_IMAGE.isFileType(resourceSource, true)) {
                        if (this.sourceIsPlayerName) {
                            this.currentlyLoadingPngSkinTexture = ResourceHandlers.getImageHandler().hasResource(resourceSource.getSourceWithPrefix()) ? ResourceHandlers.getImageHandler().get(resourceSource.getSourceWithPrefix()) : FileTypes.PNG_IMAGE.getCodec().readWeb(resourceSource.getSourceWithoutPrefix());;
                            if (this.currentlyLoadingPngSkinTexture != null) {
                                ResourceHandlers.getImageHandler().registerIfKeyAbsent(resourceSource.getSourceWithPrefix(), this.currentlyLoadingPngSkinTexture);
                            } else {
                                LOGGER.error("[FANCYMENU] SkinResourceSupplier failed to get skin by player name! PNG codec returned NULL: " + resourceSource);
                                this.current = DEFAULT_SKIN;
                            }
                        } else {
                            this.currentlyLoadingPngSkinTexture = ResourceHandlers.getImageHandler().get(resourceSource);
                        }
                        if (this.currentlyLoadingPngSkinTexture == null) {
                            LOGGER.error("[FANCYMENU] SkinResourceSupplier failed to get skin texture! Invalid source: " + resourceSource);
                            this.current = DEFAULT_SKIN;
                        }
                    } else {
                        this.current = ResourceHandlers.getImageHandler().get(resourceSource);
                        if (this.current == null) {
                            LOGGER.error("[FANCYMENU] SkinResourceSupplier failed to get skin texture! Invalid source: " + resourceSource);
                            this.current = DEFAULT_SKIN;
                        }
                    }
                }
                if ((this.currentlyLoadingPngSkinTexture != null) && (this.current == null)) {
                    if (this.currentlyLoadingPngSkinTexture.isReady()) {
                        if (this.currentlyLoadingPngSkinTexture.getHeight() >= 64) {
                            //If skin is new format (height >= 64), simply return texture without converting
                            this.current = this.currentlyLoadingPngSkinTexture;
                            this.currentlyLoadingPngSkinTexture = null;
                        } else {
                            //If skin is old format, convert to new format
                            if (this.currentlyLoadingPngSkinTexture.isReady()) {
                                ITexture converted = modernizePngSkinTexture(this.currentlyLoadingPngSkinTexture);
                                this.clearCurrentlyLoadingPngSkinTexture();
                                if (converted != null) {
                                    ResourceHandlers.getImageHandler().registerIfKeyAbsent(convertedSkinKey, converted);
                                    this.current = converted;
                                } else {
                                    LOGGER.error("[FANCYMENU] SkinResourceSupplier failed to convert old skin to new format: " + resourceSource);
                                    this.current = DEFAULT_SKIN;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] SkinResourceSupplier failed to get skin resource: " + resourceSource + " (" + this.source + ")", ex);
            }
        }
        return (this.current != null) ? this.current : DEFAULT_SKIN;
    }

    @NotNull
    public ResourceLocation getSkinLocation() {
        ResourceLocation loc = this.get().getResourceLocation();
        return (loc != null) ? loc : DEFAULT_SKIN_LOCATION;
    }

    public boolean isSlimPlayerNameSkin() {
        SkinMetadata skin = this.playerNameSkinMeta;
        return (skin != null) && skin.slim();
    }

    protected void downloadPlayerNameSkinMetadata(@NotNull String getterPlayerName) {
        Objects.requireNonNull(getterPlayerName);
        this.startedDownloadingMetadata = true;
        new Thread(() -> {
            String skinUrl = null;
            boolean isSlim = false;
            MinecraftProfileTexture texture = MinecraftUsers.getProfileTexture(getterPlayerName, MinecraftProfileTexture.Type.SKIN);
            if ((texture != null) && (texture != MinecraftUsers.MISSING_SKIN_TEXTURE)) {
                isSlim = "slim".equals(texture.getMetadata("model"));
                skinUrl = texture.getUrl();
            }
            if (skinUrl != null) {
                skinUrl = ResourceSourceType.WEB.getSourcePrefix() + skinUrl;
            } else {
                LOGGER.error("[FANCYMENU] SkinResourceSupplier failed to get URL of player skin: " + getterPlayerName, new IOException());
            }
            if (!this.startedDownloadingMetadata) return;
            this.playerNameSkinMeta = new SkinMetadata(getterPlayerName, skinUrl, isSlim);
        }).start();
    }

    protected void clearCurrentlyLoadingPngSkinTexture() {
        if (this.currentlyLoadingPngSkinTexture != null) {
            if (this.currentlyLoadingPngSkinTexture.isClosed()) CloseableUtils.closeQuietly(this.currentlyLoadingPngSkinTexture);
            this.currentlyLoadingPngSkinTexture = null;
        }
    }

    @Override
    public void setSource(@NotNull String source) {
        throw new RuntimeException("You can't update the source of SkinResourceSuppliers.");
    }

    @Override
    public @NotNull ResourceSourceType getSourceType() {
        if (this.sourceIsPlayerName) return ResourceSourceType.WEB;
        return super.getSourceType();
    }

    @Override
    public @NotNull String getSourceWithoutPrefix() {
        if (this.sourceIsPlayerName) return this.source;
        return super.getSourceWithoutPrefix();
    }

    @Override
    public @NotNull String getSourceWithPrefix() {
        if (this.sourceIsPlayerName) return this.source;
        return super.getSourceWithPrefix();
    }

    @SuppressWarnings("all")
    @Nullable
    protected static ITexture modernizePngSkinTexture(@NotNull ITexture skinTexture) {

        InputStream in = null;
        NativeImage oldTex = null;
        NativeImage newTex = null;
        ITexture iTexture = null;

        try {

            in = Objects.requireNonNull(skinTexture.open(), "Skin texture InputStream was NULL!");
            oldTex = NativeImage.read(in);
            newTex = new NativeImage(64, 64, true);

            //Copy old skin texture to new skin
            newTex.copyFrom(oldTex);

            int xOffsetLeg = 16;
            int yOffsetLeg = 32;
            //Clone small leg part 1
            cloneSkinPart(newTex, 4, 16, 4, 4, xOffsetLeg, yOffsetLeg, true);
            //Clone small leg part 2
            cloneSkinPart(newTex, 8, 16, 4, 4, xOffsetLeg, yOffsetLeg, true);
            //Clone big leg part 1
            cloneSkinPart(newTex, 0, 20, 4, 12, xOffsetLeg + 8, yOffsetLeg, true);
            //Clone big leg part 2
            cloneSkinPart(newTex, 4, 20, 4, 12, xOffsetLeg, yOffsetLeg, true);
            //Clone big leg part 3
            cloneSkinPart(newTex, 8, 20, 4, 12, xOffsetLeg - 8, yOffsetLeg, true);
            //Clone big leg part 4
            cloneSkinPart(newTex, 12, 20, 4, 12, xOffsetLeg, yOffsetLeg, true);

            int xOffsetArm = -8;
            int yOffsetArm = 32;
            //Clone small arm part 1
            cloneSkinPart(newTex, 44, 16, 4, 4, xOffsetArm, yOffsetArm, true);
            //Clone small arm part 2
            cloneSkinPart(newTex, 48, 16, 4, 4, xOffsetArm, yOffsetArm, true);
            //Clone big arm part 1
            cloneSkinPart(newTex, 40, 20, 4, 12, xOffsetArm + 8, yOffsetArm, true);
            //Clone big arm part 2
            cloneSkinPart(newTex, 44, 20, 4, 12, xOffsetArm, yOffsetArm, true);
            //Clone big arm part 3
            cloneSkinPart(newTex, 48, 20, 4, 12, xOffsetArm - 8, yOffsetArm, true);
            //Clone big arm part 4
            cloneSkinPart(newTex, 52, 20, 4, 12, xOffsetArm, yOffsetArm, true);

            iTexture = PngTexture.of(newTex);

        } catch (Exception ex) {
            ex.printStackTrace();
            CloseableUtils.closeQuietly(newTex);
            CloseableUtils.closeQuietly(iTexture);
            iTexture = null;
        }

        CloseableUtils.closeQuietly(in);
        CloseableUtils.closeQuietly(oldTex);

        return iTexture;

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

    @SuppressWarnings("all")
    protected static void cloneSkinPart(NativeImage in, int xStart, int yStart, int width, int height, int xOffset, int yOffset, boolean mirrorX) {
        copyPixelArea(in, xStart, yStart, xStart + xOffset, yStart + yOffset, width, height, mirrorX);
    }

    public record SkinMetadata(@NotNull String playerName, @Nullable String resourceSource, boolean slim) {
    }

}
