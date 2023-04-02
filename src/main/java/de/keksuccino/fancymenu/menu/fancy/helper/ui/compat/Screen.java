package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.input.Keyboard;

public class Screen extends GuiScreen {

    public ITextComponent title;
    public Minecraft minecraft = Minecraft.getMinecraft();
    public FontRenderer font = Minecraft.getMinecraft().fontRenderer;

    public Screen(ITextComponent title) {
        this.title = title;
    }

    protected void init() {}

    /** Use init() instead **/
    @Deprecated
    @Override
    public final void initGui() {
        this.init();
    }

    public void render(int mouseX, int mouseY, float partial) {
        super.drawScreen(mouseX, mouseY, partial);
    }

    /** Use render() instead **/
    @Deprecated
    @Override
    public final void drawScreen(int mouseX, int mouseY, float partial) {
        this.render(mouseX, mouseY, partial);
    }

    public void tick() {
    }

    /** Use tick() instead **/
    @Deprecated
    @Override
    public final void updateScreen() {
        this.tick();
    }

    public boolean charTyped(char character, int modifiers) {
        return false;
    }

    protected boolean keyPressed(int button, int i2, int i3) {
        if ((button == 1) && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        }
        return false;
    }

    public boolean keyReleased(int button, int i2, int i3) {
        return false;
    }

    /** Use keyPressed(), keyReleased() and charTyped() instead **/
    @Deprecated
    @Override
    protected final void keyTyped(char typedChar, int keyCode) {
        this.keyPressed(keyCode, 0, 0);
        this.charTyped(typedChar, keyCode);
    }

    /** Use keyPressed(), keyReleased() and charTyped() instead **/
    @Deprecated
    @Override
    public final void handleKeyboardInput() {
        char typedChar = Keyboard.getEventCharacter();
        int keyCode = Keyboard.getEventKey();
        if ((keyCode == 0) && (typedChar >= ' ') || Keyboard.getEventKeyState()) {
            this.keyPressed(keyCode, 0, 0);
            this.charTyped(typedChar, keyCode);
        } else if (!Keyboard.getEventKeyState()) {
            this.keyReleased(keyCode, 0, 0);
        }
        this.mc.dispatchKeypresses();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            super.mouseClicked((int)mouseX, (int)mouseY, button);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Use mouseClicked(double,double,int) instead **/
    @Deprecated
    @Override
    protected final void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        this.mouseClicked((double)mouseX, (double)mouseY, mouseButton);
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }

    public void onClose() {
        this.mc.displayGuiScreen((GuiScreen)null);
        if (this.mc.currentScreen == null) {
            this.mc.setIngameFocus();
        }
    }

    public static void fill(int left, int top, int right, int bottom, int color) {
        drawRect(left, top, right, bottom, color);
    }

    public void drawString(FontRenderer fontRendererIn, ITextComponent text, int x, int y, int color) {
        fontRendererIn.drawStringWithShadow(text.getFormattedText(), (float)x, (float)y, color);
    }

    public static boolean hasControlDown() {
        return isCtrlKeyDown();
    }

    public static boolean hasShiftDown() {
        return isShiftKeyDown();
    }

    public static boolean hasAltDown() {
        return isAltKeyDown();
    }

    public static boolean isCut(int p_231166_0_) {
        return isKeyComboCtrlX(p_231166_0_);
    }

    public static boolean isPaste(int p_231168_0_) {
        return isKeyComboCtrlV(p_231168_0_);
    }

    public static boolean isCopy(int p_231169_0_) {
        return isKeyComboCtrlC(p_231169_0_);
    }

    public static boolean isSelectAll(int p_231170_0_) {
        return isKeyComboCtrlA(p_231170_0_);
    }

}
