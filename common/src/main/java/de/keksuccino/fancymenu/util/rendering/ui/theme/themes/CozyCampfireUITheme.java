package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;

import java.awt.Color;

public class CozyCampfireUITheme extends UITheme {

    public CozyCampfireUITheme() {

        super("cozy_campfire", "fancymenu.ui.themes.cozy_campfire");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(242, 229, 208, 13));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(242, 229, 208));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(24, 19, 16, 185));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(92, 78, 66, 160));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(26, 20, 16, 215));
        ui_blur_interface_border_color = DrawableColor.of(new Color(92, 78, 66, 160));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(38, 30, 22, 215));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(34, 27, 20, 155));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(24, 20, 16, 150));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(96, 82, 70, 150));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(78, 54, 40, 150));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(36, 28, 22, 150));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(176, 98, 44, 150));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(50, 38, 28, 170));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(206, 120, 52, 170));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(96, 82, 70, 150));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(242, 229, 208));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(156, 146, 134));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(34, 27, 20, 150));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(96, 82, 70, 150));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(220, 126, 58, 200));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(242, 229, 208));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(156, 146, 134));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(138, 128, 116));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(242, 229, 208));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(28, 22, 16, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(242, 229, 208, 13));
        ui_icon_texture_color = DrawableColor.of(new Color(242, 229, 208));
        ui_overlay_background_color = DrawableColor.of(new Color(26, 20, 16));
        ui_overlay_border_color = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_background_color = DrawableColor.of(new Color(24, 19, 15));
        ui_interface_border_color = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_title_bar_color = DrawableColor.of(new Color(42, 32, 24));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(34, 27, 20));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(22, 18, 14));
        ui_interface_area_border_color = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(70, 48, 36));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(36, 28, 22));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(176, 98, 44));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(52, 40, 30));
        ui_interface_widget_border_color = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(242, 229, 208));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(156, 146, 134));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(34, 27, 20));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(86, 74, 64));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(220, 126, 58));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(242, 229, 208));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(156, 146, 134));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(138, 128, 116));
        ui_interface_generic_text_color = DrawableColor.of(new Color(242, 229, 208));
        ui_tooltip_background_color = DrawableColor.of(new Color(28, 22, 16));

        success_color = DrawableColor.of(new Color(110, 196, 122));
        error_color = DrawableColor.of(new Color(220, 92, 78));
        warning_color = DrawableColor.of(new Color(232, 142, 64));

        layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(232, 142, 64));
        layout_editor_grid_color_normal = DrawableColor.of(new Color(170, 108, 76, 110));
        layout_editor_grid_color_center = DrawableColor.of(new Color(232, 142, 64, 130));
        layout_editor_element_border_color_normal = DrawableColor.of(new Color(232, 142, 64));
        layout_editor_element_border_color_selected = DrawableColor.of(new Color(246, 170, 86));
        layout_editor_element_border_rotation_controls_color = DrawableColor.of(new Color(162, 102, 182));
        layout_editor_element_border_vertical_tilting_controls_color = DrawableColor.of(new Color(236, 164, 80));
        layout_editor_element_border_horizontal_tilting_controls_color = DrawableColor.of(new Color(120, 190, 120));
        layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(220, 92, 78, 200));
        layout_editor_element_border_display_line_background_color = DrawableColor.of(new Color(0, 0, 0, 128));
        layout_editor_element_border_display_line_text_color = DrawableColor.of(new Color(242, 229, 208));
        layout_editor_anchor_point_overlay_color_base = DrawableColor.of(new Color(92, 186, 156));
        layout_editor_anchor_point_overlay_color_border = DrawableColor.of(new Color(52, 116, 96));
        menu_bar_close_icon_color = DrawableColor.of(new Color(232, 142, 64));
        pip_docking_overlay_color = DrawableColor.of(new Color(232, 142, 64, 80));
        pip_docking_overlay_border_color = DrawableColor.of(new Color(232, 142, 64, 200));

        scroll_grabber_color_normal = DrawableColor.of(new Color(100, 90, 80, 150));
        scroll_grabber_color_hover = DrawableColor.of(new Color(140, 124, 108, 200));

        actions_entry_background_color_action = DrawableColor.of(new Color(40, 32, 24));
        actions_entry_background_color_action_hover = DrawableColor.of(new Color(54, 42, 30));
        actions_entry_background_color_if = DrawableColor.of(new Color(34, 44, 40));
        actions_entry_background_color_if_hover = DrawableColor.of(new Color(44, 58, 52));
        actions_entry_background_color_else_if = DrawableColor.of(new Color(46, 36, 40));
        actions_entry_background_color_else_if_hover = DrawableColor.of(new Color(60, 46, 52));
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
        actions_entry_background_color_generic_block = DrawableColor.of(new Color(36, 30, 32));
        actions_entry_background_color_generic_block_hover = DrawableColor.of(new Color(48, 40, 44));
        actions_chain_indicator_color = DrawableColor.of(new Color(154, 120, 98, 180));
        actions_chain_indicator_hovered_color = DrawableColor.of(new Color(232, 142, 64, 210));
        actions_chain_indicator_selected_color = DrawableColor.of(new Color(220, 126, 58, 220));
        actions_minimap_background_color = DrawableColor.of(new Color(22, 18, 14, 210));
        actions_minimap_border_color = DrawableColor.of(new Color(98, 86, 76, 220));
        actions_minimap_viewport_color = DrawableColor.of(new Color(255, 255, 255, 32));
        actions_minimap_viewport_border_color = DrawableColor.of(new Color(184, 156, 134, 120));
        actions_minimap_tooltip_border_color = DrawableColor.of(new Color(232, 142, 64, 220));

        bullet_list_dot_color_1 = DrawableColor.of(new Color(232, 142, 64));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(192, 128, 90));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(132, 186, 132));

        input_field_suggestions_background_color = DrawableColor.of(new Color(34, 27, 20));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(242, 229, 208));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(232, 142, 64));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(130, 122, 114));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(170, 160, 150));
        text_editor_text_color = DrawableColor.of(new Color(230, 218, 198));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(232, 112, 84));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(232, 142, 64));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(132, 198, 110));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(96, 190, 170));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(118, 166, 236));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(176, 126, 236));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(214, 126, 182));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(220, 92, 78));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(232, 142, 64));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(242, 188, 98));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(118, 210, 148));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(94, 166, 176));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(232, 142, 64));

    }

}
