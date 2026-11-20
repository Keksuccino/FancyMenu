package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Plants slowly take over the menu: moss, vines, leaves and flowers grow on the elements, climbing plants creep around the screen
 * edges, and grass, sprouts, mushrooms and flowers fill the ground.<br><br>
 *
 * Parts: the climbing plants and the ground are drawn across the screen ({@code FRONT}), the growth on the elements belongs to them
 * ({@code ELEMENT}). Every part grows on its own clock, so a part that gets switched back on grows in again.<br>
 * Everything that grows lives in canvases that only change where something grew, bloomed or swayed. Only the ground strip along the
 * bottom gets painted again every frame, because the wind moves its blades all the time.
 */
public class OvergrowthDecorationOverlay extends PixelDecorationOverlay<OvergrowthDecorationOverlay> {

    /** Clock units per second of all parts */
    private static final float CLOCK_SPEED = 1.25F;
    /** The plants are designed, not random: these seeds give them their shape */
    private static final int CREEPER_SEED = 606;
    private static final int LEFT_SPARE_SEED = 5353;
    private static final int RIGHT_SPARE_SEED = 7171;
    private static final int GROUND_SEED = 607;
    private static final int GROUND_SPARE_SEED = 808;
    private static final String KEY = "fancymenu.decoration_overlays.overgrowth.";

    public final Property<Boolean> grass = putProperty(Property.booleanProperty("grass", true, KEY + "grass"));
    public final Property<Boolean> leftVines = putProperty(Property.booleanProperty("left_vines", true, KEY + "left_vines"));
    public final Property<Boolean> rightVines = putProperty(Property.booleanProperty("right_vines", true, KEY + "right_vines"));
    public final Property<Boolean> onElements = putProperty(Property.booleanProperty("on_elements", true, KEY + "on_elements"));
    public final Property<Boolean> flowersOnVines = putProperty(Property.booleanProperty("flowers_on_vines", true, KEY + "flowers_on_vines"));
    public final Property<Boolean> flowersOnElements = putProperty(Property.booleanProperty("flowers_on_elements", true, KEY + "flowers_on_elements"));
    public final Property<Boolean> flowersInGrass = putProperty(Property.booleanProperty("flowers_in_grass", true, KEY + "flowers_in_grass"));
    public final DecorationOverlayOptions.Amount flowerAmount = new DecorationOverlayOptions.Amount(this, "flower_amount", DecorationOverlayOptions.Amount.MIN_PERCENT, KEY + "flower_amount", KEY + "flower_amount.desc");

    private final TiledCanvas creeperCanvas = new TiledCanvas("overgrowth_vines");
    private final TiledCanvas elementCanvas = new TiledCanvas("overgrowth_elements");
    private final TiledCanvas groundCanvas = new TiledCanvas("overgrowth_ground");
    private final CanvasPainter painter = new CanvasPainter();
    private final PixelSurfaces<ElementGrowth> surfaces = this.createSurfaces(surface -> new ElementGrowth(surface.x(), surface.y(), surface.width(), surface.height()));
    private final GrassBed ground = new GrassBed();
    private final List<ElementGrowth> elementGrowths = new ArrayList<>();
    private final List<ElementGrowth> currentGrowths = new ArrayList<>();
    private final TiledCanvas.Content creeperContent = this::paintCreepers;
    private final TiledCanvas.Content elementContent = this::paintElements;
    private final TiledCanvas.Content groundContent = this.ground::paint;
    @NotNull
    private List<Creeper> leftCreepers = List.of();
    @NotNull
    private List<Creeper> rightCreepers = List.of();
    private int surfacesVersion = -1;
    private boolean resized = false;
    private boolean groundShown = false;
    private float leftClock = 0.0F;
    private float rightClock = 0.0F;
    private float elementClock = 0.0F;
    private float grassClock = 0.0F;
    private float floorClock = 0.0F;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.addToggle(menu, this.grass, MaterialIcons.GRASS, "grass");
        this.addToggle(menu, this.leftVines, MaterialIcons.ALIGN_HORIZONTAL_LEFT, "left_vines");
        this.addToggle(menu, this.rightVines, MaterialIcons.ALIGN_HORIZONTAL_RIGHT, "right_vines");
        this.addToggle(menu, this.onElements, MaterialIcons.WIDGETS, "on_elements");

        menu.addSeparatorEntry("separator_before_flowers");

