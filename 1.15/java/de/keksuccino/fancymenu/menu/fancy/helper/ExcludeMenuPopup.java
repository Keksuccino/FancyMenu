package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization.ExcludeMode;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public class ExcludeMenuPopup extends TextInputPopup {

	private AdvancedButton noBundleExcludeButton;
	private AdvancedButton bundleExcludeButton;
	
	private boolean bundle = false;
	private ExcludeMode mode;
	
	public ExcludeMenuPopup(ExcludeMode mode) {
		super(new Color(0, 0, 0, 0), "", null, 0);
		this.mode = mode;
	}
	
	@Override
	protected void init(Color color, String title, CharacterFilter filter, Consumer<String> callback) {
		super.init(color, title, filter, callback);

		this.noBundleExcludeButton = new AdvancedButton(0, 0, 120, 20, "§a" + Locals.localize("helper.excludemenu.excludejustthis"), true, (press) -> {
			this.bundle = false;
			this.bundleExcludeButton.setMessage(Locals.localize("helper.excludemenu.excludeall"));
			press.setMessage("§a" + Locals.localize("helper.excludemenu.excludejustthis"));
		});
		this.addButton(noBundleExcludeButton);
		
		this.bundleExcludeButton = new AdvancedButton(0, 0, 120, 20, Locals.localize("helper.excludemenu.excludeall"), true, (press) -> {
			this.bundle = true;
			this.noBundleExcludeButton.setMessage(Locals.localize("helper.excludemenu.excludejustthis"));
			press.setMessage("§a" + Locals.localize("helper.excludemenu.excludeall"));
		});
		this.addButton(bundleExcludeButton);
		
		this.doneButton.setPressAction((press) -> {
			this.input = this.textField.getText();
			this.setDisplayed(false);
			if ((this.textField.getText() != null) && !this.textField.getText().replace(" ", "").equalsIgnoreCase("")) {
				MenuCustomization.addExcludedMenu(this.textField.getText().replace(" ", ""), this.mode, this.bundle);
				CustomizationHelper.getInstance().onReloadButtonPress();
			}
		});
	}
	
	@Override
	public void render(int mouseX, int mouseY, Screen renderIn) {
		if (this.isDisplayed()) {
			
			RenderSystem.enableBlend();
			fill(0, 0, renderIn.width, renderIn.height, new Color(0, 0, 0, 240).getRGB());
			RenderSystem.disableBlend();
			
			int height = 100;
			
			RenderSystem.enableBlend();
			fill((renderIn.width / 2) - (this.width / 2), (renderIn.height / 2) - (height / 2), (renderIn.width / 2) + (this.width / 2), (renderIn.height / 2) + (height / 2), this.color.getRGB());
			RenderSystem.disableBlend();
			
			drawCenteredString(Minecraft.getInstance().fontRenderer, "§l" + Locals.localize("helper.excludemenu.byidentifier"), renderIn.width / 2, (renderIn.height / 2) - (this.textField.getHeight() / 2) - 65, Color.WHITE.getRGB());
			
			drawCenteredString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.menuinfo.identifier"), renderIn.width / 2, (renderIn.height / 2) - (this.textField.getHeight() / 2) - 15, Color.WHITE.getRGB());
			
			this.textField.setX((renderIn.width / 2) - (this.textField.getWidth() / 2));
			this.textField.setY((renderIn.height / 2) - (this.textField.getHeight() / 2));
			this.textField.renderButton( mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
			
			drawCenteredString(Minecraft.getInstance().fontRenderer, Locals.localize("helper.excludemenu.excludebundle"), renderIn.width / 2, (renderIn.height / 2) + 25, Color.WHITE.getRGB());
			
			this.noBundleExcludeButton.x = (renderIn.width / 2) - this.noBundleExcludeButton.getWidth() - 5;
			this.noBundleExcludeButton.y = (renderIn.height / 2) + 40;
			
			this.bundleExcludeButton.x = (renderIn.width / 2) + 5;
			this.bundleExcludeButton.y = (renderIn.height / 2) + 40;
			
			this.doneButton.x = (renderIn.width / 2) - (this.doneButton.getWidth() / 2);
			this.doneButton.y = (renderIn.height / 2) + 90;
			
			this.renderButtons(mouseX, mouseY);
		}
	}
	
	@Override
	public void onEnterPressed(KeyboardData d) {
		super.onEnterPressed(d);
		
		if ((d.keycode == 257) && this.isDisplayed()) {
			if ((this.textField.getText() != null) && !this.textField.getText().replace(" ", "").equalsIgnoreCase("")) {
				MenuCustomization.addExcludedMenu(this.textField.getText().replace(" ", ""), this.mode, this.bundle);
				CustomizationHelper.getInstance().onReloadButtonPress();
			}
		}
	}

}
