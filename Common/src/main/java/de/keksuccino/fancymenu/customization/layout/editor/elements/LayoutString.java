package de.keksuccino.fancymenu.customization.layout.editor.elements;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.element.editor.AbstractEditorElement;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.customization.frontend.PlaceholderInputPopup;
import de.keksuccino.konkrete.localization.Locals;
import de.keksuccino.fancymenu.rendering.ui.contextmenu.ContextMenu;
import de.keksuccino.fancymenu.rendering.ui.popup.FMTextInputPopup;
import de.keksuccino.fancymenu.customization.element.AbstractElement.Alignment;
import de.keksuccino.fancymenu.customization.element.v1.StringCustomizationItem;
import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.gui.screens.popup.PopupHandler;
import de.keksuccino.konkrete.input.CharacterFilter;
import de.keksuccino.konkrete.input.StringUtils;
import de.keksuccino.konkrete.math.MathUtils;
import de.keksuccino.konkrete.properties.PropertiesSection;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

@Deprecated
public class LayoutString extends AbstractEditorElement {

	protected AdvancedButton alignmentLeftBtn;
	protected AdvancedButton alignmentRightBtn;
	protected AdvancedButton alignmentCenteredBtn;

	@Deprecated
	public LayoutString(StringCustomizationItem parent, LayoutEditorScreen handler) {
		super(parent, true, handler);
		this.setScale(this.getStringScale());
	}
	
	@Override
	public void init() {
		super.init();
		
		AdvancedButton scaleB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.string.setscale"), true, (press) -> {
			PopupHandler.displayPopup(new FMTextInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.string.setscale") + ":", CharacterFilter.getDoubleCharacterFiler(), 240, this::setScaleCallback));
		});
		this.rightClickContextMenu.addContent(scaleB);
		
		String sLabel = Locals.localize("helper.creator.items.string.setshadow");
		if (this.getObject().shadow) {
			sLabel = Locals.localize("helper.creator.items.string.setnoshadow");
		}
		AdvancedButton shadowB = new AdvancedButton(0, 0, 0, 16, sLabel, true, (press) -> {
			if (this.getObject().shadow) {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.string.setshadow"));
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				
				this.getObject().shadow = false;
			} else {
				((AdvancedButton)press).setMessage(Locals.localize("helper.creator.items.string.setnoshadow"));
				this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
				
				this.getObject().shadow = true;
			}
		});
		this.rightClickContextMenu.addContent(shadowB);

		ContextMenu alignmentMenu = new ContextMenu();
		alignmentMenu.setAutoclose(true);
		this.rightClickContextMenu.addChild(alignmentMenu);

