package de.keksuccino.fancymenu.util.rendering.ui.theme;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.awt.*;

public class UIColorTheme {

    protected String identifier;
    protected String display_name;

    //----------------------------

    public DrawableColor menu_bar_bottom_line_color = DrawableColor.of(new Color(93, 97, 100));

    public DrawableColor layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(3, 148, 252));
    public DrawableColor layout_editor_grid_color_normal = DrawableColor.of(new Color(186, 121, 241, 100));
    public DrawableColor layout_editor_grid_color_center = DrawableColor.of(new Color(91, 94, 255, 100));
    public DrawableColor layout_editor_element_border_color_normal = DrawableColor.of(new Color(3, 148, 252));
    public DrawableColor layout_editor_element_border_color_selected = DrawableColor.of(new Color(3, 219, 252));
    public DrawableColor layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(232, 54, 9, 200));
    public DrawableColor layout_editor_anchor_point_overlay_color_base = DrawableColor.of(new Color(37, 180, 121));
    public DrawableColor layout_editor_anchor_point_overlay_color_border = DrawableColor.of(new Color(17, 79, 52));
    public DrawableColor layout_editor_close_icon_color = DrawableColor.of(new Color(218, 60, 30));
    public DrawableColor scroll_grabber_color_normal = DrawableColor.of(new Color(89, 91, 93, 100));
    public DrawableColor scroll_grabber_color_hover = DrawableColor.of(new Color(102, 104, 104, 100));
    public DrawableColor screen_background_color = DrawableColor.of(new Color(60, 63, 65));
    public DrawableColor screen_background_color_darker = DrawableColor.of(new Color(38, 38, 38));
    public DrawableColor element_border_color_normal = DrawableColor.of(new Color(93, 97, 100));
    public DrawableColor element_border_color_hover = DrawableColor.of(new Color(93, 97, 100));
    public DrawableColor element_background_color_normal = DrawableColor.of(new Color(71, 71, 71));
    public DrawableColor element_background_color_hover = DrawableColor.of(new Color(83, 156, 212));
    public DrawableColor slider_handle_color_normal = DrawableColor.of(new Color(71, 132, 180));
    public DrawableColor slider_handle_color_hover = DrawableColor.of(new Color(83, 156, 212));
    public DrawableColor area_background_color = DrawableColor.of(new Color(43, 43, 43));
    public DrawableColor edit_box_background_color = DrawableColor.of(new Color(43, 43, 43));
    public DrawableColor edit_box_border_color_normal = DrawableColor.of(new Color(209, 194, 209));
    public DrawableColor edit_box_border_color_focused = DrawableColor.of(new Color(227, 211, 227));
    public DrawableColor list_entry_color_selected_hovered = DrawableColor.of(new Color(50, 50, 50));
    public DrawableColor text_editor_sidebar_color = DrawableColor.of(new Color(49, 51, 53));
    public DrawableColor text_editor_line_number_text_color_normal = DrawableColor.of(new Color(91, 92, 94));
    public DrawableColor text_editor_line_number_text_color_selected = DrawableColor.of(new Color(137, 147, 150));
    public DrawableColor listing_dot_color_1 = DrawableColor.of(new Color(62, 134, 160));
    public DrawableColor listing_dot_color_2 = DrawableColor.of(new Color(173, 108, 121));
    public DrawableColor listing_dot_color_3 = DrawableColor.of(new Color(170, 130, 63));
    public DrawableColor suggestions_background_color = DrawableColor.of(new Color(71, 71, 71));
    public DrawableColor suggestions_text_color_normal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor suggestions_text_color_selected = DrawableColor.of(new Color(100, 165, 236));

    public DrawableColor ui_texture_color = DrawableColor.of(new Color(255, 255, 255));

    public DrawableColor generic_text_base_color = DrawableColor.of(new Color(255, 255, 255));
    public DrawableColor element_label_color_normal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor element_label_color_inactive = DrawableColor.of(new Color(113, 117, 119));
    public DrawableColor edit_box_text_color_normal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor edit_box_text_color_uneditable = DrawableColor.of(new Color(113, 117, 119));
    public DrawableColor edit_box_suggestion_text_color = DrawableColor.of(new Color(-8355712));
    public DrawableColor description_area_text_color = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor text_editor_text_color = DrawableColor.of(new Color(158, 170, 184));
    public DrawableColor success_text_color = DrawableColor.of(new Color(49, 206, 5));
    public DrawableColor error_text_color = DrawableColor.of(new Color(237, 69, 69));
    public DrawableColor warning_text_color = DrawableColor.of(new Color(229, 155, 18));

    public DrawableColor text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(235, 127, 127));
    public DrawableColor text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(235, 201, 127));
    public DrawableColor text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(190, 235, 127));
    public DrawableColor text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(127, 235, 230));
    public DrawableColor text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(127, 158, 235));
    public DrawableColor text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(150, 127, 235));
    public DrawableColor text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(212, 127, 235));
    public DrawableColor text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(245, 54, 54));
    public DrawableColor text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(245, 146, 54));
    public DrawableColor text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(245, 229, 54));
    public DrawableColor text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(105, 245, 54));
    public DrawableColor text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(54, 137, 245));

    public DrawableColor text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(252, 223, 3));

    protected UIColorTheme() {
    }

    public UIColorTheme(@NotNull String identifier, @NotNull String display_name) {
        this.identifier = identifier;
        this.display_name = display_name;
    }

    public void setUITextureShaderColor(GuiGraphics graphics, float alpha) {
        UIBase.setShaderColor(graphics, ui_texture_color, alpha);
    }

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @NotNull
    public Component getDisplayName() {
        if (this.display_name.startsWith("fancymenu.ui.themes.")) return Components.translatable(this.display_name);
        return Components.literal(this.display_name);
    }

}
