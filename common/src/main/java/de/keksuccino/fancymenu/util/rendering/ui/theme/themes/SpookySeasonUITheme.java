package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;

import java.awt.Color;

public class SpookySeasonUITheme extends UITheme {

    public SpookySeasonUITheme() {

        super("spooky_season", "fancymenu.ui.themes.spooky_season");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(242, 231, 217, 13));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(242, 231, 217));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(16, 13, 20, 185));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(78, 70, 86, 160));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(18, 15, 23, 215));
        ui_blur_interface_border_color = DrawableColor.of(new Color(86, 77, 98, 180));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(30, 24, 34, 215));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(27, 22, 31, 155));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(19, 15, 23, 150));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(86, 77, 98, 150));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(63, 45, 80, 150));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(31, 25, 36, 150));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(117, 63, 25, 150));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(50, 35, 55, 170));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(145, 82, 34, 170));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(86, 77, 98, 150));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(242, 231, 217));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(150, 143, 157));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(27, 22, 31, 150));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(86, 77, 98, 150));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(212, 120, 43, 200));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(242, 231, 217));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(150, 143, 157));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(133, 124, 141));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(242, 231, 217));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(20, 16, 25, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(242, 231, 217, 13));
        ui_icon_texture_color = DrawableColor.of(new Color(242, 231, 217));
        ui_overlay_background_color = DrawableColor.of(new Color(23, 19, 28));
        ui_overlay_border_color = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_background_color = DrawableColor.of(new Color(21, 17, 26));
        ui_interface_border_color = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_title_bar_color = DrawableColor.of(new Color(34, 27, 38));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(28, 23, 33));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(20, 16, 25));
        ui_interface_area_border_color = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(58, 40, 74));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(30, 24, 35));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(122, 66, 27));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(46, 33, 52));
        ui_interface_widget_border_color = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(242, 231, 217));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(150, 143, 157));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(28, 23, 33));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(72, 65, 82));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(212, 120, 43));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(242, 231, 217));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(150, 143, 157));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(133, 124, 141));
        ui_interface_generic_text_color = DrawableColor.of(new Color(242, 231, 217));
        ui_tooltip_background_color = DrawableColor.of(new Color(24, 19, 29));

        success_color = DrawableColor.of(new Color(105, 186, 93));
        error_color = DrawableColor.of(new Color(220, 80, 72));
        warning_color = DrawableColor.of(new Color(232, 142, 56));

        layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(216, 108, 43));
        layout_editor_grid_color_normal = DrawableColor.of(new Color(150, 88, 180, 110));
        layout_editor_grid_color_center = DrawableColor.of(new Color(216, 108, 43, 130));
        layout_editor_element_border_color_normal = DrawableColor.of(new Color(216, 108, 43));
        layout_editor_element_border_color_selected = DrawableColor.of(new Color(242, 152, 74));
        layout_editor_element_border_rotation_controls_color = DrawableColor.of(new Color(156, 88, 209));
        layout_editor_element_border_vertical_tilting_controls_color = DrawableColor.of(new Color(232, 142, 56));
        layout_editor_element_border_horizontal_tilting_controls_color = DrawableColor.of(new Color(126, 186, 86));
        layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(220, 80, 72, 200));
        layout_editor_element_border_display_line_background_color = DrawableColor.of(new Color(0, 0, 0, 128));
        layout_editor_element_border_display_line_text_color = DrawableColor.of(new Color(242, 231, 217));
        layout_editor_anchor_point_overlay_color_base = DrawableColor.of(new Color(70, 168, 141));
        layout_editor_anchor_point_overlay_color_border = DrawableColor.of(new Color(36, 96, 82));
        menu_bar_close_icon_color = DrawableColor.of(new Color(232, 118, 74));
        pip_docking_overlay_color = DrawableColor.of(new Color(216, 108, 43, 80));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(216, 108, 43, 200));

        scroll_grabber_color_normal = DrawableColor.of(new Color(90, 83, 98, 140));
        scroll_grabber_color_hover = DrawableColor.of(new Color(120, 111, 129, 190));

        actions_entry_background_color_action = DrawableColor.of(new Color(38, 31, 44));
        actions_entry_background_color_action_hover = DrawableColor.of(new Color(48, 39, 58));
        actions_entry_background_color_if = DrawableColor.of(new Color(33, 41, 58));
        actions_entry_background_color_if_hover = DrawableColor.of(new Color(40, 52, 75));
        actions_entry_background_color_else_if = DrawableColor.of(new Color(42, 33, 58));
        actions_entry_background_color_else_if_hover = DrawableColor.of(new Color(54, 43, 76));
        actions_entry_background_color_else = DrawableColor.of(new Color(56, 40, 28));
        actions_entry_background_color_else_hover = DrawableColor.of(new Color(70, 52, 36));
        actions_entry_background_color_while = DrawableColor.of(new Color(28, 49, 45));
        actions_entry_background_color_while_hover = DrawableColor.of(new Color(36, 62, 56));
        actions_entry_background_color_delay = DrawableColor.of(new Color(60, 52, 26));
        actions_entry_background_color_delay_hover = DrawableColor.of(new Color(75, 64, 32));
        actions_entry_background_color_execute_later = DrawableColor.of(new Color(35, 45, 66));
        actions_entry_background_color_execute_later_hover = DrawableColor.of(new Color(45, 58, 84));
        actions_entry_background_color_folder = DrawableColor.of(new Color(55, 35, 45));
        actions_entry_background_color_folder_hover = DrawableColor.of(new Color(70, 46, 58));
        actions_entry_background_color_generic_block = DrawableColor.of(new Color(36, 31, 40));
        actions_entry_background_color_generic_block_hover = DrawableColor.of(new Color(46, 40, 52));
        actions_chain_indicator_color = DrawableColor.of(new Color(137, 112, 158, 170));
        actions_chain_indicator_hovered_color = DrawableColor.of(new Color(232, 142, 56, 210));
        actions_chain_indicator_selected_color = DrawableColor.of(new Color(212, 120, 43, 220));
        actions_minimap_background_color = DrawableColor.of(new Color(20, 16, 25, 210));
        actions_minimap_border_color = DrawableColor.of(new Color(92, 85, 104, 220));
        actions_minimap_viewport_color = DrawableColor.of(new Color(255, 255, 255, 32));
        actions_minimap_viewport_border_color = DrawableColor.of(new Color(190, 170, 200, 110));
        actions_minimap_tooltip_border_color = DrawableColor.of(new Color(232, 142, 56, 220));

        bullet_list_dot_color_1 = DrawableColor.of(new Color(232, 142, 56));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(170, 108, 202));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(140, 186, 120));

        input_field_suggestions_background_color = DrawableColor.of(new Color(30, 25, 36));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(242, 231, 217));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(232, 142, 56));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(122, 114, 131));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(156, 146, 165));
        text_editor_text_color = DrawableColor.of(new Color(216, 206, 193));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(232, 118, 74));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(236, 160, 88));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(132, 198, 96));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(94, 190, 168));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(110, 156, 236));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(170, 118, 236));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(214, 116, 176));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(220, 80, 72));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(232, 142, 56));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(242, 192, 88));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(106, 202, 132));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(94, 160, 170));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(232, 142, 56));

    }

}