		String al = Locals.localize("helper.creator.items.string.alignment.left");
		if (this.getObject().alignment == Alignment.LEFT) {
			al = "§a" + al;
		}
		alignmentLeftBtn = new AdvancedButton(0, 0, 0, 0, al, true, (press) -> {
			this.getObject().alignment = Alignment.LEFT;
			((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.items.string.alignment.left"));
			alignmentRightBtn.setMessage(Locals.localize("helper.creator.items.string.alignment.right"));
			alignmentCenteredBtn.setMessage(Locals.localize("helper.creator.items.string.alignment.centered"));
		});
		alignmentMenu.addContent(alignmentLeftBtn);

		String ar = Locals.localize("helper.creator.items.string.alignment.right");
		if (this.getObject().alignment == Alignment.RIGHT) {
			ar = "§a" + ar;
		}
		alignmentRightBtn = new AdvancedButton(0, 0, 0, 0, ar, true, (press) -> {
			this.getObject().alignment = Alignment.RIGHT;
			((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.items.string.alignment.right"));
			alignmentLeftBtn.setMessage(Locals.localize("helper.creator.items.string.alignment.left"));
			alignmentCenteredBtn.setMessage(Locals.localize("helper.creator.items.string.alignment.centered"));
		});
		alignmentMenu.addContent(alignmentRightBtn);

		String ac = Locals.localize("helper.creator.items.string.alignment.centered");
		if (this.getObject().alignment == Alignment.CENTERED) {
			ac = "§a" + ac;
		}
		alignmentCenteredBtn = new AdvancedButton(0, 0, 0, 0, ac, true, (press) -> {
			this.getObject().alignment = Alignment.CENTERED;
			((AdvancedButton)press).setMessage("§a" + Locals.localize("helper.creator.items.string.alignment.centered"));
			alignmentRightBtn.setMessage(Locals.localize("helper.creator.items.string.alignment.right"));
			alignmentLeftBtn.setMessage(Locals.localize("helper.creator.items.string.alignment.left"));
		});
		alignmentMenu.addContent(alignmentCenteredBtn);

		AdvancedButton alignmentBtn = new AdvancedButton(0, 0, 0, 0, Locals.localize("helper.creator.items.string.alignment"), true, (press) -> {
			alignmentMenu.setParentButton((AdvancedButton) press);
			alignmentMenu.openMenuAt(0, press.y);
		});
		alignmentBtn.setDescription(StringUtils.splitLines(Locals.localize("helper.creator.items.string.alignment.desc"), "%n%"));
		this.rightClickContextMenu.addContent(alignmentBtn);
		
		AdvancedButton editTextB = new AdvancedButton(0, 0, 0, 16, Locals.localize("helper.creator.items.string.edit"), true, (press) -> {
			PlaceholderInputPopup i = new PlaceholderInputPopup(new Color(0, 0, 0, 0), "§l" + Locals.localize("helper.creator.items.string.edit") + ":", null, 240, this::setTextCallback);
			i.setText(StringUtils.convertFormatCodes(this.element.value, "§", "&"));
			PopupHandler.displayPopup(i);
		});
		this.rightClickContextMenu.addContent(editTextB);
		
	}

	@Override
	protected void renderBorder(PoseStack matrix, int mouseX, int mouseY) {
		//horizontal line top
		fill(matrix, this.getStringPosX(), this.getStringPosY(), this.getStringPosX() + this.element.getWidth(), this.getStringPosY() + 1, Color.BLUE.getRGB());
		//horizontal line bottom
		fill(matrix, this.getStringPosX(), this.getStringPosY() + this.element.getHeight(), this.getStringPosX() + this.element.getWidth() + 1, this.getStringPosY() + this.element.getHeight() + 1, Color.BLUE.getRGB());
		//vertical line left
		fill(matrix, this.getStringPosX(), this.getStringPosY(), this.getStringPosX() + 1, this.getStringPosY() + this.element.getHeight(), Color.BLUE.getRGB());
		//vertical line right
		fill(matrix, this.getStringPosX() + this.element.getWidth(), this.getStringPosY(), this.getStringPosX() + this.element.getWidth() + 1, this.getStringPosY() + this.element.getHeight(), Color.BLUE.getRGB());
	
		//Render pos and size values
		Font font = Minecraft.getInstance().font;
		RenderUtils.setScale(matrix, 0.5F);
		font.draw(matrix, Locals.localize("helper.creator.items.border.orientation")+ ": " + this.element.anchorPoint, this.getStringPosX()*2, (this.getStringPosY()*2) - 44, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.string.border.scale") + ": " + this.getStringScale(), this.getStringPosX()*2, (this.getStringPosY()*2) - 35, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.string.border.alignment") + ": " + this.getObject().alignment.key, this.getStringPosX()*2, (this.getStringPosY()*2) - 26, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.border.posx") + ": " + this.getStringPosX(), this.getStringPosX()*2, (this.getStringPosY()*2) - 17, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.border.width") + ": " + this.element.getWidth(), this.getStringPosX()*2, (this.getStringPosY()*2) - 8, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.border.posy") + ": " + this.getStringPosY(), ((this.getStringPosX() + this.element.getWidth())*2)+3, ((this.getStringPosY() + this.element.getHeight())*2) - 14, Color.WHITE.getRGB());
		font.draw(matrix, Locals.localize("helper.creator.items.border.height") + ": " + this.element.getHeight(), ((this.getStringPosX() + this.element.getWidth())*2)+3, ((this.getStringPosY() + this.element.getHeight())*2) - 5, Color.WHITE.getRGB());
		RenderUtils.postScale(matrix);
	}

	@Override
	protected void renderHighlightBorder(PoseStack matrix) {
		Color c = new Color(0, 200, 255, 255);
		
		//horizontal line top
		fill(matrix, this.getStringPosX(), this.getStringPosY(), this.getStringPosX() + this.element.getWidth(), this.getStringPosY() + 1, c.getRGB());
		//horizontal line bottom
		fill(matrix, this.getStringPosX(), this.getStringPosY() + this.element.getHeight(), this.getStringPosX() + this.element.getWidth() + 1, this.getStringPosY() + this.element.getHeight() + 1, c.getRGB());
		//vertical line left
		fill(matrix, this.getStringPosX(), this.getStringPosY(), this.getStringPosX() + 1, this.getStringPosY() + this.element.getHeight(), c.getRGB());
		//vertical line right
		fill(matrix, this.getStringPosX() + this.element.getWidth(), this.getStringPosY(), this.getStringPosX() + this.element.getWidth() + 1, this.getStringPosY() + this.element.getHeight(), c.getRGB());
	}
	
	private int getStringPosX() {
		return (int)(this.element.getX(this.editor) * this.getStringScale());
	}
	
	private int getStringPosY() {
		return (int)(this.element.getY(this.editor) * this.getStringScale());
	}
	
	private float getStringScale() {
		return ((StringCustomizationItem)this.element).scale;
	}
	
	private StringCustomizationItem getObject() {
		return ((StringCustomizationItem)this.element);
	}
	
	@Override
	public boolean isGrabberPressed() {
		return false;
	}
	
	@Override
	public int getActiveResizeGrabber() {
		return -1;
	}

	@Override
	protected void setAnchorPoint(String pos) {
		super.setAnchorPoint(pos);
		if (this.getObject().alignment == Alignment.CENTERED) {
			if (this.element.anchorPoint.endsWith("-right")) {
				this.element.baseX += this.element.getWidth();
			}
			if (this.element.anchorPoint.endsWith("-centered")) {
				this.element.baseX += this.element.getWidth() / 2;
			}
		} else if (this.getObject().alignment == Alignment.RIGHT) {
			if (this.element.anchorPoint.endsWith("-right")) {
				this.element.baseX += this.element.getWidth();
			}
			if (this.element.anchorPoint.endsWith("-left")) {
				this.element.baseX += this.element.getWidth();
			}
			if (this.element.anchorPoint.endsWith("-centered")) {
				this.element.baseX += this.element.getWidth() / 2;
			}
		} else if (this.getObject().alignment == Alignment.LEFT) {
			if (this.element.anchorPoint.endsWith("-centered")) {
				this.element.baseX += this.element.getWidth() / 2;
			}
		}
	}
	
	public void setScale(float scale) {
		if (this.getObject().scale != scale) {
			this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
		}
		
		((StringCustomizationItem)this.element).scale = scale;
		this.setWidth((int)(Minecraft.getInstance().font.width(this.element.value)*scale));
		this.setHeight((int)(7*scale));
	}

	public void setText(String text) {
		if (!this.getObject().valueRaw.equals(text)) {
			this.editor.history.saveSnapshot(this.editor.history.createSnapshot());
		}

		this.getObject().valueRaw = text;
		this.getObject().value = text;
		this.setScale(this.getStringScale());
	}
	
	private void setTextCallback(String text) {
		if (text == null) {
			return;
		}
		if (text.length() > 0) {
			this.setText(StringUtils.convertFormatCodes(text, "&", "§"));
		} else {
			LayoutEditorScreen.displayNotification("§c§l" + Locals.localize("helper.creator.texttooshort.title"), "", Locals.localize("helper.creator.texttooshort.desc"), "", "", "", "");
		}
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
	protected void updateHovered(int mouseX, int mouseY) {
		if ((mouseX >= this.getStringPosX()) && (mouseX <= this.getStringPosX() + this.element.getWidth()) && (mouseY >= this.getStringPosY()) && mouseY <= this.getStringPosY() + this.element.getHeight()) {
			this.hovered = true;
		} else {
			this.hovered = false;
		}
	}
	
	@Override
	public List<PropertiesSection> getProperties() {
		List<PropertiesSection> l = new ArrayList<PropertiesSection>();
		
		PropertiesSection p1 = new PropertiesSection("customization");
		p1.addEntry("action", "addtext");
		p1.addEntry("actionid", this.element.getInstanceIdentifier());
		if (this.element.delayAppearance) {
			p1.addEntry("delayappearance", "true");
			p1.addEntry("delayappearanceeverytime", "" + this.element.delayAppearanceEverytime);
			p1.addEntry("delayappearanceseconds", "" + this.element.delayAppearanceSec);
			if (this.element.fadeIn) {
				p1.addEntry("fadein", "true");
				p1.addEntry("fadeinspeed", "" + this.element.fadeInSpeed);
			}
		}
		p1.addEntry("value", this.element.value);
		p1.addEntry("x", "" + this.element.baseX);
		p1.addEntry("y", "" + this.element.baseY);
		p1.addEntry("orientation", this.element.anchorPoint);
		if (this.element.anchorPoint.equals("element") && (this.element.anchorPointElementIdentifier != null)) {
			p1.addEntry("orientation_element", this.element.anchorPointElementIdentifier);
		}
		p1.addEntry("scale", "" + this.getObject().scale);
		p1.addEntry("shadow", "" + this.getObject().shadow);
		p1.addEntry("alignment", "" + this.getObject().alignment.key);

		this.serializeLoadingRequirementsTo(p1);

		l.add(p1);
		
		return l;
	}

}
