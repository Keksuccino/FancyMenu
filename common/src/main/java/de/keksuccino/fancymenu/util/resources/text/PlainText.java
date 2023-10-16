package de.keksuccino.fancymenu.util.resources.text;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class PlainText implements IText {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    protected volatile List<String> lines = null;
    protected ResourceLocation sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected boolean closed = false;

    @NotNull
    public static PlainText location(@NotNull ResourceLocation location) {
        return location(location, null);
    }

    @NotNull
    public static PlainText location(@NotNull ResourceLocation location, @Nullable PlainText writeTo) {

        Objects.requireNonNull(location);
        PlainText text = (writeTo != null) ? writeTo : new PlainText();

        text.sourceLocation = location;

        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), text);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read text content from ResourceLocation: " + location, ex);
        }

        return text;

    }

    @NotNull
    public static PlainText local(@NotNull File textFile) {
        return local(textFile, null);
    }

    @NotNull
    public static PlainText local(@NotNull File textFile, @Nullable PlainText writeTo) {

        Objects.requireNonNull(textFile);
        PlainText text = (writeTo != null) ? writeTo : new PlainText();

        text.sourceFile = textFile;

        if (!textFile.isFile()) {
            LOGGER.error("[FANCYMENU] Failed to read text content from file! File not found: " + textFile.getPath());
            return text;
        }

        try {
            of(new FileInputStream(textFile), textFile.getPath(), text);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read text content from file: " + textFile.getPath(), ex);
        }

        return text;

    }

    @NotNull
    public static PlainText web(@NotNull String textFileUrl) {
        return web(textFileUrl, null);
    }

    @NotNull
    public static PlainText web(@NotNull String textFileUrl, @Nullable PlainText writeTo) {

        Objects.requireNonNull(textFileUrl);
        PlainText text = (writeTo != null) ? writeTo : new PlainText();

        text.sourceURL = textFileUrl;

        if (!TextValidators.BASIC_URL_TEXT_VALIDATOR.get(textFileUrl)) {
            LOGGER.error("[FANCYMENU] Failed to read text content from URL! Invalid URL: " + textFileUrl);
            return text;
        }

        try {
            InputStream in = WebUtils.openResourceStream(textFileUrl);
            if (in != null) {
                of(in, textFileUrl, text);
            } else {
                LOGGER.error("[FANCYMENU] Failed to read text content from URL! InputStream was NULL: " + textFileUrl);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read text content from URL: " + textFileUrl, ex);
        }

        return text;

    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static PlainText of(@NotNull InputStream in) {
        return of(in, null, null);
    }

    /**
     * Closes the passed {@link InputStream}!
     */
    @NotNull
    public static PlainText of(@NotNull InputStream in, @Nullable String textSourceName, @Nullable PlainText writeTo) {

        Objects.requireNonNull(in);
        PlainText text = (writeTo != null) ? writeTo : new PlainText();

        if (textSourceName == null) textSourceName = "[Generic InputStream source]";

        try {
            text.lines = FileUtils.readTextLinesFrom(in);
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to read text context via InputStream: " + textSourceName, ex);
        }

        CloseableUtils.closeQuietly(in);

        return text;

    }

    protected PlainText() {
    }

    @Override
    public @Nullable List<String> getTextLines() {
        return this.lines;
    }

    @Override
    public void reload() {
        if (this.closed) return;
        this.lines = null;
        if (this.sourceLocation != null) {
            location(this.sourceLocation, this);
        } else if (this.sourceFile != null) {
            local(this.sourceFile, this);
        } else if (this.sourceURL != null) {
            web(this.sourceURL, this);
        }
    }

    @Override
    public void close() {
        this.closed = true;
        this.lines = null;
    }

}
