package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.File;
import java.io.IOException;

import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.DynamicValueHelper;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.sound.SoundHandler;
import net.minecraft.client.gui.GuiScreen;

public class VanillaButtonCustomizationItem extends CustomizationItemBase {
	
	private ButtonData parent;
	
	private String normalLabel = "";
	private boolean hovered = false;

	public String hoverLabelRaw;
	public String labelRaw;
	protected boolean normalLabelCached = false;
	
	public VanillaButtonCustomizationItem(PropertiesSection item, ButtonData parent) {
		super(item);
		this.parent = parent;

		if ((this.action != null) && (this.parent != null)) {
			
			if (this.action.equalsIgnoreCase("addhoversound")) {
				this.value = item.getEntryValue("path");
				if (this.value != null) {
					File f = new File(this.value);
					if (f.exists() && f.isFile()) {
						if (!SoundHandler.soundExists(this.value)) {
							MenuCustomization.registerSound(this.value, this.value);
						}
					} else {
						System.out.println("################### ERROR ###################");
						System.out.println("[FancyMenu] Soundfile '" + this.value + "'for 'addhoversound' customization action not found!");
						System.out.println("#############################################");
						this.value = null;
					}
				}
			}

			if (this.action.equalsIgnoreCase("sethoverlabel")) {
				this.hoverLabelRaw = item.getEntryValue("label");
				if (this.parent != null) {
					this.normalLabel = this.parent.getButton().displayString;
				}
				this.updateValues();
			}

			if (this.action.equalsIgnoreCase("renamebutton") || this.action.equalsIgnoreCase("setbuttonlabel")) {
				this.labelRaw = item.getEntryValue("value");
				this.updateValues();
			}
			
		}
	}

	@Override
	public void render(GuiScreen menu) throws IOException {
		if (this.parent != null) {

			this.updateValues();

			if (this.action.equals("addhoversound")) {
				if (this.parent.getButton().isMouseOver() && !hovered && (this.value != null)) {
					SoundHandler.resetSound(this.value);
					SoundHandler.playSound(this.value);
					this.hovered = true;
				}
				if (!this.parent.getButton().isMouseOver()) {
					this.hovered = false;
				}
			}

			if (this.action.equals("sethoverlabel")) {
				if (this.value != null) {
					if (this.parent.getButton().isMouseOver()) {
						if (!this.normalLabelCached) {
							this.normalLabelCached = true;
							this.normalLabel = this.parent.getButton().displayString;
						}
						this.parent.getButton().displayString = this.value;
					} else {
						if (this.normalLabelCached) {
							this.normalLabelCached = false;
							this.parent.getButton().displayString = this.normalLabel;
						}
					}
				}
			}

			if (this.action.equalsIgnoreCase("renamebutton") || this.action.equalsIgnoreCase("setbuttonlabel")) {
				if (this.value != null) {
					if (!this.parent.getButton().isMouseOver()) {
						this.parent.getButton().displayString = this.value;
					}
				}
			}
			
		}
	}

	protected void updateValues() {

		if (this.action.equalsIgnoreCase("renamebutton") || this.action.equalsIgnoreCase("setbuttonlabel")) {
			if (this.labelRaw != null) {
				if (!isEditorActive()) {
					this.value = DynamicValueHelper.convertFromRaw(this.labelRaw);
				} else {
					this.value = StringUtils.convertFormatCodes(this.labelRaw, "&", "§");
				}
			}
		}

		if (this.action.equals("sethoverlabel")) {
			if (this.hoverLabelRaw != null) {
				if (!isEditorActive()) {
					this.value = DynamicValueHelper.convertFromRaw(this.hoverLabelRaw);
				} else {
					this.value = StringUtils.convertFormatCodes(this.hoverLabelRaw, "&", "§");
				}
			}
		}

	}

}
