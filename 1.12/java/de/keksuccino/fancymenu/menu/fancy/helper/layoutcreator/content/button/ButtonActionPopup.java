package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.util.function.Consumer;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.gui.content.HorizontalSwitcher;
import de.keksuccino.konkrete.gui.screens.popup.Popup;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class ButtonActionPopup extends Popup {
	
	protected Consumer<String> contentCallback;
	protected Consumer<String> typeCallback;
	protected AdvancedTextField textField;
	protected AdvancedButton doneButton;
	protected int width = 250;
	
	protected HorizontalSwitcher actionSwitcher;
	
	public ButtonActionPopup(Consumer<String> contentCallback, Consumer<String> typeCallback, String selectedType) {
		super(240);
		
		this.textField = new AdvancedTextField(Minecraft.getMinecraft().fontRenderer, 0, 0, 200, 20, true, null);
		this.textField.setCanLoseFocus(true);
		this.textField.setFocused(false);
		this.textField.setMaxStringLength(1000);
		
		this.actionSwitcher = new HorizontalSwitcher(120, true,
				"openlink",
				"sendmessage",
				"quitgame",
				"joinserver",
				"loadworld",
				"prevbackground",
				"nextbackground",
				"opencustomgui",
				"opengui",
				"openfile",
				"movefile",
				"copyfile",
				"deletefile",
				"renamefile",
				"downloadfile",
				"unpackzip",
				"reloadmenu",
				"mutebackgroundsounds",
				"runscript");
		this.actionSwitcher.setButtonColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
		this.actionSwitcher.setValueBackgroundColor(new Color(102, 102, 153));
		
		if (selectedType != null) {
			this.actionSwitcher.setSelectedValue(selectedType);
		}
		
		this.doneButton = new AdvancedButton(0, 0, 100, 20, Locals.localize("popup.done"), true, (press) -> {
			this.setDisplayed(false);
			if (this.contentCallback != null) {
				this.contentCallback.accept(this.textField.getText());
			}
			if (this.typeCallback != null) {
				this.typeCallback.accept(this.actionSwitcher.getSelectedValue());
			}
		});
		this.addButton(this.doneButton);

		this.contentCallback = contentCallback;
		this.typeCallback = typeCallback;
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}

	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		super.render(mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			int height = 100;
			
			GlStateManager.enableBlend();
			Gui.drawRect((renderIn.width / 2) - (this.width / 2), (renderIn.height / 2) - (height / 2), (renderIn.width / 2) + (this.width / 2), (renderIn.height / 2) + (height / 2), new Color(0, 0, 0, 0).getRGB());
			GlStateManager.disableBlend();
			
			renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, "§l" + Locals.localize("helper.creator.custombutton.config"), renderIn.width / 2, (renderIn.height / 2) - (height / 2) - 40, Color.WHITE.getRGB());
			
			
			renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("helper.creator.custombutton.config.actiontype"), renderIn.width / 2, (renderIn.height / 2) - 60, Color.WHITE.getRGB());
			
			this.actionSwitcher.render((renderIn.width / 2) - (this.actionSwitcher.getTotalWidth() / 2), (renderIn.height / 2) - 45);
			
			renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("helper.creator.custombutton.config.actiontype." + this.actionSwitcher.getSelectedValue() + ".desc"), renderIn.width / 2, (renderIn.height / 2) - 20, Color.WHITE.getRGB());
			
			
			String s = this.actionSwitcher.getSelectedValue();
			if (s.equals("sendmessage") || s.equals("openlink") || (s.equals("joinserver") || (s.equals("loadworld") || s.equals("openfile") || s.equals("opencustomgui") || s.equals("opengui") || s.equals("movefile") || s.equals("copyfile") || s.equals("deletefile") || s.equals("renamefile") || s.equals("runscript") || s.equals("downloadfile") || s.equals("unpackzip") || s.equals("mutebackgroundsounds")))) {
				renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("helper.creator.custombutton.config.actionvalue", Locals.localize("helper.creator.custombutton.config.actiontype." + this.actionSwitcher.getSelectedValue() + ".desc.value")), renderIn.width / 2, (renderIn.height / 2) + 15, Color.WHITE.getRGB());
				
				this.textField.x = (renderIn.width / 2) - (this.textField.getWidth() / 2);
				this.textField.y = (renderIn.height / 2) + 30;
				this.textField.drawTextBox();
				
				renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("helper.creator.custombutton.config.actionvalue.example", Locals.localize("helper.creator.custombutton.config.actiontype." + this.actionSwitcher.getSelectedValue() + ".desc.value.example")), renderIn.width / 2, (renderIn.height / 2) + 56, Color.WHITE.getRGB());
			}
			
			
			this.doneButton.x = (renderIn.width / 2) - (this.doneButton.width / 2);
			this.doneButton.y = ((renderIn.height / 2) + (height / 2)) + 30;
			
			this.renderButtons(mouseX, mouseY);
		}
	}
	
	public void setText(String text) {
		this.textField.setText("");
		this.textField.writeText(text);
	}
	
	public String getInput() {
		return this.textField.getText();
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 28) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.contentCallback != null) {
				this.contentCallback.accept(this.textField.getText());
			}
			if (this.typeCallback != null) {
				this.typeCallback.accept(this.actionSwitcher.getSelectedValue());
			}
		}
	}

	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 1) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.contentCallback != null) {
				this.contentCallback.accept(null);
			}
			if (this.typeCallback != null) {
				this.typeCallback.accept(null);
			}
		}
	}

}
