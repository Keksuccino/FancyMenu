package de.keksuccino.fancymenu.customization.element.anchor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ElementAnchorPoints {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<ElementAnchorPoint> ANCHOR_POINTS = new ArrayList<>();

    public static final ElementAnchorPoint ELEMENT = registerAnchorPoint(new ElementAnchorPoint.AnchorElement());
    public static final ElementAnchorPoint VANILLA = registerAnchorPoint(new ElementAnchorPoint.AnchorVanilla());
    public static final ElementAnchorPoint AUTO = registerAnchorPoint(new ElementAnchorPoint.AnchorAuto());
    public static final ElementAnchorPoint TOP_LEFT = registerAnchorPoint(new ElementAnchorPoint.AnchorTopLeft());
    public static final ElementAnchorPoint MID_LEFT = registerAnchorPoint(new ElementAnchorPoint.AnchorMidLeft());
    public static final ElementAnchorPoint BOTTOM_LEFT = registerAnchorPoint(new ElementAnchorPoint.AnchorBottomLeft());
    public static final ElementAnchorPoint TOP_CENTERED = registerAnchorPoint(new ElementAnchorPoint.AnchorTopCenter());
    public static final ElementAnchorPoint MID_CENTERED = registerAnchorPoint(new ElementAnchorPoint.AnchorMidCenter());
    public static final ElementAnchorPoint BOTTOM_CENTERED = registerAnchorPoint(new ElementAnchorPoint.AnchorBottomCenter());
    public static final ElementAnchorPoint TOP_RIGHT = registerAnchorPoint(new ElementAnchorPoint.AnchorTopRight());
    public static final ElementAnchorPoint MID_RIGHT = registerAnchorPoint(new ElementAnchorPoint.AnchorMidRight());
    public static final ElementAnchorPoint BOTTOM_RIGHT = registerAnchorPoint(new ElementAnchorPoint.AnchorBottomRight());

    public static ElementAnchorPoint registerAnchorPoint(ElementAnchorPoint anchorPoint) {
        ElementAnchorPoint e = getAnchorPointByName(anchorPoint.getName());
        if (e != null) {
            LOGGER.warn("[FANCYMENU] Replacing ElementAnchorPoint: " + anchorPoint.getName());
            ANCHOR_POINTS.remove(e);
        }
        ANCHOR_POINTS.add(anchorPoint);
        return anchorPoint;
    }

    public static List<ElementAnchorPoint> getAnchorPoints() {
        return new ArrayList<>(ANCHOR_POINTS);
    }

    @Nullable
    public static ElementAnchorPoint getAnchorPointByName(@NotNull String name) {
        if (name.equals("original")) {
            return VANILLA;
        }
        for (ElementAnchorPoint p : ANCHOR_POINTS) {
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

}
