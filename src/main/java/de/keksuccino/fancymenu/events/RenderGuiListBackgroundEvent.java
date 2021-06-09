package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraftforge.eventbus.api.Event;

@SuppressWarnings("rawtypes")
public class RenderGuiListBackgroundEvent extends Event {
	
	protected AbstractList list;
	protected MatrixStack matrix;
	
	public RenderGuiListBackgroundEvent(MatrixStack matrix, AbstractList list) {
		this.list = list;
		this.matrix = matrix;
	}
	
	@Override
	public boolean isCancelable() {
		return false;
	}
	
	public AbstractList getList() {
		return this.list;
	}

	public MatrixStack getMatrixStack() {
		return this.matrix;
	}
	
	public static class Pre extends RenderGuiListBackgroundEvent {

		public Pre(MatrixStack matrix, AbstractList list) {
			super(matrix, list);
		}

	}
	
	public static class Post extends RenderGuiListBackgroundEvent {

		public Post(MatrixStack matrix, AbstractList list) {
			super(matrix, list);
		}
		
	}
	
}
