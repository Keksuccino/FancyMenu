package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat;

import net.minecraft.client.Minecraft;

public class InputConstants {

    public static final int ENTER = 28;
    public static final int LEFT_CTRL;
    public static final int RIGHT_CTRL;
    public static final int LEFT_ALT = 56;
    public static final int RIGHT_ALT = 184;
    public static final int ESC = 1;
    public static final int ARROW_LEFT = 203;
    public static final int ARROW_RIGHT = 205;
    public static final int ARROW_UP = 200;
    public static final int ARROW_DOWN = 208;
    public static final int DELETE = 211;
    public static final int BACKSPACE = 14;
    public static final int TAB = 15;
    public static final int LEFT_SHIFT = 42;
    public static final int RIGHT_SHIFT = 54;
    public static final int CAPS_LOCK = 58;
    public static final int INSERT = 210;
    public static final int PRINT = 183;
    public static final int PAGE_UP = 201;
    public static final int PAGE_DOWN = 209;
    public static final int HOME = 199;
    public static final int END = 207;
    public static final int KEY_V = 47;
    public static final int KEY_C = 46;
    public static final int KEY_A = 30;
    public static final int KEY_S = 31;
    public static final int KEY_U = 22;
    public static final int KEY_Y = 21;
    public static final int KEY_Z = 44;
    public static final int KEY_X = 45;

    static {
        if (!Minecraft.IS_RUNNING_ON_MAC) {
            LEFT_CTRL = 29;
            RIGHT_CTRL = 157;
        } else {
            LEFT_CTRL = 219;
            RIGHT_CTRL = 220;
        }
    }

}
