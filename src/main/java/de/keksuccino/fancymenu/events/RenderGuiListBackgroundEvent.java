package de.keksuccino.fancymenu.events;

import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.util.math.MatrixStack;

@SuppressWarnings("rawtypes")
public class RenderGuiListBackgroundEvent extends EventBase {
	
	protected EntryListWidget list;
	protected MatrixStack matrix;
	
	public RenderGuiListBackgroundEvent(MatrixStack matrix, EntryListWidget list) {
		this.list = list;
		this.matrix = matrix;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public EntryListWidget getList() {
		return this.list;
	}

	public MatrixStack getMatrixStack() {
		return this.matrix;
	}
	
	public static class Pre extends RenderGuiListBackgroundEvent {

		public Pre(MatrixStack matrix, EntryListWidget list) {
			super(matrix, list);
		}

	}
	
	public static class Post extends RenderGuiListBackgroundEvent {

		public Post(MatrixStack matrix, EntryListWidget list) {
			super(matrix, list);
		}
		
	}
	
}
