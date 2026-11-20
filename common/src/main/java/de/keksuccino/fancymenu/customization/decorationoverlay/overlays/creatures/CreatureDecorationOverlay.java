package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

/**
 * Base of the creature overlays (butterflies, birds, bats, bubbles, sky lanterns). It runs a {@link CreatureSimulation} with the
 * frame's {@link CreatureScene} and gives the overlay the shared Amount and Size options, if it has them.
 *
 * @param <O> the overlay class itself
 */
public abstract class CreatureDecorationOverlay<O extends CreatureDecorationOverlay<?>> extends PixelDecorationOverlay<O> {

    /** The Amount option, NULL for overlays without one. */
    @Nullable
    public final DecorationOverlayOptions.Amount amount;
    /** The Size option, NULL for overlays without one. */
    @Nullable
    public final DecorationOverlayOptions.Size size;
    private final CreatureScene scene = new CreatureScene(this.random, this.pointer, this.createSurfaces(null));
    @NotNull
    private final CreatureSimulation simulation;

    /**
     * @param simulationFactory creates the simulation of the overlay
     * @param amountDescriptionKey tooltip of the Amount option, which tells what the amount means for this overlay, NULL for no Amount option
     * @param sized whether the overlay has the Size option
     */
    protected CreatureDecorationOverlay(@NotNull Function<CreatureScene, CreatureSimulation> simulationFactory, @Nullable String amountDescriptionKey, boolean sized) {
        this.amount = (amountDescriptionKey != null) ? new DecorationOverlayOptions.Amount(this, DecorationOverlayOptions.Amount.DEFAULT_KEY, DecorationOverlayOptions.Amount.DEFAULT_PERCENT, "fancymenu.decoration_overlay.amount", amountDescriptionKey) : null;
        this.size = sized ? new DecorationOverlayOptions.Size(this) : null;
        this.simulation = simulationFactory.apply(this.scene);
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
        if (this.amount != null) this.amount.addEntry(menu, this);
        if (this.size != null) this.size.addEntry(menu, this);
    }

    @Override
    protected void update(float deltaSeconds) {
        float amountFactor = (this.amount != null) ? this.amount.getFactor() : 1.0F;
        int scale = (this.size != null) ? this.size.get() : 1;
        this.scene.update(this.getWidth(), this.getHeight(), this.getTime(), amountFactor, scale);
        this.simulation.update(deltaSeconds);
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        this.simulation.drawBack(graphics);
    }

    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        this.simulation.drawElement(graphics);
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        this.simulation.drawFront(graphics);
    }

    @Override
    protected void onClick(float x, float y, boolean primaryButton) {
        this.simulation.onClick(x, y, primaryButton);
    }

}
