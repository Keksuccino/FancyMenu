package de.keksuccino.fancymenu.util.resource.resources.text;

import de.keksuccino.fancymenu.util.CloseableUtils;
import de.keksuccino.fancymenu.util.WebUtils;
import de.keksuccino.fancymenu.util.file.FileUtils;
import de.keksuccino.fancymenu.util.input.TextValidators;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class PlainText implements IText {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    protected volatile List<String> lines = null;
    protected Identifier sourceLocation;
    protected File sourceFile;
    protected String sourceURL;
    protected volatile boolean decoded = false;
    protected volatile boolean loadingCompleted = false;
    protected volatile boolean loadingFailed = false;
    protected volatile boolean closed = false;

    @NotNull
    public static PlainText location(@NotNull Identifier location) {
        return location(location, null);
    }

    @NotNull
    public static PlainText location(@NotNull Identifier location, @Nullable PlainText writeTo) {

        Objects.requireNonNull(location);
        PlainText text = (writeTo != null) ? writeTo : new PlainText();

        text.sourceLocation = location;

        try {
            of(Minecraft.getInstance().getResourceManager().open(location), location.toString(), text);
        } catch (Exception ex) {
            text.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read text content from Identifier: " + location, ex);
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
            text.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read text content from file! File not found: " + textFile.getPath());
            return text;
        }

        try {
            of(new FileInputStream(textFile), textFile.getPath(), text);
        } catch (Exception ex) {
            text.loadingFailed = true;
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
            text.loadingFailed = true;
            LOGGER.error("[FANCYMENU] Failed to read text content from URL! Invalid URL: " + textFileUrl);
            return text;
        }

        //Get raw GitHub file
        if (textFileUrl.toLowerCase().contains("/blob/") && (textFileUrl.toLowerCase().startsWith("http://github.com/")
                || textFileUrl.toLowerCase().startsWith("https://github.com/")|| textFileUrl.toLowerCase().startsWith("http://www.github.com/")
                || textFileUrl.toLowerCase().startsWith("https://www.github.com/"))) {
            String path = textFileUrl.replace("//", "").split("/", 2)[1].replace("/blob/", "/");
            textFileUrl = "https://raw.githubusercontent.com/" + path;
        }
        //Get raw Pastebin file
        if (!textFileUrl.toLowerCase().contains("/raw/") && (textFileUrl.toLowerCase().startsWith("http://pastebin.com/")
                || textFileUrl.toLowerCase().startsWith("https://pastebin.com/")|| textFileUrl.toLowerCase().startsWith("http://www.pastebin.com/")
                || textFileUrl.toLowerCase().startsWith("https://www.pastebin.com/"))) {
            String path = textFileUrl.replace("//", "").split("/", 2)[1];
            textFileUrl = "https://pastebin.com/raw/" + path;
        }

        String url = textFileUrl;
        new Thread(() -> {
            try {
                InputStream in = WebUtils.openResourceStream(url);
                if (in != null) {
                    of(in, url, text);
                } else {
                    text.loadingFailed = true;
                    LOGGER.error("[FANCYMENU] Failed to read text content from URL! InputStream was NULL: " + url);
                }
            } catch (Exception ex) {
                text.loadingFailed = true;
                LOGGER.error("[FANCYMENU] Failed to read text content from URL: " + url, ex);
            }
        }).start();

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

        String name = textSourceName;
        new Thread(() -> {
            try {
                text.lines = FileUtils.readTextLinesFrom(in);
                text.decoded = true;
                text.loadingCompleted = true;
                if (text.closed) MainThreadTaskExecutor.executeInMainThread(text::close, MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            } catch (Exception ex) {
                text.loadingFailed = true;
                LOGGER.error("[FANCYMENU] Failed to read text context via InputStream: " + name, ex);
            }
            CloseableUtils.closeQuietly(in);
        }).start();

        return text;

    }

    protected PlainText() {
    }

    @Override
    public @Nullable List<String> getTextLines() {
        return this.lines;
    }

    @Override
    public @Nullable InputStream open() throws IOException {
        if (this.sourceURL != null) return WebUtils.openResourceStream(this.sourceURL);
        if (this.sourceFile != null) return new FileInputStream(this.sourceFile);
        if (this.sourceLocation != null) return Minecraft.getInstance().getResourceManager().open(this.sourceLocation);
        return null;
    }

    @Override
    public boolean isReady() {
        return this.decoded;
    }

    @Override
    public boolean isLoadingCompleted() {
        return !this.closed && !this.loadingFailed && this.loadingCompleted;
    }

    @Override
    public boolean isLoadingFailed() {
        return this.loadingFailed;
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        this.closed = true;
        this.decoded = false;
        this.lines = null;
    }

}
