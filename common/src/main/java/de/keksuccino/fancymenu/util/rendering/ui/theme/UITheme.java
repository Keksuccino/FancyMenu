package de.keksuccino.fancymenu.util.rendering.ui.theme;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.awt.*;

public class UITheme {

    protected String identifier;
    protected String display_name;

    //----------------------------

    public boolean allow_blur = false;
    public boolean allow_animations = true;
    public float interface_corner_rounding_radius = 6.0f;
    public float widget_corner_rounding_radius = 6.0f; // old was 4.0f

    public DrawableColor ui_blur_icon_button_hover_color = DrawableColor.of(new Color(255, 255, 255, 13));
    public DrawableColor ui_blur_icon_texture_color = DrawableColor.of(new Color(255, 255, 255));
    public DrawableColor ui_blur_overlay_background_tint = DrawableColor.of(new Color(38, 38, 38, 174));
    public DrawableColor ui_blur_overlay_border_color = DrawableColor.of(new Color(93, 97, 100, 100));
    public DrawableColor ui_blur_interface_background_tint = DrawableColor.of(new Color(38, 38, 38, 216));
    public DrawableColor ui_blur_interface_border_color = DrawableColor.of(new Color(93, 97, 100, 200));
    public DrawableColor ui_blur_interface_title_bar_tint = DrawableColor.of(new Color(79, 79, 79, 174));
    public DrawableColor ui_blur_interface_area_background_color_type_1 = DrawableColor.of(new Color(43, 43, 43, 100));
    public DrawableColor ui_blur_interface_area_background_color_type_2 = DrawableColor.of(new Color(5, 5, 5, 73));
    public DrawableColor ui_blur_interface_area_border_color = DrawableColor.of(new Color(93, 97, 100, 100));
    public DrawableColor ui_blur_interface_area_entry_selected_color = DrawableColor.of(new Color(125, 125, 131, 53));
    public DrawableColor ui_blur_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(71, 71, 71, 102));
    public DrawableColor ui_blur_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(126, 126, 126, 102));
    public DrawableColor ui_blur_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(83, 156, 212, 77));
    public DrawableColor ui_blur_interface_widget_background_color_hover_type_2 = DrawableColor.of(new Color(134, 198, 248, 77));
    public DrawableColor ui_blur_interface_widget_border_color = DrawableColor.of(new Color(93, 97, 100, 100));
    public DrawableColor ui_blur_interface_widget_label_color_normal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor ui_blur_interface_widget_label_color_inactive = DrawableColor.of(new Color(113, 117, 119));
    public DrawableColor ui_blur_interface_input_field_background_color = DrawableColor.of(new Color(43, 43, 43, 102));
    public DrawableColor ui_blur_interface_input_field_border_color_normal = DrawableColor.of(new Color(93, 97, 100, 102));
    public DrawableColor ui_blur_interface_input_field_border_color_focused = DrawableColor.of(new Color(93, 97, 100, 102));
    public DrawableColor ui_blur_interface_input_field_text_color_normal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor ui_blur_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(113, 117, 119));
    public DrawableColor ui_blur_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(128, 128, 128));
    public DrawableColor ui_blur_interface_generic_text_color = DrawableColor.of(new Color(255, 255, 255));
    public DrawableColor ui_blur_tooltip_background_tint = DrawableColor.of(new Color(19, 19, 19, 169));

    public DrawableColor ui_icon_button_hover_color = DrawableColor.of(new Color(255, 255, 255, 13));
    public DrawableColor ui_icon_texture_color = DrawableColor.of(new Color(255, 255, 255));
    public DrawableColor ui_overlay_background_color = DrawableColor.of(new Color(40, 40, 40));
    public DrawableColor ui_overlay_border_color = DrawableColor.of(new Color(62, 64, 66));
    public DrawableColor ui_interface_background_color = DrawableColor.of(new Color(38, 38, 38));
    public DrawableColor ui_interface_border_color = DrawableColor.of(new Color(59, 59, 59));
    public DrawableColor ui_interface_title_bar_color = DrawableColor.of(new Color(87, 87, 87));
    public DrawableColor ui_interface_area_background_color_type_1 = DrawableColor.of(new Color(43, 43, 43));
    public DrawableColor ui_interface_area_background_color_type_2 = DrawableColor.of(new Color(38, 38, 38));
    public DrawableColor ui_interface_area_border_color = DrawableColor.of(new Color(59, 59, 59));
    public DrawableColor ui_interface_area_entry_selected_color = DrawableColor.of(new Color(50, 50, 50));
    public DrawableColor ui_interface_widget_background_color_normal_type_1 = DrawableColor.of(new Color(40, 40, 40));
    public DrawableColor ui_interface_widget_background_color_normal_type_2 = DrawableColor.of(new Color(52, 162, 245));
    public DrawableColor ui_interface_widget_background_color_hover_type_1 = DrawableColor.of(new Color(20, 127, 208));
    public DrawableColor ui_interface_widget_border_color = DrawableColor.of(new Color(59, 59, 59));
    public DrawableColor ui_interface_widget_label_color_normal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor ui_interface_widget_label_color_inactive = DrawableColor.of(new Color(113, 117, 119));
    public DrawableColor ui_interface_input_field_background_color = DrawableColor.of(new Color(43, 43, 43));
    public DrawableColor ui_interface_input_field_border_color_normal = DrawableColor.of(new Color(59, 59, 59));
    public DrawableColor ui_interface_input_field_border_color_focused = DrawableColor.of(new Color(59, 59, 59));
    public DrawableColor ui_interface_input_field_text_color_normal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor ui_interface_input_field_text_color_uneditable = DrawableColor.of(new Color(113, 117, 119));
    public DrawableColor ui_interface_input_field_suggestion_text_color = DrawableColor.of(new Color(-8355712));
    public DrawableColor ui_interface_generic_text_color = DrawableColor.of(new Color(255, 255, 255));
    public DrawableColor ui_tooltip_background_color = DrawableColor.of(new Color(43, 43, 43));

    public DrawableColor info_color = DrawableColor.of(new Color(3, 129, 255));
    public DrawableColor success_color = DrawableColor.of(new Color(49, 206, 5));
    public DrawableColor error_color = DrawableColor.of(new Color(237, 69, 69));
    public DrawableColor warning_color = DrawableColor.of(new Color(229, 155, 18));

    public DrawableColor layout_editor_mouse_selection_rectangle_color = DrawableColor.of(new Color(3, 148, 252));
    public DrawableColor layout_editor_grid_color_normal = DrawableColor.of(new Color(186, 121, 241, 100));
    public DrawableColor layout_editor_grid_color_center = DrawableColor.of(new Color(91, 94, 255, 100));
    public DrawableColor layout_editor_element_border_color_normal = DrawableColor.of(new Color(3, 148, 252));
    public DrawableColor layout_editor_element_border_color_selected = DrawableColor.of(new Color(3, 219, 252));
    public DrawableColor layout_editor_element_border_rotation_controls_color = DrawableColor.of(new Color(158, 43, 255));
    public DrawableColor layout_editor_element_border_vertical_tilting_controls_color = DrawableColor.of(new Color(255, 181, 43));
    public DrawableColor layout_editor_element_border_horizontal_tilting_controls_color = DrawableColor.of(new Color(145, 255, 43));
    public DrawableColor layout_editor_element_dragging_not_allowed_color = DrawableColor.of(new Color(232, 54, 9, 200));
    public DrawableColor layout_editor_element_border_display_line_background_color = DrawableColor.of(new Color(0, 0, 0, 128));
    public DrawableColor layout_editor_element_border_display_line_text_color = DrawableColor.of(-1);
    public DrawableColor layout_editor_anchor_point_overlay_color_base = DrawableColor.of(new Color(37, 180, 121));
    public DrawableColor layout_editor_anchor_point_overlay_color_border = DrawableColor.of(new Color(17, 79, 52));
    public DrawableColor menu_bar_close_icon_color = DrawableColor.of(new Color(218, 60, 30));

    public DrawableColor scroll_grabber_color_normal = DrawableColor.of(new Color(89, 91, 93, 100));
    public DrawableColor scroll_grabber_color_hover = DrawableColor.of(new Color(102, 104, 104, 100));

    public DrawableColor actions_entry_background_color_action = DrawableColor.of(new Color(58, 63, 68));
    public DrawableColor actions_entry_background_color_action_hover = DrawableColor.of(new Color(68, 73, 78));
    public DrawableColor actions_entry_background_color_if = DrawableColor.of(new Color(38, 63, 85));
    public DrawableColor actions_entry_background_color_if_hover = DrawableColor.of(new Color(45, 81, 110));
    public DrawableColor actions_entry_background_color_else_if = DrawableColor.of(new Color(57, 45, 79));
    public DrawableColor actions_entry_background_color_else_if_hover = DrawableColor.of(new Color(70, 58, 95));
    public DrawableColor actions_entry_background_color_else = DrawableColor.of(new Color(78, 55, 33));
    public DrawableColor actions_entry_background_color_else_hover = DrawableColor.of(new Color(95, 70, 45));
    public DrawableColor actions_entry_background_color_while = DrawableColor.of(new Color(35, 74, 66));
    public DrawableColor actions_entry_background_color_while_hover = DrawableColor.of(new Color(45, 92, 82));
    public DrawableColor actions_entry_background_color_delay = DrawableColor.of(new Color(82, 76, 36));
    public DrawableColor actions_entry_background_color_delay_hover = DrawableColor.of(new Color(99, 92, 48));
    public DrawableColor actions_entry_background_color_execute_later = DrawableColor.of(new Color(52, 70, 96));
    public DrawableColor actions_entry_background_color_execute_later_hover = DrawableColor.of(new Color(63, 84, 112));
    public DrawableColor actions_entry_background_color_folder = DrawableColor.of(new Color(80, 48, 60));
    public DrawableColor actions_entry_background_color_folder_hover = DrawableColor.of(new Color(97, 62, 77));
    public DrawableColor actions_entry_background_color_generic_block = DrawableColor.of(new Color(58, 59, 62));
    public DrawableColor actions_entry_background_color_generic_block_hover = DrawableColor.of(new Color(71, 72, 76));
    public DrawableColor actions_chain_indicator_color = DrawableColor.of(new Color(88, 112, 150, 180));
    public DrawableColor actions_chain_indicator_hovered_color = DrawableColor.of(new Color(100, 181, 246, 210));
    public DrawableColor actions_chain_indicator_selected_color = DrawableColor.of(new Color(255, 193, 71, 220));
    public DrawableColor actions_minimap_background_color = DrawableColor.of(new Color(24, 27, 32, 200));
    public DrawableColor actions_minimap_border_color = DrawableColor.of(new Color(94, 99, 108, 220));
    public DrawableColor actions_minimap_viewport_color = DrawableColor.of(new Color(255, 255, 255, 35));
    public DrawableColor actions_minimap_viewport_border_color = DrawableColor.of(new Color(210, 222, 255, 100));
    public DrawableColor actions_minimap_tooltip_border_color = DrawableColor.of(new Color(120, 170, 220, 220));

    public DrawableColor bullet_list_dot_color_1 = DrawableColor.of(new Color(62, 134, 160));
    public DrawableColor bullet_list_dot_color_2 = DrawableColor.of(new Color(173, 108, 121));
    public DrawableColor bullet_list_dot_color_3 = DrawableColor.of(new Color(170, 130, 63));

    public DrawableColor input_field_suggestions_background_color = DrawableColor.of(new Color(71, 71, 71));
    public DrawableColor input_field_suggestions_text_color_normal = DrawableColor.of(new Color(206, 221, 237));
    public DrawableColor input_field_suggestions_text_color_selected = DrawableColor.of(new Color(100, 165, 236));

    public DrawableColor text_editor_line_number_text_color_normal = DrawableColor.of(new Color(91, 92, 94));
    public DrawableColor text_editor_line_number_text_color_selected = DrawableColor.of(new Color(137, 147, 150));
    public DrawableColor text_editor_text_color = DrawableColor.of(new Color(158, 170, 184));
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

    protected UITheme() {
    }

    public UITheme(@NotNull String identifier, @NotNull String display_name) {
        this.identifier = identifier;
        this.display_name = display_name;
    }

    public void setUITextureShaderColor(GuiGraphics graphics, float alpha) {
        boolean blur = UIBase.shouldBlur();
        UIBase.setShaderColor(graphics, blur ? ui_blur_icon_texture_color : ui_icon_texture_color, alpha);
    }

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @NotNull
    public Component getDisplayName() {
        if (this.display_name.startsWith("fancymenu.ui.themes.")) return Component.translatable(this.display_name);
        return Component.literal(this.display_name);
    }
    
}

