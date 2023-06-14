package de.keksuccino.fancymenu.rendering.ui.colorscheme.schemes;

import de.keksuccino.fancymenu.rendering.DrawableColor;
import de.keksuccino.fancymenu.rendering.ui.colorscheme.UIColorScheme;

import java.awt.*;

public class LightUIColorScheme extends UIColorScheme {

    public LightUIColorScheme() {

        menuBarBottomLineColor = DrawableColor.of(new Color(119, 119, 119));

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
        listEntryColorSelectedHovered = DrawableColor.of(new Color(164, 164, 164));
        textEditorSideBarColor = DrawableColor.of(new Color(164, 164, 164));
        textEditorLineNumberTextColorNormal = DrawableColor.of(new Color(105, 105, 105));
        textEditorLineNumberTextColorSelected = DrawableColor.of(new Color(70, 70, 70));
        listingDotColor1 = DrawableColor.of(new Color(67, 141, 208));
        listingDotColor2 = DrawableColor.of(new Color(171, 57, 80));
        listingDotColor3 = DrawableColor.of(new Color(178, 116, 12));

        contextMenuBorderColor = DrawableColor.of(new Color(119, 119, 119));

        uiTextureColor = DrawableColor.of(new Color(45, 45, 45));

        genericTextBaseColor = DrawableColor.of(new Color(37, 37, 37));
        elementLabelColorNormal = DrawableColor.of(new Color(45, 45, 45));
        elementLabelColorInactive = DrawableColor.of(new Color(154, 154, 154));
        descriptionAreaTextColor = DrawableColor.of(new Color(72, 78, 83));
        textEditorTextColor = DrawableColor.of(new Color(72, 78, 83));
        errorTextColor = DrawableColor.of(new Color(183, 51, 51));
        warningTextColor = DrawableColor.of(new Color(155, 97, 5));

        textEditorTextFormattingNestedTextColor1 = DrawableColor.of(new Color(161, 15, 15));
        textEditorTextFormattingNestedTextColor2 = DrawableColor.of(new Color(178, 125, 9));
        textEditorTextFormattingNestedTextColor3 = DrawableColor.of(new Color(102, 168, 10));
        textEditorTextFormattingNestedTextColor4 = DrawableColor.of(new Color(8, 152, 145));
        textEditorTextFormattingNestedTextColor5 = DrawableColor.of(new Color(7, 46, 141));
        textEditorTextFormattingNestedTextColor6 = DrawableColor.of(new Color(38, 6, 157));
        textEditorTextFormattingNestedTextColor7 = DrawableColor.of(new Color(106, 6, 133));
        textEditorTextFormattingNestedTextColor8 = DrawableColor.of(new Color(115, 3, 3));
        textEditorTextFormattingNestedTextColor9 = DrawableColor.of(new Color(133, 67, 6));
        textEditorTextFormattingNestedTextColor10 = DrawableColor.of(new Color(145, 133, 4));
        textEditorTextFormattingNestedTextColor11 = DrawableColor.of(new Color(38, 122, 7));
        textEditorTextFormattingNestedTextColor12 = DrawableColor.of(new Color(54, 60, 245));

        textEditorTextFormattingBracketsColor = DrawableColor.of(new Color(255, 58, 0, 100));

        fontRendererTextFormattingColorOrange = DrawableColor.of(new Color(0xAB7103));
        fontRendererTextFormattingColorGreen = DrawableColor.of(new Color(0x038103));
        fontRendererTextFormattingColorRed = DrawableColor.of(new Color(0xC40707));

    }

}
