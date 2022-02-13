package de.keksuccino.fancymenu.api.background.example.no_input_string;

import de.keksuccino.fancymenu.api.Nonnull;
import de.keksuccino.fancymenu.api.background.MenuBackground;
import de.keksuccino.fancymenu.api.background.MenuBackgroundType;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

//This is an example menu background that simply renders a color.
//The color can be set on construction.
public class ExampleMenuBackground extends MenuBackground {

    protected Color color;

    public ExampleMenuBackground(@Nonnull String uniqueBackgroundIdentifier, @Nonnull MenuBackgroundType type, Color color) {
        //The identifier needs to be UNIQUE!
        //It's not possible to register multiple backgrounds with the same identifier to the same background type.
        super(uniqueBackgroundIdentifier, type);
        //Will get rendered as background.
        this.color = color;
    }

    @Override
    public void onOpenMenu() {
        //Gets called when opening a NEW menu (not when resizing it).
        //If you want to reset stuff of your background instance when the menu changes, do it here.
    }

    //Here you will render the background instance.
    //You should always render backgrounds over the full size of the screen, otherwise it will look ugly.
    @Override
    public void render(MatrixStack matrix, Screen screen, boolean keepAspectRatio) {

        try {

            //Simply renders a colored background to the full size of the screen it is rendered in.
            //We will ignore the keepAspectRatio param here, because, well, a simple colored background has no aspect ratio.
            DrawableHelper.fill(matrix, 0, 0, screen.width, screen.height, this.color.getRGB());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
