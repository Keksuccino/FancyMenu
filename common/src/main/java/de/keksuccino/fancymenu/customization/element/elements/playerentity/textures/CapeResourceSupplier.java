package de.keksuccino.fancymenu.customization.element.elements.playerentity.textures;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.PlayerSkinUtils;
import de.keksuccino.fancymenu.util.file.type.FileMediaType;
import de.keksuccino.fancymenu.util.file.type.types.FileTypes;
import de.keksuccino.fancymenu.util.resource.ResourceHandlers;
import de.keksuccino.fancymenu.util.resource.ResourceSource;
import de.keksuccino.fancymenu.util.resource.ResourceSourceType;
import de.keksuccino.fancymenu.util.resource.ResourceSupplier;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import de.keksuccino.fancymenu.util.resource.resources.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CapeResourceSupplier extends ResourceSupplier<ITexture> {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final ResourceLocation DEFAULT_CAPE_LOCATION = new ResourceLocation("fancymenu", "textures/player_entity/default_cape_texture.png");
    public static final SimpleTexture DEFAULT_CAPE = SimpleTexture.location(DEFAULT_CAPE_LOCATION);
    protected static final Map<String, String> CACHED_PLAYER_NAME_CAPE_URLS = new HashMap<>();

    protected boolean sourceIsPlayerName;
    @Nullable
    protected volatile String playerNameCapeUrl;
    protected volatile boolean startedFindingPlayerNameCapeUrl = false;
    @Nullable
    protected String lastGetterPlayerName;

    public CapeResourceSupplier(@NotNull String source, boolean sourceIsPlayerName) {
        super(ITexture.class, FileMediaType.IMAGE, source);
        this.sourceIsPlayerName = sourceIsPlayerName;
    }

    @Override
    @NotNull
    public ITexture get() {
        String playerNameCapeUrlCached = this.playerNameCapeUrl;
        String getterPlayerName = PlaceholderParser.replacePlaceholders(this.source, false);
        if (this.sourceIsPlayerName) {
            if (!getterPlayerName.equals(this.lastGetterPlayerName)) {
                this.startedFindingPlayerNameCapeUrl = false;
                this.current = null;
                this.playerNameCapeUrl = null;
                playerNameCapeUrlCached = null;
            }
            this.lastGetterPlayerName = getterPlayerName;
            if (playerNameCapeUrlCached == null) {
                if (!this.startedFindingPlayerNameCapeUrl) {
                    if (CACHED_PLAYER_NAME_CAPE_URLS.containsKey(getterPlayerName)) {
                        this.startedFindingPlayerNameCapeUrl = true;
                        this.playerNameCapeUrl = CACHED_PLAYER_NAME_CAPE_URLS.get(getterPlayerName);
                        playerNameCapeUrlCached = this.playerNameCapeUrl;
                    } else {
                        this.findPlayerNameCapeUrl(getterPlayerName);
                    }
                }
            }
            if (playerNameCapeUrlCached == null) return DEFAULT_CAPE;
        }
        if ((this.current != null) && this.current.isClosed()) {
            this.current = null;
        }
        String getterSource = PlaceholderParser.replacePlaceholders(this.source, false);
        if (this.sourceIsPlayerName && (playerNameCapeUrlCached == null)) {
            return DEFAULT_CAPE;
        }
        if (this.sourceIsPlayerName) getterSource = ResourceSourceType.WEB.getSourcePrefix() + playerNameCapeUrlCached;
        if (!getterSource.equals(this.lastGetterSource)) {
            this.current = null;
        }
        this.lastGetterSource = getterSource;
        if (this.current == null) {
            ResourceSource resourceSource = ResourceSource.of(getterSource);
            if (this.sourceIsPlayerName && !CACHED_PLAYER_NAME_CAPE_URLS.containsKey(getterPlayerName)) CACHED_PLAYER_NAME_CAPE_URLS.put(getterPlayerName, playerNameCapeUrlCached);
            try {
                if (this.sourceIsPlayerName) {
                    this.current = ResourceHandlers.getImageHandler().hasResource(resourceSource.getSourceWithPrefix()) ? ResourceHandlers.getImageHandler().get(resourceSource) : FileTypes.PNG_IMAGE.getCodec().readWeb(resourceSource.getSourceWithoutPrefix());
                    if (this.current != null) {
                        ResourceHandlers.getImageHandler().registerIfKeyAbsent(resourceSource.getSourceWithPrefix(), this.current);
                    } else {
                        LOGGER.error("[FANCYMENU] CapeResourceSupplier failed to get cape by player name! PNG codec returned NULL: " + resourceSource);
                        this.current = DEFAULT_CAPE;
                    }
                } else {
                    this.current = ResourceHandlers.getImageHandler().get(resourceSource);
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] CapeResourceSupplier failed to get cape: " + resourceSource + " (" + this.source + ")", ex);
            }
        }
        return (this.current != null) ? this.current : DEFAULT_CAPE;
    }

    @NotNull
    public ResourceLocation getCapeLocation() {
        ResourceLocation loc = this.get().getResourceLocation();
        return (loc != null) ? loc : DEFAULT_CAPE_LOCATION;
    }

    protected void findPlayerNameCapeUrl(@NotNull String getterPlayerName) {
        Objects.requireNonNull(getterPlayerName);
        this.startedFindingPlayerNameCapeUrl = true;
        new Thread(() -> {
            String capeUrl = PlayerSkinUtils.getCapeURL(getterPlayerName);
            if (capeUrl == null) {
                LOGGER.error("[FANCYMENU] CapeResourceSupplier failed to get URL of player cape: " + getterPlayerName);
            }
            if (!this.startedFindingPlayerNameCapeUrl) return;
            this.playerNameCapeUrl = capeUrl;
        }).start();
    }

    @Override
    public void setSource(@NotNull String source) {
        throw new RuntimeException("You can't update the source of CapeResourceSuppliers.");
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

}
