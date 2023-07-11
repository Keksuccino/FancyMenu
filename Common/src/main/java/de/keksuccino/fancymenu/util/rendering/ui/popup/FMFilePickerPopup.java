package de.keksuccino.fancymenu.util.rendering.ui.popup;

import java.io.File;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.FilePickerPopup;
import de.keksuccino.konkrete.gui.screens.popup.Popup;
import net.minecraft.client.gui.screens.Screen;

//TODO remove this class
@Deprecated
public class FMFilePickerPopup extends FilePickerPopup {

	public FMFilePickerPopup(String directory, String home, Popup fallback, boolean checkForLastPath, Consumer<File> callback, String[] filetypes) {
		super(directory, home, fallback, checkForLastPath, callback, filetypes);
	}
	
	public FMFilePickerPopup(String directory, String home, Popup fallback, boolean checkForLastPath, Consumer<File> callback) {
		super(directory, home, fallback, checkForLastPath, callback);
	}
	
	@Override
	public void render(PoseStack matrix, int mouseX, int mouseY, Screen renderIn) {
		super.render(matrix, mouseX, mouseY, renderIn);
	}
	
	@Override
	protected void colorizePopupButton(AdvancedButton b) {
		UIBase.applyDefaultWidgetSkinTo(b);
	}

}
