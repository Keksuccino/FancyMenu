//TODO übernehmenn
package de.keksuccino.fancymenu.menu.fancy.helper.ui;

import de.keksuccino.fancymenu.menu.fancy.helper.layoutcreator.LayoutEditorScreen;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.widget.AdvancedButton;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.component.Component;
import de.keksuccino.fancymenu.menu.fancy.helper.ui.compat.Screen;
import de.keksuccino.konkrete.localization.Locals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import java.util.function.Consumer;

public class ConfirmationScreen extends Screen {

    protected GuiScreen parentScreen;
    protected String[] text;
    protected Consumer<Boolean> callback;

    protected AdvancedButton confirmButton;
    protected AdvancedButton cancelButton;

    public ConfirmationScreen(GuiScreen parentScreen, Consumer<Boolean> callback, String... text) {

        super(Component.literal(""));
        this.parentScreen = parentScreen;
        this.callback = callback;
        this.text = text;

        this.confirmButton = new AdvancedButton(0, 0, 150, 20, "§a" + Locals.localize("fancymenu.guicomponents.confirm"), true, (button) -> {
            Minecraft.getMinecraft().displayGuiScreen(this.parentScreen);
            this.callback.accept(true);
        });
        UIBase.applyDefaultButtonSkinTo(this.confirmButton);

        this.cancelButton = new AdvancedButton(0, 0, 150, 20, "§c" + Locals.localize("fancymenu.guicomponents.cancel"), true, (button) -> {
            Minecraft.getMinecraft().displayGuiScreen(this.parentScreen);
            this.callback.accept(false);
        });
        UIBase.applyDefaultButtonSkinTo(this.cancelButton);

    }

    @Override
    protected void init() {

        //Reset GUI scale in case it was changed by the layout editor
        if ((this.parentScreen != null) && (this.parentScreen instanceof LayoutEditorScreen)) {
            if (((LayoutEditorScreen)this.parentScreen).oriscale != -1) {
                Minecraft.getMinecraft().gameSettings.guiScale = ((LayoutEditorScreen)this.parentScreen).oriscale;
                ((LayoutEditorScreen)this.parentScreen).oriscale = -1;
                ScaledResolution res = new ScaledResolution(Minecraft.getMinecraft());
                this.width = res.getScaledWidth();
                this.height = res.getScaledHeight();
            }
        }

        super.init();

    }

    @Override
    public void render(int mouseX, int mouseY, float partial) {

        fill(0, 0, this.width, this.height, UIBase.SCREEN_BACKGROUND_COLOR.getRGB());

        int y = (this.height / 2) - ((this.text.length * 14) / 2);
        for (String s : this.text) {
            if (s.length() > 0) {
                int textWidth = this.font.getStringWidth(s);
                drawString(this.font, Component.literal(s), (this.width / 2) - (textWidth / 2), y, -1);
            }
            y += 14;
        }

        this.confirmButton.setX((this.width / 2) - this.confirmButton.getWidth() - 5);
        this.confirmButton.setY(this.height - 40);
        this.confirmButton.render(mouseX, mouseY, partial);

        this.cancelButton.setX((this.width / 2) + 5);
        this.cancelButton.setY(this.height - 40);
        this.cancelButton.render(mouseX, mouseY, partial);

        super.render(mouseX, mouseY, partial);

    }

    @Override
    public boolean keyPressed(int button, int p_96553_, int p_96554_) {

        //ENTER
        if (button == 257) {
            Minecraft.getMinecraft().displayGuiScreen(this.parentScreen);
            this.callback.accept(true);
            return true;
        }

        return super.keyPressed(button, p_96553_, p_96554_);

    }

    @Override
    public void onClose() {
        Minecraft.getMinecraft().displayGuiScreen(this.parentScreen);
        this.callback.accept(false);
    }

}
