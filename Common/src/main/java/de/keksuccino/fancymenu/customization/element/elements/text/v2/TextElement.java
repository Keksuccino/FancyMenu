package de.keksuccino.fancymenu.customization.element.elements.text.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.ListUtils;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.text.markdown.MarkdownRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v2.scrollarea.entry.ScrollAreaEntry;
import de.keksuccino.fancymenu.util.resources.texture.LocalTexture;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.web.WebUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("all")
public class TextElement extends AbstractElement {

    //TODO FIXEN: (allgemein, nicht nur text element): es werden keine snapshots erstellt, wenn element resized wird

    //TODO FIXEN: Hover bei hyperlink broke

    //TODO FIXEN: multi-line code block background broke

    //TODO add toggle to automatically remove all <br> from source

    @NotNull
    protected SourceMode sourceMode = SourceMode.DIRECT;
    protected String source; //direct text, file path, link
    protected String lastWebOrLocalSource = null;
    @Nullable
    protected volatile String text = null;
    protected String lastText = null;
    public String verticalScrollGrabberTextureNormal = null;
    public String verticalScrollGrabberTextureHover = null;
    public String horizontalScrollGrabberTextureNormal = null;
    public String horizontalScrollGrabberTextureHover = null;
    public String scrollGrabberColorHexNormal = null;
    public String scrollGrabberColorHexHover = null;
    public boolean enableScrolling = true;
    @NotNull
    public volatile MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    @NotNull
    public volatile ScrollArea scrollArea;

    public TextElement(@NotNull ElementBuilder<?, ?> builder) {

        super(builder);

        this.scrollArea = new ScrollArea(0, 0, this.getAbsoluteWidth(), this.getAbsoluteHeight()) {
            @Override
            public void updateScrollArea() {
                super.updateScrollArea();
                //Manually update scroll bar area size, so the grabbers are outside the area
                if (Minecraft.getInstance().screen != null) {
                    this.verticalScrollBar.scrollAreaEndX = TextElement.this.getAbsoluteX() + TextElement.this.getAbsoluteWidth() + 12;
                    this.horizontalScrollBar.scrollAreaEndY = TextElement.this.getAbsoluteY() + TextElement.this.getAbsoluteHeight() + 12;
                }
            }
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (isEditor()) {
                    if (this.verticalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
                    if (this.horizontalScrollBar.mouseClicked(mouseX, mouseY, button)) return true;
                    return false;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                if (isEditor()) return false;
                return super.isMouseOver(mouseX, mouseY);
            }
        };
        this.scrollArea.minimumEntryWidthIsAreaWidth = false;
        this.scrollArea.makeEntriesWidthOfArea = false;
        this.scrollArea.makeAllEntriesWidthOfWidestEntry = false;
        this.scrollArea.verticalScrollBar.grabberWidth = 10;
        this.scrollArea.verticalScrollBar.grabberHeight = 20;
        this.scrollArea.horizontalScrollBar.grabberWidth = 20;
        this.scrollArea.horizontalScrollBar.grabberHeight = 10;
        this.scrollArea.backgroundColor = () -> DrawableColor.of(0,0,0,0);
        this.scrollArea.borderColor = () -> DrawableColor.of(0,0,0,0);

        this.scrollArea.addEntry(new MarkdownRendererEntry(this.scrollArea, this.markdownRenderer));

        //Don't render markdown lines outside of visible area (for performance reasons)
        this.markdownRenderer.addLineRenderValidator(line -> {
            if ((line.parent.getY() + line.offsetY + line.getLineHeight()) < this.getAbsoluteY()) {
                return false;
            }
            if ((line.parent.getY() + line.offsetY) > (this.getAbsoluteY() + this.getAbsoluteHeight())) {
                return false;
            }
            return true;
        });

    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            try {

                this.tick();

                RenderSystem.enableBlend();
                this.scrollArea.setX(this.getAbsoluteX(), true);
                this.scrollArea.setY(this.getAbsoluteY(), true);
                this.scrollArea.setWidth(this.getAbsoluteWidth(), true);
                this.scrollArea.setHeight(this.getAbsoluteHeight(), true);
                this.scrollArea.render(pose, mouseX, mouseY, partial);

            } catch (Exception e) {
                e.printStackTrace();
            }

            RenderingUtils.resetShaderColor();

        }

    }

    @Override
    public @Nullable List<GuiEventListener> getWidgetsToRegister() {
        return ListUtils.build(this.markdownRenderer, this.scrollArea);
    }

