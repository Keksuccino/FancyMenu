//package de.keksuccino.fancymenu.customization.element.v1.button;
//
//import java.io.File;
//import java.io.IOException;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import de.keksuccino.fancymenu.customization.button.ButtonData;
//import de.keksuccino.fancymenu.customization.ScreenCustomization;
//import de.keksuccino.fancymenu.customization.element.AbstractElement;
//import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
//import de.keksuccino.fancymenu.customization.loadingrequirement.internal.LoadingRequirementContainer;
//import de.keksuccino.fancymenu.customization.placeholder.v2.PlaceholderParser;
//import de.keksuccino.fancymenu.mixin.mixins.client.IMixinAbstractWidget;
//import de.keksuccino.fancymenu.rendering.ui.tooltip.Tooltip;
//import de.keksuccino.fancymenu.rendering.ui.tooltip.TooltipHandler;
//import de.keksuccino.fancymenu.utils.LocalizationUtils;
//import de.keksuccino.konkrete.math.MathUtils;
//import de.keksuccino.fancymenu.properties.PropertiesSection;
//import de.keksuccino.konkrete.sound.SoundHandler;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.network.chat.Component;
//
//public class VanillaButtonCustomizationItem extends AbstractElement {
//
//	public ButtonData parent;
//
//	private String normalLabel = "";
//	private boolean hovered = false;
//
//	public String hoverLabelRaw;
//	public String labelRaw;
//	protected boolean normalLabelCached = false;
//	public ScreenCustomizationLayer handler;
//	public LoadingRequirementContainer loadingRequirements = null;
//	public String tooltip = null;
//
//	public VanillaButtonCustomizationItem(PropertiesSection item, ButtonData parent, ScreenCustomizationLayer handler) {
//
//		super(item);
//		this.parent = parent;
//		this.handler = handler;
//
//		if ((this.elementType != null) && (this.parent != null)) {
//
//			if (this.elementType.equalsIgnoreCase("addhoversound")) {
//				this.value = fixBackslashPath(item.getEntryValue("path"));
//				if (this.value != null) {
//					File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.value));
//					if (f.exists() && f.isFile()) {
//						if (!SoundHandler.soundExists(this.value)) {
//							ScreenCustomization.registerSound(this.value, this.value);
//						}
//					} else {
//						System.out.println("################### ERROR ###################");
//						System.out.println("[FancyMenu] Soundfile '" + this.value + "'for 'addhoversound' customization action not found!");
//						System.out.println("#############################################");
//						this.value = null;
//					}
//				}
//			}
//
//			if (this.elementType.equalsIgnoreCase("sethoverlabel")) {
//				this.hoverLabelRaw = item.getEntryValue("label");
//				if (this.parent != null) {
//					this.normalLabel = this.parent.getButton().getMessage().getString();
//				}
//				this.updateValues();
//			}
//
//			if (this.elementType.equalsIgnoreCase("renamebutton") || this.elementType.equalsIgnoreCase("setbuttonlabel")) {
//				this.labelRaw = item.getEntryValue("value");
//				this.updateValues();
//			}
//
//			if (elementType.equalsIgnoreCase("movebutton")) {
//
//				String x = item.getEntryValue("x");
//				String y = item.getEntryValue("y");
//				if (x != null) {
//					x = PlaceholderParser.replacePlaceholders(x);
//					if (MathUtils.isInteger(x)) {
//						this.baseX = Integer.parseInt(x);
//					}
//				}
//				if (y != null) {
//					y = PlaceholderParser.replacePlaceholders(y);
//					if (MathUtils.isInteger(y)) {
//						this.baseY = Integer.parseInt(y);
//					}
//				}
//
//				String o = item.getEntryValue("orientation");
//				if (o != null) {
//					this.anchorPoint = o;
//				}
//
//				String oe = item.getEntryValue("orientation_element");
//				if (oe != null) {
//					this.anchorPointElementIdentifier = oe;
//				}
//
//			}
//
//			if (this.elementType.equalsIgnoreCase("setbuttondescription")) {
//				this.tooltip = item.getEntryValue("description");
//			}
//
//		}
//	}
//
//	@Override
//	public void render(PoseStack matrix, Screen menu) throws IOException {
//		if (this.parent != null) {
//
//			this.updateValues();
//
//			if (elementType.equalsIgnoreCase("vanilla_button_visibility_requirements")) {
//				if (this.loadingRequirements != null) {
//					if (!this.handler.isVanillaButtonHidden(this.parent.getButton())) {
//						this.loadingRequirementContainer = this.loadingRequirements;
//						this.parent.getButton().visible = this.loadingRequirementsMet();
//					}
//				}
//			}
//
//			if (this.elementType.equals("addhoversound")) {
//				if (this.parent.getButton().isHoveredOrFocused() && this.parent.getButton().active && !hovered && (this.value != null)) {
//					SoundHandler.resetSound(this.value);
//					SoundHandler.playSound(this.value);
//					this.hovered = true;
//				}
//				if (!this.parent.getButton().isHoveredOrFocused()) {
//					this.hovered = false;
//				}
//			}
//
//			if (this.elementType.equals("sethoverlabel")) {
//				if (this.value != null) {
//					this.parent.hasHoverLabel = true;
//					if (this.parent.getButton().isHoveredOrFocused() && this.parent.getButton().active) {
//						if (!this.normalLabelCached) {
//							this.normalLabelCached = true;
//							this.normalLabel = this.parent.getButton().getMessage().getString();
//						}
//						this.parent.getButton().setMessage(Component.literal(this.value));
//					} else {
//						if (this.normalLabelCached) {
//							this.normalLabelCached = false;
//							this.parent.getButton().setMessage(Component.literal(this.normalLabel));
//						}
//					}
//				}
//			}
//
//			if (this.elementType.equalsIgnoreCase("renamebutton") || this.elementType.equalsIgnoreCase("setbuttonlabel")) {
//				if (this.value != null) {
//					if (!this.parent.getButton().isHoveredOrFocused() || !this.parent.hasHoverLabel) {
//						this.parent.getButton().setMessage(Component.literal(this.value));
//					}
//				}
//			}
//
//			if (elementType.equalsIgnoreCase("movebutton")) {
//				this.parent.getButton().x = this.getX(menu);
//				this.parent.getButton().y = this.getY(menu);
//			}
//
//			if (elementType.equalsIgnoreCase("resizebutton")) {
//				this.parent.getButton().setWidth(this.getWidth());
//				((IMixinAbstractWidget)this.parent.getButton()).setHeightFancyMenu(this.getHeight());
//			}
//
//		}
//	}
//
//	protected void updateValues() {
//
//		if (this.elementType.equalsIgnoreCase("setbuttondescription") && (this.tooltip != null)) {
//			if (this.parent.getButton().isHoveredOrFocused()) {
//				TooltipHandler.INSTANCE.addWidgetTooltip(this.parent.getButton(), Tooltip.create(LocalizationUtils.splitLocalizedStringLines(PlaceholderParser.replacePlaceholders(this.tooltip))), false, true);
//			}
//		}
//
//		if (this.elementType.equalsIgnoreCase("renamebutton") || this.elementType.equalsIgnoreCase("setbuttonlabel")) {
//			if (this.labelRaw != null) {
//				if (!isEditor()) {
//					this.value = PlaceholderParser.replacePlaceholders(this.labelRaw);
//				} else {
//					this.value = StringUtils.convertFormatCodes(this.labelRaw, "&", "§");
//				}
//			}
//		}
//
//		if (this.elementType.equals("sethoverlabel")) {
//			if (this.hoverLabelRaw != null) {
//				if (!isEditor()) {
//					this.value = PlaceholderParser.replacePlaceholders(this.hoverLabelRaw);
//				} else {
//					this.value = StringUtils.convertFormatCodes(this.hoverLabelRaw, "&", "§");
//				}
//			}
//		}
//
//	}
//
//	public String getButtonId() {
//		if (this.parent.getCompatibilityId() != null) {
//			return this.parent.getCompatibilityId();
//		}
//		return "" + this.parent.getId();
//	}
//
//}
