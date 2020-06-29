package de.keksuccino.fancymenu.menu.fancy.item;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.core.sound.SoundHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class VanillaButtonCustomizationItem extends CustomizationItemBase {
	
	private Widget parent;
	private AdvancedButton button;
	
	private String normalLabel = "";
	private boolean hovered = false;
	
	public VanillaButtonCustomizationItem(PropertiesSection item, Widget parent) {
		super(item);
		this.parent = parent;

		if (this.action != null) {
			if (this.action.equalsIgnoreCase("setbuttontexture")) {
				String backNormal = item.getEntryValue("backgroundnormal");
				String backHover = item.getEntryValue("backgroundhovered");
				this.button = new AdvancedButton(parent.field_230690_l_, parent.field_230691_m_, parent.func_230998_h_(), parent.getHeight(), parent.func_230458_i_().getString(), true, (press) -> {
					click();
				});
				this.button.setBackgroundTexture(TextureHandler.getResource(backNormal.replace("\\", "/")), TextureHandler.getResource(backHover.replace("\\", "/")));
			}
			
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
				this.value = item.getEntryValue("label");
				if (this.parent != null) {
					this.normalLabel = this.parent.func_230458_i_().getString();
				}
			}
		}
	}

	@Override
	public void render(MatrixStack matrix, Screen menu) throws IOException {
		if (this.parent != null) {
			
			//Sync custom button with its parent and render it
			if (this.button != null) {
				if (this.button.isHovered() && !this.parent.func_230449_g_()) {
					this.setHovered(this.parent, true);
				}
				if (!this.button.isHovered() && this.parent.func_230449_g_()) {
					this.setHovered(this.parent, false);
				}
				
				this.button.setX(this.parent.field_230690_l_);
				this.button.setY( this.parent.field_230691_m_);
				this.button.setWidth(this.parent.func_230998_h_());
				this.button.setHeight(this.parent.getHeight());
				this.button.setMessage(this.parent.func_230458_i_().getString());

				this.button.setMessage(this.parent.func_230458_i_().getString());
				
				this.button.render(matrix, MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}

			//Everything for "setbuttontexture" is done in the button sync above
			
			if (this.action.equals("addhoversound")) {
				if (this.parent.func_230449_g_() && !hovered && (this.value != null)) {
					SoundHandler.resetSound(this.value);
					SoundHandler.playSound(this.value);
					this.hovered = true;
				}
				if (!this.parent.func_230449_g_()) {
					this.hovered = false;
				}
			}
			
			if (this.action.equals("sethoverlabel")) {
				if (this.value != null) {
					if (this.parent.func_230449_g_()) {
						this.parent.func_238482_a_(new StringTextComponent(this.value));
					} else {
						this.parent.func_238482_a_(new StringTextComponent(this.normalLabel));
					}
				}
			}
			
		}
	}
	
	private void setHovered(Widget w, boolean hovered) {
		try {
			Field f = ObfuscationReflectionHelper.findField(Widget.class, "isHovered");
			f.set(w, hovered);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void click() {
		this.parent.func_230982_a_(MouseInput.getMouseX(), MouseInput.getMouseY());
	}

}
