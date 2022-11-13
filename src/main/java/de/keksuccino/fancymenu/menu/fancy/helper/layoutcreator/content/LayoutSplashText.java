package de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.content;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.menu.fancy.item.SplashTextCustomizationItem;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;

public class LayoutSplashText extends LayoutElement {
	
	public LayoutSplashText(SplashTextCustomizationItem parent, LayoutEditorScreen handler) {
		super(parent, true, handler);
	}
	
	@Override
	public void init() {
		super.init();
		
		/** SCALE **/
		AdvancedButton scaleButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.string.setscale"), true, (press) -> {
			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.string.setscale") + ":", CharacterFilter.getDoubleCharacterFiler(), 240, this::setScaleCallback);
			p.setText("" + this.getObject().scale);
			PopupHandler.displayPopup(p);
		});
		this.rightclickMenu.addContent(scaleButton);
		
		/** ROTATION **/
		AdvancedButton rotationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.splash.rotation"), true, (press) -> {
			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.splash.rotation") + ":", CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
				if (call != null) {
					if (MathUtils.isFloat(call)) {
						this.getObject().rotation = Float.parseFloat(call);
					}
				}
			});
			p.setText("" + this.getObject().rotation);
			PopupHandler.displayPopup(p);
		});
		this.rightclickMenu.addContent(rotationButton);
		
		/** BASE COLOR **/
		AdvancedButton colorButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.splash.basecolor"), true, (press) -> {
			FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.splash.basecolor") + ":", null, 240, (call) -> {
				if (call != null) {
					if (!call.equals("")) {
						Color c = RenderUtils.getColorFromHexString(call);
						if (c != null) {
							
							if (!this.getObject().basecolorString.equalsIgnoreCase(call)) {
								this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
							}
							
							this.getObject().basecolor = c;
							this.getObject().basecolorString = call;
							
						}
					} else {
						if (!this.getObject().basecolorString.equalsIgnoreCase("#ffff00")) {
							this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
						}
						
						this.getObject().basecolorString = "#ffff00";
						this.getObject().basecolor = new Color(255, 255, 0);
					}
				}

			});
			t.setText(this.getObject().basecolorString);
			
			PopupHandler.displayPopup(t);
		});
		this.rightclickMenu.addContent(colorButton);
		
		/** SHADOW **/
		String shadowLabel = Locals.localize("helper.creator.items.string.setshadow");
		if (this.getObject().shadow) {
			shadowLabel = Locals.localize("helper.creator.items.string.setnoshadow");
		}
		AdvancedButton shadowButton = new AdvancedButton(0, 0, 0, 0, shadowLabel, true, (press) -> {
			if (this.getObject().shadow) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.string.setshadow"));
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				
				this.getObject().shadow = false;
			} else {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.string.setnoshadow"));
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				
				this.getObject().shadow = true;
			}
		});
		this.rightclickMenu.addContent(shadowButton);
		
		/** BOUNCING **/
		String bounceLabel = Locals.localize("helper.creator.items.splash.bounce.off");
		if (this.getObject().bounce) {
			bounceLabel = Locals.localize("helper.creator.items.splash.bounce.on");
		}
		AdvancedButton bounceButton = new AdvancedButton(0, 0, 0, 0, bounceLabel, true, (press) -> {
			if (this.getObject().bounce) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.splash.bounce.off"));
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				
				this.getObject().bounce = false;
			} else {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.splash.bounce.on"));
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				
				this.getObject().bounce = true;
			}
		});
		this.rightclickMenu.addContent(bounceButton);
		
		/** REFRESH ON MENU RELOAD **/
		String refreshLabel = Locals.localize("helper.creator.items.splash.refresh.off");
		if (this.getObject().refreshOnMenuReload) {
			refreshLabel = Locals.localize("helper.creator.items.splash.refresh.on");
		}
		AdvancedButton refreshButton = new AdvancedButton(0, 0, 0, 0, refreshLabel, true, (press) -> {
			if (this.getObject().refreshOnMenuReload) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.splash.refresh.off"));
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				
				this.getObject().refreshOnMenuReload = false;
			} else {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.splash.refresh.on"));
				this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
				
				this.getObject().refreshOnMenuReload = true;
			}
		});
		refreshButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.splash.refresh.desc"), "%n%"));
		if (this.getObject().text == null) {
			this.rightclickMenu.addContent(refreshButton);
		}
		
	}
	
	protected float getScale() {
		return this.getObject().scale;
	}
	
	private SplashTextCustomizationItem getObject() {
		return ((SplashTextCustomizationItem)this.object);
	}
	
	@Override
	public boolean isGrabberPressed() {
		return false;
	}
	
	@Override
	public int getActiveResizeGrabber() {
		return -1;
	}
	
	public void setScale(float scale) {
		if (this.getObject().scale != scale) {
			this.handler.history.saveSnapshot(this.handler.history.createSnapshot());
		}
		
		this.getObject().scale = scale;
		this.setWidth((int)(Minecraft.getInstance().font.width(this.object.value)*scale));
		this.setHeight((int)(7*scale));
	}
	
	private void setScaleCallback(String scale) {
		if (scale == null) {
			return;
		}
		if (MathUtils.isFloat(scale)) {
			this.setScale(Float.valueOf(scale));
		} else {
			LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.items.string.scale.invalidvalue.title"), "", Locals.localize("helper.creator.items.string.scale.invalidvalue.desc"), "", "", "", "", "");
		}
	}
	
	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		PropertiesSection p1 = new PropertiesSection("customization");
		p1.addEntry("actionid", this.object.getActionId());
		if (this.object.delayAppearance) {
			p1.addEntry("delayappearance", "true");
			p1.addEntry("delayappearanceeverytime", "" + this.object.delayAppearanceEverytime);
			p1.addEntry("delayappearanceseconds", "" + this.object.delayAppearanceSec);
			if (this.object.fadeIn) {
				p1.addEntry("fadein", "true");
				p1.addEntry("fadeinspeed", "" + this.object.fadeInSpeed);
			}
		}
		p1.addEntry("action", "addsplash");
		if (this.getObject().text != null) {
			p1.addEntry("text", this.getObject().text);
		}
		if (this.getObject().splashfile != null) {
			File home = Minecraft.getInstance().gameDirectory;
			String path = this.getObject().splashfile.getAbsolutePath().replace("\\", "/");
			if (path.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
				path = path.replace(home.getAbsolutePath().replace("\\", "/"), "");
				if (path.startsWith("\\") || path.startsWith("/")) {
					path = path.substring(1);
				}
			}
			p1.addEntry("splashfilepath", path);
		}
		p1.addEntry("x", "" + this.object.posX);
		p1.addEntry("y", "" + this.object.posY);
		p1.addEntry("orientation", this.object.orientation);
		if (this.object.orientation.equals("element") && (this.object.orientationElementIdentifier != null)) {
			p1.addEntry("orientation_element", this.object.orientationElementIdentifier);
		}
		p1.addEntry("scale", "" + this.getObject().scale);
		p1.addEntry("shadow", "" + this.getObject().shadow);
		p1.addEntry("rotation", "" + this.getObject().rotation);
		p1.addEntry("basecolor", this.getObject().basecolorString);
		p1.addEntry("refresh", "" + this.getObject().refreshOnMenuReload);
		p1.addEntry("bouncing", "" + this.getObject().bounce);
		p1.addEntry("vanilla-like", "" + this.getObject().vanillaLike);

		this.addVisibilityPropertiesTo(p1);
		
		l.add(p1);
		
		return l;
	}

	@Override
	protected void renderBorder(PoseStack matrix, int mouseX, int mouseY) {
		//horizontal line top
		GuiComponent.fill(matrix, this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + this.object.getWidth(), this.object.getPosY(handler) + 1, Color.BLUE.getRGB());
		//horizontal line bottom
		GuiComponent.fill(matrix, this.object.getPosX(handler), this.object.getPosY(handler) + this.object.getHeight() - 1, this.object.getPosX(handler) + this.object.getWidth(), this.object.getPosY(handler) + this.object.getHeight(), Color.BLUE.getRGB());
		//vertical line left
		GuiComponent.fill(matrix, this.object.getPosX(handler), this.object.getPosY(handler), this.object.getPosX(handler) + 1, this.object.getPosY(handler) + this.object.getHeight(), Color.BLUE.getRGB());
		//vertical line right
		GuiComponent.fill(matrix, this.object.getPosX(handler) + this.object.getWidth() - 1, this.object.getPosY(handler), this.object.getPosX(handler) + this.object.getWidth(), this.object.getPosY(handler) + this.object.getHeight(), Color.BLUE.getRGB());
		
		//Render pos and size values
		RenderUtils.setScale(matrix, 0.5F);
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.orientation") + ": " + this.object.orientation, this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 26, Color.WHITE.getRGB());
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posx") + ": " + this.object.getPosX(handler), this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 17, Color.WHITE.getRGB());
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.width") + ": " + this.object.getWidth(), this.object.getPosX(handler)*2, (this.object.getPosY(handler)*2) - 8, Color.WHITE.getRGB());
		
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posy") + ": " + this.object.getPosY(handler), ((this.object.getPosX(handler) + this.object.getWidth())*2)+3, ((this.object.getPosY(handler) + this.object.getHeight())*2) - 14, Color.WHITE.getRGB());
		GuiComponent.drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.height") + ": " + this.object.getHeight(), ((this.object.getPosX(handler) + this.object.getWidth())*2)+3, ((this.object.getPosY(handler) + this.object.getHeight())*2) - 5, Color.WHITE.getRGB());
		RenderUtils.postScale(matrix);
	}

}
