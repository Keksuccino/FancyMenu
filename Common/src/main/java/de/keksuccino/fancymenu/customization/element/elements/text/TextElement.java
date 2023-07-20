package de.keksuccino.fancymenu.customization.element.elements.text;

import com.google.common.collect.LinkedListMultimap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.ScrollArea;
import de.keksuccino.fancymenu.util.rendering.ui.scroll.v1.scrollarea.entry.TextScrollAreaEntry;
import de.keksuccino.fancymenu.util.resources.texture.LocalTexture;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.fancymenu.util.resources.texture.TextureHandler;
import de.keksuccino.konkrete.web.WebUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.NotNull;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TextElement extends AbstractElement {

    public SourceMode sourceMode = SourceMode.DIRECT;
    public String source; //direct text, file path, link
    public boolean shadow = true;
    public CaseMode caseMode = CaseMode.NORMAL;
    public float scale = 1.0F;
    public Alignment alignment = Alignment.LEFT;
    public String baseColorHex = null;
    public int textBorder = 0;
    public int lineSpacing = 1;
    public String scrollGrabberTextureNormal = null;
    public String scrollGrabberTextureHover = null;
    public String scrollGrabberColorHexNormal = null;
    public String scrollGrabberColorHexHover = null;
    public boolean enableScrolling = true;

    protected Font font = Minecraft.getInstance().font;
    public volatile ScrollArea scrollArea;
    public volatile LinkedListMultimap<String, Float> lines = LinkedListMultimap.create();
    public volatile boolean updating = false;

    public TextElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    public void updateScrollArea() {

        this.scrollArea = new ScrollArea(0, 0, this.getAbsoluteWidth(), this.getAbsoluteHeight()) {
            @Override
            public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {
                super.render(matrix, mouseX, mouseY, partial);
                this.verticalScrollBar.active = (this.getTotalEntryHeight() > this.getInnerHeight()) && TextElement.this.enableScrolling;
            }
            @Override
            public void updateScrollArea() {
                super.updateScrollArea();
                if (Minecraft.getInstance().screen != null) {
                    this.verticalScrollBar.scrollAreaEndX = TextElement.this.getAbsoluteX() + TextElement.this.getAbsoluteWidth() + 12;
                    this.horizontalScrollBar.scrollAreaEndY = TextElement.this.getAbsoluteY() + TextElement.this.getAbsoluteHeight() + 12;
                }
            }
        };
        this.scrollArea.verticalScrollBar.grabberWidth = 10;
        this.scrollArea.verticalScrollBar.grabberHeight = 20;
        this.scrollArea.horizontalScrollBar.grabberWidth = 20;
        this.scrollArea.horizontalScrollBar.grabberHeight = 10;
        this.scrollArea.backgroundColor = new Color(0,0,0,0);
        this.scrollArea.borderColor = new Color(0,0,0,0);
        this.scrollArea.verticalScrollBar.setScrollWheelAllowed(this.enableScrolling);
        this.scrollArea.verticalScrollBar.active = false;
        this.scrollArea.horizontalScrollBar.active = false;

        if (this.scrollGrabberColorHexNormal != null) {
            Color c = de.keksuccino.konkrete.rendering.RenderUtils.getColorFromHexString(this.scrollGrabberColorHexNormal);
            if (c != null) {
                this.scrollArea.verticalScrollBar.idleBarColor = c;
                this.scrollArea.horizontalScrollBar.idleBarColor = c;
            }
        }
        if (this.scrollGrabberColorHexHover != null) {
            Color c = de.keksuccino.konkrete.rendering.RenderUtils.getColorFromHexString(this.scrollGrabberColorHexHover);
            if (c != null) {
                this.scrollArea.verticalScrollBar.hoverBarColor = c;
                this.scrollArea.horizontalScrollBar.hoverBarColor = c;
            }
        }

        if (this.scrollGrabberTextureNormal != null) {
            LocalTexture r = TextureHandler.INSTANCE.getTexture(this.scrollGrabberTextureNormal);
            if (r != null) {
                this.scrollArea.verticalScrollBar.idleBarTexture = r.getResourceLocation();
                this.scrollArea.horizontalScrollBar.idleBarTexture = r.getResourceLocation();
            }
        }
        if (this.scrollGrabberTextureHover != null) {
            LocalTexture r = TextureHandler.INSTANCE.getTexture(this.scrollGrabberTextureHover);
            if (r != null) {
                this.scrollArea.verticalScrollBar.hoverBarTexture = r.getResourceLocation();
                this.scrollArea.horizontalScrollBar.hoverBarTexture = r.getResourceLocation();
            }
        }

        LineScrollEntry borderTop = new LineScrollEntry(this.scrollArea, " ", false, 1.0F, this);
        borderTop.setHeight(this.textBorder);
        this.scrollArea.addEntry(borderTop);

        for (Map.Entry<String, Float> m : this.lines.entries()) {
            this.scrollArea.addEntry(new LineScrollEntry(this.scrollArea, m.getKey(), false, this.scale, this));
        }

        LineScrollEntry borderBottom = new LineScrollEntry(this.scrollArea, " ", false, 1.0F, this);
        borderBottom.setHeight(this.textBorder);
        this.scrollArea.addEntry(borderBottom);

    }

    public Color getBaseColor() {
        if (this.baseColorHex != null) {
            return de.keksuccino.konkrete.rendering.RenderUtils.getColorFromHexString(this.baseColorHex);
        }
        return null;
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        try {

            if (this.shouldRender()) {

                if (!this.updating) {

                    RenderSystem.enableBlend();

                    if (this.scrollArea != null) {
                        this.scrollArea.customGuiScale = this.customGuiScale;
                        this.scrollArea.setX(this.getAbsoluteX(), true);
                        this.scrollArea.setY(this.getAbsoluteY(), true);
                        this.scrollArea.setWidth(this.getAbsoluteWidth(), true);
                        this.scrollArea.setHeight(this.getAbsoluteHeight(), true);
                        this.scrollArea.render(pose, MouseInput.getMouseX(), MouseInput.getMouseY(), RenderingUtils.getPartialTick());
                    }

                } else if (isEditor()) {
                    //Render "updating" view in editor
                    fill(pose, this.getAbsoluteX(), this.getAbsoluteY(), this.getAbsoluteX() + this.getAbsoluteWidth(), this.getAbsoluteY() + this.getAbsoluteHeight(), Color.MAGENTA.getRGB());
                    drawCenteredString(pose, font, Component.translatable("fancymenu.customization.items.text.status.loading"), this.getAbsoluteX() + (this.getAbsoluteWidth() / 2), this.getAbsoluteY() + (this.getAbsoluteHeight() / 2) - (font.lineHeight / 2), -1);
                }

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateContent() {

        if (this.updating) {
            return;
        }

        updating = true;

        new Thread(() -> {

            List<String> linesRaw = new ArrayList<>();

            try {

                if ((this.source != null) && !this.source.equals("")) {
                    if (this.sourceMode == SourceMode.DIRECT) {
                        if (this.source.replace("\\n", "%n%").replace("\\r", "%n%").contains("%n%")) {
                            linesRaw.addAll(Arrays.asList(StringUtils.splitLines(this.source.replace("\\n", "%n%").replace("\\r", "%n%"), "%n%")));
                        } else {
                            linesRaw.add(this.source);
                        }
                    } else if (this.sourceMode == SourceMode.LOCAL_SOURCE) {
                        File f = new File(PlaceholderParser.replacePlaceholders(this.source));
                        if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                            f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + PlaceholderParser.replacePlaceholders(this.source));
                        }
                        if (f.isFile()) {
                            linesRaw.addAll(FileUtils.getFileLines(f));
                        }
                    } else if (this.sourceMode == SourceMode.WEB_SOURCE) {
                        if (WebUtils.isValidUrl(StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(this.source), "§", "&"))) {

                            String fixedSource = StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(this.source), "§", "&");
                            //Get raw github file
                            if (fixedSource.toLowerCase().contains("/blob/") && (fixedSource.toLowerCase().startsWith("http://github.com/")
                                    || fixedSource.toLowerCase().startsWith("https://github.com/")|| fixedSource.toLowerCase().startsWith("http://www.github.com/")
                                    || fixedSource.toLowerCase().startsWith("https://www.github.com/"))) {
                                String path = fixedSource.replace("//", "").split("/", 2)[1].replace("/blob/", "/");
                                fixedSource = "https://raw.githubusercontent.com/" + path;
                            }
                            //Get raw pastebin file
                            if (!fixedSource.toLowerCase().contains("/raw/") && (fixedSource.toLowerCase().startsWith("http://pastebin.com/")
                                    || fixedSource.toLowerCase().startsWith("https://pastebin.com/")|| fixedSource.toLowerCase().startsWith("http://www.pastebin.com/")
                                    || fixedSource.toLowerCase().startsWith("https://www.pastebin.com/"))) {
                                String path = fixedSource.replace("//", "").split("/", 2)[1];
                                fixedSource = "https://pastebin.com/raw/" + path;
                            }
                            BufferedReader r = null;
                            try {
                                URL url = new URL(fixedSource);
                                r = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
                                String s = r.readLine();
                                while(s != null) {
                                    linesRaw.add(s);
                                    s = r.readLine();
                                }
                                r.close();
                            } catch (Exception e) {
                                if (r != null) {
                                    try {
                                        r.close();
                                    } catch (Exception e2) {
                                        e2.printStackTrace();
                                    }
                                }
                                linesRaw.clear();
                            }
                        }
                    }
                } else {
                    linesRaw.add(I18n.get("fancymenu.customization.items.text.placeholder"));
                }

            } catch (Exception e) {
                linesRaw.clear();
            }

            this.lines.clear();

            if (linesRaw.isEmpty()) {
                if (isEditor()) {
                    linesRaw.add(I18n.get("fancymenu.customization.items.text.status.unable_to_load"));
                } else {
                    linesRaw.add("");
                }
            }

            for (String s : linesRaw) {
                float sc = getScaleMultiplicator(s);
                s = getWithoutHeadlineCodes(s);
                if (this.caseMode == CaseMode.ALL_LOWER) {
                    s = s.toLowerCase();
                }
                if (this.caseMode == CaseMode.ALL_UPPER) {
                    s = s.toUpperCase();
                }
                this.lines.put(s, sc);
            }

            MainThreadTaskExecutor.executeInMainThread(this::updateScrollArea, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);

            updating = false;

        }).start();

    }

    protected static float getScaleMultiplicator(String s) {
        if (s.startsWith("### ")) {
            return 1.1F;
        } else if (s.startsWith("## ")) {
            return 1.3F;
        } else if (s.startsWith("# ")) {
            return 1.5F;
        }
        return 1.0F;
    }

    protected static String getWithoutHeadlineCodes(String s) {
        if (s.startsWith("### ")) {
            return s.substring(4);
        } else if (s.startsWith("## ")) {
            return s.substring(3);
        }else if (s.startsWith("# ")) {
            return s.substring(2);
        }
        return s;
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

    public static enum CaseMode {

        NORMAL("normal"),
        ALL_LOWER("lower"),
        ALL_UPPER("upper");

        final String name;

        CaseMode(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static CaseMode getByName(String name) {
            for (CaseMode i : CaseMode.values()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
            return null;
        }

    }

    public static class LineScrollEntry extends TextScrollAreaEntry {

        public final String textRaw;
        public final boolean bold;
        public final float scale;
        public final TextElement parentItem;
        protected String lastTextToRender;

        public LineScrollEntry(ScrollArea parent, String textRaw, boolean bold, float scale, TextElement parentItem) {
            super(parent, Component.literal(""), (entry) -> {});
            this.textRaw = textRaw;
            this.bold = bold;
            this.scale = scale;
            this.parentItem = parentItem;
            this.selectable = false;
            this.playClickSound = false;
            this.backgroundColorIdle = new Color(0,0,0,0);
            this.backgroundColorHover = new Color(0,0,0,0);
            this.buttonBase.setAlpha(0.0F);
            this.setHeight(((int)(((float)Minecraft.getInstance().font.lineHeight) * scale)) + parentItem.lineSpacing);
        }

        @Override
        public void render(PoseStack matrix, int mouseX, int mouseY, float partial) {

            RenderSystem.enableBlend();

            String textToRender = isEditor() ? StringUtils.convertFormatCodes(this.textRaw, "&", "§") : PlaceholderParser.replacePlaceholders(this.textRaw);
            if (this.parentItem.caseMode == CaseMode.ALL_LOWER) {
                textToRender = textToRender.toLowerCase();
            } else if (this.parentItem.caseMode == CaseMode.ALL_UPPER) {
                textToRender = textToRender.toUpperCase();
            }
            if ((lastTextToRender == null) || !lastTextToRender.equals(textToRender)) {
                this.setTextOfLine(Component.literal(textToRender));
                ((MutableComponent)this.getText()).withStyle(Style.EMPTY.withBold(this.bold));
            }
            this.lastTextToRender = textToRender;

            this.updateEntry();

            this.buttonBase.render(matrix, mouseX, mouseY, partial);

            int textX = (int)((float)this.getX() / this.scale);
            if (this.parentItem.alignment == Alignment.LEFT) {
                textX += this.parentItem.textBorder;
            } else if (this.parentItem.alignment == Alignment.RIGHT) {
                textX = (int) (textX + (this.getWidth() - this.textWidth) / this.scale);
                textX -= this.parentItem.textBorder;
            } else if (this.parentItem.alignment == Alignment.CENTERED) {
                textX = (int) (textX + (((this.getWidth() - this.textWidth) / this.scale) / 2));
            }
            int centerY = (int)((float)this.getY() / this.scale) + (this.getHeight() / 2);
            int textY = centerY - (int)((float)(this.font.lineHeight / 2) * this.scale);

            matrix.pushPose();
            matrix.scale(this.scale, this.scale, this.scale);
            Color c = this.parentItem.getBaseColor();
            int textColor;
            if (c != null) {
                textColor = FastColor.ARGB32.color(Math.max(0, Math.min(255, (int)(this.parentItem.opacity * 255.0F))), c.getRed(), c.getGreen(), c.getBlue());
            } else {
                textColor = FastColor.ARGB32.color(Math.max(0, Math.min(255, (int)(this.parentItem.opacity * 255.0F))), 255, 255, 255);
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (!this.parentItem.shadow) {
                this.font.draw(matrix, this.text, textX, textY, textColor);
            } else {
                this.font.drawShadow(matrix, this.text, textX, textY, textColor);
            }
            matrix.popPose();

        }

        public void setTextOfLine(MutableComponent text) {
            this.text = text;
            this.textWidth = (int)((float)this.font.width(this.text) * this.scale);
            this.setWidth(this.parentItem.textBorder + this.textWidth + this.parentItem.textBorder);
        }

    }

}
