package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.button;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;



import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.menu.button.ButtonData;
import de.keksuccino.fancymenu.menu.fancy.helper.DynamicValueInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.ChooseFilePopup;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content.LayoutElement;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.FMContextMenu;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMYesNoPopup;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.resources.TextureHandler;
import net.minecraft.client.Minecraft;

//TODO übernehmen (implements entfernen)
public class LayoutVanillaButton extends LayoutElement {
	
	public final ButtonData button;
	public boolean hidden = false;
	public String backNormal = null;
	public String backHovered = null;
	public int clicks = 0;
	public String hoverLabel;
	public String hoverSound;
	//TODO übernehmen
//	public double hideforsec = 0;
//	public boolean delayonlyfirsttime = false;
	public String clicksound = null;
	public String description = null;
	
	public LayoutVanillaButton(ButtonData button, LayoutEditorScreen handler) {
		//TODO übernehmen (destroyable true)
		super(new LayoutButtonDummyCustomizationItem(button.label, button.width, button.height, button.x, button.y), true, handler);
		this.button = button;
		this.object.orientation = "original";
	}

	@Override
	//TODO übernehmen (public)
	public void init() {
		this.orderable = false;
		//TODO übernehmen
		this.copyable = false;
				
		super.init();
		
		AdvancedButton b0 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.vanillabutton.resetorientation"), (press) -> {
			if (!this.object.orientation.equals("original")) {
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			}
			
			this.object.orientation = "original";
			this.object.posX = this.button.x;
			this.object.posY = this.button.y;
			this.object.width = this.button.width;
			this.object.height = this.button.height;
			this.rightclickMenu.closeMenu();
			//TODO übernehmen
//			this.orientationMenu.closeMenu();
			//TODO übernehmen
			this.handler.postRenderTasks.add(new Runnable() {
				@Override
				public void run() {
					Minecraft.getInstance().displayGuiScreen(LayoutVanillaButton.this.handler);
				}
			});
		});
		this.rightclickMenu.addContent(b0);
		//TODO übernehmen
//		UIBase.colorizeButton(b0);
		
		//TODO übernehmen
//		AdvancedButton b1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.vanillabutton.hide"), (press) -> {
//			this.handler.hideVanillaButton(this);
//		});
//		this.rightclickMenu.addContent(b1);
		
		AdvancedButton b2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.editlabel"), (press) -> {
			//TODO übernehmen
			FMTextInputPopup i = new DynamicValueInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.button.editlabel") + ":", null, 240, this::editLabelCallback);
			i.setText(StringUtils.convertFormatCodes(this.object.value, "§", "&"));
			PopupHandler.displayPopup(i);
		});
		this.rightclickMenu.addContent(b2);
		//TODO übernehmen
//		UIBase.colorizeButton(b2);

		AdvancedButton b3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.resetlabel"), (press) -> {
			this.editLabelCallback(this.button.label);
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b3);
		//TODO übernehmen
//		UIBase.colorizeButton(b3);
		
		//TODO übernehmen (fmcontext)
		FMContextMenu texturePopup = new FMContextMenu();
		this.rightclickMenu.addChild(texturePopup);
		
		AdvancedButton tpop1 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.normal"), (press) -> {
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
							if ((this.backNormal == null) || !this.backNormal.equals(call)) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}
							
							this.backNormal = call;
							if (this.backHovered == null) {
								this.backHovered = call;
							}
							this.handler.setVanillaTexture(this, this.backNormal, this.backHovered);
							((LayoutButtonDummyCustomizationItem)this.object).setTexture(TextureHandler.getResource(this.backNormal).getResourceLocation());

						} else {
							//TODO übernehmen
							LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
						}
					} else {
						//TODO übernehmen
						LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
					}
				}
			}, "jpg", "jpeg", "png");
			
			if (this.backNormal != null) {
				cf.setText(this.backNormal);
			}
			
			PopupHandler.displayPopup(cf);
		});
		texturePopup.addContent(tpop1);
		//TODO übernehmen
