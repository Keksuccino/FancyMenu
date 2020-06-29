package de.keksuccino.fancymenu.menu.fancy.helper;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.gui.content.HorizontalSwitcher;
import de.keksuccino.core.gui.screens.popup.NotificationPopup;
import de.keksuccino.core.gui.screens.popup.Popup;
import de.keksuccino.core.gui.screens.popup.PopupHandler;
import de.keksuccino.core.properties.PropertiesSection;
import de.keksuccino.core.properties.PropertiesSet;
import de.keksuccino.core.rendering.animation.IAnimationRenderer;
import de.keksuccino.fancymenu.localization.Locals;
import de.keksuccino.fancymenu.menu.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.menu.animation.AnimationHandler;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutCreatorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.PreloadedLayoutCreatorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class EditLayoutPopup extends Popup {

	protected Map<String, List<Object>> props = new HashMap<String, List<Object>>();
	protected List<PropertiesSet> fullprops = new ArrayList<PropertiesSet>();
	
	private List<String> msg = new ArrayList<String>();

	protected AdvancedButton loadAllButton;
	protected AdvancedButton loadLayoutButton;
	protected AdvancedButton cancelButton;
	
	protected HorizontalSwitcher fileSwitcher;
	
	public EditLayoutPopup(List<PropertiesSet> properties) {
		super(240);
		
		this.fullprops = properties;

		this.setNotificationText(Locals.localize("helper.creator.editlayout.popup.msg"));
		
		for (PropertiesSet s : properties) {
			List<PropertiesSection> secs = s.getPropertiesOfType("customization-meta");
			if (secs.isEmpty()) {
				secs = s.getPropertiesOfType("type-meta");
			}
			if (secs.isEmpty()) {
				continue;
			}
			PropertiesSection meta = secs.get(0);
			String path = meta.getEntryValue("path");
			if (path != null) {
				List<Object> l = new ArrayList<Object>();
				l.add(s);
				l.add(s.getPropertiesOfType("customization").size());
				props.put(new File(path).getName(), l);
			}
		}
		
		this.fileSwitcher = new HorizontalSwitcher(120, props.keySet().toArray(new String[0]));
		this.fileSwitcher.setButtonColor(new Color(102, 102, 153), new Color(133, 133, 173), new Color(163, 163, 194), new Color(163, 163, 194), 1);
		this.fileSwitcher.setValueBackgroundColor(new Color(102, 102, 153));

		this.loadAllButton = new AdvancedButton(0, 0, 120, 20, Locals.localize("helper.creator.editlayout.popup.loadall"), true, (press) -> {
			this.setDisplayed(false);
			
			boolean b = false;
			for (PropertiesSet s : this.fullprops) {
				if (MenuCustomization.containsCalculations(s)) {
					b = true;
					break;
				}
			}
			
			if (!b) {
				Minecraft.getInstance().displayGuiScreen(new PreloadedLayoutCreatorScreen(CustomizationHelper.getInstance().current, this.fullprops));
				LayoutCreatorScreen.isActive = true;
				MenuCustomization.stopSounds();
				MenuCustomization.resetSounds();
				for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
					if (r instanceof AdvancedAnimation) {
						((AdvancedAnimation)r).stopAudio();
						if (((AdvancedAnimation)r).replayIntro()) {
							((AdvancedAnimation)r).resetAnimation();
						}
					}
				}
			} else {
				PopupHandler.displayPopup(new NotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.creator.editlayout.unsupportedvalues")));
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(loadAllButton);
		
		this.loadLayoutButton = new AdvancedButton(0, 0, 120, 20, Locals.localize("helper.creator.editlayout.popup.loadselected"), true, (press) -> {
			String s = this.fileSwitcher.getSelectedValue();
			if ((s != null) && !s.equals("")) {
				this.setDisplayed(false);
				
				List<PropertiesSet> l = new ArrayList<PropertiesSet>();
				l.add((PropertiesSet) this.props.get(s).get(0));
				
				if (!MenuCustomization.containsCalculations(l.get(0))) {
					Minecraft.getInstance().displayGuiScreen(new PreloadedLayoutCreatorScreen(CustomizationHelper.getInstance().current, l));
					LayoutCreatorScreen.isActive = true;
					MenuCustomization.stopSounds();
					MenuCustomization.resetSounds();
					for (IAnimationRenderer r : AnimationHandler.getAnimations()) {
						if (r instanceof AdvancedAnimation) {
							((AdvancedAnimation)r).stopAudio();
							if (((AdvancedAnimation)r).replayIntro()) {
								((AdvancedAnimation)r).resetAnimation();
							}
						}
					}
				} else {
					PopupHandler.displayPopup(new NotificationPopup(300, new Color(0, 0, 0, 0), 240, null, Locals.localize("helper.creator.editlayout.unsupportedvalues")));
				}
			}
		});
		LayoutCreatorScreen.colorizeCreatorButton(loadLayoutButton);
		
		this.cancelButton = new AdvancedButton(0, 0, 120, 20, Locals.localize("helper.creator.editlayout.popup.cancel"), true, (press) -> {
			this.setDisplayed(false);
		});
		LayoutCreatorScreen.colorizeCreatorButton(cancelButton);
	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, Screen renderIn) {
		RenderSystem.enableBlend();
		super.render(matrix, mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {

			renderIn.func_238472_a_(matrix, Minecraft.getInstance().fontRenderer, new StringTextComponent("§l" + Locals.localize("helper.creator.editlayout")),
					renderIn.field_230708_k_ / 2, (renderIn.field_230709_l_ / 2) - 110, Color.WHITE.getRGB());
			
			int i = 0;
			for (String s : this.msg) {
				renderIn.func_238472_a_(matrix, Minecraft.getInstance().fontRenderer, new StringTextComponent(s),
						renderIn.field_230708_k_ / 2, (renderIn.field_230709_l_ / 2) - 90 + i, Color.WHITE.getRGB());
				i += 10;
			}
			
			int startY = (renderIn.field_230709_l_ / 2) - 70 + i;
			
			this.loadAllButton.setX((renderIn.field_230708_k_ / 2) - (this.loadAllButton.getWidth() / 2));
			this.loadAllButton.setY(startY);
			this.loadAllButton.render(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
			
			
			renderIn.func_238472_a_(matrix, Minecraft.getInstance().fontRenderer, new StringTextComponent(Locals.localize("helper.creator.editlayout.popup.layouts")),
					renderIn.field_230708_k_ / 2, startY + 37, Color.WHITE.getRGB());
			
			this.fileSwitcher.render(matrix, (renderIn.field_230708_k_ / 2) - (this.fileSwitcher.getTotalWidth() / 2), startY + 50);
			
			String s = this.fileSwitcher.getSelectedValue();
			int count = 0;
			if ((s != null) && !props.isEmpty()) {
				List<Object> ol = this.props.get(s);
				if (ol.size() >= 2) {
					count = (int) this.props.get(s).get(1);
				}
			}
			
			renderIn.func_238472_a_(matrix, Minecraft.getInstance().fontRenderer,
					new StringTextComponent("(" + Locals.localize("helper.creator.editlayout.popup.actionscount", "" + count) + ")"),
					renderIn.field_230708_k_ / 2, startY + 75, Color.WHITE.getRGB());

			
			this.loadLayoutButton.setX((renderIn.field_230708_k_ / 2) - this.loadLayoutButton.getWidth() - 5);
			this.loadLayoutButton.setY(startY + 95);
			this.loadLayoutButton.render(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
			
			this.cancelButton.setX((renderIn.field_230708_k_ / 2) + 5);
			this.cancelButton.setY(startY + 95);
			this.cancelButton.render(matrix, mouseX, mouseY, Minecraft.getInstance().getRenderPartialTicks());
			
			this.renderButtons(matrix, mouseX, mouseY);
		}
	}
	
	private void setNotificationText(String... text) {
		if (text != null) {
			List<String> l = new ArrayList<String>();
			for (String s : text) {
				if (s.contains("%n%")) {
					for (String s2 : s.split("%n%")) {
						l.add(s2);
					}
				} else {
					l.add(s);
				}
			}
			this.msg = l;
		}
	}

}

