package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.Popup;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class EditSingleLayoutSavePopup extends Popup {
	
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
	public void render(MatrixStack matrix, int mouseX, int mouseY, Screen renderIn) {
		super.render(matrix, mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			int i = 0;
			for (String s : this.text) {
				AbstractGui.drawCenteredString(matrix, Minecraft.getInstance().fontRenderer, new StringTextComponent(s), renderIn.width / 2, (renderIn.height / 2) - 50 + i, Color.WHITE.getRGB());
				i += 10;
			}
			
			this.overrideButton.setX((renderIn.width / 2) - (this.overrideButton.getWidth() / 2));
			this.overrideButton.setY(((renderIn.height / 2)));
			
			this.saveAndKeepOthersButton.setX((renderIn.width / 2) - (this.saveAndKeepOthersButton.getWidth() / 2));
			this.saveAndKeepOthersButton.setY(((renderIn.height / 2) + 25));
			
			this.saveAndDisableOthersButton.setX((renderIn.width / 2) - (this.saveAndDisableOthersButton.getWidth() / 2));
			this.saveAndDisableOthersButton.setY(((renderIn.height / 2) + 50));
			
			this.cancelButton.setX((renderIn.width / 2) - (this.cancelButton.getWidth() / 2));
			this.cancelButton.setY(((renderIn.height / 2) + 75));
			
			this.renderButtons(matrix, mouseX, mouseY);
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
		if ((d.keycode == 256) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(3);
			}
		}
	}
}
