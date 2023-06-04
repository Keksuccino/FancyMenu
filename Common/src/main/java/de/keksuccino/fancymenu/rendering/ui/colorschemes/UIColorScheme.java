package de.keksuccino.fancymenu.rendering.ui.colorschemes;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.rendering.DrawableColor;

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
    public DrawableColor listEntryColorSelected = DrawableColor.of(new Color(50, 50, 50));
    public DrawableColor textEditorSideBarColor = DrawableColor.of(new Color(49, 51, 53));
    public DrawableColor textEditorLineNumberTextColorNormal = DrawableColor.of(new Color(91, 92, 94));
    public DrawableColor textEditorLineNumberTextColorSelected = DrawableColor.of(new Color(137, 147, 150));
    public DrawableColor listingDotColor1 = DrawableColor.of(new Color(62, 134, 160));
    public DrawableColor listingDotColor2 = DrawableColor.of(new Color(173, 108, 121));
    public DrawableColor listingDotColor3 = DrawableColor.of(new Color(170, 130, 63));
    public DrawableColor contextMenuShaderColor = DrawableColor.of(new Color(43, 43, 43, 100));

    public DrawableColor genericTextBaseColor = DrawableColor.of(new Color(255, 255, 255));
    public DrawableColor elementLabelColorNormal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor elementLabelColorInactive = DrawableColor.of(new Color(137, 147, 150));
    public DrawableColor uiTextColor1 = DrawableColor.of(new Color(237, 69, 69));
    public DrawableColor uiTextColor2 = DrawableColor.of(new Color(170, 130, 63));
    public DrawableColor uiTextColor3 = DrawableColor.of(new Color(158, 170, 184));
    public DrawableColor uiTextColor4 = DrawableColor.of(new Color(91, 92, 94));

    public void setUITextureShaderColor(float alpha) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
    }
    
}
