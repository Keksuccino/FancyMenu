package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.resources.TextureHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class VanillaButtonCustomizationItem extends CustomizationItemBase {
	
	private GuiButton parent;
	private AdvancedButton button;
	
	public VanillaButtonCustomizationItem(PropertiesSection item, GuiButton parent) {
		super(item);
		this.parent = parent;

		String backNormal = item.getEntryValue("backgroundnormal");
		String backHover = item.getEntryValue("backgroundhovered");
		this.button = new AdvancedButton(parent.x, parent.y, parent.width, parent.height, parent.displayString, true, (press) -> {
			click();
		});
		this.button.setBackgroundTexture(TextureHandler.getResource(backNormal.replace("\\", "/")), TextureHandler.getResource(backHover.replace("\\", "/")));
	}

	@Override
	public void render(GuiScreen menu) throws IOException {
		if (button != null) {
			this.button.x = this.parent.x;
			this.button.y = this.parent.y;
			this.button.width = this.parent.width;
			this.button.height = this.parent.height;
			this.button.displayString = this.parent.displayString;
			
			if (this.button.isMouseOver() && !this.parent.isMouseOver()) {
				this.setHovered(this.parent, true);
			}
			if (!this.button.isMouseOver() && this.parent.isMouseOver()) {
				this.setHovered(this.parent, false);
			}
			
			this.button.drawButton(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
		}
	}
	
	private void setHovered(GuiButton w, boolean hovered) {
		try {
			Field f = ReflectionHelper.findField(GuiButton.class, "field_146123_n", "hovered");
			f.set(w, hovered);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void click() {
		this.parent.visible = true;
		this.parent.mousePressed(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY());
		this.parent.visible = false;
		
		try {
			Method m = ReflectionHelper.findMethod(GuiScreen.class, "actionPerformed", "func_146284_a", GuiButton.class);
			m.invoke(Minecraft.getMinecraft().currentScreen, this.parent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
