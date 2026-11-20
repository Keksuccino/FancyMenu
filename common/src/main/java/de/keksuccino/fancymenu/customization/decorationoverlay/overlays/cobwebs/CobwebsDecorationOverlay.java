package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cobwebs;

import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Cobwebs span the two top corners of the screen, with dew drops that glint now and then. A spider sits in each web, drops down
 * on a thread, hangs for a moment and climbs back, and a third one comes down from the top edge. A spider scurries back up when
 * the pointer comes close.<br><br>
 *
 * Everything is drawn across the screen ({@code FRONT}). A web is one sprite, so the whole overlay is a handful of quads.
 */
public class CobwebsDecorationOverlay extends PixelDecorationOverlay<CobwebsDecorationOverlay> {

    private static final String KEY = "fancymenu.decoration_overlays.cobwebs.";
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    /** The spider that comes down from the top edge belongs to no web */
    private static final int NO_WEB = -1;
    private static final int DEW_PER_WEB = 9;
    private static final int THREAD_ALPHA = 150;
    /** A spider scurries up when the pointer comes this close */
    private static final float SCARE_RADIUS = 30.0F;

    private static final PixelSprite[] SPIDER = {
            PixelSprite.of(
                    "l.....l",
                    ".lbbbl.",
                    "..bmb..",
                    ".lbbbl.",
                    "l.....l"),
            PixelSprite.of(
                    "..l.l..",
                    ".lbbbl.",
                    "l.bmb.l",
                    ".lbbbl.",
                    "..l.l..")};
    private static final PixelPalette SPIDER_PALETTE = PixelPalette.builder().rgb('l', 0x6A5A7C).rgb('b', 0x1A1424).rgb('m', 0xC0392B).build();

    public final Property<Boolean> leftWeb = putProperty(Property.booleanProperty("left_web", true, KEY + "left_web"));
    public final Property<Boolean> rightWeb = putProperty(Property.booleanProperty("right_web", true, KEY + "right_web"));
    public final Property<Boolean> spiders = putProperty(Property.booleanProperty("spiders", true, KEY + "spiders"));
    public final Property<Boolean> closedFront = putProperty(Property.booleanProperty("closed_front", false, KEY + "closed_front"));

    /** Dew drops per web shape (open, closed) and web (left, right): index into the shape's pixels, phase and speed of the glint */
    private final int[][][] dewPixel = new int[2][2][DEW_PER_WEB];
    private final float[][][] dewPhase = new float[2][2][DEW_PER_WEB];
    private final float[][][] dewSpeed = new float[2][2][DEW_PER_WEB];

    private final CobwebSpider[] spiderList = new CobwebSpider[3];
    private boolean showLeft = true;
    private boolean showRight = true;
    private boolean showSpiders = true;
    private boolean closed = false;

    public CobwebsDecorationOverlay() {
        // Both kinds of web get their dew up front, each its own, so the option can change while the overlay runs
        CobwebShape[] shapes = {CobwebShape.OPEN, CobwebShape.CLOSED};
        for (int kind = 0; kind < 2; kind++) {
            for (int web = 0; web < 2; web++) {
                for (int d = 0; d < DEW_PER_WEB; d++) {
                    this.dewPixel[kind][web][d] = this.random.index(shapes[kind].pixelCount());
                    this.dewPhase[kind][web][d] = this.random.range(0.0F, PixelMath.TAU);
                    this.dewSpeed[kind][web][d] = this.random.range(0.8F, 2.0F);
                }
            }
        }
        this.spiderList[0] = new CobwebSpider(LEFT, 24, 20, this.random.range(1.0F, 5.0F));
        this.spiderList[1] = new CobwebSpider(RIGHT, 30, 16, this.random.range(1.0F, 5.0F));
        // The third spider comes down somewhere above the middle of the screen
        this.spiderList[2] = new CobwebSpider(NO_WEB, this.random.range(150.0F / 480.0F, 330.0F / 480.0F), 0, this.random.range(1.0F, 5.0F));
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
        this.addToggle(menu, this.leftWeb, MaterialIcons.ALIGN_HORIZONTAL_LEFT, "left_web");
        this.addToggle(menu, this.rightWeb, MaterialIcons.ALIGN_HORIZONTAL_RIGHT, "right_web");
        this.addToggle(menu, this.spiders, MaterialIcons.PEST_CONTROL, "spiders");
        this.addToggle(menu, this.closedFront, MaterialIcons.WEB, "closed_front");
    }

