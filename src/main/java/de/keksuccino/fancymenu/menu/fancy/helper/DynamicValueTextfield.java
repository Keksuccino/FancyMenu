package de.keksuccino.fancymenu.menu.fancy.helper;

import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.UIBase;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.content.AdvancedImageButton;
import de.keksuccino.konkrete.gui.content.AdvancedTextField;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class DynamicValueTextfield extends AdvancedTextField {

	private AdvancedImageButton variableButton;
	private FMContextMenu variableMenu;
	
	private static final ResourceLocation VARIABLES_BUTTON_RESOURCE = new ResourceLocation("keksuccino", "add_btn.png");
	
	public DynamicValueTextfield(FontRenderer fontrenderer, int x, int y, int width, int height, boolean handleTextField, CharacterFilter filter) {
		super(fontrenderer, x, y, width, height, handleTextField, filter);
		
		variableMenu = new FMContextMenu();
		variableMenu.setAutoclose(true);
		
		AdvancedButton playerName = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.playername"), true, (press) -> {
			this.writeText("%playername%");
		});
		playerName.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.playername.desc"), "%n%"));
		UIBase.colorizeButton(playerName);
		variableMenu.addContent(playerName);
		
		AdvancedButton playerUUID = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.playeruuid"), true, (press) -> {
			this.writeText("%playeruuid%");
		});
		playerUUID.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.playeruuid.desc"), "%n%"));
		UIBase.colorizeButton(playerUUID);
		variableMenu.addContent(playerUUID);
		
		AdvancedButton mcVersion = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.mcversion"), true, (press) -> {
			this.writeText("%mcversion%");
		});
		mcVersion.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.mcversion.desc"), "%n%"));
		UIBase.colorizeButton(mcVersion);
		variableMenu.addContent(mcVersion);
		
		AdvancedButton forgeVersion = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.forgeversion"), true, (press) -> {
			this.writeText("%version:forge%");
		});
		forgeVersion.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.forgeversion.desc"), "%n%"));
		UIBase.colorizeButton(forgeVersion);
		variableMenu.addContent(forgeVersion);
		
		AdvancedButton modVersion = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.modversion"), true, (press) -> {
			this.writeText("%version:<modid>%");
		});
		modVersion.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.modversion.desc"), "%n%"));
		UIBase.colorizeButton(modVersion);
		variableMenu.addContent(modVersion);
		
		AdvancedButton totalMods = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.totalmods"), true, (press) -> {
			this.writeText("%totalmods%");
		});
		totalMods.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.totalmods.desc"), "%n%"));
		UIBase.colorizeButton(totalMods);
		variableMenu.addContent(totalMods);
		
		AdvancedButton loadedMods = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.loadedmods"), true, (press) -> {
			this.writeText("%loadedmods%");
		});
		loadedMods.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.loadedmods.desc"), "%n%"));
		UIBase.colorizeButton(loadedMods);
		variableMenu.addContent(loadedMods);
		
		variableButton = new AdvancedImageButton(0, 0, height, height, VARIABLES_BUTTON_RESOURCE, true, (press) -> {
			UIBase.openScaledContextMenuAtMouse(variableMenu);
		});
		variableButton.ignoreBlockedInput = true;
		variableButton.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.desc"), "%n%"));
		UIBase.colorizeButton(variableButton);
		
		variableMenu.setParentButton(variableButton);
		
	}

	@Override
	public void drawTextBox() {
		if (this.variableButton != null) {
			
			this.variableButton.width = this.height;
			this.variableButton.height = this.height;
			
			super.drawTextBox();
			
			this.variableButton.x = this.x + this.width + 5;
			this.variableButton.y = this.y;
			this.variableButton.drawButton(Minecraft.getMinecraft(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getMinecraft().getRenderPartialTicks());
			
			float scale = UIBase.getUIScale();
			
			MouseInput.setRenderScale(scale);
			
			GlStateManager.pushMatrix();
			
			GlStateManager.scale(scale, scale, scale);
			
			if (this.variableMenu != null) {
				this.variableMenu.render( MouseInput.getMouseX(), MouseInput.getMouseY());
			}
			
			GlStateManager.popMatrix();
			
			MouseInput.resetRenderScale();
			
		}
	}

}
