package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;

import java.awt.Color;

public class DarkHighContrastUITheme extends UITheme {

    public DarkHighContrastUITheme() {

        super("dark_high_contrast", "fancymenu.ui.themes.dark_high_contrast");

        allow_blur = false;
        allow_animations = false;
        interface_corner_rounding_radius = 0.0f;
        widget_corner_rounding_radius = 0.0f;

        ui_blur_icon_texture_color = DrawableColor.of(new Color(255, 255, 255));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(0, 0, 0, 220));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(255, 255, 255, 220));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(0, 0, 0, 220));
        ui_blur_interface_border_color = DrawableColor.of(new Color(255, 255, 255, 220));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(12, 12, 12, 220));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(8, 8, 8, 220));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(0, 0, 0, 220));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(255, 255, 255, 220));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(255, 170, 0, 120));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(8, 8, 8, 220));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(255, 170, 0, 200));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(30, 30, 30, 220));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(255, 210, 0, 220));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(255, 255, 255, 220));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(255, 255, 255));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(190, 190, 190));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(0, 0, 0, 220));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(255, 255, 255, 220));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(255, 170, 0, 220));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(255, 255, 255));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(190, 190, 190));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(200, 200, 200));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(255, 255, 255));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(0, 0, 0, 230));

        ui_icon_texture_color = DrawableColor.of(new Color(255, 255, 255));
        ui_overlay_background_color = DrawableColor.of(new Color(0, 0, 0));
        ui_overlay_border_color = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_background_color = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_border_color = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_title_bar_color = DrawableColor.of(new Color(12, 12, 12));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(8, 8, 8));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_area_border_color = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(255, 170, 0));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(8, 8, 8));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(255, 170, 0));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(30, 30, 30));
        ui_interface_widget_border_color = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(190, 190, 190));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(0, 0, 0));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(255, 170, 0));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(255, 255, 255));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(190, 190, 190));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(200, 200, 200));
        ui_interface_generic_text_color = DrawableColor.of(new Color(255, 255, 255));
        ui_tooltip_background_color = DrawableColor.of(new Color(0, 0, 0));

        success_text_color = DrawableColor.of(new Color(0, 255, 128));
        error_text_color = DrawableColor.of(new Color(255, 64, 64));
        warning_text_color = DrawableColor.of(new Color(255, 208, 0));

        layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(255, 170, 0));
        layout_editor_grid_color_normal = DrawableColor.of(new Color(255, 255, 255, 140));
        layout_editor_grid_color_center = DrawableColor.of(new Color(255, 170, 0, 180));
        layout_editor_element_border_color_normal = DrawableColor.of(new Color(255, 255, 255));
        layout_editor_element_border_color_selected = DrawableColor.of(new Color(255, 170, 0));
        layout_editor_element_border_rotation_controls_color = DrawableColor.of(new Color(0, 200, 255));
        layout_editor_element_border_vertical_tilting_controls_color = DrawableColor.of(new Color(255, 208, 0));
        layout_editor_element_border_horizontal_tilting_controls_color = DrawableColor.of(new Color(0, 255, 128));
        layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(255, 64, 64, 220));
        layout_editor_element_border_display_line_background_color = DrawableColor.of(new Color(0, 0, 0, 180));
        layout_editor_element_border_display_line_text_color = DrawableColor.of(new Color(255, 255, 255));
        layout_editor_anchor_point_overlay_color_base = DrawableColor.of(new Color(0, 255, 180));
        layout_editor_anchor_point_overlay_color_border = DrawableColor.of(new Color(0, 150, 110));
        menu_bar_close_icon_color = DrawableColor.of(new Color(255, 64, 64));

        scroll_grabber_color_normal = DrawableColor.of(new Color(255, 255, 255, 140));
        scroll_grabber_color_hover = DrawableColor.of(new Color(255, 170, 0, 200));

        actions_entry_background_color_action = DrawableColor.of(new Color(15, 15, 15));
        actions_entry_background_color_action_hover = DrawableColor.of(new Color(30, 30, 30));
        actions_entry_background_color_if = DrawableColor.of(new Color(0, 30, 60));
        actions_entry_background_color_if_hover = DrawableColor.of(new Color(0, 45, 90));
        actions_entry_background_color_else_if = DrawableColor.of(new Color(35, 0, 60));
        actions_entry_background_color_else_if_hover = DrawableColor.of(new Color(55, 0, 90));
        actions_entry_background_color_else = DrawableColor.of(new Color(60, 35, 0));
        actions_entry_background_color_else_hover = DrawableColor.of(new Color(90, 55, 0));
        actions_entry_background_color_while = DrawableColor.of(new Color(0, 45, 40));
        actions_entry_background_color_while_hover = DrawableColor.of(new Color(0, 65, 58));
        actions_entry_background_color_delay = DrawableColor.of(new Color(60, 50, 0));
        actions_entry_background_color_delay_hover = DrawableColor.of(new Color(85, 70, 0));
        actions_entry_background_color_execute_later = DrawableColor.of(new Color(0, 30, 70));
        actions_entry_background_color_execute_later_hover = DrawableColor.of(new Color(0, 45, 100));
        actions_entry_background_color_folder = DrawableColor.of(new Color(60, 0, 30));
        actions_entry_background_color_folder_hover = DrawableColor.of(new Color(85, 0, 45));
        actions_entry_background_color_generic_block = DrawableColor.of(new Color(20, 20, 20));
        actions_entry_background_color_generic_block_hover = DrawableColor.of(new Color(35, 35, 35));
        actions_chain_indicator_color = DrawableColor.of(new Color(255, 255, 255, 180));
        actions_chain_indicator_hovered_color = DrawableColor.of(new Color(255, 170, 0, 220));
        actions_chain_indicator_selected_color = DrawableColor.of(new Color(255, 208, 0, 240));
        actions_minimap_background_color = DrawableColor.of(new Color(0, 0, 0, 220));
        actions_minimap_border_color = DrawableColor.of(new Color(255, 255, 255, 220));
        actions_minimap_viewport_color = DrawableColor.of(new Color(255, 255, 255, 40));
        actions_minimap_viewport_border_color = DrawableColor.of(new Color(255, 170, 0, 140));
        actions_minimap_tooltip_border_color = DrawableColor.of(new Color(255, 170, 0, 220));

        bullet_list_dot_color_1 = DrawableColor.of(new Color(255, 170, 0));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(0, 200, 255));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(0, 255, 128));

        input_field_suggestions_background_color = DrawableColor.of(new Color(0, 0, 0));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(255, 255, 255));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(255, 170, 0));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(200, 200, 200));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(255, 255, 255));
        text_editor_text_color = DrawableColor.of(new Color(255, 255, 255));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(255, 64, 64));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(255, 208, 0));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(0, 255, 128));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(0, 200, 255));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(120, 180, 255));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(200, 120, 255));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(255, 120, 200));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(255, 100, 100));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(255, 170, 0));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(255, 230, 80));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(0, 255, 160));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(0, 220, 200));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(255, 208, 0));

    }

}
