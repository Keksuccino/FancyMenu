package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.widget.AbstractButtonWidget;

public class PlayWidgetClickSoundEvent extends EventBase {
	
	private AbstractButtonWidget widget;
	
	public PlayWidgetClickSoundEvent(AbstractButtonWidget widget) {
		this.widget = widget;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public AbstractButtonWidget getWidget() {
		return this.widget;
	}
	
	public static class Pre extends PlayWidgetClickSoundEvent {

		public Pre(AbstractButtonWidget widget) {
			super(widget);
		}
		
	}
	
	public static class Post extends PlayWidgetClickSoundEvent {

		public Post(AbstractButtonWidget widget) {
			super(widget);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