//		UIBase.colorizeButton(tpop1);
		
		AdvancedButton tpop2 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.hovered"), (press) -> {
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

							if ((this.backHovered == null) || !this.backHovered.equals(call)) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}
							
							this.backHovered = call;
							if (this.backNormal == null) {
								this.backNormal = call;
							}
							this.handler.setVanillaTexture(this, this.backNormal, this.backHovered);
							((LayoutButtonDummyCustomizationItem)this.object).setTexture(TextureHandler.getResource(this.backNormal).getResourceLocation());

						} else {
							//TODO übernehmen
							LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.textures.invalidcharacters"), "", "", "", "", "", "");
						}
					} else {
						//TODO übernehmen
						LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidimage.title"), "", Locals.localize("helper.creator.invalidimage.desc"), "", "", "", "", "", "");
					}
				}
			}, "jpg", "jpeg", "png");
			
			if (this.backHovered != null) {
				cf.setText(this.backHovered);
			}
			
			PopupHandler.displayPopup(cf);
		});
		texturePopup.addContent(tpop2);
		//TODO übernehmen
//		UIBase.colorizeButton(tpop2);
		
		AdvancedButton tpop3 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture.reset"), (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());

			this.backHovered = null;
			this.backNormal = null;
			((LayoutButtonDummyCustomizationItem)this.object).setTexture(null);
			this.handler.setVanillaTexture(this, null, null);
		});
		texturePopup.addContent(tpop3);
		//TODO übernehmen
//		UIBase.colorizeButton(tpop3);
		
		AdvancedButton b4 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.custombutton.config.texture"), (press) -> {
			//TODO übernehmen
			texturePopup.setParentButton((AdvancedButton) press);
			texturePopup.openMenuAt(0, ((AdvancedButton)press).getY());
		});
		this.rightclickMenu.addContent(b4);
		//TODO übernehmen
//		UIBase.colorizeButton(b4);

		AdvancedButton b5 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel"), (press) -> {
			//TODO übernehmen
			FMTextInputPopup ip = new DynamicValueInputPopup(new Color(0, 0, 0, 0), "", null, 240, (call) -> {
				if (call != null) {
					if ((this.hoverLabel == null) || !this.hoverLabel.equals(StringUtils.convertFormatCodes(call, "&", "§"))) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}
					
					this.hoverLabel = StringUtils.convertFormatCodes(call, "&", "§");
					this.handler.setVanillaHoverLabel(this, this.hoverLabel);
				}
			});

			if (this.hoverLabel != null) {
				ip.setText(StringUtils.convertFormatCodes(this.hoverLabel, "§", "&"));
			}
			PopupHandler.displayPopup(ip);
		});
		this.rightclickMenu.addContent(b5);
		//TODO übernehmen
//		UIBase.colorizeButton(b5);

		AdvancedButton b6 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoverlabel.reset"), (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			
			this.hoverLabel = null;
			this.handler.setVanillaHoverLabel(this, null);
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b6);
		//TODO übernehmen
//		UIBase.colorizeButton(b6);

		AdvancedButton b7 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound"), (press) -> {
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					File f = new File(call);
					if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
						if ((this.hoverSound == null) || !this.hoverSound.equals(call)) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.hoverSound = call;
						this.handler.setVanillaHoverSound(this, call);
					} else {
						//TODO übernehmen
						LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
					}
				}
			}, "wav");

			if (this.hoverSound != null) {
				cf.setText(this.hoverSound);
			}
			PopupHandler.displayPopup(cf);
		});
		this.rightclickMenu.addContent(b7);
		//TODO übernehmen
//		UIBase.colorizeButton(b7);

		AdvancedButton b8 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.hoversound.reset"), (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			
			this.hoverSound = null;
			this.handler.setVanillaHoverSound(this, null);
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b8);
		//TODO übernehmen
