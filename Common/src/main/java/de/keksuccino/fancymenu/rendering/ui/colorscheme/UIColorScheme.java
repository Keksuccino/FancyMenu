package de.keksuccino.fancymenu.rendering.ui.colorscheme;

import de.keksuccino.fancymenu.rendering.DrawableColor;
import de.keksuccino.fancymenu.rendering.ui.UIBase;

import java.awt.*;

public class UIColorScheme {

    public DrawableColor layoutEditorMouseSelectionRectangleColor = DrawableColor.of(new Color(3, 148, 252));
    public DrawableColor layoutEditorGridColorNormal = DrawableColor.of(new Color(255, 255, 255, 100));
    public DrawableColor layoutEditorGridColorCenter = DrawableColor.of(new Color(150, 105, 255, 100));
    public DrawableColor layoutEditorElementBorderColorNormal = DrawableColor.of(new Color(3, 148, 252));
    public DrawableColor layoutEditorElementBorderColorSelected = DrawableColor.of(new Color(3, 219, 252));
    public DrawableColor layoutEditorElementDraggingNotAllowedColor = DrawableColor.of(new Color(232, 54, 9, 200));
    public DrawableColor scrollGrabberColorNormal = DrawableColor.of(new Color(89, 91, 93, 100));
    public DrawableColor scrollGrabberColorHover = DrawableColor.of(new Color(102, 104, 104, 100));
    public DrawableColor screenBackgroundColor = DrawableColor.of(new Color(60, 63, 65));
    public DrawableColor screenBackgroundColorDarker = DrawableColor.of(new Color(38, 38, 38));
    public DrawableColor elementBorderColorNormal = DrawableColor.of(new Color(209, 194, 209));
    public DrawableColor elementBorderColorHover = DrawableColor.of(new Color(227, 211, 227));
    public DrawableColor elementBackgroundColorNormal = DrawableColor.of(new Color(71, 71, 71));
    public DrawableColor elementBackgroundColorHover = DrawableColor.of(new Color(83, 156, 212));
    public DrawableColor areaBackgroundColor = DrawableColor.of(new Color(43, 43, 43));
    public DrawableColor listEntryColorSelectedHovered = DrawableColor.of(new Color(50, 50, 50));
    public DrawableColor textEditorSideBarColor = DrawableColor.of(new Color(49, 51, 53));
    public DrawableColor textEditorLineNumberTextColorNormal = DrawableColor.of(new Color(91, 92, 94));
    public DrawableColor textEditorLineNumberTextColorSelected = DrawableColor.of(new Color(137, 147, 150));
    public DrawableColor listingDotColor1 = DrawableColor.of(new Color(62, 134, 160));
    public DrawableColor listingDotColor2 = DrawableColor.of(new Color(173, 108, 121));
    public DrawableColor listingDotColor3 = DrawableColor.of(new Color(170, 130, 63));
    public DrawableColor contextMenuShaderColor = DrawableColor.of(new Color(43, 43, 43, 100));

    public DrawableColor uiTextureColor = DrawableColor.of(new Color(255, 255, 255));

    public DrawableColor genericTextBaseColor = DrawableColor.of(new Color(255, 255, 255));
    public DrawableColor elementLabelColorNormal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor elementLabelColorInactive = DrawableColor.of(new Color(137, 147, 150));
    public DrawableColor descriptionAreaTextColor = DrawableColor.of(new Color(158, 170, 184));
    public DrawableColor textEditorTextColor = DrawableColor.of(new Color(158, 170, 184));
    public DrawableColor errorTextColor = DrawableColor.of(new Color(237, 69, 69));
    public DrawableColor warningTextColor = DrawableColor.of(new Color(170, 130, 63));

    public DrawableColor textEditorTextFormattingNestedTextColor1 = DrawableColor.of(new Color(235, 127, 127));
    public DrawableColor textEditorTextFormattingNestedTextColor2 = DrawableColor.of(new Color(235, 201, 127));
    public DrawableColor textEditorTextFormattingNestedTextColor3 = DrawableColor.of(new Color(190, 235, 127));
    public DrawableColor textEditorTextFormattingNestedTextColor4 = DrawableColor.of(new Color(127, 235, 230));
    public DrawableColor textEditorTextFormattingNestedTextColor5 = DrawableColor.of(new Color(127, 158, 235));
    public DrawableColor textEditorTextFormattingNestedTextColor6 = DrawableColor.of(new Color(150, 127, 235));
    public DrawableColor textEditorTextFormattingNestedTextColor7 = DrawableColor.of(new Color(212, 127, 235));
    public DrawableColor textEditorTextFormattingNestedTextColor8 = DrawableColor.of(new Color(245, 54, 54));
    public DrawableColor textEditorTextFormattingNestedTextColor9 = DrawableColor.of(new Color(245, 146, 54));
    public DrawableColor textEditorTextFormattingNestedTextColor10 = DrawableColor.of(new Color(245, 229, 54));
    public DrawableColor textEditorTextFormattingNestedTextColor11 = DrawableColor.of(new Color(105, 245, 54));
    public DrawableColor textEditorTextFormattingNestedTextColor12 = DrawableColor.of(new Color(54, 137, 245));

    public DrawableColor textEditorTextFormattingBracketsColor = DrawableColor.of(new Color(252, 223, 3));

    public DrawableColor fontRendererTextFormattingColorOrange = DrawableColor.of(new Color(16755200));
    public DrawableColor fontRendererTextFormattingColorGreen = DrawableColor.of(new Color(5635925));
    public DrawableColor fontRendererTextFormattingColorRed = DrawableColor.of(new Color(16733525));

    public void setUITextureShaderColor(float alpha) {
        UIBase.setShaderColor(uiTextureColor, alpha);
    }
    
}
