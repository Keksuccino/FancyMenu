package de.keksuccino.fancymenu.customization.decorationoverlay;

import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * The Layers setting of decoration overlays: which render pass draws which part of an overlay.<br>
 * The setting covers what an overlay draws across the screen ({@link DecorationOverlayPart#BACK} and {@link DecorationOverlayPart#FRONT}).
 * What it attaches to elements ({@link DecorationOverlayPart#ELEMENT}) always stays in the foreground pass, whatever the setting.
 * Every part gets drawn by exactly one pass, so an overlay with only one kind of part follows the setting too and never draws twice.
 */
public enum DecorationOverlayLayers {

    /** Far parts behind the elements, near parts in front of them. The default. */
    BACKGROUND_AND_FOREGROUND("background_and_foreground", List.of(DecorationOverlayPart.BACK), List.of(DecorationOverlayPart.ELEMENT, DecorationOverlayPart.FRONT)),
    /** Everything screen-wide behind the elements. */
    ONLY_BACKGROUND("only_background", List.of(DecorationOverlayPart.BACK, DecorationOverlayPart.FRONT), List.of(DecorationOverlayPart.ELEMENT)),
    /** Everything in front of the elements. */
    ONLY_FOREGROUND("only_foreground", List.of(), List.of(DecorationOverlayPart.BACK, DecorationOverlayPart.ELEMENT, DecorationOverlayPart.FRONT));

    public static final DecorationOverlayLayers DEFAULT = BACKGROUND_AND_FOREGROUND;

    @NotNull
    private final String name;
    @NotNull
    private final List<DecorationOverlayPart> backgroundParts;
    @NotNull
    private final List<DecorationOverlayPart> foregroundParts;

    DecorationOverlayLayers(@NotNull String name, @NotNull List<DecorationOverlayPart> backgroundParts, @NotNull List<DecorationOverlayPart> foregroundParts) {
        this.name = name;
        this.backgroundParts = backgroundParts;
        this.foregroundParts = foregroundParts;
    }

    /**
     * The parts the pass draws, in drawing order. The list is immutable and shared.
     */
    @NotNull
    public List<DecorationOverlayPart> getParts(@NotNull DecorationOverlayPass pass) {
        return (pass == DecorationOverlayPass.BACKGROUND) ? this.backgroundParts : this.foregroundParts;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getLocalizationKey() {
        return "fancymenu.decoration_overlay.layers." + this.name;
    }

}
