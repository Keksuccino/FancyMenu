package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.components.AbstractWidget;

public class RenderWidgetBackgroundEvent extends EventBase {
	
	protected AbstractWidget widget;
	protected float alpha;
	protected GuiGraphics graphics;
	
	public RenderWidgetBackgroundEvent(GuiGraphics graphics, AbstractWidget widget, float alpha) {
		this.widget = widget;
		this.alpha = alpha;
		this.graphics = graphics;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public AbstractWidget getWidget() {
		return this.widget;
	}
	
	public float getAlpha() {
		return this.alpha;
	}

	public GuiGraphics getGuiGraphics() {
		return this.graphics;
	}

	public PoseStack getPoseStack() {
		return this.graphics.pose();
	}

	@Deprecated
	public PoseStack getMatrixStack() {
		return this.graphics.pose();
	}
	
	public static class Pre extends RenderWidgetBackgroundEvent {

		public Pre(GuiGraphics graphics, AbstractWidget widget, float alpha) {
			super(graphics, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetBackgroundEvent {

		public Post(GuiGraphics graphics, AbstractWidget widget, float alpha) {
			super(graphics, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
