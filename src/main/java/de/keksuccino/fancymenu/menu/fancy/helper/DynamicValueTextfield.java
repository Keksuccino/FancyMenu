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
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class DynamicValueTextfield extends AdvancedTextField {

	private AdvancedImageButton variableButton;
	private FMContextMenu variableMenu;
	
	private static final Identifier VARIABLES_BUTTON_RESOURCE = new Identifier("keksuccino", "add_btn.png");
	
	public DynamicValueTextfield(TextRenderer fontrenderer, int x, int y, int width, int height, boolean handleTextField, CharacterFilter filter) {
		super(fontrenderer, x, y, width, height, handleTextField, filter);
		
		variableMenu = new FMContextMenu();
		variableMenu.setAutoclose(true);
		
		AdvancedButton playerName = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.playername"), true, (press) -> {
			this.write("%playername%");
		});
		playerName.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.playername.desc"), "%n%"));
		UIBase.colorizeButton(playerName);
		variableMenu.addContent(playerName);
		
		AdvancedButton playerUUID = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.playeruuid"), true, (press) -> {
			this.write("%playeruuid%");
		});
		playerUUID.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.playeruuid.desc"), "%n%"));
		UIBase.colorizeButton(playerUUID);
		variableMenu.addContent(playerUUID);
		
		AdvancedButton mcVersion = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.mcversion"), true, (press) -> {
			this.write("%mcversion%");
		});
		mcVersion.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.mcversion.desc"), "%n%"));
		UIBase.colorizeButton(mcVersion);
		variableMenu.addContent(mcVersion);
		
		AdvancedButton forgeVersion = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.forgeversion"), true, (press) -> {
			this.write("%version:forge%");
		});
		forgeVersion.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.forgeversion.desc"), "%n%"));
		UIBase.colorizeButton(forgeVersion);
		variableMenu.addContent(forgeVersion);
		
		AdvancedButton modVersion = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.modversion"), true, (press) -> {
			this.write("%version:<modid>%");
		});
		modVersion.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.modversion.desc"), "%n%"));
		UIBase.colorizeButton(modVersion);
		variableMenu.addContent(modVersion);
		
		AdvancedButton totalMods = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.totalmods"), true, (press) -> {
			this.write("%totalmods%");
		});
		totalMods.setDescription(StringUtils.splitLines(Locals.localize("helper.ui.dynamicvariabletextfield.variables.totalmods.desc"), "%n%"));
		UIBase.colorizeButton(totalMods);
		variableMenu.addContent(totalMods);
		
		AdvancedButton loadedMods = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.ui.dynamicvariabletextfield.variables.loadedmods"), true, (press) -> {
			this.write("%loadedmods%");
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
	public void renderButton(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		if (this.variableButton != null) {
			
			this.variableButton.setWidth(this.height);
			this.variableButton.setHeight(this.height);
			
			super.renderButton(matrix, mouseX, mouseY, partialTicks);
			
			this.variableButton.setX(this.x + this.width + 5);
			this.variableButton.setY(this.y);
			this.variableButton.render(matrix, mouseX, mouseY, partialTicks);
			
			float scale = UIBase.getUIScale();
			
			MouseInput.setRenderScale(scale);
			
			matrix.push();
			
			matrix.scale(scale, scale, scale);
			
			if (this.variableMenu != null) {
				this.variableMenu.render(matrix, MouseInput.getMouseX(), MouseInput.getMouseY());
			}
			
			matrix.pop();
			
			MouseInput.resetRenderScale();
			
		}
	}

}
