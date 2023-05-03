package de.keksuccino.fancymenu.customization.backend.layer.layers;

public class MoreRefinedStorageTitleScreenLayer extends TitleScreenLayer {
	
	@Override
	public String getIdentifier() {
		//PLEASE don't do stuff like overriding a whole screen just to alter the splash text, thanks.
		return "be.nevoka.morerefinedstorage.gui.GuiCustomMainMenu";
	}

}
