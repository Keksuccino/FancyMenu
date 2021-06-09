package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

public class MoreRefinedStorageMainHandler extends MainMenuHandler {
	
	@Override
	public String getMenuIdentifier() {
		//PLEASE don't do stuff like overriding a whole screen just to alter the splash text, thanks.
		return "be.nevoka.morerefinedstorage.gui.GuiCustomMainMenu";
	}

}
