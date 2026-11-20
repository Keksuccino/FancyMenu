package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cherryblossoms;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayLook;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves.Gust;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Cherry blossom petals that drift on a steady breeze, from blossoming branches in the top corners and from above.
 * Petals settle as a loose pink carpet on elements and along the bottom. Every gust sweeps the carpet up and shakes a flurry of petals
 * out of the branches. The detailed look has shaded petals that flutter, whole blossoms and shaded branches, the simple look flat petals
 * that only flip, flat clusters on plain bark and dashed wind strokes.<br>
 * The far specks are back content and the carpets on elements element content. Everything else is front content,
 * the carpet on the floor included.
 */
public class CherryBlossomsDecorationOverlay extends PixelDecorationOverlay<CherryBlossomsDecorationOverlay> {

    /** Persistent petals per layer (specks, small, large, blossoms) on a screen of the mockup's size (480 x 270), at an amount of 100% */
    private static final int[] PETALS_PER_LAYER = {60, 50, 26, 3};
    /** Petals there can be at most on a screen of the mockup's size, together with the ones gusts set loose */
    private static final int PETAL_CAP = 230;
    private static final float REFERENCE_AREA = 480.0F * 270.0F;
    private static final int LEFT_BRANCH_SEED = 901;
    private static final int RIGHT_BRANCH_SEED = 977;
    /** Petals a gust shakes out of the branches */
    private static final int FLURRY_PETALS = 24;
    /** Reading the options parses placeholders, so they get read a few times per second instead of every frame */
    private static final float SETTINGS_REFRESH_SECONDS = 0.1F;
    /** Canvases of vanished carpets kept at most for new carpets */
    private static final int MAX_SPARE_CANVASES = 8;

    public final DecorationOverlayOptions.Look look = new DecorationOverlayOptions.Look(this, DecorationOverlayLook.DETAILED);
    public final Property.BooleanProperty leftBranch = putProperty(Property.booleanProperty("cherry_blossoms_left_branch", true, "fancymenu.decoration_overlays.cherry_blossoms.left_branch"));
    public final Property.BooleanProperty rightBranch = putProperty(Property.booleanProperty("cherry_blossoms_right_branch", true, "fancymenu.decoration_overlays.cherry_blossoms.right_branch"));
    public final DecorationOverlayOptions.Amount amount = new DecorationOverlayOptions.Amount(this);

    /** The carpets of all surfaces, so the ones of vanished surfaces can hand their petals and canvas on */
    private final List<PetalCarpet> carpets = new ArrayList<>();
    /** Canvases of vanished carpets, with their textures, for new carpets */
    private final ArrayDeque<PixelCanvas> spareCanvases = new ArrayDeque<>();
    private final List<PetalCarpet> vanishedCarpets = new ArrayList<>();
    private final PixelSurfaces<PetalCarpet> surfaces = this.createSurfaces(this::createCarpet);
    private final CherryBlossomBranch[] branches = {new CherryBlossomBranch(LEFT_BRANCH_SEED, false), new CherryBlossomBranch(RIGHT_BRANCH_SEED, true)};
    @SuppressWarnings("unchecked")
    private final List<Petal>[] layers = new List[]{new ArrayList<Petal>(), new ArrayList<Petal>(), new ArrayList<Petal>(), new ArrayList<Petal>()};
    /** Petals a gust set loose (swept up from the carpet or shaken out of a branch). They go away once they land or leave. */
    private final List<Petal> loose = new ArrayList<>();
    private final ArrayDeque<Petal> spare = new ArrayDeque<>();
    private final Gust gust = new Gust();
    private float nextGust = 3.0F;
    /** Extra breeze a gust leaves behind, decays over time */
    private float boost = 0.0F;
    private int surfacesVersion = -1;
    private boolean settingsRead = false;
    private float settingsTimer = 0.0F;
    private boolean branchesDirty = true;
    private boolean simple = false;
    private boolean showLeft = true;
    private boolean showRight = true;
    private float amountFactor = 1.0F;
    private int petalCap = PETAL_CAP;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.look.addEntry(menu, this);

