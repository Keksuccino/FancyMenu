package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UIColorTheme;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class LightUIColorTheme extends UIColorTheme {

    public LightUIColorTheme() {

        super("light", "Light");

        menu_bar_bottom_line_color = DrawableColor.of(new Color(119, 119, 119));

        layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(3, 148, 252));
        layout_editor_grid_color_normal = DrawableColor.of(new Color(101, 101, 101, 100));
        layout_editor_grid_color_center = DrawableColor.of(new Color(161, 79, 255, 100));
        layout_editor_element_border_color_normal = DrawableColor.of(new Color(3, 148, 252));
        layout_editor_element_border_color_selected = DrawableColor.of(new Color(3, 219, 252));
        layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(232, 54, 9, 200));
        scroll_grabber_color_normal = DrawableColor.of(new Color(89, 91, 93, 100));
        scroll_grabber_color_hover = DrawableColor.of(new Color(102, 104, 104, 100));
        screen_background_color = DrawableColor.of(new Color(178, 178, 178));
        screen_background_color_darker = DrawableColor.of(new Color(173, 173, 173));
        element_border_color_normal = DrawableColor.of(new Color(56, 56, 56));
        element_border_color_hover = DrawableColor.of(new Color(68, 68, 68));
        element_background_color_normal = DrawableColor.of(new Color(203, 203, 203));
        element_background_color_hover = DrawableColor.of(new Color(175, 175, 175));
        area_background_color = DrawableColor.of(new Color(203, 203, 203));
        edit_box_background_color = DrawableColor.of(new Color(203, 203, 203));
        edit_box_border_color_normal = DrawableColor.of(new Color(56, 56, 56));
        edit_box_border_color_focused = DrawableColor.of(new Color(68, 68, 68));
        list_entry_color_selected_hovered = DrawableColor.of(new Color(175, 175, 175));
        text_editor_sidebar_color = DrawableColor.of(new Color(164, 164, 164));
        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(105, 105, 105));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(70, 70, 70));
        listing_dot_color_1 = DrawableColor.of(new Color(67, 141, 208));
        listing_dot_color_2 = DrawableColor.of(new Color(171, 57, 80));
        listing_dot_color_3 = DrawableColor.of(new Color(178, 116, 12));

        context_menu_border_color = DrawableColor.of(new Color(119, 119, 119));

        ui_texture_color = DrawableColor.of(new Color(45, 45, 45));

        generic_text_base_color = DrawableColor.of(new Color(37, 37, 37));
        element_label_color_normal = DrawableColor.of(new Color(45, 45, 45));
        element_label_color_inactive = DrawableColor.of(new Color(138, 137, 137));
        edit_box_text_color_normal = DrawableColor.of(new Color(45, 45, 45));
        edit_box_text_color_uneditable = DrawableColor.of(new Color(138, 137, 137));
        description_area_text_color = DrawableColor.of(new Color(45, 45, 45));
        text_editor_text_color = DrawableColor.of(new Color(72, 78, 83));
        success_text_color = DrawableColor.of(new Color(25, 126, 2));
        error_text_color = DrawableColor.of(new Color(164, 27, 27));
        warning_text_color = DrawableColor.of(new Color(155, 97, 5));

        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(161, 15, 15));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(178, 125, 9));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(102, 168, 10));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(8, 152, 145));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(7, 46, 141));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(38, 6, 157));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(106, 6, 133));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(115, 3, 3));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(133, 67, 6));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(145, 133, 4));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(38, 122, 7));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(54, 60, 245));

        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(255, 58, 0, 100));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.ui.color_scheme.schemes.light");
    }

}
