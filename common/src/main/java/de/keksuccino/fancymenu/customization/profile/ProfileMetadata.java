package de.keksuccino.fancymenu.customization.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Everything a profile tells about itself. Gets stored as {@link ProfileStorage#METADATA_FILE_NAME} in the root of every profile.<br>
 * Profiles get shared between users, so every value gets sanitized on read and write. Links only ever accept http(s) URLs,
 * because opening other URI schemes could launch local programs.
 */
public class ProfileMetadata {

    public static final int FORMAT_VERSION = 1;
    public static final int MAX_TITLE_LENGTH = 64;
    public static final int MAX_VERSION_LENGTH = 32;
    public static final int MAX_AUTHOR_LENGTH = 48;
    public static final int MAX_AUTHORS = 16;
    public static final int MAX_DESCRIPTION_LENGTH = 20000;
    public static final int MAX_URL_LENGTH = 2048;
    public static final int MAX_LINK_LABEL_LENGTH = 32;
    public static final int MAX_LINKS = 16;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @NotNull
    private String id;
    @NotNull
    private String title = "";
    @NotNull
    private String version = "";
    @NotNull
    private List<String> authors = new ArrayList<>();
    @NotNull
    private String description = "";
    @Nullable
    private String issueTrackerUrl;
    @NotNull
    private List<Link> links = new ArrayList<>();
    /** The FancyMenu version that last wrote this profile, so imports can warn about profiles made with newer versions. */
    @Nullable
    private String fancyMenuVersion;

    public ProfileMetadata(@NotNull String id) {
        this.id = Objects.requireNonNull(id);
    }

    @NotNull
    public ProfileMetadata copy() {
        ProfileMetadata copy = new ProfileMetadata(this.id);
        copy.title = this.title;
        copy.version = this.version;
        copy.authors = new ArrayList<>(this.authors);
        copy.description = this.description;
        copy.issueTrackerUrl = this.issueTrackerUrl;
        copy.links = new ArrayList<>(this.links);
        copy.fancyMenuVersion = this.fancyMenuVersion;
        return copy;
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    public void setId(@NotNull String id) {
        this.id = Objects.requireNonNull(id);
    }

    @NotNull
    public String getTitle() {
        return this.title;
    }

    public void setTitle(@Nullable String title) {
        this.title = sanitizeSingleLine(title, MAX_TITLE_LENGTH);
    }

    @NotNull
    public String getVersion() {
        return this.version;
    }

    public void setVersion(@Nullable String version) {
        this.version = sanitizeSingleLine(version, MAX_VERSION_LENGTH);
    }

    @NotNull
    public List<String> getAuthors() {
        return List.copyOf(this.authors);
    }

    public void setAuthors(@Nullable List<String> authors) {
        Set<String> unique = new LinkedHashSet<>();
        if (authors != null) {
            for (String author : authors) {
                String sanitized = sanitizeSingleLine(author, MAX_AUTHOR_LENGTH);
                if (!sanitized.isEmpty()) unique.add(sanitized);
                if (unique.size() >= MAX_AUTHORS) break;
            }
        }
        this.authors = new ArrayList<>(unique);
    }

    /**
     * Splits a comma-separated list of names into authors.
     */
    public void setAuthorsFromString(@Nullable String authors) {
        this.setAuthors((authors == null) ? null : List.of(authors.split(",")));
    }

    @NotNull
    public String getAuthorsAsString() {
        return String.join(", ", this.authors);
    }

    /**
     * The description in Markdown format.
     */
    @NotNull
    public String getDescription() {
        return this.description;
    }

    public void setDescription(@Nullable String description) {
        String normalized = (description == null) ? "" : description.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            int end = Character.isHighSurrogate(normalized.charAt(MAX_DESCRIPTION_LENGTH - 1)) ? (MAX_DESCRIPTION_LENGTH - 1) : MAX_DESCRIPTION_LENGTH;
            normalized = normalized.substring(0, end);
        }
        this.description = normalized;
    }

    @Nullable
    public String getIssueTrackerUrl() {
        return this.issueTrackerUrl;
    }

    public void setIssueTrackerUrl(@Nullable String issueTrackerUrl) {
        this.issueTrackerUrl = normalizeWebUrl(issueTrackerUrl);
    }

    @NotNull
    public List<Link> getLinks() {
        return List.copyOf(this.links);
    }

    public void setLinks(@Nullable List<Link> links) {
        List<Link> sanitized = new ArrayList<>();
        if (links != null) {
            for (Link link : links) {
                Link valid = Link.of(link.type(), link.label(), link.url());
                if (valid != null) sanitized.add(valid);
                if (sanitized.size() >= MAX_LINKS) break;
            }
        }
        this.links = sanitized;
    }

    @Nullable
    public String getFancyMenuVersion() {
        return this.fancyMenuVersion;
    }

    public void setFancyMenuVersion(@Nullable String fancyMenuVersion) {
        String sanitized = sanitizeSingleLine(fancyMenuVersion, MAX_VERSION_LENGTH);
        this.fancyMenuVersion = sanitized.isEmpty() ? null : sanitized;
    }

    @NotNull
    public String toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("format_version", FORMAT_VERSION);
        json.addProperty("id", this.id);
        json.addProperty("title", this.title);
        json.addProperty("version", this.version);
        JsonArray authorsArray = new JsonArray();
        this.authors.forEach(authorsArray::add);
        json.add("authors", authorsArray);
        json.addProperty("description", this.description);
        if (this.issueTrackerUrl != null) json.addProperty("issue_tracker_url", this.issueTrackerUrl);
        JsonArray linksArray = new JsonArray();
        for (Link link : this.links) {
            JsonObject linkJson = new JsonObject();
            linkJson.addProperty("type", link.type().getSerializedName());
            if (!link.label().isEmpty()) linkJson.addProperty("label", link.label());
            linkJson.addProperty("url", link.url());
            linksArray.add(linkJson);
        }
        json.add("links", linksArray);
        if (this.fancyMenuVersion != null) json.addProperty("fancymenu_version", this.fancyMenuVersion);
        return GSON.toJson(json);
    }

    /**
     * Parses metadata leniently. Unknown fields get ignored and invalid values get dropped.
     *
     * @param fallbackId used when the JSON has no valid ID.
     * @throws IllegalArgumentException if the text is no JSON object.
     */
    @NotNull
    public static ProfileMetadata fromJson(@NotNull String jsonText, @NotNull String fallbackId) {
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(jsonText);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Profile metadata is no valid JSON!", ex);
        }
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Profile metadata is no JSON object!");
        JsonObject json = parsed.getAsJsonObject();
        String id = getString(json, "id");
        ProfileMetadata metadata = new ProfileMetadata(((id != null) && ProfileStorage.isValidProfileId(id)) ? id : fallbackId);
        metadata.setTitle(getString(json, "title"));
        metadata.setVersion(getString(json, "version"));
        metadata.setDescription(getString(json, "description"));
        metadata.setIssueTrackerUrl(getString(json, "issue_tracker_url"));
        metadata.setFancyMenuVersion(getString(json, "fancymenu_version"));
        List<String> authors = new ArrayList<>();
        if ((json.get("authors") instanceof JsonArray authorsArray)) {
            for (JsonElement element : authorsArray) {
                if (element.isJsonPrimitive()) authors.add(element.getAsString());
            }
        }
        metadata.setAuthors(authors);
        List<Link> links = new ArrayList<>();
        if ((json.get("links") instanceof JsonArray linksArray)) {
            for (JsonElement element : linksArray) {
                if (!(element instanceof JsonObject linkJson)) continue;
                Link link = Link.of(ProfileLinkType.fromSerializedName(getString(linkJson, "type")), getString(linkJson, "label"), getString(linkJson, "url"));
                if (link != null) links.add(link);
            }
        }
        metadata.setLinks(links);
        return metadata;
    }

    @Nullable
    private static String getString(@NotNull JsonObject json, @NotNull String key) {
        JsonElement element = json.get(key);
        if ((element == null) || !element.isJsonPrimitive()) return null;
        return element.getAsString();
    }

    @NotNull
    static String sanitizeSingleLine(@Nullable String value, int maxLength) {
        if (value == null) return "";
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            builder.append(Character.isISOControl(c) ? ' ' : c);
        }
        String sanitized = builder.toString().strip();
        if (sanitized.length() > maxLength) {
            // Never cut a surrogate pair in half
            int end = Character.isHighSurrogate(sanitized.charAt(maxLength - 1)) ? (maxLength - 1) : maxLength;
            sanitized = sanitized.substring(0, end).stripTrailing();
        }
        return sanitized;
    }

    /**
     * Parses a web URL. URLs without scheme get treated as https URLs, so users can paste links like "discord.gg/abc".
     *
     * @return the parsed http(s) URI, or NULL if the text is no valid web URL.
     */
    @Nullable
    public static URI parseWebUri(@Nullable String url) {
        if (url == null) return null;
        String trimmed = url.strip();
        if (trimmed.isEmpty() || (trimmed.length() > MAX_URL_LENGTH)) return null;
        if (!trimmed.contains("://")) trimmed = "https://" + trimmed;
        try {
            URI uri = new URI(trimmed);
            String scheme = (uri.getScheme() != null) ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
            if (!scheme.equals("http") && !scheme.equals("https")) return null;
            if ((uri.getHost() == null) || uri.getHost().isBlank() || !uri.getHost().contains(".")) return null;
            return uri;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * @return the normalized http(s) URL, or NULL if the text is no valid web URL.
     */
    @Nullable
    public static String normalizeWebUrl(@Nullable String url) {
        URI uri = parseWebUri(url);
        return (uri != null) ? uri.toString() : null;
    }

    /**
     * A link to a social platform or website.
     *
     * @param label only used by {@link ProfileLinkType#OTHER} links. Empty for all other types.
     */
    public record Link(@NotNull ProfileLinkType type, @NotNull String label, @NotNull String url) {

        /**
         * @return the sanitized link, or NULL if the URL is invalid.
         */
        @Nullable
        public static Link of(@NotNull ProfileLinkType type, @Nullable String label, @Nullable String url) {
            String normalizedUrl = normalizeWebUrl(url);
            if (normalizedUrl == null) return null;
            String sanitizedLabel = (type == ProfileLinkType.OTHER) ? sanitizeSingleLine(label, MAX_LINK_LABEL_LENGTH) : "";
            return new Link(type, sanitizedLabel, normalizedUrl);
        }

    }

}
