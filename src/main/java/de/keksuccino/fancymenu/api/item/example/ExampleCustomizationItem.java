package de.keksuccino.fancymenu.api.item.example;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.api.item.CustomizationItem;
import de.keksuccino.fancymenu.api.item.CustomizationItemContainer;
import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import java.awt.*;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.Mth;

public class ExampleCustomizationItem extends CustomizationItem {

    public String displayText = "placeholder";
    public String backgroundColorString = "#38ff38";
    public Color backgroundColor = new Color(56, 255, 56);

    //The constuctor is used to de-serialize the PropertiesSection and set all of its values to the new real item instance.
    public ExampleCustomizationItem(CustomizationItemContainer parentContainer, PropertiesSection item) {

        //The superclass will automatically get values like orientation, x pos, y pos, width and height and will set it to the real item instance.
        super(parentContainer, item);

        //Getting the background HEX color from the serialized item
        String backColorHex = item.getEntryValue("background_color");
        if (backColorHex != null) {
            Color c = RenderUtils.getColorFromHexString(backColorHex);
            if (c != null) {
                this.backgroundColor = c;
                this.backgroundColorString = backColorHex;
            }
        }

        //Getting the display text string from the serialized item
        String dText = item.getEntryValue("display_text");
        if (dText != null) {
            this.displayText = dText;
        }

    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        //This is really important and should be in every item render method, to check for visibility requirements and more.
        if (this.shouldRender()) {

            //Always use getPosX() and getPosY() to get the X and Y positions of the item.
            //The fields posX and posY aren't the final position, just the base pos without the orientation!
            int x = this.getPosX(menu);
            int y = this.getPosY(menu);

            RenderSystem.enableBlend();

            //Rendering the background color as background of the item.
            fill(matrix, x, y, x + this.getWidth(), y + this.getHeight(), this.backgroundColor.getRGB() | Mth.ceil(this.opacity * 255.0F) << 24);

            //Rendering the display text to the upper-left side of the item
            if (this.displayText != null) {
                //We want to use placeholder text values for the display text, so we use the DynamicValueHelper to convert them,
                //but they should look like placeholders in the editor, so we only convert them when not in the editor.
                String text;
                if (!isEditorActive()) {
                    text = DynamicValueHelper.convertFromRaw(this.displayText);
                } else {
                    text = StringUtils.convertFormatCodes(this.displayText, "&", "§");
                }
                //The 'opacity' field is used to set the fade-in opacity of the item when the "delay appearance" option is enabled for it.
                //Always try to make your items' opacity changeable by setting the 'opacity' field! (I also used it in the fill method for the background)
                drawString(matrix, Minecraft.getInstance().font, new TextComponent(text), x + 10, y + 10, -1 | Mth.ceil(this.opacity * 255.0F) << 24);
            }

        }

    }

}