    protected void tick() {

        //If placeholders used in the URL or path change, update content
        if ((this.sourceMode == SourceMode.WEB_SOURCE) || (this.sourceMode == SourceMode.LOCAL_SOURCE)) {
            String source = (this.source != null) ? StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(this.source), "§", "&") : null;
            if ((this.lastWebOrLocalSource != null) && (source != null) && !this.lastWebOrLocalSource.equals(source)) {
                this.updateContent();
            }
            this.lastWebOrLocalSource = source;
        } else {
            this.lastWebOrLocalSource = null;
        }

        //Update markdown renderer text if changed
        String t = this.text;
        if ((t != null) && ((this.lastText == null) || !this.lastText.equals(t))) {
            this.markdownRenderer.setText(t);
        }
        this.lastText = t;

        //Update scroll wheel ONLY FOR vertical bar (horizontal never scrolls via scroll wheel)
        this.scrollArea.verticalScrollBar.setScrollWheelAllowed(this.enableScrolling);

        //Update active state of scroll bars
        this.scrollArea.verticalScrollBar.active = (this.scrollArea.getTotalEntryHeight() > this.scrollArea.getInnerHeight()) && this.enableScrolling;
        this.scrollArea.horizontalScrollBar.active = (this.scrollArea.getTotalEntryWidth() > this.scrollArea.getInnerWidth()) && this.enableScrolling;

        //Update scroll grabber colors
        if (this.scrollGrabberColorHexNormal != null) {
            DrawableColor c = DrawableColor.of(this.scrollGrabberColorHexNormal);
            this.scrollArea.verticalScrollBar.idleBarColor = () -> c;
            this.scrollArea.horizontalScrollBar.idleBarColor = () -> c;
        }
        if (this.scrollGrabberColorHexHover != null) {
            DrawableColor c = DrawableColor.of(this.scrollGrabberColorHexHover);
            this.scrollArea.verticalScrollBar.hoverBarColor = () -> c;
            this.scrollArea.horizontalScrollBar.hoverBarColor = () -> c;
        }

