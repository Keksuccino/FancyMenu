package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.butterflies;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureDecorationOverlay;

/**
 * Butterflies that wander in loops, rest on the elements and shy away from the pointer (see {@link ButterflySwarm}).<br>
 * Back: the small far butterflies. Element: butterflies resting on an element. Front: flying butterflies and the ones resting on the floor.
 */
public class ButterfliesDecorationOverlay extends CreatureDecorationOverlay<ButterfliesDecorationOverlay> {

    public ButterfliesDecorationOverlay() {
        super(ButterflySwarm::new, "fancymenu.decoration_overlays.butterflies.amount.desc", true);
    }

}
