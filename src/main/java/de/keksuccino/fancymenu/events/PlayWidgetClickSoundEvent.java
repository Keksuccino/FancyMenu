package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.components.AbstractButton;

public class PlayWidgetClickSoundEvent extends EventBase {
	
	private AbstractButton widget;
	
	public PlayWidgetClickSoundEvent(AbstractButton widget) {
		this.widget = widget;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public AbstractButton getWidget() {
		return this.widget;
	}
	
	public static class Pre extends PlayWidgetClickSoundEvent {

		public Pre(AbstractButton widget) {
			super(widget);
		}
		
	}
	
	public static class Post extends PlayWidgetClickSoundEvent {

		public Post(AbstractButton widget) {
			super(widget);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
