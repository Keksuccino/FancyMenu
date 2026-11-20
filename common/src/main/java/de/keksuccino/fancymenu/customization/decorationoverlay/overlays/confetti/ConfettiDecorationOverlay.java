package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.OverlayColorTint;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.ParticleLayer;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Confetti: two poppers fire from the bottom corners, the pieces shoot up, brake hard and flutter down. Paper pieces flip while they fall
 * (full face, narrow, edge-on, then the darker back), streamers twist, foil flashes white, and a click fires a burst at the pointer.
 * Landed pieces lie flat as thin litter on every element and along the bottom. When there is too much of it, the oldest pieces blink away.
 * <br><br>
 * Litter lying on an element is {@code ELEMENT} content, so it stays in front of the element and of background elements whatever the Layers
 * setting. Flying pieces and the litter along the bottom of the screen are {@code FRONT}.<br>
 * Launch speeds follow the screen height, so the poppers reach as high on every screen as on the 480x270 mockup stage,
 * and counts follow the screen area.
 */
public class ConfettiDecorationOverlay extends PixelDecorationOverlay<ConfettiDecorationOverlay> {

    public static final int DEFAULT_LITTER_CAP = 260;
    static final int MAX_LITTER_CAP = 2000;
    static final float MIN_FALL_SPEED = 0.01F;
    static final float MAX_FALL_SPEED = 5.0F;
    private static final float STAGE_HEIGHT = 270.0F;
    private static final float STAGE_AREA = 480.0F * 270.0F;
    /** The litter the mockup stage starts with, for its 1080 pixels of surfaces (the floor and four 150 pixel buttons) */
    private static final float START_LITTER_PER_PIXEL = 110.0F / 1080.0F;
    private static final int MAX_PIECES = 2048;
    private static final int PIECES_PER_POP = 26;
    private static final int PIECES_PER_CLICK = 22;
    private static final float POP_INTERVAL = 1.3F;
    private static final float DOUBLE_POP_CHANCE = 0.3F;
    private static final float GRAVITY = 220.0F;
    private static final float DRAG = 1.5F;

    public final DecorationOverlayOptions.Amount amount = new DecorationOverlayOptions.Amount(this);
    public final DecorationOverlayOptions.Size size = new DecorationOverlayOptions.Size(this);
    public final Property<Boolean> confettiPoppers = putProperty(Property.booleanProperty("confetti_poppers", true, "fancymenu.decoration_overlays.confetti.poppers"));
    public final Property<Boolean> confettiClickBursts = putProperty(Property.booleanProperty("confetti_click_bursts", true, "fancymenu.decoration_overlays.confetti.click_bursts"));
    public final Property.FloatProperty confettiFallSpeed = putProperty(Property.floatProperty("confetti_fall_speed", 1.0F, "fancymenu.decoration_overlays.confetti.fall_speed"));
    public final Property.IntegerProperty confettiParticleCap = putProperty(Property.integerProperty("confetti_particle_cap", DEFAULT_LITTER_CAP, "fancymenu.decoration_overlays.confetti.particle_cap"));
    public final Property<Boolean> confettiColorMixMode = putProperty(Property.booleanProperty("confetti_color_mix_mode", true, "fancymenu.decoration_overlays.confetti.color_mix"));
    public final Property.ColorProperty confettiColorHex = putProperty(Property.hexColorProperty("confetti_color_hex", "#FFFFFF", true, "fancymenu.decoration_overlays.confetti.color"));

