package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.common.eventhandler.Event;

public class RenderWidgetBackgroundEvent extends Event {
	
	protected GuiButton widget;
	protected float alpha;
	
	public RenderWidgetBackgroundEvent(GuiButton widget, float alpha) {
		this.widget = widget;
		this.alpha = alpha;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public GuiButton getWidget() {
		return this.widget;
	}
	
	public float getAlpha() {
		return this.alpha;
	}
	
	public static class Pre extends RenderWidgetBackgroundEvent {

		public Pre(GuiButton widget, float alpha) {
			super(widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetBackgroundEvent {

		public Post(GuiButton widget, float alpha) {
			super(widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
