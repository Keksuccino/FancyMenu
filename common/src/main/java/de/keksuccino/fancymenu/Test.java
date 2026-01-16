package de.keksuccino.fancymenu;

import de.keksuccino.fancymenu.events.screen.InitOrResizeScreenCompletedEvent;
import de.keksuccino.fancymenu.events.screen.RenderScreenEvent;
import de.keksuccino.fancymenu.util.event.acara.EventListener;
import de.keksuccino.fancymenu.util.event.acara.EventPriority;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.SmoothCircleRenderer;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFont;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothFontManager;
import de.keksuccino.fancymenu.util.rendering.text.smooth.SmoothTextRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.widget.button.ExtendedButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nonnull;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Test {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final DrawableColor TINT = DrawableColor.of(new Color(255, 0, 234, 95));
    private static final DrawableColor BORDER_TINT = DrawableColor.of(new Color(12, 32, 92, 200));
    private static final float BORDER_THICKNESS = 2.0F;
    private static final float SMOOTH_FONT_BASE_SIZE = 32.0F;
    private static final float SAMPLE_SIZE_SMALL = 9.0F;
    private static final float SAMPLE_SIZE_MEDIUM = 14.0F;
    private static final float SAMPLE_SIZE_LARGE = 20.0F;
    private static final float TEXT_SAMPLE_X = 380.0F;
    private static final float TEXT_SAMPLE_Y = 40.0F;
    private static final float SHARPNESS_MIN = 0.25F;
    private static final float SHARPNESS_MAX = 3.0F;

    private static final ResourceLocation FOLDER = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans");
    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans.ttf");
    private static final ResourceLocation JP   = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_jp.ttf");
    private static final ResourceLocation KR   = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_kr.ttf");
    private static final ResourceLocation SC   = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_sc.ttf");
    private static final ResourceLocation TC   = ResourceLocation.fromNamespaceAndPath("fancymenu", "fonts/noto_sans/noto_sans_tc.ttf");
    private static final Map<String, List<ResourceLocation>> OVERRIDES = Map.of(
            "ja_jp", List.of(BASE, JP, SC, TC, KR),
            "ko_kr", List.of(BASE, KR, SC, JP, TC),
            "zh_cn", List.of(BASE, SC, TC, JP, KR),
            "zh_tw", List.of(BASE, TC, SC, JP, KR),
            "zh_hk", List.of(BASE, TC, SC, JP, KR)
    );

    private boolean blurFirst = false;
    private boolean blurSecond = false;
    private boolean blurThird = false;
    private boolean circleFirst = false;
    private boolean circleSecond = false;
    private boolean circleThird = false;
    private boolean textSantaTime = false;
    private boolean textScab = false;
    private boolean textSchizm = false;
    private boolean textSeattleAvenue = false;
    private boolean textNotoSans = false;
    private boolean textSextonSans = false;

    @EventListener(priority = EventPriority.VERY_LOW)
    public void onRenderPost(RenderScreenEvent.Post e) {

        if (blurFirst) {
            SmoothRectangleRenderer.renderSmoothRect(e.getGraphics(), 50, 40, 300, 300, 4, TINT.getColorInt(), e.getPartial());
            SmoothRectangleRenderer.renderSmoothBorder(e.getGraphics(), 50, 40, 300, 300, BORDER_THICKNESS, 4, BORDER_TINT.getColorInt(), e.getPartial());
        }

        if (blurSecond) {
            SmoothRectangleRenderer.renderSmoothRect(e.getGraphics(), e.getScreen().width - 300, e.getScreen().height - 300, 200, 200, 4, TINT.getColorInt(), e.getPartial());
            SmoothRectangleRenderer.renderSmoothBorder(e.getGraphics(), e.getScreen().width - 300, e.getScreen().height - 300, 200, 200, BORDER_THICKNESS, 4, BORDER_TINT.getColorInt(), e.getPartial());
        }

        if (blurThird) {
            SmoothRectangleRenderer.renderSmoothRect(e.getGraphics(), e.getScreen().width - 300, 40, 100, 100, 4, TINT.getColorInt(), e.getPartial());
            SmoothRectangleRenderer.renderSmoothBorder(e.getGraphics(), e.getScreen().width - 300, 40, 100, 100, BORDER_THICKNESS, 4, BORDER_TINT.getColorInt(), e.getPartial());
        }

        if (circleFirst) {
            SmoothCircleRenderer.renderSmoothCircle(e.getGraphics(), 50, 380, 120, 120, 2.0F, TINT.getColorInt(), e.getPartial());
            SmoothCircleRenderer.renderSmoothCircleBorder(e.getGraphics(), 50, 380, 120, 120, BORDER_THICKNESS, 2.0F, BORDER_TINT.getColorInt(), e.getPartial());
        }

        if (circleSecond) {
            SmoothCircleRenderer.renderSmoothCircle(e.getGraphics(), e.getScreen().width - 260, e.getScreen().height - 260, 180, 120, 2.0F, TINT.getColorInt(), e.getPartial());
            SmoothCircleRenderer.renderSmoothCircleBorder(e.getGraphics(), e.getScreen().width - 260, e.getScreen().height - 260, 180, 120, BORDER_THICKNESS, 2.0F, BORDER_TINT.getColorInt(), e.getPartial());
        }

        if (circleThird) {
            SmoothCircleRenderer.renderSmoothCircle(e.getGraphics(), e.getScreen().width - 220, 180, 120, 180, 4.0F, TINT.getColorInt(), e.getPartial());
            SmoothCircleRenderer.renderSmoothCircleBorder(e.getGraphics(), e.getScreen().width - 220, 180, 120, 180, BORDER_THICKNESS, 4.0F, BORDER_TINT.getColorInt(), e.getPartial());
        }

        float currentY = TEXT_SAMPLE_Y;

        currentY = renderFontSamplesIfEnabled(e, currentY, textSantaTime, "santa_time.ttf", "Santa Time");
        currentY = renderFontSamplesIfEnabled(e, currentY, textScab, "scab.ttf", "Scab");
        currentY = renderFontSamplesIfEnabled(e, currentY, textSchizm, "schizm.ttf", "Schizm");
        currentY = renderFontSamplesIfEnabled(e, currentY, textSeattleAvenue, "seattle_avenue.ttf", "Seattle Avenue");
        currentY = renderFontNotoSansIfEnabled(e, currentY, textNotoSans);
        renderFontSamplesIfEnabled(e, currentY, textSextonSans, "sexton_sans.ttf", "Sexton Sans");

    }

    @EventListener
    public void onInitScreenPost(InitOrResizeScreenCompletedEvent e) {

        e.addRenderableWidget(new ExtendedButton(20, 20, 100, 20, "Toggle First Blur", button -> {
            blurFirst = !blurFirst;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 40, 100, 20, "Toggle Second Blur", button -> {
            blurSecond = !blurSecond;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 60, 100, 20, "Toggle Third Blur", button -> {
            blurThird = !blurThird;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 80, 100, 20, "Toggle First Circle", button -> {
            circleFirst = !circleFirst;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 100, 100, 20, "Toggle Second Circle", button -> {
            circleSecond = !circleSecond;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 120, 100, 20, "Toggle Third Circle", button -> {
            circleThird = !circleThird;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 140, 100, 20, "Toggle Santa", button -> {
            textSantaTime = !textSantaTime;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 160, 100, 20, "Toggle Scab", button -> {
            textScab = !textScab;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 180, 100, 20, "Toggle Schizm", button -> {
            textSchizm = !textSchizm;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 200, 100, 20, "Toggle Seattle", button -> {
            textSeattleAvenue = !textSeattleAvenue;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 220, 100, 20, "Toggle Noto Sans", button -> {
            textNotoSans = !textNotoSans;
        }));
        e.addRenderableWidget(new ExtendedButton(20, 240, 100, 20, "Toggle Sexton", button -> {
            textSextonSans = !textSextonSans;
        }));

    }

    private static float renderFontNotoSansIfEnabled(@Nonnull RenderScreenEvent.Post e, float startY, boolean enabled) {
        if (!enabled) {
            return startY;
        }
        SmoothFont font = SmoothFontManager.getFontFromFolder(FOLDER, SMOOTH_FONT_BASE_SIZE, OVERRIDES);
        if (font == null) {
            return startY;
        }

        float x = TEXT_SAMPLE_X;
        float y = startY;
        int white = DrawableColor.WHITE.getColorInt();

        SmoothTextRenderer.renderText(e.getGraphics(), font, "Noto Sans", x, y, white, SAMPLE_SIZE_MEDIUM, false);
        y += font.getLineHeight(SAMPLE_SIZE_MEDIUM) + 2.0F;

        SmoothTextRenderer.renderText(e.getGraphics(), font, buildSampleText(), x, y, white, SAMPLE_SIZE_SMALL, false);
        y += SmoothTextRenderer.getTextHeight(font, buildSampleText(), SAMPLE_SIZE_SMALL) + 4.0F;

        SmoothTextRenderer.renderText(e.getGraphics(), font, buildSampleText(), x, y, white, SAMPLE_SIZE_MEDIUM, false);
        y += SmoothTextRenderer.getTextHeight(font, buildSampleText(), SAMPLE_SIZE_MEDIUM) + 4.0F;

        SmoothTextRenderer.renderText(e.getGraphics(), font, buildSampleText(), x, y, white, SAMPLE_SIZE_LARGE, false);
        y += SmoothTextRenderer.getTextHeight(font, buildSampleText(), SAMPLE_SIZE_LARGE) + 12.0F;

        return y;
    }

    private static float renderFontSamplesIfEnabled(@Nonnull RenderScreenEvent.Post e, float startY, boolean enabled, String fontFile, String displayName) {
        if (!enabled) {
            return startY;
        }
        Path fontPath = getFontDirectory().resolve(fontFile);
        SmoothFont font = SmoothFontManager.getFont(fontPath, SMOOTH_FONT_BASE_SIZE);
        if (font == null) {
            return startY;
        }

        float x = TEXT_SAMPLE_X;
        float y = startY;
        int white = DrawableColor.WHITE.getColorInt();

        SmoothTextRenderer.renderText(e.getGraphics(), font, displayName, x, y, white, SAMPLE_SIZE_MEDIUM, false);
        y += font.getLineHeight(SAMPLE_SIZE_MEDIUM) + 2.0F;

        SmoothTextRenderer.renderText(e.getGraphics(), font, buildSampleText(), x, y, white, SAMPLE_SIZE_SMALL, false);
        y += SmoothTextRenderer.getTextHeight(font, buildSampleText(), SAMPLE_SIZE_SMALL) + 4.0F;

        SmoothTextRenderer.renderText(e.getGraphics(), font, buildSampleText(), x, y, white, SAMPLE_SIZE_MEDIUM, false);
        y += SmoothTextRenderer.getTextHeight(font, buildSampleText(), SAMPLE_SIZE_MEDIUM) + 4.0F;

        SmoothTextRenderer.renderText(e.getGraphics(), font, buildSampleText(), x, y, white, SAMPLE_SIZE_LARGE, false);
        y += SmoothTextRenderer.getTextHeight(font, buildSampleText(), SAMPLE_SIZE_LARGE) + 12.0F;

        return y;
    }

    private static String buildSampleText() {
        String prefix = String.valueOf(ChatFormatting.PREFIX_CODE);
        StringBuilder sb = new StringBuilder();
        sb.append(I18n.get("block.minecraft.cherry_wood")).append(" ");
        sb.append(prefix).append('x').append("Green ");
        sb.append(prefix).append('r');
        sb.append(prefix).append('l').append("Bold ");
        sb.append(prefix).append('r');
        sb.append(prefix).append('o').append("Italic ");
        sb.append(prefix).append('r');
        sb.append(prefix).append('n').append("Underline ");
        sb.append(prefix).append('r');
        sb.append(prefix).append('m').append("Strike ");
        sb.append(prefix).append('r');
        sb.append(prefix).append('k').append("Magic ");
        sb.append(prefix).append('r').append("Reset");
        sb.append('\n');
        sb.append(prefix).append('x')
                .append(prefix).append('F').append(prefix).append('F')
                .append(prefix).append('7').append(prefix).append('7')
                .append(prefix).append('0').append(prefix).append('0')
                .append("Hex Color ")
                .append(prefix).append('r')
                .append("Normal");
        return sb.toString();
    }

    private static Path getFontDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("fancymenu")
                .resolve("assets")
                .resolve("fonts");
    }

}
