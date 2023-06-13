package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraftforge.eventbus.api.Event;

public class RenderWidgetEvent extends Event {
	
	protected AbstractWidget widget;
	protected float alpha;
	protected GuiGraphics graphics;
	
	public RenderWidgetEvent(GuiGraphics graphics, AbstractWidget widget, float alpha) {
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

	public PoseStack getPoseStack() {
		return this.graphics.pose();
	}

	public GuiGraphics getGuiGraphics() {
		return this.graphics;
	}
	
	public static class Pre extends RenderWidgetEvent {

		public Pre(GuiGraphics graphics, AbstractWidget widget, float alpha) {
			super(graphics, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetEvent {

		public Post(GuiGraphics graphics, AbstractWidget widget, float alpha) {
			super(graphics, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
