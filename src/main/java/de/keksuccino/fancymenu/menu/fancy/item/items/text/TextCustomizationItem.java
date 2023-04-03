//TODO übernehmen
package de.keksuccino.fancymenu.menu.fancy.item.items.text;

import com.google.common.collect.LinkedListMultimap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.menu.fancy.helper.CustomizationHelper;
import de.keksuccino.fancymenu.menu.placeholder.v1.DynamicValueHelper;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.ScrollableScreen;
import de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.content.scrollarea.ScrollArea;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import de.keksuccino.konkrete.resources.TextureHandler;
import de.keksuccino.konkrete.web.WebUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TextCustomizationItem extends CustomizationItem {

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
    public volatile int cachedTextWidth = 0; //currently unused

    public TextCustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {

        super(parentContainer, item);

        this.source = item.getEntryValue("source");

        String sourceModeString = item.getEntryValue("source_mode");
        if (sourceModeString != null) {
            SourceMode s = SourceMode.getByName(sourceModeString);
            if (s != null) {
                this.sourceMode = s;
            }
        }

        String shadowString = item.getEntryValue("shadow");
        if ((shadowString != null) && shadowString.equals("false")) {
            this.shadow = false;
        }

        String caseModeString = item.getEntryValue("case_mode");
        if (caseModeString != null) {
            CaseMode c = CaseMode.getByName(caseModeString);
            if (c != null) {
                this.caseMode = c;
            }
        }

        String scaleString = item.getEntryValue("scale");
        if (scaleString != null) {
            if (MathUtils.isFloat(scaleString)) {
                this.scale = Float.parseFloat(scaleString);
            }
        }

        String alignmentString = item.getEntryValue("alignment");
        if (alignmentString != null) {
            Alignment a = Alignment.getByName(alignmentString);
            if (a != null) {
                this.alignment = a;
            }
        }

        String baseColorString = item.getEntryValue("base_color");
        if (baseColorString != null) {
            Color c = RenderUtils.getColorFromHexString(baseColorString);
            if (c != null) {
                this.baseColorHex = baseColorString;
            }
        }

        String textBorderString = item.getEntryValue("text_border");
        if ((textBorderString != null) && MathUtils.isInteger(textBorderString)) {
            this.textBorder = Integer.parseInt(textBorderString);
        }

        String lineSpacingString = item.getEntryValue("line_spacing");
        if ((lineSpacingString != null) && MathUtils.isInteger(lineSpacingString)) {
            this.lineSpacing = Integer.parseInt(lineSpacingString);
        }

        this.scrollGrabberColorHexNormal = item.getEntryValue("grabber_color_normal");
        this.scrollGrabberColorHexHover = item.getEntryValue("grabber_color_hover");

        this.scrollGrabberTextureNormal = item.getEntryValue("grabber_texture_normal");
        this.scrollGrabberTextureHover = item.getEntryValue("grabber_texture_hover");

        String enableScrollingString = item.getEntryValue("enable_scrolling");
        if ((enableScrollingString != null) && enableScrollingString.equals("false")) {
            this.enableScrolling = false;
        }

        this.updateContent();

    }

    public void updateScrollArea() {

//        this.getWidth() = this.cachedTextWidth;

        this.scrollArea = new ScrollArea(0, 0, this.getWidth(), this.getHeight());
        this.scrollArea.backgroundColor = new Color(0,0,0,0);
        this.scrollArea.enableScrolling = this.enableScrolling;

        if (this.scrollGrabberColorHexNormal != null) {
            Color c = RenderUtils.getColorFromHexString(this.scrollGrabberColorHexNormal);
            if (c != null) {
                this.scrollArea.grabberColorNormal = c;
            }
        }
        if (this.scrollGrabberColorHexHover != null) {
            Color c = RenderUtils.getColorFromHexString(this.scrollGrabberColorHexHover);
            if (c != null) {
                this.scrollArea.grabberColorHover = c;
            }
        }

        if (this.scrollGrabberTextureNormal != null) {
            ExternalTextureResourceLocation r = TextureHandler.getResource(this.scrollGrabberTextureNormal);
            if (r != null) {
                this.scrollArea.grabberTextureNormal = r.getResourceLocation();
            }
        }
        if (this.scrollGrabberTextureHover != null) {
            ExternalTextureResourceLocation r = TextureHandler.getResource(this.scrollGrabberTextureHover);
            if (r != null) {
                this.scrollArea.grabberTextureHover = r.getResourceLocation();
            }
        }

        for (Map.Entry<String, Float> m : this.lines.entries()) {

            TextElementLineEntry e = new TextElementLineEntry(this.scrollArea, this, m.getKey(), false, (this.scale * m.getValue()));
            this.scrollArea.addEntry(e);

        }

        //Add some empty space at the end of the scroll area
        TextElementLineEntry e = new TextElementLineEntry(this.scrollArea, this, " ", false, 3.0F);
        this.scrollArea.addEntry(e);

    }

    public Color getBaseColor() {
        if (this.baseColorHex != null) {
            return RenderUtils.getColorFromHexString(this.baseColorHex);
        }
        return null;
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        try {

            if (this.source != null) {
                this.value = this.source;
            }

            if (this.shouldRender()) {

                if (!this.updating) {

                    RenderSystem.enableBlend();

                    if (this.scrollArea != null) {
                        this.scrollArea.x = this.getPosX(menu);
                        this.scrollArea.y = this.getPosY(menu);
                        this.scrollArea.width = this.getWidth();
                        this.scrollArea.height = this.getHeight();
                        this.scrollArea.render(matrix);
                    }

                } else if (isEditorActive()) {
                    //Render "updating" view in editor
                    fill(matrix, this.getPosX(menu), this.getPosY(menu), this.getPosX(menu) + this.getWidth(), this.getPosY(menu) + this.getHeight(), Color.MAGENTA.getRGB());
                    drawCenteredString(matrix, Minecraft.getInstance().font, Locals.localize("fancymenu.customization.items.text.status.loading"), this.getPosX(menu) + (this.getWidth() / 2), this.getPosY(menu) + (this.getHeight() / 2) - (Minecraft.getInstance().font.lineHeight / 2), -1);
                }

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
        cachedTextWidth = 0;

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
                        File f = new File(de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(this.source));
                        if (!f.exists() || !f.getAbsolutePath().replace("\\", "/").startsWith(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/"))) {
                            f = new File(Minecraft.getInstance().gameDirectory.getAbsolutePath().replace("\\", "/") + "/" + de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(this.source));
                        }
                        if (f.isFile()) {
                            linesRaw.addAll(FileUtils.getFileLines(f));
                        }
                    } else if (this.sourceMode == SourceMode.WEB_SOURCE) {
                        if (WebUtils.isValidUrl(StringUtils.convertFormatCodes(PlaceholderParser.replacePlaceholders(this.source), "§", "&"))) {
                            //TODO übernehmen
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
                        } else {
                            linesRaw.clear();
                        }
                    }
                } else {
                    linesRaw.add(Locals.localize("fancymenu.customization.items.text.placeholder"));
                }

            } catch (Exception e) {
                linesRaw.clear();
            }

            this.lines.clear();

            if (linesRaw.isEmpty()) {
                if (isEditorActive()) {
                    linesRaw.add(Locals.localize("fancymenu.customization.items.text.status.unable_to_load"));
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

            CustomizationHelper.runTaskInMainThread(this::updateScrollArea);

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

        String name;

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

        String name;

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

    public static class TextElementLineEntry extends ScrollableScreen.ScrollAreaEntryBase {

        public String text;
        public boolean bold;
        public TextCustomizationItem parentItem;

        public TextElementLineEntry(ScrollArea parent, TextCustomizationItem parentItem, String text, boolean bold, float scale) {
            super(parent, null);
            this.parentItem = parentItem;
            this.text = text;
            this.bold = bold;
            this.setHeight((((int)(((float)Minecraft.getInstance().font.lineHeight) * scale))) + parentItem.lineSpacing);
            this.renderBody = (render) -> {
                if (this.text != null) {

                    //Render Debug Border
//                    fill(render.matrix, this.x, this.y, this.x + this.getWidth(), this.y + 1, Color.red.getRGB());
//                    fill(render.matrix, this.x, this.y + this.getHeight() - 1, this.x + this.getWidth(), this.y + this.getHeight(), Color.red.getRGB());

                    Font font = Minecraft.getInstance().font;
                    String s = this.text;
                    if (this.bold) {
                        s = "§l" + this.text;
                    }
                    if (isEditorActive()) {
                        s = StringUtils.convertFormatCodes(s, "&", "§");
                    } else {
                        //Update placeholders every render tick
                        s = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(s);
                        //Support for a second layer of placeholders (this is only useful for when getting text with the web text placeholder and similar placeholders)
                        s = de.keksuccino.fancymenu.menu.placeholder.v2.PlaceholderParser.replacePlaceholders(s);
                    }
                    Color baseColor = this.parentItem.getBaseColor();
                    int color = -1;
                    if (baseColor != null) {
                        color = baseColor.getRGB();
                    }

                    if (!s.contains("%n%")) {
                        //Single line text entry
                        renderLine(s, font, scale, color, 0, this, render);
                    } else {
                        //Multi line text entry
                        List<String> lines = Arrays.asList(StringUtils.splitLines(s, "%n%"));
                        this.setHeight(((((int)(((float)Minecraft.getInstance().font.lineHeight) * scale))) * lines.size()) + parentItem.lineSpacing + (parentItem.lineSpacing * lines.size()));
                        int i = 0;
                        for (String line : lines) {
                            renderLine(line, font, scale, color, i, this, render);
                            i++;
                        }
                    }

                }
            };
        }

        protected static void renderLine(String s, Font font, float scale, int color, int line, TextElementLineEntry entry, EntryRenderCallback render) {

            float textWidth = font.width(s) * scale;
            int y = (int) (render.entry.y / scale);
            int x = (int) (render.entry.x / scale);
            if (entry.parentItem.alignment == Alignment.LEFT) {
                x += entry.parentItem.textBorder;
            }
            if (entry.parentItem.alignment == Alignment.RIGHT) {
                x = (int) (x + (entry.getWidth() - textWidth) / scale);
                x -= entry.parentItem.textBorder;
            }
            if (entry.parentItem.alignment == Alignment.CENTERED) {
                x = (int) (x + (((entry.getWidth() - textWidth) / scale) / 2));
            }

            render.matrix.pushPose();
            render.matrix.scale(scale, scale, scale);

            int lineHeight = 10;
            float textY = y + (lineHeight * line);
            if (entry.parentItem.lineSpacing > 0) {
                textY += (int) ((entry.parentItem.lineSpacing / scale) * line);
            }

            if (entry.parentItem.shadow) {
                font.drawShadow(render.matrix, s, x, textY, color | Mth.ceil(entry.parentItem.opacity * 255.0F) << 24);
            } else {
                font.draw(render.matrix, s, x, textY, color | Mth.ceil(entry.parentItem.opacity * 255.0F) << 24);
            }

            render.matrix.popPose();

        }

    }

}
