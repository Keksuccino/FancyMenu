package de.keksuccino.fancymenu.customization.decorationoverlay;

/**
 * The two render passes of decoration overlays in a frame.
 */
public enum DecorationOverlayPass {

    /** Behind all elements: after the menu backgrounds (with their blur and background overlay), before the background elements. */
    BACKGROUND,
    /** In front of all elements: after the foreground elements. */
    FOREGROUND

}
