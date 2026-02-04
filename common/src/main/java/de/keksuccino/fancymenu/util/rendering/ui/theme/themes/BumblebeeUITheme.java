package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;
import java.awt.Color;

public class BumblebeeUITheme extends UITheme {

    public BumblebeeUITheme() {

        super("bumblebee", "fancymenu.ui.themes.bumblebee");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(255, 220, 60, 18));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(236, 230, 210));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(20, 20, 22, 190));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(86, 86, 94, 160));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(20, 20, 22, 215));
        ui_blur_interface_border_color = DrawableColor.of(new Color(90, 90, 100, 180));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(34, 34, 38, 215));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(30, 30, 34, 155));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(22, 22, 26, 150));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(90, 90, 100, 150));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(120, 92, 24, 150));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(32, 32, 36, 150));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(240, 195, 0, 160));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(52, 52, 60, 170));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(255, 210, 40, 180));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(90, 90, 100, 150));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(240, 235, 210));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(150, 145, 130));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(30, 30, 34, 150));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(90, 90, 100, 150));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(240, 195, 0, 210));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(240, 235, 210));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(150, 145, 130));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(140, 130, 110));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(240, 235, 210));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(24, 24, 28, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(255, 220, 60, 18));
        ui_icon_texture_color = DrawableColor.of(new Color(236, 230, 210));
        ui_overlay_background_color = DrawableColor.of(new Color(24, 24, 26));
        ui_overlay_border_color = DrawableColor.of(new Color(70, 70, 78));
        ui_interface_background_color = DrawableColor.of(new Color(22, 22, 24));
        ui_interface_border_color = DrawableColor.of(new Color(70, 70, 78));
        ui_interface_title_bar_color = DrawableColor.of(new Color(34, 34, 38));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(28, 28, 32));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(20, 20, 24));
        ui_interface_area_border_color = DrawableColor.of(new Color(70, 70, 78));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(120, 92, 24));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(30, 30, 34));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(240, 195, 0));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(52, 52, 60));
        ui_interface_widget_border_color = DrawableColor.of(new Color(70, 70, 78));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(236, 230, 210));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(150, 145, 130));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(28, 28, 32));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(70, 70, 78));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(240, 195, 0));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(236, 230, 210));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(150, 145, 130));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(140, 130, 110));
        ui_interface_generic_text_color = DrawableColor.of(new Color(236, 230, 210));
        ui_tooltip_background_color = DrawableColor.of(new Color(26, 26, 30));

        success_color = DrawableColor.of(new Color(84, 200, 120));
        error_color = DrawableColor.of(new Color(220, 84, 74));
        warning_color = DrawableColor.of(new Color(240, 195, 0));

        layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(240, 195, 0));
        layout_editor_grid_color_normal = DrawableColor.of(new Color(200, 170, 60, 110));
        layout_editor_grid_color_center = DrawableColor.of(new Color(240, 195, 0, 130));
        layout_editor_element_border_color_normal = DrawableColor.of(new Color(240, 195, 0));
        layout_editor_element_border_color_selected = DrawableColor.of(new Color(255, 210, 40));
        layout_editor_element_border_rotation_controls_color = DrawableColor.of(new Color(180, 120, 40));
        layout_editor_element_border_vertical_tilting_controls_color = DrawableColor.of(new Color(232, 162, 64));
        layout_editor_element_border_horizontal_tilting_controls_color = DrawableColor.of(new Color(132, 190, 118));
        layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(220, 84, 74, 200));
        layout_editor_element_border_display_line_background_color = DrawableColor.of(new Color(0, 0, 0, 128));
        layout_editor_element_border_display_line_text_color = DrawableColor.of(new Color(236, 230, 210));
        layout_editor_anchor_point_overlay_color_base = DrawableColor.of(new Color(92, 186, 156));
        layout_editor_anchor_point_overlay_color_border = DrawableColor.of(new Color(52, 116, 96));
        menu_bar_close_icon_color = DrawableColor.of(new Color(240, 195, 0));
        pip_docking_overlay_color = DrawableColor.of(new Color(240, 195, 0, 90));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(240, 195, 0, 220));

        scroll_grabber_color_normal = DrawableColor.of(new Color(110, 110, 120, 150));
        scroll_grabber_color_hover = DrawableColor.of(new Color(150, 140, 110, 200));

        actions_entry_background_color_action = DrawableColor.of(new Color(34, 34, 38));
        actions_entry_background_color_action_hover = DrawableColor.of(new Color(46, 46, 52));
        actions_entry_background_color_if = DrawableColor.of(new Color(34, 44, 58));
        actions_entry_background_color_if_hover = DrawableColor.of(new Color(44, 58, 78));
        actions_entry_background_color_else_if = DrawableColor.of(new Color(44, 34, 58));
        actions_entry_background_color_else_if_hover = DrawableColor.of(new Color(58, 46, 78));
        actions_entry_background_color_else = DrawableColor.of(new Color(56, 40, 28));
        actions_entry_background_color_else_hover = DrawableColor.of(new Color(70, 52, 36));
        actions_entry_background_color_while = DrawableColor.of(new Color(28, 48, 44));
        actions_entry_background_color_while_hover = DrawableColor.of(new Color(36, 62, 56));
        actions_entry_background_color_delay = DrawableColor.of(new Color(60, 52, 28));
        actions_entry_background_color_delay_hover = DrawableColor.of(new Color(76, 66, 34));
        actions_entry_background_color_execute_later = DrawableColor.of(new Color(36, 44, 56));
        actions_entry_background_color_execute_later_hover = DrawableColor.of(new Color(46, 58, 76));
        actions_entry_background_color_folder = DrawableColor.of(new Color(56, 38, 42));
        actions_entry_background_color_folder_hover = DrawableColor.of(new Color(72, 50, 56));
        actions_entry_background_color_generic_block = DrawableColor.of(new Color(32, 32, 36));
        actions_entry_background_color_generic_block_hover = DrawableColor.of(new Color(44, 44, 50));
        actions_chain_indicator_color = DrawableColor.of(new Color(170, 150, 90, 180));
        actions_chain_indicator_hovered_color = DrawableColor.of(new Color(240, 195, 0, 210));
        actions_chain_indicator_selected_color = DrawableColor.of(new Color(240, 195, 0, 220));
        actions_minimap_background_color = DrawableColor.of(new Color(20, 20, 24, 210));
        actions_minimap_border_color = DrawableColor.of(new Color(96, 88, 76, 220));
        actions_minimap_viewport_color = DrawableColor.of(new Color(255, 255, 255, 32));
        actions_minimap_viewport_border_color = DrawableColor.of(new Color(184, 156, 134, 120));
        actions_minimap_tooltip_border_color = DrawableColor.of(new Color(240, 195, 0, 220));

        bullet_list_dot_color_1 = DrawableColor.of(new Color(240, 195, 0));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(200, 150, 40));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(132, 190, 118));

        input_field_suggestions_background_color = DrawableColor.of(new Color(28, 28, 32));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(236, 230, 210));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(240, 195, 0));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(130, 122, 114));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(170, 160, 150));
        text_editor_text_color = DrawableColor.of(new Color(230, 218, 198));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(232, 112, 84));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(240, 195, 0));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(132, 198, 110));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(96, 190, 170));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(118, 166, 236));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(176, 126, 236));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(214, 126, 182));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(220, 92, 78));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(240, 195, 0));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(242, 188, 98));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(118, 210, 148));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(94, 166, 176));

    }

}
