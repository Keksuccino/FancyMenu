package de.keksuccino.fancymenu.events.widget;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

public class RenderWidgetEvent extends EventBase {
	
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

	public GuiGraphics getGraphics() {
		return graphics;
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
