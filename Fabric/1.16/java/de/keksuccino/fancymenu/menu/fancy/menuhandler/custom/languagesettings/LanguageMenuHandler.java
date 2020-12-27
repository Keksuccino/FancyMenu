package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom.languagesettings;

import java.lang.reflect.Field;
import java.util.List;

import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.MenuCustomization;
import de.keksuccino.fancymenu.menu.fancy.helper.MenuReloadedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import de.keksuccino.konkrete.events.SubscribeEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.BackgroundDrawnEvent;
import de.keksuccino.konkrete.events.client.GuiScreenEvent.InitGuiEvent.Pre;
import de.keksuccino.konkrete.input.MouseInput;
import de.keksuccino.konkrete.reflection.ReflectionHelper;
import de.keksuccino.konkrete.rendering.CurrentScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.options.GameOptionsScreen;
import net.minecraft.client.gui.screen.options.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionButtonWidget;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.Option;
import net.minecraft.client.resource.language.LanguageManager;

public class LanguageMenuHandler extends MenuHandlerBase {
	
	private LanguageMenuList list;
	private ButtonWidget confirmSettingsBtn;
	private ButtonWidget unicodeButton;

	public LanguageMenuHandler() {
		super(LanguageOptionsScreen.class.getName());
	}
	
	@SubscribeEvent
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			
			AbstractButtonWidget uni = null;
			AbstractButtonWidget confirm = null;
			for (AbstractButtonWidget w : e.getWidgetList()) {
				if (w instanceof OptionButtonWidget) {
					uni = w;
				} else {
					confirm = w;
				}
			}
			if (uni != null) {
				e.removeWidget(uni);
			}
			if (confirm != null) {
				e.removeWidget(confirm);
			}
			
			e.getGui().children().clear();
			
			GameOptions s = MinecraftClient.getInstance().options;
			
			this.unicodeButton = new OptionButtonWidget(e.getGui().width / 2 - 155, e.getGui().height - 38, 150, 20, Option.FORCE_UNICODE_FONT, Option.FORCE_UNICODE_FONT.getDisplayString(s), (press) -> {
				Option.FORCE_UNICODE_FONT.toggle(s);
				s.write();
				press.setMessage(Option.FORCE_UNICODE_FONT.getDisplayString(s));
				MinecraftClient.getInstance().onResolutionChanged();
			});
			e.addWidget(this.unicodeButton);
			
			this.confirmSettingsBtn = new ButtonWidget(e.getGui().width / 2 - 155 + 160, e.getGui().height - 38, 150, 20, ScreenTexts.DONE, (press) -> {
				LanguageManager lm = MinecraftClient.getInstance().getLanguageManager();
				GameOptions o = MinecraftClient.getInstance().options;
				LanguageMenuList.LanguageEntry languageEntry = this.list.getSelected();
				if (languageEntry != null && !languageEntry.languageDefinition.getCode().equals(lm.getLanguage().getCode())) {
					lm.setLanguage(languageEntry.languageDefinition);
					o.language = languageEntry.languageDefinition.getCode();
					MinecraftClient.getInstance().reloadResources();
					press.setMessage(ScreenTexts.DONE);
					this.unicodeButton.setMessage(Option.FORCE_UNICODE_FONT.getDisplayString(o));
					o.write();
				}
				
				MinecraftClient.getInstance().openScreen(this.getParent());
			});
			e.addWidget(this.confirmSettingsBtn);
			
			this.list = new LanguageMenuList((LanguageOptionsScreen) e.getGui(), MinecraftClient.getInstance(), this);
			addChildren(e.getGui(), this.list);

		}
	}

	@SubscribeEvent
	@Override
	public void onRenderPost(GuiScreenEvent.DrawScreenEvent.Post e) {
		if (this.shouldCustomize(e.getGui()) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			e.getGui().renderBackground(e.getMatrixStack());
		}
	}

	@SubscribeEvent
	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		super.drawToBackground(e);

		if (this.shouldCustomize(e.getGui()) && (this.list != null) && MenuCustomization.isMenuCustomizable(e.getGui())) {
			if (this.list != null) {
				this.list.render(CurrentScreenHandler.getMatrixStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), MinecraftClient.getInstance().getTickDelta());
			}
			if (this.unicodeButton != null) {
				this.unicodeButton.render(CurrentScreenHandler.getMatrixStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), MinecraftClient.getInstance().getTickDelta());
			}
			if (this.confirmSettingsBtn != null) {
				this.confirmSettingsBtn.render(CurrentScreenHandler.getMatrixStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), MinecraftClient.getInstance().getTickDelta());
			}
		}
	}

	private Screen getParent() {
		if (this.shouldCustomize(MinecraftClient.getInstance().currentScreen)) {
			try {
				Field f = ReflectionHelper.findField(GameOptionsScreen.class, "parent", "field_21335");
				return (Screen) f.get(MinecraftClient.getInstance().currentScreen);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private static void addChildren(Screen s, Element e) {
		try {
			Field f = ReflectionHelper.findField(Screen.class, "children", "field_22786");
			((List<Element>)f.get(s)).add(e);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	

	@SubscribeEvent
	@Override
	public void onButtonsCached(ButtonCachedEvent e) {
		super.onButtonsCached(e);
	}
	
	@SubscribeEvent
	@Override
	public void onInitPre(Pre e) {
		super.onInitPre(e);
	}
	
	@SubscribeEvent
	@Override
	public void onMenuReloaded(MenuReloadedEvent e) {
		super.onMenuReloaded(e);
	}

}
