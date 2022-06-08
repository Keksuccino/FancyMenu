package de.keksuccino.fancymenu.events;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.konkrete.events.EventBase;
import net.minecraft.client.gui.components.AbstractButton;

public class RenderWidgetLabelEvent extends EventBase {
	
	protected AbstractButton widget;
	protected float alpha;
	protected PoseStack matrix;
	
	public RenderWidgetLabelEvent(PoseStack matrix, AbstractButton widget, float alpha) {
		this.widget = widget;
		this.alpha = alpha;
		this.matrix = matrix;
	}
	
	@Override
	public boolean isCancelable() {
		return true;
	}
	
	public AbstractButton getWidget() {
		return this.widget;
	}
	
	public float getAlpha() {
		return this.alpha;
	}
	
	public PoseStack getMatrixStack() {
		return this.matrix;
	}
	
	public static class Pre extends RenderWidgetLabelEvent {

		public Pre(PoseStack matrix, AbstractButton widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		public void setAlpha(float alpha) {
			this.alpha = alpha;
		}
		
	}
	
	public static class Post extends RenderWidgetLabelEvent {

		public Post(PoseStack matrix, AbstractButton widget, float alpha) {
			super(matrix, widget, alpha);
		}
		
		@Override
		public boolean isCancelable() {
			return false;
		}
		
	}
	
}
