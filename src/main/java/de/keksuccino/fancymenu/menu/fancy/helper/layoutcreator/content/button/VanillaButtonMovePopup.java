package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;



import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMNotificationPopup;
import de.keksuccino.konkrete.config.exceptions.InvalidValueException;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

//TODO übernehmen (pop type)
public class VanillaButtonMovePopup extends FMNotificationPopup {

	protected AdvancedButton dontShowAgainBtn;
	
	public VanillaButtonMovePopup(LayoutEditorScreen editor) {
		super(300, new Color(0, 0, 0, 0), 240, null, "§c§l" + Locals.localize("helper.creator.items.vanillabutton.noorientation.title"), "", Locals.localize("helper.creator.items.vanillabutton.noorientation.desc"), "", "", "");
		
		this.dontShowAgainBtn = new AdvancedButton(0, 0, 150, 20, Locals.localize("helper.creator.vanillabutton.move.notification.dontshowagain"), true, (press) -> {
			try {
				FancyMenu.config.setValue("showvanillamovewarning", false);
				setDisplayed(false);
			} catch (InvalidValueException e) {
				e.printStackTrace();
			}
		});
		this.dontShowAgainBtn.ignoreBlockedInput = true;
		this.colorizePopupButton(this.dontShowAgainBtn);
	}
	
	@Override
	public void render(int mouseX, int mouseY, Screen renderIn) {
		super.render(mouseX, mouseY, renderIn);
		
		this.dontShowAgainBtn.x = (renderIn.width / 2) - (this.dontShowAgainBtn.getWidth() / 2);
		this.dontShowAgainBtn.y = this.accept.y + 20 + 15;
		this.dontShowAgainBtn.render(mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
	}

}
