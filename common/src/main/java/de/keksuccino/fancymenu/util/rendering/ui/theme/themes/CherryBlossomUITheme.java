package de.keksuccino.fancymenu.util.rendering.ui.theme.themes;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.theme.UITheme;

import java.awt.Color;

public class CherryBlossomUITheme extends UITheme {

    public CherryBlossomUITheme() {

        super("cherry_blossom", "fancymenu.ui.themes.cherry_blossom");

        allow_blur = true;

        ui_blur_icon_button_hover_color = DrawableColor.of(new Color(57, 50, 61, 13));
        ui_blur_icon_texture_color = DrawableColor.of(new Color(57, 50, 61));
        ui_blur_overlay_background_tint = DrawableColor.of(new Color(250, 244, 248, 185));
        ui_blur_overlay_border_color = DrawableColor.of(new Color(221, 207, 218, 160));
        ui_blur_interface_background_tint = DrawableColor.of(new Color(249, 243, 247, 215));
        ui_blur_interface_border_color = DrawableColor.of(new Color(215, 202, 213, 180));
        ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(240, 232, 238, 215));
        ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 251, 253, 155));
        ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(245, 235, 242, 145));
        ui_blur_interface_area_border_color = DrawableColor.of(new Color(215, 202, 213, 150));
        ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(255, 216, 235, 140));
        ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 251, 253, 155));
        ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(255, 221, 236, 155));
        ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(255, 232, 244, 175));
        ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(255, 214, 232, 175));
        ui_blur_interface_widget_border_color = DrawableColor.of(new Color(211, 199, 210, 150));
        ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(57, 50, 61));
        ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(129, 122, 134));
        ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(255, 251, 253, 150));
        ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(211, 199, 210, 150));
        ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(224, 116, 156, 210));
        ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(57, 50, 61));
        ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(129, 122, 134));
        ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(157, 150, 162));
        ui_blur_interface_generic_text_color = DrawableColor.of(new Color(57, 50, 61));
        ui_blur_tooltip_background_tint = DrawableColor.of(new Color(255, 250, 253, 220));

        ui_icon_button_hover_color = DrawableColor.of(new Color(62, 55, 67, 13));
        ui_icon_texture_color = DrawableColor.of(new Color(62, 55, 67));
        ui_overlay_background_color = DrawableColor.of(new Color(245, 239, 243));
        ui_overlay_border_color = DrawableColor.of(new Color(217, 205, 215));
        ui_interface_background_color = DrawableColor.of(new Color(251, 246, 249));
        ui_interface_border_color = DrawableColor.of(new Color(217, 205, 215));
        ui_interface_title_bar_color = DrawableColor.of(new Color(238, 230, 236));
        ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(255, 252, 254));
        ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(244, 236, 242));
        ui_interface_area_border_color = DrawableColor.of(new Color(217, 205, 215));
        ui_interface_area_entry_selected_color = DrawableColor.of(new Color(255, 216, 235));
        ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(255, 251, 253));
        ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(255, 221, 236));
        ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(255, 232, 244));
        ui_interface_widget_border_color = DrawableColor.of(new Color(208, 197, 208));
        ui_interface_widget_label_color_normal = DrawableColor.of(new Color(52, 46, 58));
        ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(132, 125, 137));
        ui_interface_input_field_background_color = DrawableColor.of(new Color(255, 251, 253));
        ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(210, 199, 210));
        ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(224, 116, 156));
        ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(52, 46, 58));
        ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(148, 142, 154));
        ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(158, 151, 163));
        ui_interface_generic_text_color = DrawableColor.of(new Color(52, 46, 58));
        ui_tooltip_background_color = DrawableColor.of(new Color(247, 240, 245));

        success_color = DrawableColor.of(new Color(34, 158, 110));
        error_color = DrawableColor.of(new Color(219, 63, 96));
        warning_color = DrawableColor.of(new Color(224, 123, 52));

        layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(221, 104, 143));
        layout_editor_grid_color_normal = DrawableColor.of(new Color(216, 120, 166, 95));
        layout_editor_grid_color_center = DrawableColor.of(new Color(188, 89, 136, 115));
        layout_editor_element_border_color_normal = DrawableColor.of(new Color(219, 90, 136));
        layout_editor_element_border_color_selected = DrawableColor.of(new Color(236, 124, 171));
        layout_editor_element_border_rotation_controls_color = DrawableColor.of(new Color(176, 96, 205));
        layout_editor_element_border_vertical_tilting_controls_color = DrawableColor.of(new Color(236, 153, 88));
        layout_editor_element_border_horizontal_tilting_controls_color = DrawableColor.of(new Color(139, 196, 106));
        layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(219, 63, 96, 200));
        layout_editor_element_border_display_line_background_color = DrawableColor.of(new Color(0, 0, 0, 96));
        layout_editor_element_border_display_line_text_color = DrawableColor.of(new Color(255, 255, 255));
        layout_editor_anchor_point_overlay_color_base = DrawableColor.of(new Color(78, 190, 156));
        layout_editor_anchor_point_overlay_color_border = DrawableColor.of(new Color(43, 120, 102));
        menu_bar_close_icon_color = DrawableColor.of(new Color(224, 75, 108));

        scroll_grabber_color_normal = DrawableColor.of(new Color(186, 175, 186, 140));
        scroll_grabber_color_hover = DrawableColor.of(new Color(162, 151, 164, 180));

        actions_entry_background_color_action = DrawableColor.of(new Color(238, 233, 238));
        actions_entry_background_color_action_hover = DrawableColor.of(new Color(226, 219, 227));
        actions_entry_background_color_if = DrawableColor.of(new Color(229, 243, 255));
        actions_entry_background_color_if_hover = DrawableColor.of(new Color(210, 231, 252));
        actions_entry_background_color_else_if = DrawableColor.of(new Color(243, 233, 252));
        actions_entry_background_color_else_if_hover = DrawableColor.of(new Color(232, 218, 247));
        actions_entry_background_color_else = DrawableColor.of(new Color(255, 241, 227));
        actions_entry_background_color_else_hover = DrawableColor.of(new Color(255, 228, 204));
        actions_entry_background_color_while = DrawableColor.of(new Color(228, 246, 241));
        actions_entry_background_color_while_hover = DrawableColor.of(new Color(210, 238, 231));
        actions_entry_background_color_delay = DrawableColor.of(new Color(255, 246, 221));
        actions_entry_background_color_delay_hover = DrawableColor.of(new Color(255, 235, 195));
        actions_entry_background_color_execute_later = DrawableColor.of(new Color(230, 237, 255));
        actions_entry_background_color_execute_later_hover = DrawableColor.of(new Color(212, 223, 252));
        actions_entry_background_color_folder = DrawableColor.of(new Color(248, 230, 238));
        actions_entry_background_color_folder_hover = DrawableColor.of(new Color(241, 212, 223));
        actions_entry_background_color_generic_block = DrawableColor.of(new Color(241, 237, 240));
        actions_entry_background_color_generic_block_hover = DrawableColor.of(new Color(229, 223, 229));
        actions_chain_indicator_color = DrawableColor.of(new Color(173, 125, 158, 150));
        actions_chain_indicator_hovered_color = DrawableColor.of(new Color(219, 90, 136, 205));
        actions_chain_indicator_selected_color = DrawableColor.of(new Color(235, 164, 98, 220));
        actions_minimap_background_color = DrawableColor.of(new Color(250, 244, 248, 225));
        actions_minimap_border_color = DrawableColor.of(new Color(206, 195, 206, 220));
        actions_minimap_viewport_color = DrawableColor.of(new Color(0, 0, 0, 28));
        actions_minimap_viewport_border_color = DrawableColor.of(new Color(120, 100, 130, 120));
        actions_minimap_tooltip_border_color = DrawableColor.of(new Color(192, 110, 148, 220));

        bullet_list_dot_color_1 = DrawableColor.of(new Color(219, 90, 136));
        bullet_list_dot_color_2 = DrawableColor.of(new Color(227, 133, 156));
        bullet_list_dot_color_3 = DrawableColor.of(new Color(236, 163, 92));

        input_field_suggestions_background_color = DrawableColor.of(new Color(250, 244, 248));
        input_field_suggestions_text_color_normal = DrawableColor.of(new Color(52, 46, 58));
        input_field_suggestions_text_color_selected = DrawableColor.of(new Color(198, 76, 122));

        text_editor_line_number_text_color_normal = DrawableColor.of(new Color(155, 147, 159));
        text_editor_line_number_text_color_selected = DrawableColor.of(new Color(121, 112, 126));
        text_editor_text_color = DrawableColor.of(new Color(52, 46, 58));
        text_editor_text_formatting_nested_text_color_1 = DrawableColor.of(new Color(219, 63, 96));
        text_editor_text_formatting_nested_text_color_2 = DrawableColor.of(new Color(224, 123, 52));
        text_editor_text_formatting_nested_text_color_3 = DrawableColor.of(new Color(109, 159, 64));
        text_editor_text_formatting_nested_text_color_4 = DrawableColor.of(new Color(52, 152, 136));
        text_editor_text_formatting_nested_text_color_5 = DrawableColor.of(new Color(57, 118, 207));
        text_editor_text_formatting_nested_text_color_6 = DrawableColor.of(new Color(149, 86, 210));
        text_editor_text_formatting_nested_text_color_7 = DrawableColor.of(new Color(202, 81, 154));
        text_editor_text_formatting_nested_text_color_8 = DrawableColor.of(new Color(210, 74, 92));
        text_editor_text_formatting_nested_text_color_9 = DrawableColor.of(new Color(233, 120, 66));
        text_editor_text_formatting_nested_text_color_10 = DrawableColor.of(new Color(223, 168, 64));
        text_editor_text_formatting_nested_text_color_11 = DrawableColor.of(new Color(62, 170, 124));
        text_editor_text_formatting_nested_text_color_12 = DrawableColor.of(new Color(46, 118, 131));
        text_editor_text_formatting_brackets_color = DrawableColor.of(new Color(236, 163, 92));

    }

}
