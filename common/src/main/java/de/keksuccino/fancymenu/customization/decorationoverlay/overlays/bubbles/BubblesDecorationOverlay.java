package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bubbles;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureDecorationOverlay;

/**
 * Bubbles that wobble upward in streams, slide along the underside of elements and pop on click (see {@link BubbleStreams}).<br>
 * Back: tiny far bubbles. Element: bubbles caught under an element and the pops of bubbles that hit one. Front: free bubbles and their pops.
 */
public class BubblesDecorationOverlay extends CreatureDecorationOverlay<BubblesDecorationOverlay> {

    public BubblesDecorationOverlay() {
        super(BubbleStreams::new, null, false);
    }

}
