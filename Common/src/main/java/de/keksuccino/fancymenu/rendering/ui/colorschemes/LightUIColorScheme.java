package de.keksuccino.fancymenu.rendering.ui.colorschemes;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.rendering.DrawableColor;

import java.awt.*;

public class LightUIColorScheme extends UIColorScheme {

    public LightUIColorScheme() {

        layoutEditorMouseSelectionRectangleColor = DrawableColor.of(new Color(3, 148, 252));
        layoutEditorGridColorNormal = DrawableColor.of(new Color(101, 101, 101, 100));
        layoutEditorGridColorCenter = DrawableColor.of(new Color(161, 79, 255, 100));
        layoutEditorElementBorderColorNormal = DrawableColor.of(new Color(3, 148, 252));
        layoutEditorElementBorderColorSelected = DrawableColor.of(new Color(3, 219, 252));
        layoutEditorElementDraggingNotAllowedColor = DrawableColor.of(new Color(232, 54, 9, 200));
        scrollGrabberColorNormal = DrawableColor.of(new Color(89, 91, 93, 100));
        scrollGrabberColorHover = DrawableColor.of(new Color(102, 104, 104, 100));
        screenBackgroundColor = DrawableColor.of(new Color(206, 206, 206));
        screenBackgroundColorDarker = DrawableColor.of(new Color(173, 173, 173));
        elementBorderColorNormal = DrawableColor.of(new Color(56, 56, 56));
        elementBorderColorHover = DrawableColor.of(new Color(68, 68, 68));
        elementBackgroundColorNormal = DrawableColor.of(new Color(213, 213, 213));
        elementBackgroundColorHover = DrawableColor.of(new Color(83, 156, 212));
        areaBackgroundColor = DrawableColor.of(new Color(180, 180, 180));
        listEntryColorSelected = DrawableColor.of(new Color(203, 203, 203));
        textEditorSideBarColor = DrawableColor.of(new Color(164, 164, 164));
        textEditorLineNumberTextColorNormal = DrawableColor.of(new Color(82, 82, 82));
        textEditorLineNumberTextColorSelected = DrawableColor.of(new Color(105, 105, 105));
        listingDotColor1 = DrawableColor.of(new Color(67, 141, 208));
        listingDotColor2 = DrawableColor.of(new Color(171, 57, 80));
        listingDotColor3 = DrawableColor.of(new Color(178, 116, 12));

        genericTextBaseColor = DrawableColor.of(new Color(23, 23, 23));
        elementLabelColorNormal = DrawableColor.of(new Color(45, 45, 45));
        elementLabelColorInactive = DrawableColor.of(new Color(154, 154, 154));
        uiTextColor1 = DrawableColor.of(new Color(183, 51, 51));
        uiTextColor2 = DrawableColor.of(new Color(155, 97, 5));
        uiTextColor3 = DrawableColor.of(new Color(72, 78, 83));
        uiTextColor4 = DrawableColor.of(new Color(91, 92, 94));

    }

    @Override
    public void setUITextureShaderColor(float alpha) {
        RenderSystem.setShaderColor(0.09F, 0.09F, 0.09F, alpha);
    }

}
