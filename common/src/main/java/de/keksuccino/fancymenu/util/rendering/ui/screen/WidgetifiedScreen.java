package de.keksuccino.fancymenu.util.rendering.ui.screen;

import net.minecraft.client.gui.components.AbstractWidget;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import  net.minecraft.client.gui.screens.Screen;

/**
 * Classes annotated with {@link WidgetifiedScreen} replace parts of a {@link Screen} with {@link AbstractWidget}s, to make said parts customizable.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface WidgetifiedScreen {
}
