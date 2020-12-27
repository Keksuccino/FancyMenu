package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.IOException;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Util;

public class ButtonCustomizationItem extends CustomizationItemBase {

	public AdvancedButton button;
	
	public ButtonCustomizationItem(PropertiesSection item) {
		super(item);
		
		if ((this.action != null) && this.action.equalsIgnoreCase("addbutton")) {
			this.value = item.getEntryValue("label");
			if (this.value == null) {
				return;
			}
			String buttonaction = item.getEntryValue("buttonaction");
			String actionvalue = item.getEntryValue("value");
			if ((buttonaction == null) || (actionvalue == null)) {
				return;
			}
			if (buttonaction.equalsIgnoreCase("openlink")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					Util.getOSType().openURI(actionvalue);
				});
			}
			if (buttonaction.equalsIgnoreCase("sendmessage")) {
				this.button = new AdvancedButton(0, 0, this.width, this.height, this.value, true, (press) -> {
					if (Minecraft.getInstance().world != null) {
						Minecraft.getInstance().player.sendChatMessage(actionvalue);
					}
				});
			}
		}
	}

	public void render(GuiScreen menu) throws IOException {
		if (!this.shouldRender()) {
			return;
		}

		int x = this.getPosX(menu);
		int y = this.getPosY(menu);
		
		this.button.x = x;
		this.button.y = y;
		
		this.button.render(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
	}
	
	@Override
	public boolean shouldRender() {
		if (this.button == null) {
			return false;
		}
		return super.shouldRender();
	}

}
