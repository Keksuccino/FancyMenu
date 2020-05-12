package de.keksuccino.core.gui.screens.popup;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.GlStateManager;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.input.KeyboardData;
import de.keksuccino.core.input.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.gui.screen.Screen;

public class YesNoPopup extends Popup {
	
	private List<String> text;
	private AdvancedButton confirmButton;
	private AdvancedButton cancelButton;
	private int width;
	private Color color = new Color(76, 0, 128);
	private Consumer<Boolean> callback;
	
	public YesNoPopup(int width, @Nullable Color color, int backgroundAlpha, @Nullable Consumer<Boolean> callback, @Nonnull String... text) {
		super(backgroundAlpha);
		
		this.setNotificationText(text);
		this.width = width;
		
		this.confirmButton = new AdvancedButton(0, 0, 100, 20, "Confirm", true, (press) -> {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(true);
			}
		});
		this.addButton(this.confirmButton);
		
		this.cancelButton = new AdvancedButton(0, 0, 100, 20, "Cancel", true, (press) -> {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(false);
			}
		});
		this.addButton(this.cancelButton);
		
		if (color != null) {
			this.color = color;
		}
		
		this.callback = callback;
		
		KeyboardHandler.addKeyPressedListener(this::onEnterPressed);
		KeyboardHandler.addKeyPressedListener(this::onEscapePressed);
	}
	
	@Override
	public void render(int mouseX, int mouseY, Screen renderIn) {
		super.render(mouseX, mouseY, renderIn);
		
		if (this.isDisplayed()) {
			int height = 50;
			
			for (int i = 0; i < this.text.size(); i++) {
				height += 10;
			}
			
			GlStateManager.enableBlend();
			IngameGui.fill((renderIn.width / 2) - (this.width / 2), (renderIn.height / 2) - (height / 2), (renderIn.width / 2) + (this.width / 2), (renderIn.height / 2) + (height / 2), this.color.getRGB());
			GlStateManager.disableBlend();
			
			int i = 0;
			for (String s : this.text) {
				renderIn.drawCenteredString(Minecraft.getInstance().fontRenderer, s, renderIn.width / 2, (renderIn.height / 2) - (height / 2) + 10 + i, Color.WHITE.getRGB());
				i += 10;
			}
			
			this.confirmButton.x = (renderIn.width / 2) - this.confirmButton.getWidth() - 20;
			this.confirmButton.y = ((renderIn.height / 2) + (height / 2)) - this.confirmButton.getHeight() - 5;
			
			this.cancelButton.x = (renderIn.width / 2) + 20;
			this.cancelButton.y = ((renderIn.height / 2) + (height / 2)) - this.cancelButton.getHeight() - 5;
			
			this.renderButtons(mouseX, mouseY);
		}
	}
	
	public void setNotificationText(String... text) {
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
			this.text = l;
		}
	}
	
	public void onEnterPressed(KeyboardData d) {
		if ((d.keycode == 257) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(true);
			}
		}
	}
	
	public void onEscapePressed(KeyboardData d) {
		if ((d.keycode == 256) && this.isDisplayed()) {
			this.setDisplayed(false);
			if (this.callback != null) {
				this.callback.accept(false);
			}
		}
	}
}
