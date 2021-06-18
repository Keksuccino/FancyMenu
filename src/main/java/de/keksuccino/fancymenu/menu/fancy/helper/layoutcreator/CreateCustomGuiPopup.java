package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiBase;
import de.keksuccino.fancymenu.menu.fancy.guicreator.CustomGuiLoader;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMPopup;
import de.keksuccino.konkrete.file.FileUtils;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.input.KeyboardHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

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
		
		this.identifierText = new AdvancedTextField(MinecraftClient.getInstance().textRenderer, 0, 0, 200, 20, true, null);
		
		this.titleText = new AdvancedTextField(MinecraftClient.getInstance().textRenderer, 0, 0, 200, 20, true, null);
		
		this.allowEscButton = new AdvancedButton(0, 0, 120, 20, "§a" + Locals.localize("helper.buttons.tools.creategui.allowesc"), true, (press) -> {
			press.setMessage(new LiteralText("§a" + Locals.localize("helper.buttons.tools.creategui.allowesc")));
			this.doNotAllowEscButton.setMessage(Locals.localize("helper.buttons.tools.creategui.donotallowesc"));
			this.allowEsc = true;
		});
		this.addButton(allowEscButton);
		
		this.doNotAllowEscButton = new AdvancedButton(0, 0, 120, 20, Locals.localize("helper.buttons.tools.creategui.donotallowesc"), true, (press) -> {
			press.setMessage(new LiteralText("§a" + Locals.localize("helper.buttons.tools.creategui.donotallowesc")));
			this.allowEscButton.setMessage(Locals.localize("helper.buttons.tools.creategui.allowesc"));
			this.allowEsc = false;
		});
		this.addButton(doNotAllowEscButton);
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}
	
	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, Screen renderIn) {
		super.render(matrix, mouseX, mouseY, renderIn);
		
		float partial = MinecraftClient.getInstance().getTickDelta();
		String id = this.identifierText.getText();
		TextRenderer font = MinecraftClient.getInstance().textRenderer;	
		
		drawCenteredText(matrix, font, "§l" + Locals.localize("helper.buttons.tools.creategui"), renderIn.width / 2, (renderIn.height / 2) - 110, Color.WHITE.getRGB());
		
		drawCenteredText(matrix, font, Locals.localize("helper.buttons.tools.creategui.menuidentifier"), renderIn.width / 2, (renderIn.height / 2) - 80, Color.WHITE.getRGB());
		this.identifierText.x = (renderIn.width / 2) - (this.identifierText.getWidth() / 2);
		this.identifierText.y = (renderIn.height / 2) - 65;
		this.identifierText.render(matrix, mouseX, mouseY, partial);
		
		drawCenteredText(matrix, font, Locals.localize("helper.buttons.tools.creategui.menutitle"), renderIn.width / 2, (renderIn.height / 2) - 37, Color.WHITE.getRGB());
		this.titleText.x = (renderIn.width / 2) - (this.titleText.getWidth() / 2);
		this.titleText.y = (renderIn.height / 2) - 22;
		this.titleText.render(matrix, mouseX, mouseY, partial);

		drawCenteredText(matrix, font, Locals.localize("helper.buttons.tools.creategui.allowescdesc"), renderIn.width / 2, (renderIn.height / 2) + 5, Color.WHITE.getRGB());
		this.allowEscButton.x = (renderIn.width / 2) - this.allowEscButton.getWidth() - 5;
		this.allowEscButton.y = (renderIn.height / 2) + 20;
		
		this.doNotAllowEscButton.x = (renderIn.width / 2) + 5;
		this.doNotAllowEscButton.y = (renderIn.height / 2) + 20;

		this.cancelButton.x = (renderIn.width / 2) - (this.cancelButton.getWidth() / 2);
		this.cancelButton.y = (renderIn.height / 2) + 55;
		
		if ((id != null) && (id.length() > 0) && !id.contains(" ")) {
			List<String> l = CustomGuiLoader.getCustomGuis();
			if (!l.contains(id)) {
				this.doneButton.x = (renderIn.width / 2) - (this.doneButton.getWidth() / 2);
				this.doneButton.y = (renderIn.height / 2) + 80;
				this.doneButton.visible = true;
			} else {
				this.doneButton.visible = false;
				drawCenteredText(matrix, font, Locals.localize("helper.buttons.tools.creategui.identifieralreadyused"), renderIn.width / 2, (renderIn.height / 2) + 85, Color.WHITE.getRGB());
			}
		} else {
			this.doneButton.visible = false;
			drawCenteredText(matrix, font, Locals.localize("helper.buttons.tools.creategui.invalididentifier"), renderIn.width / 2, (renderIn.height / 2) + 85, Color.WHITE.getRGB());
		}
		
		this.renderButtons(matrix, mouseX, mouseY);
		
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
				CustomGuiBase gui = CustomGuiLoader.getGui(this.identifierText.getText(), MinecraftClient.getInstance().currentScreen, null);
				if (gui != null) {
					MinecraftClient.getInstance().openScreen(gui);
					MinecraftClient.getInstance().openScreen(new LayoutEditorScreen(gui));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
			if ((this.doneButton != null) && this.doneButton.visible) {
				this.setDisplayed(false);
				this.onDoneButtonPressed();
			}
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 256) && this.isDisplayed()) {
			this.setDisplayed(false);
		}
	}

}
