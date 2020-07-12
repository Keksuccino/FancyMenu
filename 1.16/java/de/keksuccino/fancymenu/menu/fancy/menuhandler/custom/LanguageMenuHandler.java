package de.keksuccino.fancymenu.menu.fancy.menuhandler.custom;

import java.lang.reflect.Field;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import de.keksuccino.core.input.MouseInput;
import de.keksuccino.core.rendering.CurrentScreenHandler;
import de.keksuccino.fancymenu.menu.button.ButtonCachedEvent;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.MenuHandlerBase;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.LanguageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.SettingsScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.OptionButton;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class LanguageMenuHandler extends MenuHandlerBase {

	private Screen current;
	private List list;
	private java.util.List<Widget> currentButtons;

	public LanguageMenuHandler() {
		super(LanguageScreen.class.getName());
	}

	@Override
	public void onInitPost(ButtonCachedEvent e) {
		if (this.shouldCustomize(e.getGui())) {
			this.current = e.getGui();

			//children
			e.getGui().children().clear();

			this.list = new List(Minecraft.getInstance());
			addChildren(e.getGui(), this.list);

			GameSettings s = Minecraft.getInstance().gameSettings;

			OptionButton optionbutton = new OptionButton(e.getGui().width / 2 - 155, e.getGui().height - 38, 150, 20, AbstractOption.FORCE_UNICODE_FONT, AbstractOption.FORCE_UNICODE_FONT.func_238152_c_(s), (p_213037_1_) -> {
				AbstractOption.FORCE_UNICODE_FONT.nextValue(s);
				s.saveOptions();
				p_213037_1_.setMessage(AbstractOption.FORCE_UNICODE_FONT.func_238152_c_(s));
				Minecraft.getInstance().updateWindowSize();
			});
			e.addWidget(optionbutton);

			Button confirmSettingsBtn = new Button(e.getGui().width / 2 - 155 + 160, e.getGui().height - 38, 150, 20, DialogTexts.field_240632_c_, (press) -> {
				LanguageMenuHandler.List.LanguageEntry languagescreen$list$languageentry = this.list.getSelected();
				if (languagescreen$list$languageentry != null && !languagescreen$list$languageentry.field_214398_b.getCode().equals(Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode())) {
					Minecraft.getInstance().getLanguageManager().setCurrentLanguage(languagescreen$list$languageentry.field_214398_b);
					s.language = languagescreen$list$languageentry.field_214398_b.getCode();
					net.minecraftforge.client.ForgeHooksClient.refreshResources(Minecraft.getInstance(), net.minecraftforge.resource.VanillaResourceType.LANGUAGES);
					press.setMessage(DialogTexts.field_240632_c_);
					optionbutton.setMessage(AbstractOption.FORCE_UNICODE_FONT.func_238152_c_(s));
					s.saveOptions();
				}

				Minecraft.getInstance().displayGuiScreen(LanguageMenuHandler.this.getParent());
			});
			e.addWidget(confirmSettingsBtn);

			this.currentButtons = e.getWidgetList();
		}

		super.onInitPost(e);
	}

	@SubscribeEvent
	public void onRenderPre(GuiScreenEvent.DrawScreenEvent.Pre e) {
		if (this.shouldCustomize(e.getGui())) {
			e.setCanceled(true);
			e.getGui().renderBackground(e.getMatrixStack());
		}
	}

	@Override
	public void drawToBackground(BackgroundDrawnEvent e) {
		super.drawToBackground(e);

		if (this.shouldCustomize(e.getGui()) && (this.list != null)) {
			this.list.render(CurrentScreenHandler.getMatrixStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			e.getGui().drawCenteredString(CurrentScreenHandler.getMatrixStack(), Minecraft.getInstance().fontRenderer, e.getGui().getTitle(), e.getGui().width / 2, 16, 16777215);
			e.getGui().drawCenteredString(CurrentScreenHandler.getMatrixStack(), Minecraft.getInstance().fontRenderer, new StringTextComponent("(" + I18n.format("options.languageWarning") + ")"), e.getGui().width / 2, e.getGui().height - 56, 8421504);

			for(int i = 0; i < this.currentButtons.size(); ++i) {
				this.currentButtons.get(i).render(CurrentScreenHandler.getMatrixStack(), MouseInput.getMouseX(), MouseInput.getMouseY(), Minecraft.getInstance().getRenderPartialTicks());
			}
		}
	}

	private Screen getParent() {
		try {
			Field f = ObfuscationReflectionHelper.findField(SettingsScreen.class, "field_228182_a_");
			return (Screen) f.get(this.current);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	class List extends ExtendedList<LanguageMenuHandler.List.LanguageEntry> {
		public List(Minecraft mcIn) {
			super(mcIn, LanguageMenuHandler.this.current.width, LanguageMenuHandler.this.current.height, 32, LanguageMenuHandler.this.current.height - 65 + 4, 18);

			for(Language language : Minecraft.getInstance().getLanguageManager().getLanguages()) {
				LanguageMenuHandler.List.LanguageEntry languagescreen$list$languageentry = new LanguageMenuHandler.List.LanguageEntry(language);
				this.addEntry(languagescreen$list$languageentry);
				if (Minecraft.getInstance().getLanguageManager().getCurrentLanguage().getCode().equals(language.getCode())) {
					this.setSelected(languagescreen$list$languageentry);
				}
			}

			if (this.getSelected() != null) {
				this.centerScrollOn(this.getSelected());
			}

		}

		@SuppressWarnings("deprecation")
		@Override
		public void render(MatrixStack matrix, int p_230430_2_, int p_230430_3_, float p_230430_4_) {
			int i = this.getScrollbarPosition();
			int j = i + 6;
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferbuilder = tessellator.getBuffer();

			if (!LanguageMenuHandler.this.canRenderBackground()) {
				Minecraft.getInstance().getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
				bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
				bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y1 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
				bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex((float)this.x1 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
				bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex((float)this.x0 / 32.0F, (float)(this.y0 + (int)this.getScrollAmount()) / 32.0F).color(32, 32, 32, 255).endVertex();
				tessellator.draw();
			}

			int k = this.getRowLeft();
			int l = this.y0 + 4 - (int)this.getScrollAmount();
			if (this.renderHeader()) {
				this.renderHeader(matrix, k, l, tessellator);
			}

			this.renderList(matrix, k, l, p_230430_2_, p_230430_3_, p_230430_4_);
			this.minecraft.getTextureManager().bindTexture(AbstractGui.BACKGROUND_LOCATION);
			RenderSystem.enableDepthTest();
			RenderSystem.depthFunc(519);
			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)this.x0, (double)this.y0, -100.0D).tex(0.0F, (float)this.y0 / 32.0F).color(64, 64, 64, 255).endVertex();
			bufferbuilder.pos((double)(this.x0 + this.width), (double)this.y0, -100.0D).tex((float)this.width / 32.0F, (float)this.y0 / 32.0F).color(64, 64, 64, 255).endVertex();
			bufferbuilder.pos((double)(this.x0 + this.width), 0.0D, -100.0D).tex((float)this.width / 32.0F, 0.0F).color(64, 64, 64, 255).endVertex();
			bufferbuilder.pos((double)this.x0, 0.0D, -100.0D).tex(0.0F, 0.0F).color(64, 64, 64, 255).endVertex();
			bufferbuilder.pos((double)this.x0, (double)this.height, -100.0D).tex(0.0F, (float)this.height / 32.0F).color(64, 64, 64, 255).endVertex();
			bufferbuilder.pos((double)(this.x0 + this.width), (double)this.height, -100.0D).tex((float)this.width / 32.0F, (float)this.height / 32.0F).color(64, 64, 64, 255).endVertex();
			bufferbuilder.pos((double)(this.x0 + this.width), (double)this.y1, -100.0D).tex((float)this.width / 32.0F, (float)this.y1 / 32.0F).color(64, 64, 64, 255).endVertex();
			bufferbuilder.pos((double)this.x0, (double)this.y1, -100.0D).tex(0.0F, (float)this.y1 / 32.0F).color(64, 64, 64, 255).endVertex();
			tessellator.draw();
			RenderSystem.depthFunc(515);
			RenderSystem.disableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			RenderSystem.disableAlphaTest();
			RenderSystem.shadeModel(7425);
			RenderSystem.disableTexture();
			bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
			bufferbuilder.pos((double)this.x0, (double)(this.y0 + 4), 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 0).endVertex();
			bufferbuilder.pos((double)this.x1, (double)(this.y0 + 4), 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 0).endVertex();
			bufferbuilder.pos((double)this.x1, (double)this.y0, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)this.x0, (double)this.y0, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)this.x0, (double)this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)this.x1, (double)this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
			bufferbuilder.pos((double)this.x1, (double)(this.y1 - 4), 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 0).endVertex();
			bufferbuilder.pos((double)this.x0, (double)(this.y1 - 4), 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 0).endVertex();
			tessellator.draw();
			int k1 = this.getMaxScroll();
			if (k1 > 0) {
				int l1 = (int)((float)((this.y1 - this.y0) * (this.y1 - this.y0)) / (float)this.getMaxPosition());
				l1 = MathHelper.clamp(l1, 32, this.y1 - this.y0 - 8);
				int i2 = (int)this.getScrollAmount() * (this.y1 - this.y0 - l1) / k1 + this.y0;
				if (i2 < this.y0) {
					i2 = this.y0;
				}

				bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
				bufferbuilder.pos((double)i, (double)this.y1, 0.0D).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
				bufferbuilder.pos((double)j, (double)this.y1, 0.0D).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
				bufferbuilder.pos((double)j, (double)this.y0, 0.0D).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
				bufferbuilder.pos((double)i, (double)this.y0, 0.0D).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
				bufferbuilder.pos((double)i, (double)(i2 + l1), 0.0D).tex(0.0F, 1.0F).color(128, 128, 128, 255).endVertex();
				bufferbuilder.pos((double)j, (double)(i2 + l1), 0.0D).tex(1.0F, 1.0F).color(128, 128, 128, 255).endVertex();
				bufferbuilder.pos((double)j, (double)i2, 0.0D).tex(1.0F, 0.0F).color(128, 128, 128, 255).endVertex();
				bufferbuilder.pos((double)i, (double)i2, 0.0D).tex(0.0F, 0.0F).color(128, 128, 128, 255).endVertex();
				bufferbuilder.pos((double)i, (double)(i2 + l1 - 1), 0.0D).tex(0.0F, 1.0F).color(192, 192, 192, 255).endVertex();
				bufferbuilder.pos((double)(j - 1), (double)(i2 + l1 - 1), 0.0D).tex(1.0F, 1.0F).color(192, 192, 192, 255).endVertex();
				bufferbuilder.pos((double)(j - 1), (double)i2, 0.0D).tex(1.0F, 0.0F).color(192, 192, 192, 255).endVertex();
				bufferbuilder.pos((double)i, (double)i2, 0.0D).tex(0.0F, 0.0F).color(192, 192, 192, 255).endVertex();
				tessellator.draw();
			}

			this.renderDecorations(matrix, p_230430_2_, p_230430_3_);
			RenderSystem.enableTexture();
			RenderSystem.shadeModel(7424);
			RenderSystem.enableAlphaTest();
			RenderSystem.disableBlend();
		}

		private boolean renderHeader() {
			try {
				Field f = ObfuscationReflectionHelper.findField(AbstractList.class, "field_230680_q_");
				return f.getBoolean(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		private int getMaxScroll() {
			return Math.max(0, this.getMaxPosition() - (this.y1 - this.y0 - 4));
		}

		//getScrollbarPosition
		@Override
		protected int getScrollbarPosition() {
			return super.getScrollbarPosition() + 20;
		}

		//getRowWidth
		@Override
		public int getRowWidth() {
			return super.getRowWidth() + 50;
		}

		//setSelected
		@Override
		public void setSelected(@Nullable LanguageMenuHandler.List.LanguageEntry p_setSelected_1_) {
			super.setSelected(p_setSelected_1_);
			if (p_setSelected_1_ != null) {
				NarratorChatListener.INSTANCE.say((new TranslationTextComponent("narrator.select", p_setSelected_1_.field_214398_b)).getString());
			}

		}

		public class LanguageEntry extends ExtendedList.AbstractListEntry<LanguageMenuHandler.List.LanguageEntry> {
			private final Language field_214398_b;

			public LanguageEntry(Language p_i50494_2_) {
				this.field_214398_b = p_i50494_2_;
			}

			@Override
			public void render(MatrixStack p_230432_1_, int p_230432_2_, int p_230432_3_, int p_230432_4_, int p_230432_5_, int p_230432_6_, int p_230432_7_, int p_230432_8_, boolean p_230432_9_, float p_230432_10_) {
				String s = this.field_214398_b.toString();
				Minecraft.getInstance().fontRenderer.func_238406_a_(p_230432_1_, s, (float)(List.this.width / 2 - Minecraft.getInstance().fontRenderer.getStringWidth(s) / 2), (float)(p_230432_3_ + 1), 16777215, true);
			}

			@Override
			public boolean mouseClicked(double p_231044_1_, double p_231044_3_, int p_231044_5_) {
				if (p_231044_5_ == 0) {
					this.func_214395_a();
					return true;
				} else {
					return false;
				}
			}

			private void func_214395_a() {
				List.this.setSelected(this);
			}
		}
	}

	private static void addChildren(Screen s, IGuiEventListener e) {
		try {
			Field f = ObfuscationReflectionHelper.findField(Screen.class, "field_230705_e_");
			((java.util.List<IGuiEventListener>)f.get(s)).add(e);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}


}
