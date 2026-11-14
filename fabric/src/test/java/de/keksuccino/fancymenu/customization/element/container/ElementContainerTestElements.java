package de.keksuccino.fancymenu.customization.element.container;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Elements for tests of containers and their children. They never touch the screen or the game.
 */
final class ElementContainerTestElements {

    private ElementContainerTestElements() {
    }

    /** A plain element, which only ever asks its container where things are. */
    static class Child extends AbstractElement {

        Child(@NotNull String identifier) {
            super(null);
            this.setInstanceIdentifier(identifier);
            this.stayOnScreen = false;
        }

        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        }

        /** Auto-sizing asks the game for the size of the screen, which tests don't have. */
        @Override
        public void updateAutoSizing(boolean ignoreLastTickScreenSize) {
        }

    }

    /** A container with a geometry tests set directly. */
    static class Container extends Child implements ElementContainer {

        final List<AbstractElement> children = new ArrayList<>();
        int contentWidth = 300;
        int contentHeight = 600;
        int contentScreenX = 0;
        int contentScreenY = 0;
        float contentScale = 1.0F;
        boolean scalesChildVisuals = false;
        boolean managesChildBoxes = false;
        boolean showingChildren = true;
        float childOpacity = 1.0F;
        int visibleX = Integer.MIN_VALUE / 2;
        int visibleY = Integer.MIN_VALUE / 2;
        int visibleWidth = Integer.MAX_VALUE;
        int visibleHeight = Integer.MAX_VALUE;

        Container(@NotNull String identifier) {
            super(identifier);
        }

        @Override
        public @NotNull AbstractElement getContainerElement() {
            return this;
        }

        @Override
        public void setContainerChildren(@NotNull List<? extends AbstractElement> children) {
            this.children.clear();
            this.children.addAll(children);
        }

        @Override
        public int getContentWidth() {
            return this.contentWidth;
        }

        @Override
        public int getContentHeight() {
            return this.contentHeight;
        }

        @Override
        public int getContentScreenX() {
            return this.contentScreenX;
        }

        @Override
        public int getContentScreenY() {
            return this.contentScreenY;
        }

        @Override
        public float getContentScale() {
            return this.contentScale;
        }

        @Override
        public boolean scalesChildVisuals() {
            return this.scalesChildVisuals;
        }

        @Override
        public boolean managesChildBoxes() {
            return this.managesChildBoxes;
        }

        @Override
        public boolean isScreenPointVisible(double screenX, double screenY) {
            return (screenX >= this.visibleX) && (screenX < ((long) this.visibleX + this.visibleWidth)) && (screenY >= this.visibleY) && (screenY < ((long) this.visibleY + this.visibleHeight));
        }

        @Override
        public boolean isShowingChildren() {
            return this.showingChildren;
        }

        @Override
        public float getChildOpacity() {
            return this.childOpacity;
        }

    }

}
