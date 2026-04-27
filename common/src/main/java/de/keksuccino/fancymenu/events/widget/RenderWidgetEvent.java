package de.keksuccino.fancymenu.events.widget;

import de.keksuccino.fancymenu.util.event.acara.EventBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

public class RenderWidgetEvent extends EventBase {
	
	protected AbstractWidget widget;
	protected float alpha;
	protected GuiGraphicsExtractor graphics;
	
	public RenderWidgetEvent(GuiGraphicsExtractor graphics, AbstractWidget widget, float alpha) {
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

	public GuiGraphicsExtractor getGraphics() {
		return graphics;
	}

	public static class Pre extends RenderWidgetEvent {

		public Pre(GuiGraphicsExtractor graphics, AbstractWidget widget, float alpha) {
			super(graphics, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetEvent {

		public Post(GuiGraphicsExtractor graphics, AbstractWidget widget, float alpha) {
			super(graphics, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
