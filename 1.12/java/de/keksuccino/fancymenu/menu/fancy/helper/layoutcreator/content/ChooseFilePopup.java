package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.io.File;
import java.util.function.Consumer;

import de.keksuccino.core.filechooser.FileChooser;
import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.screens.popup.TextInputPopup;
import de.keksuccino.core.input.CharacterFilter;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

public class ChooseFilePopup extends TextInputPopup {

	protected AdvancedButton chooseFileBtn;
	private String[] fileTypes;
	
	public ChooseFilePopup(Consumer<String> callback, String... fileTypes) {
		super(new Color(0, 0, 0, 0), Locals.localize("helper.creator.choosefile.enterorchoose"), null, 0, callback);
		this.fileTypes = fileTypes;
	}
	
	@Override
	protected void init(Color color, String title, CharacterFilter filter, Consumer<String> callback) {
		super.init(color, title, filter, callback);
		
		this.chooseFileBtn = new AdvancedButton(0, 0, 100, 20, Locals.localize("helper.creator.choosefile.choose"), true, (press) -> {
			if (FancyMenu.isNotHeadless()) {
				FileChooser.askForFile(new File("").getAbsoluteFile(), (call) -> {
					if (call != null) {
						String path = call.getAbsolutePath();
						File home = new File("");
						if (path.startsWith(home.getAbsolutePath())) {
							path = path.replace(home.getAbsolutePath(), "");
							if (path.startsWith("\\") || path.startsWith("/")) {
								path = path.substring(1);
							}
						}
						this.setText(path);
					}
				}, fileTypes);
			}
		});
		this.addButton(chooseFileBtn);
	}
	
	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		if (!this.isDisplayed()) {
			return;
		}
		GlStateManager.enableBlend();
		GuiScreen.drawRect(0, 0, renderIn.width, renderIn.height, new Color(0, 0, 0, 240).getRGB());
		GlStateManager.disableBlend();
		
		renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, title, renderIn.width / 2, (renderIn.height / 2) - 40, Color.WHITE.getRGB());
		
		this.textField.x = (renderIn.width / 2) - (this.textField.getWidth() / 2);
		this.textField.y = (renderIn.height / 2) - (this.textField.height / 2);
		this.textField.drawTextBox();
		
		this.doneButton.x = (renderIn.width / 2) - (this.doneButton.width / 2);
		this.doneButton.y = ((renderIn.height / 2) + 100) - this.doneButton.height - 5;
		
		this.chooseFileBtn.x = (renderIn.width / 2) - (this.doneButton.width / 2);
		this.chooseFileBtn.y = ((renderIn.height / 2) + 50) - this.doneButton.height - 5;
		
		if (!FancyMenu.isNotHeadless()) {
			renderIn.drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("helper.creator.choosefile.notsupported"), (renderIn.width / 2), ((renderIn.height / 2) + 50), Color.WHITE.getRGB());
		}
		
		this.renderButtons(mouseX, mouseY);
	}

}
