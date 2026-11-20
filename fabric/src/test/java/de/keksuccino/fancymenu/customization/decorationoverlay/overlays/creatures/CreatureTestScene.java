package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPointer;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Runs creature simulations without a game, the way their overlay does: every frame syncs the surfaces with the element boxes,
 * moves the pointer, updates the scene and then the simulation. Seeded, so every run is the same.
 */
public final class CreatureTestScene {

    /** The four buttons of the mockup stage */
    public static final List<CollisionAreaBounds> MOCKUP_BUTTONS = List.of(
            new CollisionAreaBounds(165, 92, 150, 18),
            new CollisionAreaBounds(165, 116, 150, 18),
            new CollisionAreaBounds(165, 140, 150, 18),
            new CollisionAreaBounds(165, 164, 150, 18));
    public static final float FRAME = 1.0F / 60.0F;

    public final PixelSurfaces<Void> surfaces = new PixelSurfaces<>();
    public final PixelPointer pointer = new PixelPointer();
    public final CreatureScene scene;
    @NotNull
    private List<CollisionAreaBounds> boxes;
    private int width = CreatureScene.STAGE_WIDTH;
    private int height = CreatureScene.STAGE_HEIGHT;
    private float time = 0.0F;
    private float amount = 1.0F;
    private int scale = 1;
    private float pointerX = -1.0F;
    private float pointerY = -1.0F;

    public CreatureTestScene(int seed, @NotNull List<CollisionAreaBounds> boxes) {
        this.scene = new CreatureScene(new PixelRandom(seed), this.pointer, this.surfaces);
        this.boxes = boxes;
    }

    public void setBoxes(@NotNull List<CollisionAreaBounds> boxes) {
        this.boxes = boxes;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public void movePointer(float x, float y) {
        this.pointerX = x;
        this.pointerY = y;
    }

    public void hidePointer() {
        this.movePointer(-1.0F, -1.0F);
    }

    /**
     * Runs one frame of the given length.
     */
    public void frame(@NotNull CreatureSimulation simulation, float deltaSeconds) {
        this.time += deltaSeconds;
        this.surfaces.update(this.boxes, this.width, this.height);
        this.pointer.update(this.pointerX, this.pointerY, this.width, this.height);
        this.scene.update(this.width, this.height, this.time, this.amount, this.scale);
        simulation.update(deltaSeconds);
    }

    /**
     * Runs frames of 1/60 seconds for about the given time.
     */
    public void run(@NotNull CreatureSimulation simulation, float seconds) {
        int frames = Math.round(seconds / FRAME);
        for (int i = 0; i < frames; i++) this.frame(simulation, FRAME);
    }

}
