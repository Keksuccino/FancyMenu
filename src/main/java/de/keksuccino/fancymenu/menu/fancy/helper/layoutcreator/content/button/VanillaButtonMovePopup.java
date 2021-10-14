package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMNotificationPopup;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.gui.GuiScreen;

public class VanillaButtonMovePopup extends FMNotificationPopup {
	
	public VanillaButtonMovePopup(LayoutEditorScreen editor) {
		super(300, new Color(0, 0, 0, 0), 240, null, "§c§l" + Locals.localize("helper.creator.items.vanillabutton.noorientation.title"), "", Locals.localize("helper.creator.items.vanillabutton.noorientation.desc"), "", "", "");
	}
	
	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		super.render(mouseX, mouseY, renderIn);
	}

}
