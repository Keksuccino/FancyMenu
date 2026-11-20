package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.birds;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureDecorationOverlay;

/**
 * Small birds that fly in, hop and peck on the elements and take off when the pointer comes close (see {@link BirdFlock}).<br>
 * Element: birds perched on an element. Front: birds flying in or off and the ones perched on the floor.
 */
public class BirdsDecorationOverlay extends CreatureDecorationOverlay<BirdsDecorationOverlay> {

    public BirdsDecorationOverlay() {
        super(BirdFlock::new, "fancymenu.decoration_overlays.birds.amount.desc", true);
    }

}