//		UIBase.colorizeButton(b8);

		AdvancedButton b11 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound"), (press) -> {
			ChooseFilePopup cf = new ChooseFilePopup((call) -> {
				if (call != null) {
					File f = new File(call);
					if (f.exists() && f.isFile() && f.getName().endsWith(".wav")) {
						if ((this.clicksound == null) || !this.clicksound.equals(call)) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.clicksound = call;
						this.handler.setVanillaClickSound(this, call);
					} else {
						//TODO übernehmen
						LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.invalidaudio.title"), "", Locals.localize("helper.creator.invalidaudio.desc"), "", "", "", "", "", "");
					}
				}
			}, "wav");
			
			if (this.clicksound != null) {
				cf.setText(this.clicksound);
			}
			PopupHandler.displayPopup(cf);
		});
		this.rightclickMenu.addContent(b11);
		//TODO übernehmen
//		UIBase.colorizeButton(b11);

		AdvancedButton b12 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.clicksound.reset"), (press) -> {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
			
			this.clicksound = null;
			this.handler.setVanillaClickSound(this, null);
			this.rightclickMenu.closeMenu();
		});
		this.rightclickMenu.addContent(b12);
		//TODO übernehmen
//		UIBase.colorizeButton(b12);
		
		AdvancedButton b9 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.vanillabutton.autoclick"), (press) -> {
			//TODO übernehmen
			FMTextInputPopup pop = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.vanillabutton.autoclick.popup"), CharacterFilter.getIntegerCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (call.equals("") || !MathUtils.isInteger(call)) {
						if (this.clicks != 0) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.clicks = 0;
					} else {
						if (this.clicks != Integer.parseInt(call)) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.clicks = Integer.parseInt(call);
					}
					this.handler.setVanillaClicks(this, this.clicks);
				}
			});
			pop.setText("" + this.clicks);
			PopupHandler.displayPopup(pop);
		});
		this.rightclickMenu.addContent(b9);
		//TODO übernehmen
//		UIBase.colorizeButton(b9);

		//TODO übernehmen
//		AdvancedButton b10 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.delayappearance"), (press) -> {
//			FMTextInputPopup in = new HideForPopup(Locals.localize("helper.creator.items.button.delayappearance.desc"), CharacterFilter.getDoubleCharacterFiler(), 240, this);
//			
//			in.setText("" + this.hideforsec);
//			PopupHandler.displayPopup(in);
//		});
//		this.rightclickMenu.addContent(b10);
//		UIBase.colorizeButton(b10);

		AdvancedButton b13 = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.button.btndescription"), (press) -> {
			//TODO übernehmeen
			FMTextInputPopup in = new DynamicValueInputPopup(new Color(0, 0, 0, 0), Locals.localize("helper.creator.items.button.btndescription"), null, 240, (call) -> {
				if (call != null) {
					if ((this.description == null) || (call == null) || !this.description.equals(call)) {
						this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
					}
					this.description = call;
					if (call.equals("")) {
						this.description = null;
					}
					this.handler.setVanillaDescription(this, this.description);
				}
			});
			
			if (this.description != null) {
				in.setText(this.description);
			}
			PopupHandler.displayPopup(in);
		});
		//TODO übernehmen
		List<String> l = new ArrayList<String>();
		for (String s : StringUtils.splitLines(Locals.localize("helper.creator.items.button.btndescription.desc"), "%n%")) {
			l.add(s.replace("#n#", "%n%"));
		}
		b13.setDescription(l.toArray(new String[0]));
		//----------------------
		this.rightclickMenu.addContent(b13);
		//TODO übernehmen
