"""Writes the configs of the Elements page images: one screenshot scene per visible element type (scenes.json), and the GIF of
adding a Button through New Element, moving it, widening it and giving it a label (add_element.json).

The Player Entity has no scene: it needs the Fancy Entity Renderer mod, which the test client doesn't have. Slideshow, Video and
Browser have none either, because they need a slideshow folder, Watermedia or Rinku. The Scrollable Area uses the image of its own page.

Usage: python3 gen_configs.py [output folder, default: this folder]
"""
import json, os, sys

BG = "/config/fancymenu/assets/docs_bg.png"
OFF = [-40, -40]


def text(x, y, w, h, content, **extra):
    return {"x": x, "y": y, "w": w, "h": h, "look": "pixel", "background": False, "content": content, **extra}


def scene(name, elements, **extra):
    return {"name": name, "wait": 4, "pointer": OFF, "elements": elements, **extra}


SHADER = """void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    vec3 col = 0.5 + 0.5 * cos(iTime + uv.xyx * 3.0 + vec3(0.0, 2.0, 4.0));
    fragColor = vec4(col, 1.0);
}"""

scenes = [
    scene("elements_button", [
        {"type": "custom_button", "x": 405, "y": 250, "w": 150, "h": 20, "props": {"label": "Join Server"}},
        {"type": "custom_button", "x": 405, "y": 274, "w": 150, "h": 20, "props": {"label": "Open Wiki"}},
    ]),
    scene("elements_slider", [
        {"type": "slider_v2", "x": 395, "y": 250, "w": 170, "h": 20,
         "props": {"slider_type": "integer_range", "min_range_value": "0", "max_range_value": "100", "pre_selected_value": "70",
                   "slider_label": "Music Volume: $$value%"}},
        {"type": "slider_v2", "x": 395, "y": 274, "w": 170, "h": 20,
         "props": {"slider_type": "list", "slider_list_value_0": "Peaceful", "slider_list_value_1": "Easy", "slider_list_value_2": "Normal",
                   "slider_list_value_3": "Hard", "pre_selected_value": "Normal", "slider_label": "Difficulty: $$value"}},
    ]),
    scene("elements_checkbox", [
        {"type": "checkbox", "x": 380, "y": 260, "w": 20, "h": 20, "props": {"variable_mode": "true", "linked_variable": "show_tips"}},
        text(406, 265, 220, 20, "Show tips on the title screen"),
    ], variables={"show_tips": "true"}),
    scene("elements_input_field", [
        {"type": "fancymenu_customization_item_input_field", "x": 390, "y": 250, "w": 180, "h": 20, "props": {"linked_variable": "server_address"}},
        {"type": "fancymenu_customization_item_input_field", "x": 390, "y": 276, "w": 180, "h": 20, "props": {"hint_text": "Your player name"}},
    ], variables={"server_address": "play.example.net"}),
    scene("elements_text", [
        {"x": 345, "y": 230, "w": 270, "h": 72, "look": "pixel", "background": True,
         "content": "### Patch Notes 1.4\n- Added **3 dungeons** to the Nether\n- Fixed the quest book crash\n- Faster world loading"},
    ]),
    scene("elements_tooltip", [
        {"type": "custom_button", "x": 300, "y": 260, "w": 100, "h": 20, "props": {"label": "Need Help?"}},
        {"type": "tooltip", "x": 408, "y": 252, "w": 200, "h": 36,
         "props": {"source_mode": "direct", "source": "Join our **Discord** for help%n%and weekly events!"}},
    ]),
    scene("elements_splash_text", [
        {"type": "splash_text", "x": 430, "y": 260, "w": 100, "h": 20,
         "props": {"source_mode": "direct", "source": "Season 3 is live!", "bouncing": "false"}},
    ]),
    scene("elements_image", [
        {"type": "image", "x": 352, "y": 238, "w": 256, "h": 64, "props": {"source": "[source:location]minecraft:textures/gui/title/minecraft.png"}},
    ]),
    scene("elements_glsl_shader", [
        {"type": "glsl_shader", "x": 360, "y": 202, "w": 240, "h": 135, "props": {"inline_shader_source": " ".join(SHADER.split())}},
    ]),
    scene("elements_rectangle_shape", [
        {"type": "shape", "x": 370, "y": 225, "w": 220, "h": 82,
         "props": {"color": "#101828C8", "blur_enabled": "true", "corner_radius_top_left": "6", "corner_radius_top_right": "6",
                   "corner_radius_bottom_right": "6", "corner_radius_bottom_left": "6"}},
        text(380, 233, 200, 70, "### Server Rules\n1. Be kind to others\n2. No griefing\n3. Have fun!"),
    ]),
    scene("elements_circle_shape", [
        {"type": "shape_circle", "x": 400, "y": 256, "w": 10, "h": 10, "props": {"color": "#55FF55"}},
        text(416, 255, 150, 20, "Survival: online"),
        {"type": "shape_circle", "x": 400, "y": 276, "w": 10, "h": 10, "props": {"color": "#FF5555"}},
        text(416, 275, 150, 20, "Creative: offline"),
    ]),
    scene("elements_item", [
        {"type": "item", "x": 380, "y": 254, "w": 32, "h": 32, "props": {"item_key": "minecraft:diamond_sword", "enchanted": "true"}},
        {"type": "item", "x": 430, "y": 254, "w": 32, "h": 32, "props": {"item_key": "minecraft:apple", "item_count": "16"}},
        {"type": "item", "x": 480, "y": 254, "w": 32, "h": 32, "props": {"item_key": "minecraft:golden_apple"}},
        {"type": "item", "x": 530, "y": 254, "w": 32, "h": 32, "props": {"item_key": "minecraft:totem_of_undying"}},
    ]),
    scene("elements_json_model", [
        {"type": "json_model", "x": 430, "y": 220, "w": 100, "h": 100,
         "props": {"model_source": "[source:location]minecraft:models/block/crafting_table.json"}},
    ]),
    scene("elements_progress_bar", [
        text(380, 244, 200, 20, "^^^\nLoading world... 65%\n^^^"),
        {"type": "progress_bar", "x": 380, "y": 264, "w": 200, "h": 10, "props": {"progress_source": "65", "smooth_filling_animation": "false"}},
    ]),
]

add_element = {
    "script": "add_element",
    "background_image": BG,
    "elements": [text(120, 48, 400, 44, "^^^\n## Skyblock Odyssey\n^^^", scale=2)],
    "right_click": [190, 190],
    "element_type": "custom_button",
    "drag_to": [295, 150],
    "widen_by": 50,
    "label_entry": "edit_label",
    "label": "Join Server",
    "free_spot": [560, 300],
}

out = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
json.dump({"gui_width": 960, "gui_height": 540, "gui_scale": 2, "scenes": scenes}, open(os.path.join(out, "scenes.json"), "w"), indent=1)
json.dump(add_element, open(os.path.join(out, "add_element.json"), "w"), indent=1)
print("wrote", out)
