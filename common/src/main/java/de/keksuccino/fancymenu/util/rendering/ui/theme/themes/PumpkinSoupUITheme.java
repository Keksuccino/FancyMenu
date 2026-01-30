package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;

import java.awt.Color;

public class PumpkinSoupUITheme extends UITheme {

    public PumpkinSoupUITheme() {

        super("pumpkin_soup", "fancymenu.ui.themes.pumpkin_soup");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(60, 45, 36, 13));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(60, 45, 36));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(250, 244, 236, 185));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(210, 192, 175, 160));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(249, 242, 234, 215));
        ui_blur_interface_border_color = DrawableColor.of(new Color(204, 186, 170, 180));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(238, 227, 214, 215));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 250, 245, 155));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(244, 232, 220, 150));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(204, 186, 170, 150));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(255, 213, 176, 150));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 250, 245, 155));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(255, 204, 158, 155));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(255, 226, 198, 175));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(255, 190, 128, 175));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(200, 184, 170, 150));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(60, 45, 36));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(132, 120, 110));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(255, 250, 245, 150));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(200, 184, 170, 150));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(230, 126, 60, 210));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(60, 45, 36));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(132, 120, 110));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(160, 148, 136));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(60, 45, 36));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(255, 249, 242, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(60, 45, 36, 13));
        ui_icon_texture_color = DrawableColor.of(new Color(60, 45, 36));
        ui_overlay_background_color = DrawableColor.of(new Color(245, 238, 229));
        ui_overlay_border_color = DrawableColor.of(new Color(208, 190, 174));
        ui_interface_background_color = DrawableColor.of(new Color(251, 245, 237));
        ui_interface_border_color = DrawableColor.of(new Color(208, 190, 174));
        ui_interface_title_bar_color = DrawableColor.of(new Color(236, 225, 212));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 251, 246));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(244, 233, 221));
        ui_interface_area_border_color = DrawableColor.of(new Color(208, 190, 174));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(255, 213, 176));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 250, 245));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(255, 204, 158));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(255, 226, 198));
        ui_interface_widget_border_color = DrawableColor.of(new Color(198, 182, 168));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(56, 42, 34));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(136, 124, 112));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(255, 250, 245));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(200, 184, 170));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(230, 126, 60));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(56, 42, 34));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(150, 138, 126));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(162, 150, 138));
        ui_interface_generic_text_color = DrawableColor.of(new Color(56, 42, 34));
        ui_tooltip_background_color = DrawableColor.of(new Color(248, 241, 233));

        success_text_color = DrawableColor.of(new Color(32, 153, 106));
        error_text_color = DrawableColor.of(new Color(218, 78, 58));
        warning_text_color = DrawableColor.of(new Color(230, 126, 60));

        layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(230, 126, 60));
        layout_editor_grid_color_normal = DrawableColor.of(new Color(219, 140, 88, 100));
        layout_editor_grid_color_center = DrawableColor.of(new Color(193, 110, 64, 120));
        layout_editor_element_border_color_normal = DrawableColor.of(new Color(230, 126, 60));
        layout_editor_element_border_color_selected = DrawableColor.of(new Color(244, 164, 96));
        layout_editor_element_border_rotation_controls_color = DrawableColor.of(new Color(170, 104, 54));
        layout_editor_element_border_vertical_tilting_controls_color = DrawableColor.of(new Color(236, 156, 78));
        layout_editor_element_border_horizontal_tilting_controls_color = DrawableColor.of(new Color(120, 170, 110));
        layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(218, 78, 58, 200));
        layout_editor_element_border_display_line_background_color = DrawableColor.of(new Color(0, 0, 0, 96));
        layout_editor_element_border_display_line_text_color = DrawableColor.of(new Color(255, 255, 255));
        layout_editor_anchor_point_overlay_color_base = DrawableColor.of(new Color(92, 176, 140));
        layout_editor_anchor_point_overlay_color_border = DrawableColor.of(new Color(56, 120, 94));
        menu_bar_close_icon_color = DrawableColor.of(new Color(230, 126, 60));

        scroll_grabber_color_normal = DrawableColor.of(new Color(186, 170, 156, 140));
        scroll_grabber_color_hover = DrawableColor.of(new Color(160, 146, 134, 180));

        actions_entry_background_color_action = DrawableColor.of(new Color(239, 232, 224));
        actions_entry_background_color_action_hover = DrawableColor.of(new Color(228, 219, 209));
        actions_entry_background_color_if = DrawableColor.of(new Color(231, 242, 255));
        actions_entry_background_color_if_hover = DrawableColor.of(new Color(214, 232, 250));
        actions_entry_background_color_else_if = DrawableColor.of(new Color(244, 233, 250));
        actions_entry_background_color_else_if_hover = DrawableColor.of(new Color(232, 217, 243));
        actions_entry_background_color_else = DrawableColor.of(new Color(255, 239, 221));
        actions_entry_background_color_else_hover = DrawableColor.of(new Color(255, 226, 198));
        actions_entry_background_color_while = DrawableColor.of(new Color(227, 245, 239));
        actions_entry_background_color_while_hover = DrawableColor.of(new Color(210, 238, 229));
        actions_entry_background_color_delay = DrawableColor.of(new Color(255, 246, 214));
        actions_entry_background_color_delay_hover = DrawableColor.of(new Color(255, 235, 190));
        actions_entry_background_color_execute_later = DrawableColor.of(new Color(230, 238, 255));
        actions_entry_background_color_execute_later_hover = DrawableColor.of(new Color(210, 224, 255));
        actions_entry_background_color_folder = DrawableColor.of(new Color(250, 229, 220));
        actions_entry_background_color_folder_hover = DrawableColor.of(new Color(243, 212, 200));
        actions_entry_background_color_generic_block = DrawableColor.of(new Color(242, 236, 230));
        actions_entry_background_color_generic_block_hover = DrawableColor.of(new Color(230, 222, 214));
        actions_chain_indicator_color = DrawableColor.of(new Color(174, 132, 106, 160));
        actions_chain_indicator_hovered_color = DrawableColor.of(new Color(230, 126, 60, 210));
        actions_chain_indicator_selected_color = DrawableColor.of(new Color(236, 156, 78, 220));
        actions_minimap_background_color = DrawableColor.of(new Color(250, 244, 236, 225));
        actions_minimap_border_color = DrawableColor.of(new Color(198, 184, 170, 220));
        actions_minimap_viewport_color = DrawableColor.of(new Color(0, 0, 0, 28));
        actions_minimap_viewport_border_color = DrawableColor.of(new Color(152, 126, 110, 120));
        actions_minimap_tooltip_border_color = DrawableColor.of(new Color(230, 126, 60, 220));

        bullet_list_dot_color_1 = DrawableColor.of(new Color(230, 126, 60));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(192, 118, 82));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(150, 170, 114));

        input_field_suggestions_background_color = DrawableColor.of(new Color(250, 244, 236));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(56, 42, 34));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(208, 104, 46));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(156, 145, 134));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(120, 110, 100));
        text_editor_text_color = DrawableColor.of(new Color(56, 42, 34));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(218, 78, 58));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(230, 126, 60));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(118, 166, 92));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(54, 156, 134));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(74, 132, 210));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(160, 102, 210));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(202, 92, 150));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(218, 78, 58));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(230, 126, 60));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(214, 172, 86));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(88, 170, 118));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(62, 130, 140));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(230, 126, 60));

    }

}
