package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.skylanterns;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureDecorationOverlay;

/**
 * Paper lanterns that float up in three depths, sometimes released as a whole group (see {@link LanternSky}).<br>
 * Back: the small far lanterns. Front: middle and near lanterns with their halos.
 */
public class SkyLanternsDecorationOverlay extends CreatureDecorationOverlay<SkyLanternsDecorationOverlay> {

    public SkyLanternsDecorationOverlay() {
        super(LanternSky::new, "fancymenu.decoration_overlays.sky_lanterns.amount.desc", true);
    }

}
