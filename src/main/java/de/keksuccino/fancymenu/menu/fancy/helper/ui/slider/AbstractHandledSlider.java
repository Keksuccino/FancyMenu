package de.keksuccino.fancymenu.menu.fancy.helper.ui.slider;

import de.keksuccino.konkrete.gui.content.AdvancedButton;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

/** Very lazy way to get 1.16+ slider-behaviour in 1.12, because very lazy **/
public abstract class AbstractHandledSlider extends AdvancedButton {

   protected double value;

   public AbstractHandledSlider(int x, int y, int width, int height, String message, double defaultValue) {
      super(x, y, width, height, message, (press) -> {});
      this.press = (press) -> {
         this.onPress();
      };
      this.handleClick = true;
      this.value = defaultValue;
   }

   @Override
   protected void renderDefaultBackground() {

      Minecraft.getMinecraft().getTextureManager().bindTexture(BUTTON_TEXTURES);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      //Draw background
      this.drawTexturedModalRect(this.x, this.y, 0, 46 + 0 * 20, this.width / 2, this.height);
      this.drawTexturedModalRect(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + 0 * 20, this.width / 2, this.height);

      //Draw slider
      this.drawTexturedModalRect(this.x + (int)(this.value * (double)(this.width - 8)), this.y, 0, 66, 4, 20);
      this.drawTexturedModalRect(this.x + (int)(this.value * (double)(this.width - 8)) + 4, this.y, 196, 66, 4, 20);

      if (this.isMouseOver() && MouseInput.isLeftMouseDown()) {
         this.changeSliderValue(MouseInput.getMouseX());
      }

   }

   protected void onPress() {
   }

   private void changeSliderValue(double mouseX) {
      this.setValue((mouseX - (double)(this.x + 4)) / (double)(this.width - 8));
   }

   private void setValue(double value) {
      double d0 = this.value;
      this.value = MathHelper.clamp(value, 0.0D, 1.0D);
      if (d0 != this.value) {
         this.applyValue();
      }
      this.updateMessage();
   }

   @Override
   public void playPressSound(SoundHandler soundHandlerIn) {
   }

   protected abstract void updateMessage();

   protected abstract void applyValue();

}