package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.PopupMenu;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.gui.screens.popup.TextInputPopup;
import de.keksuccino.core.input.CharacterFilter;
import de.keksuccino.core.math.MathUtils;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.resources.TextureHandler;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutObject;
import net.minecraft.client.Minecraft;

public class LayoutVanillaButton extends LayoutObject {
	
	public final ButtonData button;
	public boolean hidden = false;
	public String backNormal = null;
	public String backHovered = null;
	public int clicks = 0;
	public String hoverLabel;
	public String hoverSound;
	
	public LayoutVanillaButton(ButtonData button, LayoutCreatorScreen handler) {
		super(new LayoutButtonDummyCustomizationItem(button.label, button.width, button.height, button.x, button.y), false, handler);
		this.button = button;
		this.object.orientation = "original";
	}

	@Override
	public void init() {
		super.init();
		
		AdvancedButton b0 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.vanillabutton.resetorientation"), (press) -> {
			this.object.orientation = "original";
			this.object.posX = this.button.x;
			this.object.posY = this.button.y;
			this.object.width = this.button.width;
			this.object.height = this.button.height;
			this.rightclickMenu.closeMenu();
			this.orientationMenu.closeMenu();
			Minecraft.getInstance().displayGuiScreen(this.handler);
		});
		this.rightclickMenu.addContent(b0);
		LayoutCreatorScreen.colorizeCreatorButton(b0);
		
		AdvancedButton b1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.vanillabutton.hide"), (press) -> {
			this.handler.hideVanillaButton(this);
		});
		this.rightclickMenu.addContent(b1);
		LayoutCreatorScreen.colorizeCreatorButton(b1);
		
		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup i = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.editlabel") + ":", null, 240, this::editLabelCallback);
			i.setText(this.object.value);
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b2);
		LayoutCreatorScreen.colorizeCreatorButton(b2);
		
		PopupMenu texturePopup = new PopupMenu(100, 16, -1);
		this.rightclickMenu.addChild(texturePopup);
		
		AdvancedButton tpop1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.normal"), (press) -> {
			this.handler.setMenusUseable(false);
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					File home = new File("");
					call = call.replace("\\", "/");
					File f = new File(call);
					String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
					if (f.exists() && f.isFile() && (f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg") || f.getName().endsWith(".png"))) {
						if (filename.equals(f.getName())) {
							if (call.startsWith(home.getAbsolutePath())) {
								call = call.replace(home.getAbsolutePath(), "");
								if (call.startsWith("\\") || call.startsWith("/")) {
									call = call.substring(1);
								}
							}
							this.backNormal = call;
							if (this.backHovered == null) {
								this.backHovered = call;
							}
							this.handler.setVanillaTexture(this, this.backNormal, this.backHovered);
							((LayoutButtonDummyCustomizationItem)this.object).setTexture(TextureHandler.getResource(this.backNormal).getResourceLocation());
							
							this.handler.setMenusUseable(true);
						} else {
							this.handler.displayNotification(300, Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
						}
					} else {
						this.handler.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
					}
				} else {
					this.handler.setMenusUseable(true);
				}
			}, "jpg", "jpeg", "png");
			
			if (this.backNormal != null) {
				cf.setText(this.backNormal);
			}
			
			PopupHandler.displayPopup(cf);
		});
		texturePopup.addContent(tpop1);
		LayoutCreatorScreen.colorizeCreatorButton(tpop1);
		
		AdvancedButton tpop2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.hovered"), (press) -> {
			this.handler.setMenusUseable(false);
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					File home = new File("");
					call = call.replace("\\", "/");
					File f = new File(call);
					String filename = CharacterFilter.getBasicFilenameCharacterFilter().filterForAllowedChars(f.getName());
					if (f.exists() && f.isFile() && (f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg") || f.getName().endsWith(".png"))) {
						if (filename.equals(f.getName())) {
							if (call.startsWith(home.getAbsolutePath())) {
								call = call.replace(home.getAbsolutePath(), "");
								if (call.startsWith("\\") || call.startsWith("/")) {
									call = call.substring(1);
								}
							}
							this.backHovered = call;
							if (this.backNormal == null) {
								this.backNormal = call;
							}
							this.handler.setVanillaTexture(this, this.backNormal, this.backHovered);
							((LayoutButtonDummyCustomizationItem)this.object).setTexture(TextureHandler.getResource(this.backNormal).getResourceLocation());
							
							this.handler.setMenusUseable(true);
						} else {
							this.handler.displayNotification(300, Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
						}
					} else {
						this.handler.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
					}
				} else {
					this.handler.setMenusUseable(true);
				}
			}, "jpg", "jpeg", "png");
			
			if (this.backHovered != null) {
				cf.setText(this.backHovered);
			}
			
			PopupHandler.displayPopup(cf);
		});
		texturePopup.addContent(tpop2);
		LayoutCreatorScreen.colorizeCreatorButton(tpop2);
		
		AdvancedButton tpop3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.reset"), (press) -> {
			this.backHovered = null;
			this.backNormal = null;
			((LayoutButtonDummyCustomizationItem)this.object).setTexture(null);
		});
		texturePopup.addContent(tpop3);
		LayoutCreatorScreen.colorizeCreatorButton(tpop3);
		
		AdvancedButton b4 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture"), (press) -> {
			texturePopup.openMenuAt(0, press.y);
		});
		this.rightclickMenu.addContent(b4);
		LayoutCreatorScreen.colorizeCreatorButton(b4);

		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup ip = new TextInputPopup(new Color(0, 0, 0, 0), "", null, 240, (call) -> {
				if (call != null) {
					this.hoverLabel = call;
					this.handler.setVanillaHoverLabel(this, call);
				}
				this.handler.setMenusUseable(true);
			});

			if (this.hoverLabel != null) {
				ip.setText(this.hoverLabel);
			}
			PopupHandler.displayPopup(ip);
		});
		this.rightclickMenu.addContent(b5);
		LayoutCreatorScreen.colorizeCreatorButton(b5);

		AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel.reset"), (press) -> {
			this.hoverLabel = null;
			this.handler.setVanillaHoverLabel(this, null);
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b6);
		LayoutCreatorScreen.colorizeCreatorButton(b6);

		AdvancedButton b7 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound"), (press) -> {
			this.handler.setMenusUseable(false);
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					File f = new File(call);
					if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
						this.hoverSound = call;
						this.handler.setVanillaHoverSound(this, call);
						this.handler.setMenusUseable(true);
					} else {
						this.handler.displayNotification(300, "§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
					}
				} else {
					this.handler.setMenusUseable(true);
				}
			}, "wav");

			if (this.hoverSound != null) {
				cf.setText(this.hoverSound);
			}
			PopupHandler.displayPopup(cf);
		});
		this.rightclickMenu.addContent(b7);
		LayoutCreatorScreen.colorizeCreatorButton(b7);

		AdvancedButton b8 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound.reset"), (press) -> {
			this.hoverSound = null;
			this.handler.setVanillaHoverSound(this, null);
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b8);
		LayoutCreatorScreen.colorizeCreatorButton(b8);
		
		AdvancedButton b9 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.vanillabutton.autoclick"), (press) -> {
			this.handler.setMenusUseable(false);
			TextInputPopup pop = new TextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.vanillabutton.autoclick.popup"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (call.equals("") || !MathUtils.isInteger(call)) {
						this.clicks = 0;
					} else {
						this.clicks = Integer.parseInt(call);
					}
					this.handler.setVanillaClicks(this, this.clicks);
				}
				this.handler.setMenusUseable(true);
			});
			pop.setText("" + this.clicks);
			PopupHandler.displayPopup(pop);
		});
		this.rightclickMenu.addContent(b9);
		LayoutCreatorScreen.colorizeCreatorButton(b9);
	}
	
	@Override
	public void render(int mouseX, int mouseY) {
		if (this.hidden) {
			this.rightclickMenu.closeMenu();
			this.orientationMenu.closeMenu();
		}
		
		if (!this.canBeModified()) {
			//Cancel dragging
			if (this.isDragged() && this.handler.isFocused(this) && ((this.startX != this.object.posX) || (this.startY != this.object.posY))) {
				this.handler.setObjectFocused(this, false);
				this.dragging = false;
				this.object.posX = this.button.x;
				this.object.posY = this.button.y;
				this.object.width = this.button.width;
				this.object.height = this.button.height;
				GLFW.glfwSetCursor(Minecraft.getInstance().mainWindow.getHandle(), normalCursor);
				this.displaySetOrientationNotification();
				return;
			}
			//Cancel resize
			if ((this.isGrabberPressed() || this.resizing) && !this.isDragged() && this.handler.isFocused(this)) {
				this.resizing = false;
				this.dragging = false;
				this.object.posX = this.button.x;
				this.object.posY = this.button.y;
				this.object.width = this.button.width;
				this.object.height = this.button.height;
				this.handler.setObjectFocused(this, false);
				GLFW.glfwSetCursor(Minecraft.getInstance().mainWindow.getHandle(), normalCursor);
				this.displaySetOrientationNotification();
				return;
			}
		}
		
        super.render(mouseX, mouseY);
	}
	
	@Override
	public boolean isGrabberPressed() {
		if (!this.canBeModified()) {
			return false;
		}
		return super.isGrabberPressed();
	}
	
	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		//hidebutton
		if (this.hidden) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "hidebutton");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			l.add(s);
			//Return because no more sections needed
			return l;
		}
		//movebutton
		if (this.canBeModified()) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "movebutton");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("orientation", this.object.orientation);
			s.addEntry("x", "" + this.object.posX);
			s.addEntry("y", "" + this.object.posY);
			l.add(s);
		}
		// resizebutton
		if (this.canBeModified() && ((this.getWidth() != this.button.width) || (this.getHeight() != this.button.height))) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "resizebutton");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("width", "" + this.object.width);
			s.addEntry("height", "" + this.object.height);
			l.add(s);
		}
		// renamebutton
		if (this.object.value != this.button.label) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttonlabel");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("value", this.object.value);
			l.add(s);
		}
		// setbuttontexture
		if ((this.backHovered != null) && (this.backNormal != null)) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttontexture");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("backgroundnormal", this.backNormal);
			s.addEntry("backgroundhovered", this.backHovered);
			l.add(s);
		}
		// clickbutton
		if (this.clicks > 0) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "clickbutton");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("clicks", "" + this.clicks);
			l.add(s);
		}
		//addhoversound
		if (this.hoverSound != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "addhoversound");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("path", this.hoverSound);
			l.add(s);
		}
		//sethoverlabel
		if (this.hoverLabel != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "sethoverlabel");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("label", this.hoverLabel);
			l.add(s);
		}
		
		return l;
	}
	
	public void displaySetOrientationNotification() {
		this.handler.displayNotification(300, "§c§l" + Locals.localize("helper.creator.items.vanillabutton.noorientation.title"), "", Locals.localize("helper.creator.items.vanillabutton.noorientation.desc"), "", "", "");
	}
	
	private boolean canBeModified() {
		return !this.object.orientation.equals("original");
	}
	
	private void editLabelCallback(String text) {
		if (text == null) {
			this.handler.setMenusUseable(true);
			return;
		} else {
			this.handler.setVanillaButtonName(this, text);
		}
		this.handler.setMenusUseable(true);
	}
	
}