        this.leftBranch.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.LOCAL_FLORIST)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.cherry_blossoms.left_branch.desc")));

        this.rightBranch.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.LOCAL_FLORIST)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.cherry_blossoms.right_branch.desc")));

        this.amount.addEntry(menu, this);

    }

    @NotNull
    private PetalCarpet createCarpet(@NotNull PixelSurfaces.Surface<PetalCarpet> surface) {
        PetalCarpet carpet = new PetalCarpet(surface, this.random, this.takeCanvas(surface.width()));
        this.carpets.add(carpet);
        return carpet;
    }

    /**
     * A spare canvas, preferably one of the given width, which keeps its texture, or a new one.
     */
    @NotNull
    private PixelCanvas takeCanvas(int width) {
        for (PixelCanvas canvas : this.spareCanvases) {
            if (canvas.width() == width) {
                this.spareCanvases.remove(canvas);
                return canvas;
            }
        }
        PixelCanvas spare = this.spareCanvases.poll();
        return (spare != null) ? spare : new PixelCanvas("cherry_blossoms_carpet");
    }

    @Override
    protected void onResize(int width, int height) {
        this.branchesDirty = true;
        for (List<Petal> layer : this.layers) {
            for (Petal petal : layer) this.spare.add(petal);
            layer.clear();
        }
        this.spare.addAll(this.loose);
        this.loose.clear();
    }

    @Override
    protected void update(float deltaSeconds) {
        this.settingsTimer -= deltaSeconds;
        if (!this.settingsRead || (this.settingsTimer <= 0.0F)) {
            this.refreshSettings();
            this.settingsTimer = SETTINGS_REFRESH_SECONDS;
        }
        int width = this.getWidth();
        int height = this.getHeight();
        if (this.branchesDirty) {
            double lengthFactor = PixelMath.clamp(Math.min(width / 480.0F, height / 270.0F), 0.75F, 3.0F);
            for (CherryBlossomBranch branch : this.branches) branch.build(width, height, lengthFactor, this.simple);
            this.branchesDirty = false;
        }
        if (this.surfaces.version() != this.surfacesVersion) {
            this.surfacesVersion = this.surfaces.version();
            this.handOverVanishedCarpets();
        }
        float areaFactor = (width * height) / REFERENCE_AREA;
        this.petalCap = PixelMath.clamp(Math.round(PETAL_CAP * areaFactor * Math.max(1.0F, this.amountFactor)), 40, 2400);
        for (int layer = 0; layer < this.layers.length; layer++) {
            this.fitLayer(layer, Math.round(PETALS_PER_LAYER[layer] * areaFactor * this.amountFactor), width, height);
        }
        this.boost *= (float)Math.exp(-1.2F * deltaSeconds);
        this.updateGust(deltaSeconds, width, height);
        float breeze = 10.0F + 5.0F * (float)Math.sin(this.getTime() * 0.3F) + this.boost;
        List<PixelSurfaces.Surface<PetalCarpet>> all = this.surfaces.all();
        for (int i = 0; i < all.size(); i++) {
            PetalCarpet carpet = all.get(i).state();
            if (carpet != null) carpet.thin(deltaSeconds, this.random);
        }
        for (int layer = 0; layer < this.layers.length; layer++) {
            List<Petal> petals = this.layers[layer];
            for (int i = 0; i < petals.size(); i++) {
                Petal petal = petals.get(i);
                if (this.movePetal(petal, deltaSeconds, breeze, width, height)) this.respawn(petal, petal.layer, false, width, height);
            }
        }
        for (int i = this.loose.size() - 1; i >= 0; i--) {
            Petal petal = this.loose.get(i);
            if (!this.movePetal(petal, deltaSeconds, breeze, width, height)) continue;
            // Order doesn't matter for loose petals, so the last one takes the free place
            Petal last = this.loose.remove(this.loose.size() - 1);
            if (i < this.loose.size()) this.loose.set(i, last);
            this.spare.add(petal);
        }
    }

    private void fitLayer(int layer, int wanted, int width, int height) {
        List<Petal> petals = this.layers[layer];
        boolean fresh = petals.isEmpty();
        while (petals.size() < wanted) {
            Petal petal = this.takePetal();
            this.respawn(petal, layer, fresh, width, height);
            petals.add(petal);
        }
        while (petals.size() > wanted) this.spare.add(petals.remove(petals.size() - 1));
    }

    @NotNull
    private Petal takePetal() {
        Petal petal = this.spare.poll();
        return (petal != null) ? petal : new Petal();
    }

    private int petalCount() {
        int count = this.loose.size();
        for (List<Petal> layer : this.layers) count += layer.size();
        return count;
    }

    /**
     * Starts a petal again. A third of the petals that start again let go of a branch tip.
     */
    private void respawn(@NotNull Petal petal, int layer, boolean anywhere, int width, int height) {
        int tips = anywhere ? 0 : this.tipCount();
        if ((tips > 0) && this.random.chance(0.35F)) {
            int tip = this.random.index(tips);
            petal.respawn(layer, false, this.tipX(tip), this.tipY(tip), width, height, this.random);
        } else {
            petal.respawn(layer, anywhere, Float.NaN, 0.0F, width, height, this.random);
        }
    }

    private int tipCount() {
        return (this.showLeft ? this.branches[0].tipCount() : 0) + (this.showRight ? this.branches[1].tipCount() : 0);
    }

    private float tipX(int tip) {
        CherryBlossomBranch branch = this.branchOfTip(tip);
        return branch.tipX(this.indexInBranch(tip));
    }

    private float tipY(int tip) {
        CherryBlossomBranch branch = this.branchOfTip(tip);
        return branch.tipY(this.indexInBranch(tip));
    }

    @NotNull
    private CherryBlossomBranch branchOfTip(int tip) {
        if (!this.showLeft) return this.branches[1];
        return (tip < this.branches[0].tipCount()) ? this.branches[0] : this.branches[1];
    }

    private int indexInBranch(int tip) {
        if (!this.showLeft) return tip;
        int leftTips = this.branches[0].tipCount();
        return (tip < leftTips) ? tip : (tip - leftTips);
    }

    private void updateGust(float deltaSeconds, int width, int height) {
        if (this.gust.isActive()) {
            float previousX = this.gust.x();
            this.gust.advance(deltaSeconds, width, 140.0F);
            this.boost = Math.max(this.boost, 70.0F * (float)Math.sin(Math.min(1.0F, this.gust.time() / 1.2F) * Math.PI));
            this.sweepCarpets(previousX, this.gust.x(), height);
            return;
        }
        this.nextGust -= deltaSeconds;
        if (this.nextGust > 0.0F) return;
        this.gust.start(1, -40.0F, height, this.random);
        this.nextGust = this.random.range(6.0F, 10.0F);
        int tips = this.tipCount();
        for (int k = 0; (tips > 0) && (k < FLURRY_PETALS) && (this.petalCount() < this.petalCap); k++) {
            Petal petal = this.takePetal();
            int tip = this.random.index(tips);
            petal.respawn(this.random.chance(0.5F) ? Petal.SMALL : Petal.LARGE, false, this.tipX(tip), this.tipY(tip), width, height, this.random);
            petal.kick = this.random.range(20.0F, 60.0F);
            this.loose.add(petal);
        }
    }

    /**
     * The front of the gust sweeps every carpet column it passed since the last frame: half of them lose a petal,
     * and half of those petals fly up again.
     */
    private void sweepCarpets(float fromX, float toX, int height) {
        List<PixelSurfaces.Surface<PetalCarpet>> all = this.surfaces.all();
        for (int s = 0; s < all.size(); s++) {
            PetalCarpet carpet = all.get(s).state();
            if (carpet == null) continue;
            int first = Math.max(0, PixelMath.floor(fromX) + 1 - carpet.x);
            int last = Math.min(carpet.width - 1, PixelMath.floor(toX) - carpet.x);
            for (int column = first; column <= last; column++) {
                if ((carpet.height(column) <= 0) || !this.random.chance(0.5F)) continue;
                carpet.remove(column);
                if ((this.petalCount() >= this.petalCap) || !this.random.chance(0.5F)) continue;
                Petal petal = this.takePetal();
                petal.respawn(this.random.chance(0.6F) ? Petal.SMALL : Petal.LARGE, true, Float.NaN, 0.0F, this.getWidth(), height, this.random);
                petal.x = carpet.x + column;
                petal.y = carpet.restY - 4;
                petal.phase = 0.0F;
                petal.lift = this.random.range(25.0F, 55.0F);
                petal.kick = this.random.range(30.0F, 80.0F);
                this.loose.add(petal);
            }
        }
    }

    /**
     * Moves a petal and lands it on a carpet. Returns true if it is gone (landed or off the screen).
     */
    private boolean movePetal(@NotNull Petal petal, float deltaSeconds, float breeze, int width, int height) {
        float previousY = petal.y;
        petal.move(deltaSeconds, breeze);
        if ((petal.y > height + 8) || (petal.x > width + 50)) return true;
        if ((petal.layer == Petal.SPECK) || (petal.lift >= 4.0F)) return false;
        int size = petal.size();
        float centerX = petal.swayX() + size * 0.5F;
        List<PixelSurfaces.Surface<PetalCarpet>> all = this.surfaces.all();
        for (int i = 0; i < all.size(); i++) {
            PixelSurfaces.Surface<PetalCarpet> surface = all.get(i);
            int top = surface.restY() + 1;
            if (!surface.spans(centerX) || (previousY + size > top) || (petal.y + size <= top)) continue;
            PetalCarpet carpet = surface.state();
            if (carpet != null) {
                int column = PixelMath.floor(centerX - surface.x());
                // The petal spreads over its column and, now and then, the columns next to it
                for (int c = column - 1; c <= column + 1; c++) {
                    if ((c == column) || this.random.chance(0.6F)) carpet.add(c);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Hands the petals and canvases of carpets whose surface vanished on to new carpets. An element that moves gets a new surface
     * every frame, and this keeps its carpet the same instead of making a new one each time.
     */
    private void handOverVanishedCarpets() {
        List<PixelSurfaces.Surface<PetalCarpet>> all = this.surfaces.all();
        this.vanishedCarpets.clear();
        for (int i = this.carpets.size() - 1; i >= 0; i--) {
            PetalCarpet carpet = this.carpets.get(i);
            boolean alive = false;
            for (int s = 0; s < all.size(); s++) {
                if (all.get(s).state() == carpet) {
                    alive = true;
                    break;
                }
            }
            if (!alive) {
                this.vanishedCarpets.add(carpet);
                this.carpets.remove(i);
            }
        }
        for (PetalCarpet carpet : this.carpets) {
            if (!carpet.fresh) continue;
            carpet.fresh = false;
            for (int i = 0; i < this.vanishedCarpets.size(); i++) {
                PetalCarpet vanished = this.vanishedCarpets.get(i);
                if (carpet.canInherit(vanished)) {
                    carpet.inherit(vanished);
                    this.vanishedCarpets.remove(i);
                    this.recycle(vanished);
                    break;
                }
            }
        }
        for (PetalCarpet vanished : this.vanishedCarpets) this.recycle(vanished);
        this.vanishedCarpets.clear();
    }

    private void recycle(@NotNull PetalCarpet vanished) {
        if (this.spareCanvases.size() < MAX_SPARE_CANVASES) {
            this.spareCanvases.add(vanished.canvas());
        } else {
            vanished.canvas().close();
        }
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        List<Petal> specks = this.layers[Petal.SPECK];
        for (int i = 0; i < specks.size(); i++) {
            Petal petal = specks.get(i);
            graphics.pixel(PixelMath.floor(petal.swayX()), PixelMath.floor(petal.y), ((i & 1) != 0) ? CherryBlossomArt.SPECK_DARK : CherryBlossomArt.SPECK_LIGHT);
        }
    }

    /**
     * The carpets on elements belong to the elements, so they stay in front of them (and of background elements) with any Layers setting.
     */
    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        List<PixelSurfaces.Surface<PetalCarpet>> elements = this.surfaces.elements();
        for (int i = 0; i < elements.size(); i++) {
            PetalCarpet carpet = elements.get(i).state();
            if (carpet != null) carpet.draw(graphics);
        }
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        if (this.showLeft) this.branches[0].draw(graphics);
        if (this.showRight) this.branches[1].draw(graphics);
        PetalCarpet floorCarpet = this.surfaces.floor().state();
        if (floorCarpet != null) floorCarpet.draw(graphics);
        float time = this.getTime();
        for (int layer = Petal.SMALL; layer < this.layers.length; layer++) {
            List<Petal> petals = this.layers[layer];
            for (int i = 0; i < petals.size(); i++) this.drawPetal(graphics, petals.get(i), time);
        }
        for (int i = 0; i < this.loose.size(); i++) this.drawPetal(graphics, this.loose.get(i), time);
        this.gust.drawStrokes(graphics, this.simple, CherryBlossomArt.STROKE_RGB);
    }

    /**
     * Detailed petals flutter through four frames: face, edge-on sliver, darker underside, mirrored sliver. Simple petals only flip.
     */
    private void drawPetal(@NotNull PixelGraphics graphics, @NotNull Petal petal, float time) {
        int x = PixelMath.floor(petal.swayX());
        int y = PixelMath.floor(petal.y);
        if (this.simple) {
            PixelSprite sprite = ((petal.layer == Petal.SMALL) ? CherryBlossomArt.SMALL_PETAL_TURNS : CherryBlossomArt.PETAL_TURNS)[petal.turn];
            graphics.sprite(sprite, CherryBlossomArt.SIMPLE_FACE, x, y, (PixelMath.floor(time * 2.5F + petal.flutterOffset) & 1) == 1, false, 1);
            return;
        }
        if (petal.layer == Petal.BLOSSOM) {
            graphics.sprite(CherryBlossomArt.BLOSSOM_TURNS[PixelMath.floor(time * 2.0F + petal.flutterOffset) & 1], CherryBlossomArt.FACE, x, y);
            return;
        }
        boolean large = petal.layer == Petal.LARGE;
        PixelSprite[] turns = large ? CherryBlossomArt.PETAL_TURNS : CherryBlossomArt.SMALL_PETAL_TURNS;
        int frame = PixelMath.floor(time * petal.flutterSpeed + petal.flutterOffset) & 3;
        if (frame == 0) {
            graphics.sprite(turns[petal.turn], CherryBlossomArt.FACE, x, y);
        } else if (frame == 2) {
            graphics.sprite(turns[(petal.turn + 1) & 3], CherryBlossomArt.BACK, x, y);
        } else {
            boolean mirrored = frame == 3;
            graphics.sprite(large ? CherryBlossomArt.SLIVER : CherryBlossomArt.SMALL_SLIVER, mirrored ? CherryBlossomArt.BACK : CherryBlossomArt.FACE, x, y + 1, mirrored, mirrored, 1);
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        for (CherryBlossomBranch branch : this.branches) branch.releaseTexture();
        for (PetalCarpet carpet : this.carpets) carpet.releaseTexture();
        for (PixelCanvas canvas : this.spareCanvases) canvas.releaseTexture();
    }

    /**
     * Reads all options.
     */
    void refreshSettings() {
        this.settingsRead = true;
        boolean simple = this.look.isSimple();
        if (simple != this.simple) {
            this.simple = simple;
            for (CherryBlossomBranch branch : this.branches) branch.repaint(this.getWidth(), simple);
        }
        this.showLeft = this.leftBranch.getBoolean();
        this.showRight = this.rightBranch.getBoolean();
        this.amountFactor = this.amount.getFactor();
    }

    @NotNull
    CherryBlossomBranch getBranch(int index) {
        return this.branches[index];
    }

}
