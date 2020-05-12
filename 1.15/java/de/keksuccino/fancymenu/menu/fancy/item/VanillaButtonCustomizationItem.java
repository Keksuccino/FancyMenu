package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;
import java.lang.reflect.Field;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.resources.ExternalTextureHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class VanillaButtonCustomizationItem extends CustomizationItemBase {
	
	private Widget parent;
	private AdvancedButton button;
	
	public VanillaButtonCustomizationItem(PropertiesSection item, Widget parent) {
		super(item);
		this.parent = parent;

		String backNormal = item.getEntryValue("backgroundnormal");
		String backHover = item.getEntryValue("backgroundhovered");
		this.button = new AdvancedButton(parent.x, parent.y, parent.getWidth(), parent.getHeight(), parent.getMessage(), true, (press) -> {
			click();
		});
		this.button.setBackgroundTexture(ExternalTextureHandler.getResource(backNormal.replace("\\", "/")), ExternalTextureHandler.getResource(backHover.replace("\\", "/")));
	}

	@Override
	public void render(Screen menu) throws IOException {
		if (button != null) {
			this.button.x = this.parent.x;
			this.button.y = this.parent.y;
			this.button.setWidth(this.parent.getWidth());
			this.button.setHeight(this.parent.getHeight());
			this.button.setMessage(this.parent.getMessage());
			
			if (this.button.isHovered() && !this.parent.isHovered()) {
				this.setHovered(this.parent, true);
			}
			if (!this.button.isHovered() && this.parent.isHovered()) {
				this.setHovered(this.parent, false);
			}
			
			this.button.render(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
		}
	}
	
	private void setHovered(Widget w, boolean hovered) {
		try {
			Field f = ObfuscationReflectionHelper.findField(Widget.class, "isHovered");
			f.set(w, hovered);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void click() {
		this.parent.onClick(MouseInput.getMouseX(), MouseInput.getMouseY());
	}

}
