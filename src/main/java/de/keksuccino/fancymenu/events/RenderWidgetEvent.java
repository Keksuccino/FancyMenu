package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.eventbus.api.Event;

public class RenderWidgetEvent extends Event {
	
	protected Widget widget;
	protected float alpha;
	protected MatrixStack matrix;
	
	public RenderWidgetEvent(MatrixStack matrix, Widget widget, float alpha) {
		this.widget = widget;
		this.alpha = alpha;
		this.matrix = matrix;
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
	
	public MatrixStack getMatrixStack() {
		return this.matrix;
	}
	
	public static class Pre extends RenderWidgetEvent {

		public Pre(MatrixStack matrix, Widget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetEvent {

		public Post(MatrixStack matrix, Widget widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
