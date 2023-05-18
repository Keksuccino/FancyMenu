//package de.keksuccino.fancymenu.customization.layout.editor.elements;
//
//import java.awt.Color;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
//import de.keksuccino.konkrete.localization.Locals;
//import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
//import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
//import de.keksuccino.fancymenu.customization.element.v1.SplashTextCustomizationItem;
//import de.keksuccino.konkrete.gui.content.AdvancedButton;
//import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
//import de.keksuccino.konkrete.input.CharacterFilter;
//import de.keksuccino.konkrete.input.StringUtils;
//import de.keksuccino.konkrete.math.MathUtils;
//import de.keksuccino.fancymenu.properties.PropertyContainer;
//import de.keksuccino.konkrete.rendering.RenderUtils;
//import net.minecraft.client.Minecraft;
//
//public class LayoutSplashText extends AbstractEditorElement {
//
//	public LayoutSplashText(SplashTextCustomizationItem parent, LayoutEditorScreen handler) {
//		super(parent, true, handler);
//	}
//
//	@Override
//	public void init() {
//		super.init();
//
//		/** SCALE **/
//		AdvancedButton scaleButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.string.setscale"), true, (press) -> {
//			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.string.setscale") + ":", CharacterFilter.getDoubleCharacterFiler(), 240, this::setScaleCallback);
//			p.setText("" + this.getObject().scale);
//			PopupHandler.displayPopup(p);
//		});
//		this.rightClickContextMenu.addContent(scaleButton);
//
//		/** ROTATION **/
//		AdvancedButton rotationButton = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.splash.rotation"), true, (press) -> {
//			FMTextInputPopup p = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.splash.rotation") + ":", CharacterFilter.getDoubleCharacterFiler(), 240, (call) -> {
//				if (call != null) {
//					if (MathUtils.isFloat(call)) {
//						this.getObject().rotation = Float.parseFloat(call);
//					}
//				}
//			});
//			p.setText("" + this.getObject().rotation);
//			PopupHandler.displayPopup(p);
//		});
//		this.rightClickContextMenu.addContent(rotationButton);
//
//		/** BASE COLOR **/
//		AdvancedButton colorButton = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.splash.basecolor"), true, (press) -> {
//			FMTextInputPopup t = new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.splash.basecolor") + ":", null, 240, (call) -> {
//				if (call != null) {
//					if (!call.equals("")) {
//						Color c = RenderUtils.getColorFromHexString(call);
//						if (c != null) {
//
//							if (!this.getObject().basecolorString.equalsIgnoreCase(call)) {
//								this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//							}
//
//							this.getObject().basecolor = c;
//							this.getObject().basecolorString = call;
//
//						}
//					} else {
//						if (!this.getObject().basecolorString.equalsIgnoreCase("#ffff00")) {
//							this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//						}
//
//						this.getObject().basecolorString = "#ffff00";
//						this.getObject().basecolor = new Color(255, 255, 0);
//					}
//				}
//
//			});
//			t.setText(this.getObject().basecolorString);
//
//			PopupHandler.displayPopup(t);
//		});
//		this.rightClickContextMenu.addContent(colorButton);
//
//		/** SHADOW **/
//		String shadowLabel = Locals.localize("helper.creator.items.string.setshadow");
//		if (this.getObject().shadow) {
//			shadowLabel = Locals.localize("helper.creator.items.string.setnoshadow");
//		}
//		AdvancedButton shadowButton = new AdvancedButton(0, 0, 0, 0, shadowLabel, true, (press) -> {
//			if (this.getObject().shadow) {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.string.setshadow"));
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//				this.getObject().shadow = false;
//			} else {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.string.setnoshadow"));
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//				this.getObject().shadow = true;
//			}
//		});
//		this.rightClickContextMenu.addContent(shadowButton);
//
//		/** BOUNCING **/
//		String bounceLabel = Locals.localize("helper.creator.items.splash.bounce.off");
//		if (this.getObject().bounce) {
//			bounceLabel = Locals.localize("helper.creator.items.splash.bounce.on");
//		}
//		AdvancedButton bounceButton = new AdvancedButton(0, 0, 0, 0, bounceLabel, true, (press) -> {
//			if (this.getObject().bounce) {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.splash.bounce.off"));
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//				this.getObject().bounce = false;
//			} else {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.splash.bounce.on"));
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//				this.getObject().bounce = true;
//			}
//		});
//		this.rightClickContextMenu.addContent(bounceButton);
//
//		/** REFRESH ON MENU RELOAD **/
//		String refreshLabel = Locals.localize("helper.creator.items.splash.refresh.off");
//		if (this.getObject().refreshOnMenuReload) {
//			refreshLabel = Locals.localize("helper.creator.items.splash.refresh.on");
//		}
//		AdvancedButton refreshButton = new AdvancedButton(0, 0, 0, 0, refreshLabel, true, (press) -> {
//			if (this.getObject().refreshOnMenuReload) {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.splash.refresh.off"));
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//				this.getObject().refreshOnMenuReload = false;
//			} else {
//				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.splash.refresh.on"));
//				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//
//				this.getObject().refreshOnMenuReload = true;
//			}
//		});
//		refreshButton.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.splash.refresh.desc"), "%n%"));
//		if (this.getObject().text == null) {
//			this.rightClickContextMenu.addContent(refreshButton);
//		}
//
//	}
//
//	protected float getScale() {
//		return this.getObject().scale;
//	}
//
//	private SplashTextCustomizationItem getObject() {
//		return ((SplashTextCustomizationItem)this.element);
//	}
//
//	@Override
//	public boolean isGrabberPressed() {
//		return false;
//	}
//
//	@Override
//	public int getActiveResizeGrabber() {
//		return -1;
//	}
//
//	public void setScale(float scale) {
//		if (this.getObject().scale != scale) {
//			this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
//		}
//
//		this.getObject().scale = scale;
//		this.setWidth((int)(Minecraft.getInstance().font.width(this.element.value)*scale));
//		this.setHeight((int)(7*scale));
//	}
//
//	private void setScaleCallback(String scale) {
//		if (scale == null) {
//			return;
//		}
//		if (MathUtils.isFloat(scale)) {
//			this.setScale(Float.valueOf(scale));
//		} else {
//			LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.items.string.scale.invalidvalue.title"), "", Locals.localize("helper.creator.items.string.scale.invalidvalue.desc"), "", "", "", "", "");
//		}
//	}
//
//	@Override
//	public List<PropertyContainer> getProperties() {
//		List<PropertyContainer> l = new ArrayList<PropertyContainer>();
//
//		PropertyContainer p1 = new PropertyContainer("customization");
//		p1.putProperty("actionid", this.element.getInstanceIdentifier());
//		if (this.element.advancedX != null) {
//			p1.putProperty("advanced_posx", this.element.advancedX);
//		}
//		if (this.element.advancedY != null) {
//			p1.putProperty("advanced_posy", this.element.advancedY);
//		}
//		if (this.element.advancedWidth != null) {
//			p1.putProperty("advanced_width", this.element.advancedWidth);
//		}
//		if (this.element.advancedHeight != null) {
//			p1.putProperty("advanced_height", this.element.advancedHeight);
//		}
//		if (this.element.delayAppearance) {
//			p1.putProperty("delayappearance", "true");
//			p1.putProperty("delayappearanceeverytime", "" + this.element.delayAppearanceEverytime);
//			p1.putProperty("delayappearanceseconds", "" + this.element.appearanceDelayInSeconds);
//			if (this.element.fadeIn) {
//				p1.putProperty("fadein", "true");
//				p1.putProperty("fadeinspeed", "" + this.element.fadeInSpeed);
//			}
//		}
//		p1.putProperty("action", "addsplash");
//		if (this.getObject().text != null) {
//			p1.putProperty("text", this.getObject().text);
//		}
//		if (this.getObject().splashfile != null) {
//			File home = Minecraft.getInstance().gameDirectory;
//			String path = this.getObject().splashfile.getAbsolutePath().replace("\\", "/");
//			if (path.startsWith(home.getAbsolutePath().replace("\\", "/"))) {
//				path = path.replace(home.getAbsolutePath().replace("\\", "/"), "");
//				if (path.startsWith("\\") || path.startsWith("/")) {
//					path = path.substring(1);
//				}
//			}
//			p1.putProperty("splashfilepath", path);
//		}
//		p1.putProperty("x", "" + this.element.baseX);
//		p1.putProperty("y", "" + this.element.baseY);
//		p1.putProperty("orientation", this.element.anchorPoint);
//		if (this.element.anchorPoint.equals("element") && (this.element.anchorPointElementIdentifier != null)) {
//			p1.putProperty("orientation_element", this.element.anchorPointElementIdentifier);
//		}
//		p1.putProperty("scale", "" + this.getObject().scale);
//		p1.putProperty("shadow", "" + this.getObject().shadow);
//		p1.putProperty("rotation", "" + this.getObject().rotation);
//		p1.putProperty("basecolor", this.getObject().basecolorString);
//		p1.putProperty("refresh", "" + this.getObject().refreshOnMenuReload);
//		p1.putProperty("bouncing", "" + this.getObject().bounce);
//		p1.putProperty("vanilla-like", "" + this.getObject().vanillaLike);
//
//		this.serializeLoadingRequirementsTo(p1);
//
//		l.add(p1);
//
//		return l;
//	}
//
//	@Override
//	protected void renderBorder(PoseStack matrix, int mouseX, int mouseY) {
//		//horizontal line top
//		fill(matrix, this.element.getX(editor), this.element.getY(editor), this.element.getX(editor) + this.element.getWidth(), this.element.getY(editor) + 1, Color.BLUE.getRGB());
//		//horizontal line bottom
//		fill(matrix, this.element.getX(editor), this.element.getY(editor) + this.element.getHeight() - 1, this.element.getX(editor) + this.element.getWidth(), this.element.getY(editor) + this.element.getHeight(), Color.BLUE.getRGB());
//		//vertical line left
//		fill(matrix, this.element.getX(editor), this.element.getY(editor), this.element.getX(editor) + 1, this.element.getY(editor) + this.element.getHeight(), Color.BLUE.getRGB());
//		//vertical line right
//		fill(matrix, this.element.getX(editor) + this.element.getWidth() - 1, this.element.getY(editor), this.element.getX(editor) + this.element.getWidth(), this.element.getY(editor) + this.element.getHeight(), Color.BLUE.getRGB());
//
//		//Render pos and size values
//		RenderUtils.setScale(matrix, 0.5F);
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.orientation") + ": " + this.element.anchorPoint, this.element.getX(editor)*2, (this.element.getY(editor)*2) - 26, Color.WHITE.getRGB());
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posx") + ": " + this.element.getX(editor), this.element.getX(editor)*2, (this.element.getY(editor)*2) - 17, Color.WHITE.getRGB());
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.width") + ": " + this.element.getWidth(), this.element.getX(editor)*2, (this.element.getY(editor)*2) - 8, Color.WHITE.getRGB());
//
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.posy") + ": " + this.element.getY(editor), ((this.element.getX(editor) + this.element.getWidth())*2)+3, ((this.element.getY(editor) + this.element.getHeight())*2) - 14, Color.WHITE.getRGB());
//		drawString(matrix, Minecraft.getInstance().font, Locals.localize("helper.creator.items.border.height") + ": " + this.element.getHeight(), ((this.element.getX(editor) + this.element.getWidth())*2)+3, ((this.element.getY(editor) + this.element.getHeight())*2) - 5, Color.WHITE.getRGB());
//		RenderUtils.postScale(matrix);
//	}
//
//}
