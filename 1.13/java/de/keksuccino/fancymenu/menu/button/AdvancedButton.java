package de.keksuccino.fancymenu.menu.button;

import de.keksuccino.math.MathUtils;
import net.minecraft.client.gui.GuiButton;

public class AdvancedButton extends GuiButton {

	private IPressable press;
	
	public AdvancedButton(int x, int y, int widthIn, int heightIn, String buttonText, IPressable onPress) {
		super(MathUtils.getRandomNumberInRange(100, 999), x, y, widthIn, heightIn, buttonText);
		this.press = onPress;
	}
	
	@Override
	public void onClick(double mouseX, double mouseY) {
		super.onClick(mouseX, mouseY);
		this.press.onPress(this);
	}
	
	public interface IPressable {
		void onPress(GuiButton button);
	}

}