//		UIBase.colorizeButton(b13);
		
	}
	
	@Override
	public void render(int mouseX, int mouseY) {
		
		if (!this.canBeModified()) {
			//Cancel dragging
			if (this.isDragged() && this.handler.isFocused(this) && ((this.startX != this.object.posX) || (this.startY != this.object.posY))) {
				this.handler.setObjectFocused(this, false, true);
				this.dragging = false;
				this.object.posX = this.button.x;
				this.object.posY = this.button.y;
				this.object.width = this.button.width;
				this.object.height = this.button.height;
				GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), normalCursor);
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
				this.handler.setObjectFocused(this, false, true);
				GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), normalCursor);
				this.displaySetOrientationNotification();
				return;
			}
		}
		
        super.render(mouseX, mouseY);
        
        //TODO übernehmen
        if (this.object.delayAppearance) {
			this.handler.vanillaDelayAppearance.put(this.button.getId(), this.object.delayAppearanceSec);
		} else {
			this.handler.vanillaDelayAppearance.remove(this.button.getId());
		}
		if (!this.object.delayAppearanceEverytime) {
			this.handler.vanillaDelayAppearanceFirstTime.put(this.button.getId(), true);
		} else {
			this.handler.vanillaDelayAppearanceFirstTime.put(this.button.getId(), false);
		}
		if (this.object.fadeIn) {
			this.handler.vanillaFadeIn.put(this.button.getId(), this.object.fadeInSpeed);
		} else {
			this.handler.vanillaFadeIn.remove(this.button.getId());
		}
		//-----------------------
		
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
		//hidebuttonfor
		//TODO übernehmen
		if (this.object.delayAppearance) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "hidebuttonfor");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("seconds", "" + this.object.delayAppearanceSec);
			s.addEntry("onlyfirsttime", "" + !this.object.delayAppearanceEverytime);
			if (this.object.fadeIn) {
				s.addEntry("fadein", "true");
				s.addEntry("fadeinspeed", "" + this.object.fadeInSpeed);
			}
			l.add(s);
		}
		//setbuttonclicksound
		if (this.clicksound != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttonclicksound");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("path", this.clicksound);
			l.add(s);
		}
		//setbuttondescription
		if (this.description != null) {
			PropertiesSection s = new PropertiesSection("customization");
			s.addEntry("action", "setbuttondescription");
			s.addEntry("identifier", "%id=" + this.button.getId() + "%");
			s.addEntry("description", this.description);
			l.add(s);
		}
		
		return l;
	}

	public void displaySetOrientationNotification() {
		if (FancyMenu.config.getOrDefault("showvanillamovewarning", true)) {
			PopupHandler.displayPopup(new VanillaButtonMovePopup(this.handler));
		}
	}
	
	private boolean canBeModified() {
		return !this.object.orientation.equals("original");
	}
	
	private void editLabelCallback(String text) {
		if (text == null) {
			return;
		} else {
			this.handler.setVanillaButtonName(this, StringUtils.convertFormatCodes(text, "&", "§"));
		}
	}

	//TODO übernehmen
//	@Override
//	public void setAppearanceDelay(String sec, boolean onlyfirsttime) {
//		if (sec != null) {
//			if (MathUtils.isDouble(sec)) {
//				double s = Double.parseDouble(sec);
//				if ((this.hideforsec != s) || (this.delayonlyfirsttime != onlyfirsttime)) {
//					this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
//				}
//				
//				this.hideforsec = s;
//				this.handler.setVanillaHideFor(this, this.hideforsec);
//				this.delayonlyfirsttime = onlyfirsttime;
//				this.handler.setVanillaDelayOnlyFirstTime(this, this.delayonlyfirsttime);
//			} else {
//				LayoutEditorScreen.displayNotification(Locals.localize("helper.creator.items.button.delayappearance.invalidvalue"));
//			}
//		}
//	}
//
//	@Override
//	public boolean isDelayedOnlyFirstTime() {
//		return this.delayonlyfirsttime;
//	}
//
//	@Override
//	public double getAppearanceDelay() {
//		return this.hideforsec;
//	}
	
	//TODO übernehmen
	@Override
	public void destroyObject() {
		if (FancyMenu.config.getOrDefault("editordeleteconfirmation", true)) {
			PopupHandler.displayPopup(new FMYesNoPopup(300, new Color(0, 0, 0, 0), 240, (call) -> {
				if (call) {
					
					this.handler.hideVanillaButton(this);
					
				}
			}, "§c§l" + Locals.localize("helper.creator.messages.sure"), "", Locals.localize("helper.creator.deleteobject"), "", "", "", "", ""));
		} else {

			this.handler.hideVanillaButton(this);
			
		}
	}
	
}
