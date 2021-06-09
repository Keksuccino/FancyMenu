//TODO übernehmen
package de.keksuccino.fancymenu.events;

import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.eventbus.api.Event;

public class PlayWidgetClickSoundEvent extends Event {
	
	private Widget widget;
	
	public PlayWidgetClickSoundEvent(Widget widget) {
		this.widget = widget;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public Widget getWidget() {
		return this.widget;
	}
	
	public static class Pre extends PlayWidgetClickSoundEvent {

		public Pre(Widget widget) {
			super(widget);
		}
		
	}
	
	public static class Post extends PlayWidgetClickSoundEvent {

		public Post(Widget widget) {
			super(widget);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
