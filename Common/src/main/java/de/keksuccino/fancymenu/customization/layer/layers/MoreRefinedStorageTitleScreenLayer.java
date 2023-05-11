package de.keksuccino.fancymenu.customization.layer.layers;

import org.jetbrains.annotations.NotNull;

public class MoreRefinedStorageTitleScreenLayer extends TitleScreenLayer {
	
	@Override
	public @NotNull String getIdentifier() {
		//PLEASE don't do stuff like overriding a whole screen just to alter the splash text, thanks.
		return "be.nevoka.morerefinedstorage.gui.GuiCustomMainMenu";
	}

}
