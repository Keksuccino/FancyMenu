package de.keksuccino.fancymenu.menu.button;

import de.keksuccino.input.MouseInput;
import de.keksuccino.math.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public class AdvancedButton extends GuiButton {

	private IPressable press;
	private boolean leftDown = false;
	
	public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, IPressable onPress) {
		super(MathUtils.getRandomNumberInRange(100, 999), x, y, widthIn, heightIn, buttonText);
		this.press = onPress;
	}
	
	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		super.drawButton(mc, mouseX, mouseY, partialTicks);
		
		if (this.isMouseOver() && MouseInput.isLeftMouseDown() && !this.leftDown) {
			this.leftDown = true;
			this.press.onPress(this);
		}
		if (!MouseInput.isLeftMouseDown()) {
			this.leftDown = false;
		}
	}
	
	public interface IPressable {
		void onPress(GuiButton button);
	}

}
