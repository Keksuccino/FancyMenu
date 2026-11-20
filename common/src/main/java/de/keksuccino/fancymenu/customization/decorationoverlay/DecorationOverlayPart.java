package de.keksuccino.fancymenu.customization.decorationoverlay;

/**
 * The kinds of content a decoration overlay draws. The Layers setting ({@link DecorationOverlayLayers}) decides which render pass
 * draws which part, see {@link AbstractDecorationOverlay}.
 */
public enum DecorationOverlayPart {

    /** Screen-wide content meant to be behind the elements, like far snow or the aurora. */
    BACK,
    /** Decorations attached to elements, like snow caps, frost or moss on a button, lit edges or a bird resting on a button. */
    ELEMENT,
    /** Screen-wide content meant to be in front of the elements, like near snow or fog. */
    FRONT

}