    private final OverlayColorTint tint = new OverlayColorTint(0xFFFFFF);
    private final ConfettiArt art = new ConfettiArt();
    final Piece[] pieces = new Piece[MAX_PIECES];
    int pieceCount = 0;
    final ConfettiLitter litter = new ConfettiLitter(DEFAULT_LITTER_CAP);
    /** Gets randomized for the litter a new screen starts with */
    private final Piece scratchPiece = new Piece();
    private final PixelSurfaces<ConfettiSurface> surfaces = this.createSurfaces(ConfettiSurface::new);
    private final ParticleLayer frontLayer = new ParticleLayer("confetti_front");
    private final ParticleLayer elementLayer = new ParticleLayer("confetti_element");
    private int scale = 1;
    private boolean colorMix = true;
    private float fallSpeed = 1.0F;
    private int checkedSurfacesVersion = -1;
    /** Set when the litter has to start over (new screen size, new size or color mode), which happens in the next update */
    private boolean restartLitter = true;
    private float popTimer = 0.8F;
    private int popCount = 0;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.confettiPoppers.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.CELEBRATION)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.poppers.desc")));

        this.confettiClickBursts.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.ADS_CLICK)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.click_bursts.desc")));

        this.confettiColorMixMode.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SHUFFLE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.color_mix.desc")));

        this.confettiColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.color.desc")));

        this.amount.addEntry(menu, this);

        this.size.addEntry(menu, this);

        this.confettiFallSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SPEED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.fall_speed.desc")));

        this.confettiParticleCap.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.CHECKLIST)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.confetti.particle_cap.desc")));

    }

    @Override
    protected void onResize(int width, int height) {
        this.pieceCount = 0;
        this.restartLitter = true;
    }

    @Override
    protected void onClick(float x, float y, boolean primaryButton) {
        if (!primaryButton || !this.confettiClickBursts.tryGetNonNullElse(true)) return;
        int count = Math.round(PIECES_PER_CLICK * this.amount.getFactor());
        for (int i = 0; i < count; i++) {
            float angle = this.random.range(0.0F, PixelMath.TAU);
            float speed = this.random.range(120.0F, 380.0F);
            this.spawn(x, y, (float)Math.cos(angle) * speed, (float)Math.sin(angle) * speed - this.random.range(40.0F, 160.0F));
        }
    }

    @Override
    protected void update(float deltaSeconds) {
        this.readOptions();
        this.followSurfaceChanges();
        if (this.restartLitter) {
            this.restartLitter = false;
            this.clearLitter();
            this.scatterStartLitter();
        }
        int width = this.getWidth();
        int height = this.getHeight();
        float launchScale = PixelMath.clamp(height / STAGE_HEIGHT, 0.5F, 3.0F);
        if (this.confettiPoppers.tryGetNonNullElse(true) && ((this.popTimer -= deltaSeconds) <= 0.0F)) {
            this.popTimer = POP_INTERVAL;
            this.pop((this.popCount++ & 1) == 1, launchScale);
            if (this.random.chance(DOUBLE_POP_CHANCE)) this.pop((this.popCount & 1) == 1, launchScale);
        }
        float damping = (float)Math.exp(-DRAG * deltaSeconds);
        float gravity = GRAVITY * launchScale;
        List<PixelSurfaces.Surface<ConfettiSurface>> all = this.surfaces.all();
        int kept = 0;
        for (int i = 0; i < this.pieceCount; i++) {
            Piece piece = this.pieces[i];
            if (this.movePiece(piece, deltaSeconds, damping, gravity, width, height, all)) {
                // Moves the living ones to the front and keeps the dead objects behind them for reuse
                this.pieces[i] = this.pieces[kept];
                this.pieces[kept++] = piece;
            }
        }
        this.pieceCount = kept;
        this.litter.update(deltaSeconds, this.scale);
    }

    private void readOptions() {
        if (this.tint.update(this.confettiColorHex)) this.art.setCustomColor(this.tint.argb());
        int newScale = this.size.get();
        if (newScale != this.scale) {
            // Litter lies on a grid of art pixels, so a new size starts it over
            this.scale = newScale;
            this.restartLitter = true;
        }
        boolean newColorMix = this.confettiColorMixMode.tryGetNonNullElse(true);
        if (newColorMix != this.colorMix) {
            this.colorMix = newColorMix;
            this.restartLitter = true;
        }
        this.fallSpeed = PixelMath.clamp(this.confettiFallSpeed.getFloat(), MIN_FALL_SPEED, MAX_FALL_SPEED);
        this.litter.cap = PixelMath.clamp(this.confettiParticleCap.getInteger(), 0, MAX_LITTER_CAP);
    }

    /**
     * Fires one popper from a bottom corner: pieces shoot up and inward at 50 to 80 degrees.
     */
    private void pop(boolean fromRight, float launchScale) {
        float areaFactor = PixelMath.clamp((this.getWidth() * this.getHeight()) / STAGE_AREA, 0.25F, 4.0F);
        int count = Math.round(PIECES_PER_POP * this.amount.getFactor() * areaFactor);
        float x = fromRight ? (this.getWidth() + 2.0F) : -2.0F;
        float y = this.getHeight() - 8.0F;
        float inward = fromRight ? -1.0F : 1.0F;
        for (int i = 0; i < count; i++) {
            float angle = (float)Math.toRadians(this.random.range(50.0F, 80.0F));
            float speed = this.random.range(380.0F, 640.0F) * launchScale;
            this.spawn(x, y, (float)Math.cos(angle) * speed * inward, -(float)Math.sin(angle) * speed);
        }
    }

    /**
     * Adds a flying piece: 60 percent paper, 16 percent streamers, 14 percent dots and 10 percent gold or silver foil.
     */
    private void spawn(float x, float y, float vx, float vy) {
        if (this.pieceCount >= MAX_PIECES) return;
        Piece piece = this.pieces[this.pieceCount];
        if (piece == null) this.pieces[this.pieceCount] = piece = new Piece();
        this.pieceCount++;
        this.randomizePiece(piece);
        piece.x = x;
        piece.y = y;
        piece.vx = vx;
        piece.vy = vy;
    }

    private void randomizePiece(@NotNull Piece piece) {
        float r = this.random.next();
        piece.type = (r < 0.6F) ? ConfettiArt.TYPE_PAPER : ((r < 0.76F) ? ConfettiArt.TYPE_STREAMER : ((r < 0.9F) ? ConfettiArt.TYPE_DOT : ConfettiArt.TYPE_FOIL));
        if (!this.colorMix) {
            piece.pair = ConfettiArt.CUSTOM_PAIR;
        } else if (piece.type == ConfettiArt.TYPE_FOIL) {
            piece.pair = ConfettiArt.FOIL_PAIRS[this.random.index(ConfettiArt.FOIL_PAIRS.length)];
        } else {
            piece.pair = this.random.index(ConfettiArt.PAIRS.length);
        }
        boolean dot = piece.type == ConfettiArt.TYPE_DOT;
        piece.width = dot ? 2 : this.random.rangeInt(ConfettiArt.PIECE_MIN_SIZE, ConfettiArt.PIECE_MAX_SIZE);
        piece.height = dot ? 2 : this.random.rangeInt(ConfettiArt.PIECE_MIN_SIZE, ConfettiArt.PIECE_MAX_SIZE);
        piece.length = this.random.rangeInt(ConfettiArt.STREAMER_MIN_LENGTH, ConfettiArt.STREAMER_MAX_LENGTH);
        piece.flip = this.random.range(0.0F, PixelMath.TAU);
        piece.flipSpeed = this.random.range(5.0F, 9.0F);
        piece.sway = this.random.range(0.0F, PixelMath.TAU);
        piece.swaySpeed = this.random.range(2.5F, 4.5F);
        piece.swayAmplitude = this.random.range(8.0F, 18.0F);
        piece.terminalSpeed = this.random.range(24.0F, 40.0F) * ((piece.type == ConfettiArt.TYPE_STREAMER) ? 0.7F : 1.0F);
    }

    /**
     * Moves a piece: it brakes hard, falls, gets clamped to its flutter speed on the way down and sways while it falls.
     * Returns false when it landed or left the screen.
     */
    private boolean movePiece(@NotNull Piece piece, float deltaSeconds, float damping, float gravity, int width, int height, @NotNull List<PixelSurfaces.Surface<ConfettiSurface>> all) {
        float previousY = piece.y;
        piece.vx *= damping;
        piece.vy = piece.vy * damping + gravity * deltaSeconds;
        float terminal = piece.terminalSpeed * this.fallSpeed;
        if (piece.vy > terminal) piece.vy = PixelMath.lerp(piece.vy, terminal, Math.min(1.0F, 6.0F * deltaSeconds));
        piece.flip += piece.flipSpeed * deltaSeconds;
        piece.sway += piece.swaySpeed * deltaSeconds;
        float swayVelocity = (piece.vy > 0.0F) ? (float)Math.sin(piece.sway) * piece.swayAmplitude : 0.0F;
        piece.x += (piece.vx + swayVelocity) * deltaSeconds;
        piece.y += piece.vy * deltaSeconds;
        if (piece.vy > 0.0F) {
            int bodyHeight = ((piece.type == ConfettiArt.TYPE_STREAMER) ? piece.length : piece.height) * this.scale;
            int column = PixelMath.floor(piece.x + this.scale);
            for (int i = 0; i < all.size(); i++) {
                ConfettiSurface surface = all.get(i).state();
                if ((surface == null) || !surface.spans(column)) continue;
                int top = surface.y - surface.levels[column - surface.x] * this.scale;
                if ((previousY + bodyHeight <= top) && (piece.y + bodyHeight > top)) {
                    this.litter.settle(ConfettiArt.litterWidth(piece.type, this.random.chance(0.5F)), piece.pair, surface, piece.x, this.scale);
                    return false;
                }
            }
        }
        return (piece.y <= height + 20) && (piece.x >= -60.0F) && (piece.x <= width + 60);
    }

    /**
     * Follows changed elements: drops the litter of surfaces that are gone (an element got removed or moved) and measures the room
     * above every surface again.
     */
    private void followSurfaceChanges() {
        int version = this.surfaces.version();
        if (version == this.checkedSurfacesVersion) return;
        this.checkedSurfacesVersion = version;
        List<PixelSurfaces.Surface<ConfettiSurface>> all = this.surfaces.all();
        List<PixelSurfaces.Surface<ConfettiSurface>> elements = this.surfaces.elements();
        for (int i = 0; i < all.size(); i++) {
            ConfettiSurface surface = all.get(i).state();
            if (surface == null) continue;
            surface.seenInVersion = version;
            surface.fitUnder(all.get(i), elements);
        }
        this.litter.dropUnseen(version);
    }

    private void clearLitter() {
        this.litter.clear();
        List<PixelSurfaces.Surface<ConfettiSurface>> all = this.surfaces.all();
        for (int i = 0; i < all.size(); i++) {
            ConfettiSurface surface = all.get(i).state();
            if (surface != null) surface.clear();
        }
    }

    /**
     * Scatters some litter over the surfaces, so a new screen doesn't start bare. Wider surfaces get more of it.
     */
    private void scatterStartLitter() {
        int count = Math.min(this.litter.cap, Math.round(this.surfaces.totalWidth() * START_LITTER_PER_PIXEL));
        for (int i = 0; i < count; i++) {
            PixelSurfaces.Surface<ConfettiSurface> picked = this.surfaces.pick(this.random);
            ConfettiSurface surface = picked.state();
            if (surface == null) continue;
            this.randomizePiece(this.scratchPiece);
            int width = ConfettiArt.litterWidth(this.scratchPiece.type, this.random.chance(0.5F));
            this.litter.settle(width, this.scratchPiece.pair, surface, surface.x + this.random.next() * surface.width, this.scale);
        }
    }

    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        ParticleLayer layer = this.elementLayer;
        layer.begin(graphics, this.litter.size());
        this.drawLitter(layer, false);
        layer.end();
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        int n = this.scale;
        ParticleLayer layer = this.frontLayer;
        layer.begin(graphics, this.litter.size() + this.pieceCount);
        this.drawLitter(layer, true);
        for (int i = 0; i < this.pieceCount; i++) this.drawPiece(layer, this.pieces[i], n);
        layer.end();
    }

    /**
     * Draws the litter lying on the floor or on the elements.
     */
    private void drawLitter(@NotNull ParticleLayer layer, boolean floor) {
        int n = this.scale;
        List<ConfettiLitter.Piece> settled = this.litter.pieces;
        for (int i = 0; i < settled.size(); i++) {
            ConfettiLitter.Piece piece = settled.get(i);
            if (piece.surface.floor == floor) layer.sprite(ConfettiArt.litter(piece.width / n, piece.blinkStep()), this.art.palettes[piece.pair], piece.x, piece.y, n);
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.frontLayer.release();
        this.elementLayer.release();
    }

    /**
     * Draws a flying piece in the current step of its flip or twist.
     */
    private void drawPiece(@NotNull ParticleLayer layer, @NotNull Piece piece, int n) {
        int x = PixelMath.floor(piece.x);
        int y = PixelMath.floor(piece.y);
        switch (piece.type) {
            case ConfettiArt.TYPE_STREAMER -> {
                int step = ConfettiArt.streamerPhaseStep(piece.flip);
                PixelSprite sprite = ConfettiArt.streamer(piece.length, step);
                layer.sprite(sprite, this.art.palettes[piece.pair], x + ConfettiArt.streamerOffset(piece.length, step) * n, y, n);
            }
            case ConfettiArt.TYPE_DOT -> layer.sprite(ConfettiArt.DOT, this.art.palettes[piece.pair], x, y, n);
            default -> {
                // Paper turns around its vertical axis: it narrows to an edge, shows its back, and shears while it is turned halfway
                float cos = (float)Math.cos(piece.flip);
                float turned = Math.abs(cos);
                int visible = Math.max(1, Math.round(piece.width * turned));
                int shear = ((turned > 0.25F) && (turned < 0.75F)) ? ((Math.sin(piece.flip) > 0.0) ? 1 : -1) : 0;
                boolean glint = (piece.type == ConfettiArt.TYPE_FOIL) && (cos > 0.93F);
                PixelSprite sprite = ConfettiArt.paper(piece.width, piece.height, visible, shear, cos <= 0.0F, glint);
                int left = x + ((piece.width - visible) / 2) * n + Math.min(shear, 0) * n;
                layer.sprite(sprite, this.art.palettes[piece.pair], left, y, n);
            }
        }
    }

    static final class Piece {

        int type;
        int pair;
        float x;
        float y;
        float vx;
        float vy;
        int width;
        int height;
        int length;
        float flip;
        float flipSpeed;
        float sway;
        float swaySpeed;
        float swayAmplitude;
        float terminalSpeed;

    }

}
