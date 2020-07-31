package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.languagesettings;

import java.lang.reflect.Field;

import de.keksuccino.core.gui.content.AdvancedButton;
import de.keksuccino.core.input.MouseInput;
import de.keksuccino.fancymenu.menu.button.ButtonCache;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.LanguageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.AbstractOption;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class LanguageMenuHandler extends MenuHandlerBase {

	private LanguageMenuList list;
	private AdvancedButton confirmSettingsBtn;
	private AdvancedButton unicodeButton;

	public LanguageMenuHandler() {
		super(LanguageScreen.class.getName());
	}

	@Override
	public void onInitPost(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {

			GameSettings s = Minecraft.getInstance().gameSettings;

			e.getGui().children().clear();
			
			this.unicodeButton = ButtonCache.convertToAdvancedButton(ButtonCache.getIdForButton(this.getUnicodeButton()), true);
			if (this.unicodeButton != null) {
				this.unicodeButton.setPressAction((press) -> {
					AbstractOption.FORCE_UNICODE_FONT.nextValue(s);
					s.saveOptions();
					press.setMessage(AbstractOption.FORCE_UNICODE_FONT.getText(s));
					Minecraft.getInstance().updateWindowSize();
				});
			}

			this.confirmSettingsBtn = ButtonCache.convertToAdvancedButton(ButtonCache.getIdForButton(this.getConfirmButton()), true);
			if (this.confirmSettingsBtn != null) {
				this.confirmSettingsBtn.setPressAction((press) -> {
					LanguageMenuList.LanguageEntry languagescreen$list$languageentry = this.list.getSelected();
					if (languagescreen$list$languageentry != null && !languagescreen$list$languageentry.field_214398_b.getCode().equals(Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode())) {
						Minecraft.getInstance().getLanguageManager().setCurrentLanguage(languagescreen$list$languageentry.field_214398_b);
						s.language = languagescreen$list$languageentry.field_214398_b.getCode();
						net.minecraftforge.client.ForgeHooksClient.refreshResources(Minecraft.getInstance(), net.minecraftforge.resource.VanillaResourceType.LANGUAGES);
						Minecraft.getInstance().fontRenderer.setBidiFlag(Minecraft.getInstance().getLanguageManager().isCurrentLanguageBidirectional());
						press.setMessage(I18n.format("gui.done"));
						this.unicodeButton.setMessage(AbstractOption.FORCE_UNICODE_FONT.getText(s));
						s.saveOptions();
					}

					Minecraft.getInstance().displayGuiScreen(this.getParent());
				});
			}
			
			this.list = new LanguageMenuList((LanguageScreen) e.getGui(), Minecraft.getInstance(), this);
			addChildren(e.getGui(), this.list);
		}

		super.onInitPost(e);
	}

	@SubscribeEvent
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (this.shouldCustomize(e.getGui())) {
			e.getGui().renderBackground();
		}
	}

	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		super.drawToBackground(e);

		if (this.shouldCustomize(e.getGui()) && (this.list != null)) {
			if (this.list != null) {
				this.list.render(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
			if (this.unicodeButton != null) {
				this.unicodeButton.render(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
			if (this.confirmSettingsBtn != null) {
				this.confirmSettingsBtn.render(MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
		}
	}

	private Screen getParent() {
		if (this.shouldCustomize(Minecraft.getInstance().currentScreen)) {
			try {
				Field f = ObfuscationReflectionHelper.findField(SettingsScreen.class, "field_228182_a_");
				return (Screen) f.get(Minecraft.getInstance().currentScreen);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private Widget getConfirmButton() {
		if (this.shouldCustomize(Minecraft.getInstance().currentScreen)) {
			try {
				Field f = ObfuscationReflectionHelper.findField(LanguageScreen.class, "field_146452_r");
				return (Widget) f.get(Minecraft.getInstance().currentScreen);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private Widget getUnicodeButton() {
		if (this.shouldCustomize(Minecraft.getInstance().currentScreen)) {
			try {
				Field f = ObfuscationReflectionHelper.findField(LanguageScreen.class, "field_211832_i");
				return (Widget) f.get(Minecraft.getInstance().currentScreen);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private static void addChildren(Screen s, IGuiEventListener e) {
		try {
			Field f = ObfuscationReflectionHelper.findField(Screen.class, "children");
			((java.util.List<IGuiEventListener>)f.get(s)).add(e);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}


}
