package de.keksuccino.fancymenu.menu.fancy.guicreator;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class CustomGuiBase extends Screen {

	public final String identifier;
	public boolean closeOnEsc;
	
	protected CustomGuiBase(String title, String identifier, boolean closeOnEsc) {
		super(new StringTextComponent(title));
		this.identifier = identifier;
		this.closeOnEsc = closeOnEsc;
	}
	
	//shouldCloseOnEsc
	@Override
	public boolean func_231178_ax__() {
		return this.closeOnEsc;
	}

}