        this.addToggle(menu, this.flowersOnVines, MaterialIcons.LOCAL_FLORIST, "flowers_on_vines");
        this.addToggle(menu, this.flowersOnElements, MaterialIcons.FILTER_VINTAGE, "flowers_on_elements");
        this.addToggle(menu, this.flowersInGrass, MaterialIcons.YARD, "flowers_in_grass");
        this.flowerAmount.addEntry(menu, this);

    }

    private void addToggle(@NotNull ContextMenu menu, @NotNull Property<Boolean> property, @NotNull MaterialIcon icon, @NotNull String name) {
        property.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(icon)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(KEY + name + ".desc")));
    }

    @Override
    protected void onResize(int width, int height) {
        this.creeperCanvas.resizeToScreen(width, height);
        this.elementCanvas.resizeToScreen(width, height);
        this.groundCanvas.resize(0, height - GrassBed.STRIP_HEIGHT, width, GrassBed.STRIP_HEIGHT, width, GrassBed.STRIP_HEIGHT);
        // Both sides come from one random source, like they were grown together
        PixelRandom creeperRandom = new PixelRandom(CREEPER_SEED);
        this.leftCreepers = CreeperGrower.grow(creeperRandom, LEFT_SPARE_SEED, false, width, height);
        this.rightCreepers = CreeperGrower.grow(creeperRandom, RIGHT_SPARE_SEED, true, width, height);
        this.ground.build(new PixelRandom(GROUND_SEED), GROUND_SPARE_SEED, width, height);
        this.elementGrowths.clear();
        this.surfacesVersion = -1;
        this.groundShown = false;
        this.resized = true;
    }

    @Override
    protected void update(float deltaSeconds) {
        boolean grassOn = this.grass.tryGetNonNullElse(true);
        boolean leftOn = this.leftVines.tryGetNonNullElse(true);
        boolean rightOn = this.rightVines.tryGetNonNullElse(true);
        boolean elementsOn = this.onElements.tryGetNonNullElse(true);
        boolean vineFlowers = this.flowersOnVines.tryGetNonNullElse(true);
        boolean elementFlowers = this.flowersOnElements.tryGetNonNullElse(true);
        boolean grassFlowers = this.flowersInGrass.tryGetNonNullElse(true);
        float step = deltaSeconds * CLOCK_SPEED;
        this.leftClock = leftOn ? (this.leftClock + step) : 0.0F;
        this.rightClock = rightOn ? (this.rightClock + step) : 0.0F;
        this.elementClock = elementsOn ? (this.elementClock + step) : 0.0F;
        this.grassClock = grassOn ? (this.grassClock + step) : 0.0F;
        // The flowers on the floor follow their own clock, which runs while the grass or these flowers are on, so they can stand without grass
        this.floorClock = (grassOn || grassFlowers) ? (this.floorClock + step) : 0.0F;
        // A flower shows while its rank is below a third of the flower amount
        float threshold = this.flowerAmount.getFactor() / 3.0F;
        float time = this.getTime();

        for (Creeper creeper : this.leftCreepers) creeper.update(this.leftClock, time, leftOn, vineFlowers ? threshold : 0.0F, this.creeperCanvas);
        for (Creeper creeper : this.rightCreepers) creeper.update(this.rightClock, time, rightOn, vineFlowers ? threshold : 0.0F, this.creeperCanvas);
        this.creeperCanvas.repaint(this.painter, this.creeperContent);

        this.syncElements(elementsOn);
        for (ElementGrowth growth : this.elementGrowths) growth.update(this.elementClock, time, elementsOn, elementFlowers ? threshold : 0.0F, this.elementCanvas);
        // A resize empties the canvas, but elements that kept their place also keep their growth, which has to be painted again
        if (this.resized) this.elementCanvas.invalidateAll();
        this.elementCanvas.repaint(this.painter, this.elementContent);

        boolean shown = this.ground.update(this.grassClock, this.floorClock, time, grassOn, grassFlowers, threshold);
        // The wind moves the ground every frame, and once it is gone it gets erased one last time
        if (shown || this.groundShown) {
            this.groundCanvas.invalidateAll();
            this.groundCanvas.repaint(this.painter, this.groundContent);
        }
        this.groundShown = shown;
        this.resized = false;
    }

    /**
     * Keeps the growth in step with the elements: growth of elements that are gone gets erased, and new elements get their growth.
     * Elements that are there from the start or after a resize follow the clock of the part, ones that show up later grow in.
     */
    private void syncElements(boolean elementsOn) {
        if (this.surfaces.version() != this.surfacesVersion) {
            this.surfacesVersion = this.surfaces.version();
            this.currentGrowths.clear();
            for (PixelSurfaces.Surface<ElementGrowth> surface : this.surfaces.elements()) {
                ElementGrowth growth = surface.state();
                if (growth != null) this.currentGrowths.add(growth);
            }
            for (ElementGrowth old : this.elementGrowths) {
                if (!this.currentGrowths.contains(old)) old.invalidateAll(this.elementCanvas);
            }
            this.currentGrowths.sort(Comparator.comparingInt((ElementGrowth growth) -> growth.top).thenComparingInt(growth -> growth.left));
            for (int i = 0; i < this.currentGrowths.size(); i++) {
                ElementGrowth growth = this.currentGrowths.get(i);
                if (growth.isBuilt()) continue;
                growth.build(i);
                growth.startClock = this.resized ? 0.0F : this.elementClock;
            }
            this.elementGrowths.clear();
            this.elementGrowths.addAll(this.currentGrowths);
        }
        if (!elementsOn) {
            for (ElementGrowth growth : this.elementGrowths) growth.startClock = 0.0F;
        }
    }

    private void paintCreepers(@NotNull CanvasPainter painter) {
        paintSide(painter, this.leftCreepers);
        paintSide(painter, this.rightCreepers);
    }

    /**
     * Stems first, then the leaves and flowers of the side on top of them.
     */
    private static void paintSide(@NotNull CanvasPainter painter, @NotNull List<Creeper> creepers) {
        for (Creeper creeper : creepers) creeper.paintStem(painter);
        for (Creeper creeper : creepers) creeper.paintDecor(painter);
    }

    private void paintElements(@NotNull CanvasPainter painter) {
        for (ElementGrowth growth : this.elementGrowths) growth.paint(painter);
    }

    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        this.elementCanvas.draw(graphics);
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        this.creeperCanvas.draw(graphics);
        this.groundCanvas.draw(graphics);
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.creeperCanvas.releaseTextures();
        this.elementCanvas.releaseTextures();
        this.groundCanvas.releaseTextures();
    }

}
