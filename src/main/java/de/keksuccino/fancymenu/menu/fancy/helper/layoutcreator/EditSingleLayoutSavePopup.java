package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public class EditSingleLayoutSavePopup extends FMPopup {
	
	private List<String> text;
	private AdvancedButton saveAndDisableOthersButton;
	private AdvancedButton saveAndKeepOthersButton;
	private AdvancedButton overrideButton;
	private AdvancedButton cancelButton;
	private Consumer<Integer> callback;
	
	public EditSingleLayoutSavePopup(Consumer<Integer> callback) {
		super(240);

		this.callback = callback;
		
		this.setNotificationText("§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.savefile"));
		
		this.overrideButton = new AdvancedButton(0, 0, 260, 20, Locals.localize("helper.creator.editlayout.single.override"), true, (press) -> {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(1);
			}
		});
		this.addButton(this.overrideButton);
		
		this.saveAndKeepOthersButton = new AdvancedButton(0, 0, 260, 20, Locals.localize("helper.creator.editlayout.single.asnew.keep"), true, (press) -> {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(2);
			}
		});
		this.addButton(this.saveAndKeepOthersButton);
		
		this.saveAndDisableOthersButton = new AdvancedButton(0, 0, 260, 20, Locals.localize("helper.creator.editlayout.single.asnew.disable"), true, (press) -> {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(3);
			}
		});
		this.addButton(this.saveAndDisableOthersButton);
		
		this.cancelButton = new AdvancedButton(0, 0, 80, 20, Locals.localize("helper.creator.savefile.cancel"), true, (press) -> {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(4);
			}
		});
		this.addButton(this.cancelButton);
		
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}
	
	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		super.render(mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			int i = 0;
			for (String s : this.text) {
				renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, s, renderIn.width / 2, (renderIn.height / 2) - 50 + i, Color.WHITE.getRGB());
				i += 10;
			}
			
			this.overrideButton.x = (renderIn.width / 2) - (this.overrideButton.width / 2);
			this.overrideButton.y = ((renderIn.height / 2));
			
			this.saveAndKeepOthersButton.x = (renderIn.width / 2) - (this.saveAndKeepOthersButton.width / 2);
			this.saveAndKeepOthersButton.y = ((renderIn.height / 2) + 25);
			
			this.saveAndDisableOthersButton.x = (renderIn.width / 2) - (this.saveAndDisableOthersButton.width / 2);
			this.saveAndDisableOthersButton.y = ((renderIn.height / 2) + 50);
			
			this.cancelButton.x = (renderIn.width / 2) - (this.cancelButton.width / 2);
			this.cancelButton.y = ((renderIn.height / 2) + 75);
			
			this.renderButtons(mouseX, mouseY);
		}
	}
	
	private void setNotificationText(String... text) {
		if (text != null) {
			List<String> l = new ArrayList<String>();
			for (String s : text) {
				if (s.contains("%n%")) {
					for (String s2 : s.split("%n%")) {
						l.add(s2);
					}
				} else {
					l.add(s);
				}
			}
			this.text = l;
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 1) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(3);
			}
		}
	}
}
