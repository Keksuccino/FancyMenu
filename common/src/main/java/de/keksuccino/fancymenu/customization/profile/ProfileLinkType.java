package de.keksuccino.fancymenu.customization.profile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.net.URI;
import java.util.Locale;

/**
 * Platforms a profile can link to. The serialized name is stored in profile metadata files, so it must never change.
 */
public enum ProfileLinkType {

    WEBSITE("website", null),
    DISCORD("discord", "Discord", "discord.gg", "discord.com", "discordapp.com"),
    YOUTUBE("youtube", "YouTube", "youtube.com", "youtu.be"),
    X("x", "X", "x.com", "twitter.com"),
    TWITCH("twitch", "Twitch", "twitch.tv"),
    BLUESKY("bluesky", "Bluesky", "bsky.app"),
    GITHUB("github", "GitHub", "github.com"),
    MODRINTH("modrinth", "Modrinth", "modrinth.com"),
    CURSEFORGE("curseforge", "CurseForge", "curseforge.com"),
    PATREON("patreon", "Patreon", "patreon.com"),
    KOFI("kofi", "Ko-fi", "ko-fi.com"),
    REDDIT("reddit", "Reddit", "reddit.com", "redd.it"),
    INSTAGRAM("instagram", "Instagram", "instagram.com"),
    TIKTOK("tiktok", "TikTok", "tiktok.com"),
    OTHER("other", null);

    @NotNull
    private final String serializedName;
    /** The platform's brand name, which never gets localized. NULL for generic types that use a localized name instead. */
    @Nullable
    private final String brandName;
    @NotNull
    private final String[] hosts;

    ProfileLinkType(@NotNull String serializedName, @Nullable String brandName, @NotNull String... hosts) {
        this.serializedName = serializedName;
        this.brandName = brandName;
        this.hosts = hosts;
    }

    @NotNull
    public String getSerializedName() {
        return this.serializedName;
    }

    @Nullable
    public String getBrandName() {
        return this.brandName;
    }

    @NotNull
    public static ProfileLinkType fromSerializedName(@Nullable String name) {
        if (name != null) {
            String normalized = name.trim().toLowerCase(Locale.ROOT);
            for (ProfileLinkType type : values()) {
                if (type.serializedName.equals(normalized)) return type;
            }
        }
        return OTHER;
    }

    /**
     * Guesses the platform of a URL by its host, so users only need to paste a link.
     *
     * @return the matching platform, or {@link #WEBSITE} if no known platform matches.
     */
    @NotNull
    public static ProfileLinkType detectFromUrl(@Nullable String url) {
        URI uri = ProfileMetadata.parseWebUri(url);
        if ((uri == null) || (uri.getHost() == null)) return WEBSITE;
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        for (ProfileLinkType type : values()) {
            for (String knownHost : type.hosts) {
                if (host.equals(knownHost) || host.endsWith("." + knownHost)) return type;
            }
        }
        return WEBSITE;
    }

}
