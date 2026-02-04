package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;

import java.awt.Color;

public class PurpleVoidUITheme extends UITheme {

    public PurpleVoidUITheme() {

        super("purple_void", "fancymenu.ui.themes.purple_void");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(210, 180, 255, 16));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(235, 235, 238));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(12, 12, 14, 190));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(52, 52, 60, 170));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(10, 10, 12, 220));
        ui_blur_interface_border_color = DrawableColor.of(new Color(58, 58, 64, 190));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(16, 16, 18, 220));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(14, 14, 16, 170));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(10, 10, 12, 155));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(58, 58, 64, 160));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(60, 32, 92, 150));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(16, 16, 18, 160));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(22, 22, 26, 160));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(60, 30, 96, 180));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(168, 92, 240, 180));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(58, 58, 64, 170));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(235, 235, 238));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(145, 145, 154));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(16, 16, 18, 160));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(58, 58, 64, 160));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(170, 96, 255, 210));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(235, 235, 238));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(145, 145, 154));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(120, 120, 130));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(235, 235, 238));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(12, 12, 14, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(210, 180, 255, 16));
        ui_icon_texture_color = DrawableColor.of(new Color(235, 235, 238));
        ui_overlay_background_color = DrawableColor.of(new Color(12, 12, 14));
        ui_overlay_border_color = DrawableColor.of(new Color(52, 52, 60));
        ui_interface_background_color = DrawableColor.of(new Color(10, 10, 12));
        ui_interface_border_color = DrawableColor.of(new Color(52, 52, 60));
        ui_interface_title_bar_color = DrawableColor.of(new Color(16, 16, 18));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(14, 14, 16));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(10, 10, 12));
        ui_interface_area_border_color = DrawableColor.of(new Color(52, 52, 60));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(46, 24, 70));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(16, 16, 18));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(22, 22, 26));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(60, 30, 96));
        ui_interface_widget_border_color = DrawableColor.of(new Color(52, 52, 60));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(235, 235, 238));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(145, 145, 154));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(14, 14, 16));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(52, 52, 60));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(170, 96, 255));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(235, 235, 238));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(145, 145, 154));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(120, 120, 130));
        ui_interface_generic_text_color = DrawableColor.of(new Color(235, 235, 238));
        ui_tooltip_background_color = DrawableColor.of(new Color(16, 16, 18));

        info_color = DrawableColor.of(new Color(170, 96, 255));
        success_color = DrawableColor.of(new Color(92, 198, 120));
        error_color = DrawableColor.of(new Color(236, 78, 96));
        warning_color = DrawableColor.of(new Color(236, 168, 74));

        layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(170, 96, 255));
        layout_editor_grid_color_normal = DrawableColor.of(new Color(134, 82, 204, 120));
        layout_editor_grid_color_center = DrawableColor.of(new Color(170, 96, 255, 140));
        layout_editor_element_border_color_normal = DrawableColor.of(new Color(160, 88, 248));
        layout_editor_element_border_color_selected = DrawableColor.of(new Color(200, 132, 255));
        layout_editor_element_border_rotation_controls_color = DrawableColor.of(new Color(186, 106, 255));
        layout_editor_element_border_vertical_tilting_controls_color = DrawableColor.of(new Color(236, 168, 74));
        layout_editor_element_border_horizontal_tilting_controls_color = DrawableColor.of(new Color(96, 210, 170));
        layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(236, 78, 96, 200));
        layout_editor_element_border_display_line_background_color = DrawableColor.of(new Color(0, 0, 0, 140));
        layout_editor_element_border_display_line_text_color = DrawableColor.of(new Color(235, 222, 255));
        layout_editor_anchor_point_overlay_color_base = DrawableColor.of(new Color(96, 210, 170));
        layout_editor_anchor_point_overlay_color_border = DrawableColor.of(new Color(46, 118, 92));
        menu_bar_close_icon_color = DrawableColor.of(new Color(236, 98, 118));
        pip_docking_overlay_color = DrawableColor.of(new Color(160, 88, 248, 90));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(160, 88, 248, 200));

        scroll_grabber_color_normal = DrawableColor.of(new Color(84, 84, 92, 140));
        scroll_grabber_color_hover = DrawableColor.of(new Color(132, 92, 186, 190));

        actions_entry_background_color_action = DrawableColor.of(new Color(20, 20, 24));
        actions_entry_background_color_action_hover = DrawableColor.of(new Color(28, 28, 32));
        actions_entry_background_color_if = DrawableColor.of(new Color(20, 20, 24));
        actions_entry_background_color_if_hover = DrawableColor.of(new Color(28, 28, 32));
        actions_entry_background_color_else_if = DrawableColor.of(new Color(20, 20, 24));
        actions_entry_background_color_else_if_hover = DrawableColor.of(new Color(28, 28, 32));
        actions_entry_background_color_else = DrawableColor.of(new Color(20, 20, 24));
        actions_entry_background_color_else_hover = DrawableColor.of(new Color(28, 28, 32));
        actions_entry_background_color_while = DrawableColor.of(new Color(20, 20, 24));
        actions_entry_background_color_while_hover = DrawableColor.of(new Color(28, 28, 32));
        actions_entry_background_color_delay = DrawableColor.of(new Color(20, 20, 24));
        actions_entry_background_color_delay_hover = DrawableColor.of(new Color(28, 28, 32));
        actions_entry_background_color_execute_later = DrawableColor.of(new Color(20, 20, 24));
        actions_entry_background_color_execute_later_hover = DrawableColor.of(new Color(28, 28, 32));
        actions_entry_background_color_folder = DrawableColor.of(new Color(20, 20, 24));
        actions_entry_background_color_folder_hover = DrawableColor.of(new Color(28, 28, 32));
        actions_entry_background_color_generic_block = DrawableColor.of(new Color(20, 20, 24));
        actions_entry_background_color_generic_block_hover = DrawableColor.of(new Color(28, 28, 32));
        actions_chain_indicator_color = DrawableColor.of(new Color(126, 98, 162, 180));
        actions_chain_indicator_hovered_color = DrawableColor.of(new Color(170, 96, 255, 210));
        actions_chain_indicator_selected_color = DrawableColor.of(new Color(200, 132, 255, 220));
        actions_minimap_background_color = DrawableColor.of(new Color(10, 10, 12, 210));
        actions_minimap_border_color = DrawableColor.of(new Color(60, 60, 68, 220));
        actions_minimap_viewport_color = DrawableColor.of(new Color(255, 255, 255, 32));
        actions_minimap_viewport_border_color = DrawableColor.of(new Color(190, 190, 200, 120));
        actions_minimap_tooltip_border_color = DrawableColor.of(new Color(170, 96, 255, 220));

        bullet_list_dot_color_1 = DrawableColor.of(new Color(170, 96, 255));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(132, 86, 196));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(96, 210, 170));

        input_field_suggestions_background_color = DrawableColor.of(new Color(18, 18, 20));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(235, 235, 238));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(170, 96, 255));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(96, 96, 104));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(136, 136, 146));
        text_editor_text_color = DrawableColor.of(new Color(218, 218, 224));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(200, 132, 255));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(236, 168, 74));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(126, 214, 166));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(112, 186, 224));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(134, 140, 232));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(180, 120, 232));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(220, 120, 200));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(236, 78, 96));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(236, 168, 74));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(238, 204, 96));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(120, 220, 160));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(112, 170, 210));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(200, 132, 255));

    }

}
