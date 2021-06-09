package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.eventbus.api.Event;

public class RenderWidgetLabelEvent extends Event {
	
	protected Widget widget;
	protected float alpha;
	
	public RenderWidgetLabelEvent(Widget widget, float alpha) {
		this.widget = widget;
		this.alpha = alpha;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public Widget getWidget() {
		return this.widget;
	}
	
	public float getAlpha() {
		return this.alpha;
	}
	
	public static class Pre extends RenderWidgetLabelEvent {

		public Pre(Widget widget, float alpha) {
			super(widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetLabelEvent {

		public Post(Widget widget, float alpha) {
			super(widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
