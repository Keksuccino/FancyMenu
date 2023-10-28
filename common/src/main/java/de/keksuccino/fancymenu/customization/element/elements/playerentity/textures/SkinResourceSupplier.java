package de.keksuccino.fancymenu.customization.element.elements.playerentity.textures;

import com.mojang.blaze3d.platform.NativeImage;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.PlayerSkinUtils;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.resources.ResourceHandlers;
import de.keksuccino.fancymenu.util.resources.ResourceSourceType;
import de.keksuccino.fancymenu.util.resources.ResourceSupplier;
import de.keksuccino.fancymenu.util.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resources.texture.SimpleTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SkinResourceSupplier extends ResourceSupplier<ITexture> {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final ResourceLocation DEFAULT_SKIN_LOCATION = new ResourceLocation("textures/entity/player/wide/zuri.png");
    public static final SimpleTexture DEFAULT_SKIN = SimpleTexture.location(DEFAULT_SKIN_LOCATION);
    protected static final Map<String, PlayerSkin> CACHED_PLAYER_NAME_SKINS = new HashMap<>();

    protected boolean sourceIsPlayerName;
    @Nullable
    protected ITexture currentlyLoadingPngSkinTexture;
    @Nullable
    protected volatile PlayerSkin playerNameSkin;
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
        PlayerSkin playerNameSkinCached = this.playerNameSkin;
        String getterPlayerName = PlaceholderParser.replacePlaceholders(this.source, false);
        if (this.sourceIsPlayerName) {
            if (!getterPlayerName.equals(this.lastGetterPlayerName)) {
                this.startedDownloadingMetadata = false;
                this.current = null;
                this.playerNameSkin = null;
                playerNameSkinCached = null;
                this.releaseCurrentlyLoadingPngSkin();
            }
            this.lastGetterPlayerName = getterPlayerName;
            if (playerNameSkinCached == null) {
                if (!this.startedDownloadingMetadata) {
                    if (CACHED_PLAYER_NAME_SKINS.containsKey(getterPlayerName)) {
                        this.startedDownloadingMetadata = true;
                        this.playerNameSkin = CACHED_PLAYER_NAME_SKINS.get(getterPlayerName);
                        playerNameSkinCached = this.playerNameSkin;
                    } else {
                        this.downloadPlayerNameSkinMetadata(getterPlayerName);
                    }
                }
            }
            if (playerNameSkinCached == null) return DEFAULT_SKIN;
        }
        if ((this.current != null) && this.current.isClosed()) {
            this.current = null;
        }
        String getterSource = PlaceholderParser.replacePlaceholders(this.source, false);
        if (this.sourceIsPlayerName && ((playerNameSkinCached == null) || (playerNameSkinCached.resourceSource() == null))) {
            return DEFAULT_SKIN;
        }
        if (this.sourceIsPlayerName) getterSource = playerNameSkinCached.resourceSource();
        if (!getterSource.equals(this.lastGetterSource)) {
            this.current = null;
            this.releaseCurrentlyLoadingPngSkin();
        }
        if ((this.currentlyLoadingPngSkinTexture != null) && this.currentlyLoadingPngSkinTexture.isClosed()) {
            this.releaseCurrentlyLoadingPngSkin();
        }
        this.lastGetterSource = getterSource;
        String convertedSkinKey = "converted_skin_" + getterSource;
        if (this.current == null) {
            if (this.sourceIsPlayerName && !CACHED_PLAYER_NAME_SKINS.containsKey(getterPlayerName)) CACHED_PLAYER_NAME_SKINS.put(getterPlayerName, playerNameSkinCached);
            try {
                if (this.currentlyLoadingPngSkinTexture == null) {
                    if (this.sourceIsPlayerName || FileTypes.PNG_IMAGE.isFileType(getterSource)) {
                        //Searches for an already converted version of the given skin
                        ITexture cached = ResourceHandlers.getImageHandler().getIfRegistered(convertedSkinKey);
                        if (cached != null) {
                            this.current = cached;
                            return this.current;
                        }
                        if (this.sourceIsPlayerName) {
                            this.currentlyLoadingPngSkinTexture = FileTypes.PNG_IMAGE.getCodec().readWeb(ResourceSourceType.getWithoutSourcePrefix(getterSource));
                            if (this.currentlyLoadingPngSkinTexture != null) {
                                ResourceHandlers.getImageHandler().registerIfKeyAbsent(getterSource, this.currentlyLoadingPngSkinTexture);
                            } else {
                                LOGGER.error("[FANCYMENU] Failed to get skin by player name! PNG codec returned NULL: " + getterSource);
                                this.current = DEFAULT_SKIN;
                            }
                        } else {
                            this.currentlyLoadingPngSkinTexture = ResourceHandlers.getImageHandler().get(getterSource);
                        }
                        if (this.currentlyLoadingPngSkinTexture == null) {
                            LOGGER.error("[FANCYMENU] Failed to get skin texture! Invalid resource source: " + getterSource);
                            this.current = DEFAULT_SKIN;
                        }
                    } else {
                        this.current = ResourceHandlers.getImageHandler().get(getterSource);
                        if (this.current == null) {
                            LOGGER.error("[FANCYMENU] Failed to get skin texture! Invalid resource source: " + getterSource);
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
                            ResourceLocation loc = this.currentlyLoadingPngSkinTexture.getResourceLocation();
                            if (loc != null) {
                                ITexture converted = modernizePngSkinTexture(loc);
                                this.releaseCurrentlyLoadingPngSkin();
                                if (converted != null) {
                                    ResourceHandlers.getImageHandler().release(convertedSkinKey, false);
                                    ResourceHandlers.getImageHandler().registerIfKeyAbsent(convertedSkinKey, converted);
                                    this.current = converted;
                                } else {
                                    LOGGER.error("[FANCYMENU] Failed to convert old skin to new format: " + getterSource);
                                    this.current = DEFAULT_SKIN;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to get skin resource: " + getterSource + " (" + this.source + ")", ex);
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
        PlayerSkin skin = this.playerNameSkin;
        return (skin != null) && skin.slim();
    }

    protected void downloadPlayerNameSkinMetadata(@NotNull String getterPlayerName) {
        Objects.requireNonNull(getterPlayerName);
        this.startedDownloadingMetadata = true;
        new Thread(() -> {
            String skinUrl = PlayerSkinUtils.getSkinURL(getterPlayerName);
            if (skinUrl != null) {
                skinUrl = ResourceSourceType.WEB.getSourcePrefix() + skinUrl;
            } else {
                LOGGER.error("[FANCYMENU] Failed to get URL of player skin: " + getterPlayerName);
            }
            boolean isSlim = PlayerSkinUtils.hasSlimSkin(getterPlayerName);
            if (!this.startedDownloadingMetadata) return;
            this.playerNameSkin = new PlayerSkin(getterPlayerName, skinUrl, isSlim);
        }).start();
    }

    protected void releaseCurrentlyLoadingPngSkin() {
        if (this.currentlyLoadingPngSkinTexture != null) {
            ResourceHandlers.getImageHandler().release(this.currentlyLoadingPngSkinTexture);
            this.currentlyLoadingPngSkinTexture = null;
        }
    }

    @Override
    public void setSource(@NotNull String source) {
        throw new RuntimeException("You can't update the source of SkinResourceSuppliers.");
    }

    @Override
    public @NotNull ResourceSourceType getResourceSourceType() {
        if (this.sourceIsPlayerName) return ResourceSourceType.WEB;
        return super.getResourceSourceType();
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
    protected static ITexture modernizePngSkinTexture(@NotNull ResourceLocation location) {

        InputStream in = null;
        NativeImage oldTex = null;
        NativeImage newTex = null;
        ITexture iTexture = null;

        try {

            in = Minecraft.getInstance().getResourceManager().open(location);
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

            iTexture = SimpleTexture.of(newTex);

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

    public record PlayerSkin(@NotNull String playerName, @Nullable String resourceSource, boolean slim) {
    }

}