    private void addToggle(@NotNull ContextMenu menu, @NotNull Property<Boolean> property, @NotNull MaterialIcon icon, @NotNull String name) {
        property.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(icon)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(KEY + name + ".desc")));
    }

    @Override
    protected void update(float deltaSeconds) {
        this.showLeft = this.leftWeb.tryGetNonNullElse(true);
        this.showRight = this.rightWeb.tryGetNonNullElse(true);
        this.showSpiders = this.spiders.tryGetNonNullElse(true);
        this.closed = this.closedFront.tryGetNonNullElse(false);
        for (CobwebSpider spider : this.spiderList) {
            boolean scared = (spider.drop > 2.0F) && this.pointer.isWithin(this.spiderX(spider), spider.anchorY + spider.drop, SCARE_RADIUS);
            spider.advance(deltaSeconds, scared, this.random);
        }
    }

    private int spiderX(@NotNull CobwebSpider spider) {
        return switch (spider.web) {
            case LEFT -> (int)spider.anchor;
            case RIGHT -> this.getWidth() - 1 - (int)spider.anchor;
            default -> (int)Math.round(spider.anchor * this.getWidth());
        };
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        CobwebShape shape = this.closed ? CobwebShape.CLOSED : CobwebShape.OPEN;
        int kind = this.closed ? 1 : 0;
        float time = this.getTime();
        int width = this.getWidth();
        if (this.showLeft) this.drawWeb(graphics, shape, kind, LEFT, time, width);
        if (this.showRight) this.drawWeb(graphics, shape, kind, RIGHT, time, width);
        if (!this.showSpiders) return;
        for (CobwebSpider spider : this.spiderList) {
            // A web that is switched off takes its spider with it
            if (((spider.web == LEFT) && !this.showLeft) || ((spider.web == RIGHT) && !this.showRight)) continue;
            int x = this.spiderX(spider);
            float swing = (spider.state == CobwebSpider.HANGING) ? (float)(Math.sin(time * 2.2) * 1.2) : 0.0F;
            int y = (int)Math.round(spider.anchorY + spider.drop + swing);
            if (spider.drop > 0.0F) graphics.rect(x, spider.anchorY, 1, y - spider.anchorY, PixelMath.withAlpha(CobwebShape.THREAD_COLOR, THREAD_ALPHA));
            if ((spider.drop > 0.0F) || (spider.anchorY > 0)) {
                boolean moving = spider.isMoving();
                PixelSprite sprite = SPIDER[moving ? (PixelMath.floor(time * 9.0F) & 1) : 0];
                graphics.sprite(sprite, SPIDER_PALETTE, x - 3, y - 2);
            }
        }
    }

    private void drawWeb(@NotNull PixelGraphics graphics, @NotNull CobwebShape shape, int kind, int web, float time, int width) {
        boolean right = web == RIGHT;
        int spriteWidth = shape.sprite.width();
        // The right web is the mirror image of the left one: its pixel x lands on width - 1 - x
        graphics.sprite(shape.sprite, CobwebShape.palette(), right ? (width - spriteWidth) : 0, 0, right, false, 1);
        for (int d = 0; d < DEW_PER_WEB; d++) {
            if (Math.sin(time * this.dewSpeed[kind][web][d] + this.dewPhase[kind][web][d]) <= 0.5) continue;
            int pixel = this.dewPixel[kind][web][d];
            int x = shape.xs[pixel];
            graphics.pixel(right ? (width - 1 - x) : x, shape.ys[pixel], 0xFFFFFFFF);
        }
    }

}
