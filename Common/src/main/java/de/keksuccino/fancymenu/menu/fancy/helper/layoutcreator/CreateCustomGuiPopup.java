package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
		
		this.identifierText = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 200, 20, true, null);
		this.identifierText.setMaxLength(10000);
		
		this.titleText = new AdvancedTextField(Minecraft.getInstance().font, 0, 0, 200, 20, true, null);
		this.titleText.setMaxLength(10000);
		
		this.allowEscButton = new AdvancedButton(0, 0, 120, 20, "§a" + Locals.localize("helper.buttons.tools.creategui.allowesc"), true, (press) -> {
			press.setMessage(Component.literal("§a" + Locals.localize("helper.buttons.tools.creategui.allowesc")));
			this.doNotAllowEscButton.setMessage(Locals.localize("helper.buttons.tools.creategui.donotallowesc"));
			this.allowEsc = true;
		});
		this.addButton(allowEscButton);
		
		this.doNotAllowEscButton = new AdvancedButton(0, 0, 120, 20, Locals.localize("helper.buttons.tools.creategui.donotallowesc"), true, (press) -> {
			press.setMessage(Component.literal("§a" + Locals.localize("helper.buttons.tools.creategui.donotallowesc")));
			this.allowEscButton.setMessage(Locals.localize("helper.buttons.tools.creategui.allowesc"));
			this.allowEsc = false;
		});
		this.addButton(doNotAllowEscButton);
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}
	
	@Override
	public void render(PoseStack matrix, int mouseX, int mouseY, Screen renderIn) {
		super.render(matrix, mouseX, mouseY, renderIn);
		
		float partial = Minecraft.getInstance().getFrameTime();
		String id = this.identifierText.getValue();
		Font font = Minecraft.getInstance().font;
		
		GuiComponent.drawCenteredString(matrix, font, "§l" + Locals.localize("helper.buttons.tools.creategui"), renderIn.width / 2, (renderIn.height / 2) - 110, Color.WHITE.getRGB());
		
		GuiComponent.drawCenteredString(matrix, font, Locals.localize("helper.buttons.tools.creategui.menuidentifier"), renderIn.width / 2, (renderIn.height / 2) - 80, Color.WHITE.getRGB());
		this.identifierText.x = (renderIn.width / 2) - (this.identifierText.getWidth() / 2);
		this.identifierText.y = (renderIn.height / 2) - 65;
		this.identifierText.render(matrix, mouseX, mouseY, partial);
		
		GuiComponent.drawCenteredString(matrix, font, Locals.localize("helper.buttons.tools.creategui.menutitle"), renderIn.width / 2, (renderIn.height / 2) - 37, Color.WHITE.getRGB());
		this.titleText.x = (renderIn.width / 2) - (this.titleText.getWidth() / 2);
		this.titleText.y = (renderIn.height / 2) - 22;
		this.titleText.render(matrix, mouseX, mouseY, partial);

		GuiComponent.drawCenteredString(matrix, font, Locals.localize("helper.buttons.tools.creategui.allowescdesc"), renderIn.width / 2, (renderIn.height / 2) + 5, Color.WHITE.getRGB());
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
				GuiComponent.drawCenteredString(matrix, font, Locals.localize("helper.buttons.tools.creategui.identifieralreadyused"), renderIn.width / 2, (renderIn.height / 2) + 85, Color.WHITE.getRGB());
			}
		} else {
			this.doneButton.visible = false;
			GuiComponent.drawCenteredString(matrix, font, Locals.localize("helper.buttons.tools.creategui.invalididentifier"), renderIn.width / 2, (renderIn.height / 2) + 85, Color.WHITE.getRGB());
		}
		
		this.renderButtons(matrix, mouseX, mouseY);
		
	}
	
	private void onDoneButtonPressed() {
		try {
			String name = "";
			if (this.identifierText.getValue() != null) {
				name = FileUtils.generateAvailableFilename(FancyMenu.getCustomGuisDirectory().getPath(), this.identifierText.getValue(), "txt");
				
				File f = new File(FancyMenu.getCustomGuisDirectory().getPath() + "/" + name);
				if (!f.exists()) {
					f.createNewFile();
				}
				
				List<String> l = new ArrayList<String>();
				l.add("identifier = " + this.identifierText.getValue());
				if (this.titleText.getValue() != null) {
					l.add("title = " + this.titleText.getValue());
				}
				l.add("allowesc = " + this.allowEsc);
				
				FileUtils.writeTextToFile(f, false, l.toArray(new String[0]));
				
				this.setDisplayed(false);
				CustomGuiLoader.loadCustomGuis();
				CustomGuiBase gui = CustomGuiLoader.getGui(this.identifierText.getValue(), Minecraft.getInstance().screen, null);
				if (gui != null) {
					Minecraft.getInstance().setScreen(gui);
					Minecraft.getInstance().setScreen(new LayoutEditorScreen(gui));
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