        //Update scroll grabber textures
        if (this.verticalScrollGrabberTextureNormal != null) {
            LocalTexture r = TextureHandler.INSTANCE.getTexture(this.verticalScrollGrabberTextureNormal);
            if (r != null) {
                this.scrollArea.verticalScrollBar.idleBarTexture = r.getResourceLocation();
            }
        } else {
            this.scrollArea.verticalScrollBar.idleBarTexture = null;
        }
        if (this.verticalScrollGrabberTextureHover != null) {
            LocalTexture r = TextureHandler.INSTANCE.getTexture(this.verticalScrollGrabberTextureHover);
            if (r != null) {
                this.scrollArea.verticalScrollBar.hoverBarTexture = r.getResourceLocation();
            }
        } else {
            this.scrollArea.verticalScrollBar.hoverBarTexture = null;
        }
        if (this.horizontalScrollGrabberTextureNormal != null) {
            LocalTexture r = TextureHandler.INSTANCE.getTexture(this.horizontalScrollGrabberTextureNormal);
            if (r != null) {
                this.scrollArea.horizontalScrollBar.idleBarTexture = r.getResourceLocation();
            }
        } else {
            this.scrollArea.horizontalScrollBar.idleBarTexture = null;
        }
        if (this.horizontalScrollGrabberTextureHover != null) {
            LocalTexture r = TextureHandler.INSTANCE.getTexture(this.horizontalScrollGrabberTextureHover);
            if (r != null) {
                this.scrollArea.horizontalScrollBar.hoverBarTexture = r.getResourceLocation();
            }
        } else {
            this.scrollArea.horizontalScrollBar.hoverBarTexture = null;
        }

    }

    public void updateContent() {

        new Thread(() -> {

            List<String> linesRaw = new ArrayList<>();

            try {

                if ((this.source != null) && !this.source.equals("")) {
                    if (this.sourceMode == SourceMode.DIRECT) {
                        String s = this.source.replace("%n%", "\n").replace("\r", "\n");
                        if (s.contains("\n")) {
                            linesRaw.addAll(Arrays.asList(StringUtils.splitLines(s, "\n")));
                        } else {
                            linesRaw.add(s);
                        }
                    } else if (this.sourceMode == SourceMode.LOCAL_SOURCE) {
                        File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(PlaceholderParser.replacePlaceholders(this.source)));
                        if (f.isFile()) {
                            linesRaw.addAll(FileUtils.getFileLines(f));
                        }
                    } else if (this.sourceMode == SourceMode.WEB_SOURCE) {
                        String s = StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(this.source), "§", "&");
                        if (WebUtils.isValidUrl(s)) {
                            //Get raw GitHub file
                            if (s.toLowerCase().contains("/blob/") && (s.toLowerCase().startsWith("http://github.com/")
                                    || s.toLowerCase().startsWith("https://github.com/")|| s.toLowerCase().startsWith("http://www.github.com/")
                                    || s.toLowerCase().startsWith("https://www.github.com/"))) {
                                String path = s.replace("//", "").split("/", 2)[1].replace("/blob/", "/");
                                s = "https://raw.githubusercontent.com/" + path;
                            }
                            //Get raw Pastebin file
                            if (!s.toLowerCase().contains("/raw/") && (s.toLowerCase().startsWith("http://pastebin.com/")
                                    || s.toLowerCase().startsWith("https://pastebin.com/")|| s.toLowerCase().startsWith("http://www.pastebin.com/")
                                    || s.toLowerCase().startsWith("https://www.pastebin.com/"))) {
                                String path = s.replace("//", "").split("/", 2)[1];
                                s = "https://pastebin.com/raw/" + path;
                            }
                            BufferedReader r = null;
                            try {
                                URL url = new URL(s);
                                r = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
                                String s2 = r.readLine();
                                while(s2 != null) {
                                    linesRaw.add(s2);
                                    s2 = r.readLine();
                                }
                                IOUtils.closeQuietly(r);
                            } catch (Exception ex) {
                                if (r != null) {
                                    IOUtils.closeQuietly(r);
                                }
                                linesRaw.clear();
                            }
                        }
                    }
                } else {
                    linesRaw.add(I18n.get("fancymenu.customization.items.text.placeholder"));
                }

            } catch (Exception ex) {
                linesRaw.clear();
            }

            if (linesRaw.isEmpty()) {
                if (isEditor()) {
                    linesRaw.add(I18n.get("fancymenu.customization.items.text.status.unable_to_load"));
                } else {
                    linesRaw.add("");
                }
            }

            String text = "";
            for (String s : linesRaw) {
                if (!text.isEmpty()) {
                    text += "\n";
                }
                text += s;
            }
            this.text = text;

        }).start();

    }

    public void setSource(@NotNull SourceMode sourceMode, @NotNull String source) {
        this.sourceMode = Objects.requireNonNull(sourceMode);
        this.source = Objects.requireNonNull(source);
        this.text = null;
        this.lastText = null;
        this.lastWebOrLocalSource = null;
        this.updateContent();
    }

    public static enum SourceMode {

        DIRECT("direct"),
        LOCAL_SOURCE("local"),
        WEB_SOURCE("web");

        final String name;

        SourceMode(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static SourceMode getByName(String name) {
            for (SourceMode i : SourceMode.values()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

    }

    protected static class MarkdownRendererEntry extends ScrollAreaEntry {

        protected MarkdownRenderer markdownRenderer;

        public MarkdownRendererEntry(ScrollArea parent, MarkdownRenderer markdownRenderer) {
            super(parent, 20, 20);
            this.markdownRenderer = markdownRenderer;
            this.selectable = false;
            this.playClickSound = false;
            this.backgroundColorNormal = () -> DrawableColor.of(0,0,0,0);
            this.backgroundColorHover = () -> DrawableColor.of(0,0,0,0);
        }

        @Override
        public void renderEntry(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
            this.markdownRenderer.setOptimalWidth(this.parent.getInnerWidth());
            this.markdownRenderer.setX(this.x);
            this.markdownRenderer.setY(this.y);
            this.setWidth(this.markdownRenderer.getRealWidth());
            this.setHeight(this.markdownRenderer.getRealHeight());
//            LogManager.getLogger().info("################ SCROLL ENTRY HEIGHT: " + this.getHeight() + " | AREA HEIGHT: " + this.parent.getTotalEntryHeight() + " | AREA SCROLL HEIGHT: " + this.parent.getTotalScrollHeight() + " | MARKDOWN RENDERER HEIGHT: " + this.markdownRenderer.getRealHeight() + " | SCROLL: " + this.parent.verticalScrollBar.getScroll());
            this.markdownRenderer.render(pose, mouseX, mouseY, partial);
        }

        @Override
        public void onClick(ScrollAreaEntry entry, double mouseX, double mouseY, int button) {
        }

    }

}
