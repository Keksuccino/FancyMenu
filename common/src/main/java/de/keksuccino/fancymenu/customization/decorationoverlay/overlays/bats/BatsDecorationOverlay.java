package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bats;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureDecorationOverlay;

/**
 * Bats that cross the screen in groups and hang asleep under the elements (see {@link BatColony}).<br>
 * Back: distant groups. Element: hanging bats. Front: near groups and single bats flying off or back to their spot.
 */
public class BatsDecorationOverlay extends CreatureDecorationOverlay<BatsDecorationOverlay> {

    public BatsDecorationOverlay() {
        super(BatColony::new, "fancymenu.decoration_overlays.bats.amount.desc", true);
    }

}
