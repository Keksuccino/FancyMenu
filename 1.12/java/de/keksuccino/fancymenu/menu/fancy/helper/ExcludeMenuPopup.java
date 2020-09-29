package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.util.function.Consumer;

import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization.ExcludeMode;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.TextInputPopup;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.KeyboardData;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

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
			this.bundleExcludeButton.displayString = Locals.localize("helper.excludemenu.excludeall");
			press.displayString = "§a" + Locals.localize("helper.excludemenu.excludejustthis");
		});
		this.addButton(noBundleExcludeButton);
		
		this.bundleExcludeButton = new AdvancedButton(0, 0, 120, 20, Locals.localize("helper.excludemenu.excludeall"), true, (press) -> {
			this.bundle = true;
			this.noBundleExcludeButton.displayString = Locals.localize("helper.excludemenu.excludejustthis");
			press.displayString = "§a" + Locals.localize("helper.excludemenu.excludeall");
		});
		this.addButton(bundleExcludeButton);
		
		this.doneButton.setPressAction((press) -> {
			this.setDisplayed(false);
			if ((this.getInput() != null) && !this.getInput().replace(" ", "").equalsIgnoreCase("")) {
				MenuCustomization.addExcludedMenu(this.getInput().replace(" ", ""), this.mode, this.bundle);
				CustomizationHelper.getInstance().onReloadButtonPress();
			}
		});
	}
	
	@Override
	public void render(int mouseX, int mouseY, GuiScreen renderIn) {
		if (this.isDisplayed()) {
			
			GlStateManager.enableBlend();
			drawRect(0, 0, renderIn.width, renderIn.height, new Color(0, 0, 0, 240).getRGB());
			GlStateManager.disableBlend();
			
			int height = 100;
			
			GlStateManager.enableBlend();
			drawRect((renderIn.width / 2) - (this.width / 2), (renderIn.height / 2) - (height / 2), (renderIn.width / 2) + (this.width / 2), (renderIn.height / 2) + (height / 2), this.color.getRGB());
			GlStateManager.disableBlend();
			
			drawCenteredString(Minecraft.getMinecraft().fontRenderer, "§l" + Locals.localize("helper.excludemenu.byidentifier"), renderIn.width / 2, (renderIn.height / 2) - (this.textField.height / 2) - 65, Color.WHITE.getRGB());
			
			drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("helper.menuinfo.identifier"), renderIn.width / 2, (renderIn.height / 2) - (this.textField.height / 2) - 15, Color.WHITE.getRGB());
			
			this.textField.x = (renderIn.width / 2) - (this.textField.width / 2);
			this.textField.y = (renderIn.height / 2) - (this.textField.height / 2);
			this.textField.drawTextBox();
			
			drawCenteredString(Minecraft.getMinecraft().fontRenderer, Locals.localize("helper.excludemenu.excludebundle"), renderIn.width / 2, (renderIn.height / 2) + 25, Color.WHITE.getRGB());
			
			this.noBundleExcludeButton.x = (renderIn.width / 2) - this.noBundleExcludeButton.width - 5;
			this.noBundleExcludeButton.y = (renderIn.height / 2) + 40;
			
			this.bundleExcludeButton.x = (renderIn.width / 2) + 5;
			this.bundleExcludeButton.y = (renderIn.height / 2) + 40;
			
			this.doneButton.x = (renderIn.width / 2) - (this.doneButton.width / 2);
			this.doneButton.y = (renderIn.height / 2) + 90;
			
			this.renderButtons(mouseX, mouseY);
		}
	}
	
	@Override
	public String getInput() {
		CharacterFilter f = CharacterFilter.getFilenameFilterWithUppercaseSupport();
		if (this.textField.getText() != null) {
			return f.filterForAllowedChars(this.textField.getText());
		}
		return null;
	}
	
	@Override
	public void onEnterPressed(KeyboardData d) {
		super.onEnterPressed(d);
		
		if ((d.keycode == 28) && this.isDisplayed()) {
			if ((this.getInput() != null) && !this.getInput().replace(" ", "").equalsIgnoreCase("")) {
				MenuCustomization.addExcludedMenu(this.getInput().replace(" ", ""), this.mode, this.bundle);
				CustomizationHelper.getInstance().onReloadButtonPress();
			}
		}
	}

}
