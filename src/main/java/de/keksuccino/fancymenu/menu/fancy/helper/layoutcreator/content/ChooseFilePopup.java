package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.io.File;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMFilePickerPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public class ChooseFilePopup extends FMTextInputPopup {

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
			PopupHandler.displayPopup(new FMFilePickerPopup(Minecraft.getInstance().gameDirectory.getAbsoluteFile().getAbsolutePath().replace("\\", "/"), Minecraft.getInstance().gameDirectory.getAbsoluteFile().getAbsolutePath().replace("\\", "/"), this, true, (call) -> {
				if (call != null) {
					String path = call.getAbsolutePath().replace("\\", "/");
					File home = Minecraft.getInstance().gameDirectory;
					if (path.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
						path = path.replace(home.getAbsolutePath().replace("\\", "/"), "");
						if (path.startsWith("\\") || path.startsWith("/")) {
							path = path.substring(1);
						}
					}
					path = path.replace("\\", "/");
					this.setText(path);
				}
			}, fileTypes));
		});
		this.addButton(chooseFileBtn);
	}
	
	@Override
	public void render(PoseStack matrix, int mouseX, int mouseY, Screen renderIn) {
		if (!this.isDisplayed()) {
			return;
		}
		RenderSystem.enableBlend();
		fill(matrix, 0, 0, renderIn.width, renderIn.height, new Color(0, 0, 0, 240).getRGB());
		RenderSystem.disableBlend();
		
		GuiComponent.drawCenteredString(matrix, Minecraft.getInstance().font, new TextComponent(title), renderIn.width / 2, (renderIn.height  / 2) - 40, Color.WHITE.getRGB());
		
		this.textField.setX((renderIn.width / 2) - (this.textField.getWidth() / 2));
		this.textField.setY((renderIn.height  / 2) - (this.textField.getHeight() / 2));
		this.textField.renderButton(matrix, mouseX, mouseY, Minecraft.getInstance().getFrameTime());
		
		this.doneButton.setX((renderIn.width / 2) - (this.doneButton.getWidth() / 2));
		this.doneButton.setY(((renderIn.height  / 2) + 100) - this.doneButton.getHeight() - 5);
		
		this.chooseFileBtn.setX((renderIn.width / 2) - (this.doneButton.getWidth() / 2));
		this.chooseFileBtn.setY(((renderIn.height  / 2) + 50) - this.doneButton.getHeight() - 5);
		
		this.renderButtons(matrix, mouseX, mouseY);
	}

}
