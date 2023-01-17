package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

public class CreateCustomGuiPopup extends FMPopup {

	private AdvancedButton cancelButton;
	private AdvancedButton doneButton;
	
	private AdvancedTextField identifierText;
	private AdvancedTextField titleText;
	
	private AdvancedButton allowEscButton;
	private AdvancedButton doNotAllowEscButton;
	
	private boolean allowEsc = true;
	
	public CreateCustomGuiPopup() {
		super(240);
		
		this.cancelButton = new AdvancedButton(0, 0, 80, 20, Locals.localize("popup.yesno.cancel"), true, (press) -> {
			this.setDisplayed(false);
		});
		this.addButton(cancelButton);
		
		this.doneButton = new AdvancedButton(0, 0, 80, 20, Locals.localize("helper.buttons.tools.creategui.create"), true, (press) -> {
			this.onDoneButtonPressed();
		});
		this.addButton(doneButton);
		
		this.identifierText = new AdvancedTextField(Minecraft.getMinecraft().fontRenderer, 0, 0, 200, 20, true, null);
		
		this.titleText = new AdvancedTextField(Minecraft.getMinecraft().fontRenderer, 0, 0, 200, 20, true, null);
		
		this.allowEscButton = new AdvancedButton(0, 0, 120, 20, "§a" + Locals.localize("helper.buttons.tools.creategui.allowesc"), true, (press) -> {
			press.displayString = "§a" + Locals.localize("helper.buttons.tools.creategui.allowesc");
			this.doNotAllowEscButton.displayString = Locals.localize("helper.buttons.tools.creategui.donotallowesc");
			this.allowEsc = true;
		});
		this.addButton(allowEscButton);
		
		this.doNotAllowEscButton = new AdvancedButton(0, 0, 120, 20, Locals.localize("helper.buttons.tools.creategui.donotallowesc"), true, (press) -> {
			press.displayString = "§a" + Locals.localize("helper.buttons.tools.creategui.donotallowesc");
			this.allowEscButton.displayString = Locals.localize("helper.buttons.tools.creategui.allowesc");
			this.allowEsc = false;
		});
		this.addButton(doNotAllowEscButton);
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}
	
	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		super.render(mouseX, mouseY, renderIn);

		String id = this.identifierText.getText();
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;	
		
		renderIn.drawCenteredString(font, "§l" + Locals.localize("helper.buttons.tools.creategui"), renderIn.width / 2, (renderIn.height / 2) - 110, Color.WHITE.getRGB());
		
		renderIn.drawCenteredString(font, Locals.localize("helper.buttons.tools.creategui.menuidentifier"), renderIn.width / 2, (renderIn.height / 2) - 80, Color.WHITE.getRGB());
		this.identifierText.x = (renderIn.width / 2) - (this.identifierText.getWidth() / 2);
		this.identifierText.y = (renderIn.height / 2) - 65;
		this.identifierText.drawTextBox();
		
		renderIn.drawCenteredString(font, Locals.localize("helper.buttons.tools.creategui.menutitle"), renderIn.width / 2, (renderIn.height / 2) - 37, Color.WHITE.getRGB());
		this.titleText.x = (renderIn.width / 2) - (this.titleText.getWidth() / 2);
		this.titleText.y = (renderIn.height / 2) - 22;
		this.titleText.drawTextBox();
		
		
		renderIn.drawCenteredString(font, Locals.localize("helper.buttons.tools.creategui.allowescdesc"), renderIn.width / 2, (renderIn.height / 2) + 5, Color.WHITE.getRGB());
		this.allowEscButton.x = (renderIn.width / 2) - this.allowEscButton.width - 5;
		this.allowEscButton.y = (renderIn.height / 2) + 20;
		
		this.doNotAllowEscButton.x = (renderIn.width / 2) + 5;
		this.doNotAllowEscButton.y = (renderIn.height / 2) + 20;
		
		
		this.cancelButton.x = (renderIn.width / 2) - (this.cancelButton.width / 2);
		this.cancelButton.y = (renderIn.height / 2) + 55;
		
		if ((id != null) && (id.length() > 0) && !id.contains(" ")) {
			List<String> l = CustomGuiLoader.getCustomGuis();
			if (!l.contains(id)) {
				this.doneButton.x = (renderIn.width / 2) - (this.doneButton.width / 2);
				this.doneButton.y = (renderIn.height / 2) + 80;
				this.doneButton.visible = true;
			} else {
				this.doneButton.visible = false;
				renderIn.drawCenteredString(font, Locals.localize("helper.buttons.tools.creategui.identifieralreadyused"), renderIn.width / 2, (renderIn.height / 2) + 85, Color.WHITE.getRGB());
			}
		} else {
			this.doneButton.visible = false;
			renderIn.drawCenteredString(font, Locals.localize("helper.buttons.tools.creategui.invalididentifier"), renderIn.width / 2, (renderIn.height / 2) + 85, Color.WHITE.getRGB());
		}
		
		this.renderButtons(mouseX, mouseY);
		
	}
	
	private void onDoneButtonPressed() {
		try {
			String name = "";
			if (this.identifierText.getText() != null) {
				name = FileUtils.generateAvailableFilename(FancyMenu.getCustomGuiPath().getPath(), this.identifierText.getText(), "txt");
				
				File f = new File(FancyMenu.getCustomGuiPath().getPath() + "/" + name);
				if (!f.exists()) {
					f.createNewFile();
				}
				
				List<String> l = new ArrayList<String>();
				l.add("identifier = " + this.identifierText.getText());
				if (this.titleText.getText() != null) {
					l.add("title = " + this.titleText.getText());
				}
				l.add("allowesc = " + this.allowEsc);
				
				FileUtils.writeTextToFile(f, false, l.toArray(new String[0]));
				
				this.setDisplayed(false);
				CustomGuiLoader.loadCustomGuis();
				CustomGuiBase gui = CustomGuiLoader.getGui(this.identifierText.getText(), Minecraft.getMinecraft().currentScreen, null);
				if (gui != null) {
					Minecraft.getMinecraft().displayGuiScreen(gui);
					Minecraft.getMinecraft().displayGuiScreen(new LayoutEditorScreen(gui));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 28) && this.isDisplayed()) {
			if ((this.doneButton != null) && this.doneButton.visible) {
				this.setDisplayed(false);
				this.onDoneButtonPressed();
			}
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 1) && this.isDisplayed()) {
			this.setDisplayed(false);
		}
	}

}
