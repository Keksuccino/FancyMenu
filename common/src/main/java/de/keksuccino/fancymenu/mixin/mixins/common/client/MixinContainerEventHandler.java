package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.*;
import org.apache.logging.log4j.LogManager;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import org.spongepowered.asm.mixin.Shadow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(ContainerEventHandler.class)
public interface MixinContainerEventHandler {

    /**
     * @author Keksuccino (FancyMenu)
     * @reason This is to add handling for {@link NavigatableWidget}s. Since you can't inject into interfaces, this is the only way to achieve that.
     */
    @Overwrite
    default ComponentPath nextFocusPathInDirection(ScreenRectangle $$0, ScreenDirection $$1, GuiEventListener $$2, FocusNavigationEvent $$3) {

        LogManager.getLogger().info("########################## NEXT FOCUS PATH IN DIRECTION");

        ScreenAxis $$4 = $$1.getAxis();
        ScreenAxis $$5 = $$4.orthogonal();
        ScreenDirection $$6 = $$5.getPositive();
        int $$7 = $$0.getBoundInDirection($$1.getOpposite());
        List<GuiEventListener> $$8 = new ArrayList<>();

        List<GuiEventListener> filteredChildren = new ArrayList<>(this.children());
        filteredChildren.removeIf(guiEventListener -> (guiEventListener instanceof NavigatableWidget n) && !n.isNavigatable());

        for(GuiEventListener $$9 : filteredChildren) {
            if ($$9 != $$2) {
                ScreenRectangle $$10 = $$9.getRectangle();
                if ($$10.overlapsInAxis($$0, $$5)) {
                    int $$11 = $$10.getBoundInDirection($$1.getOpposite());
                    if ($$1.isAfter($$11, $$7)) {
                        $$8.add($$9);
                    } else if ($$11 == $$7 && $$1.isAfter($$10.getBoundInDirection($$1), $$0.getBoundInDirection($$1))) {
                        $$8.add($$9);
                    }
                }
            }
        }

        Comparator<GuiEventListener> $$12 = Comparator.comparing(
                $$1x -> $$1x.getRectangle().getBoundInDirection($$1.getOpposite()), $$1.coordinateValueComparator()
        );
        Comparator<GuiEventListener> $$13 = Comparator.comparing(
                $$1x -> $$1x.getRectangle().getBoundInDirection($$6.getOpposite()), $$6.coordinateValueComparator()
        );
        $$8.sort($$12.thenComparing($$13));

        for(GuiEventListener $$14 : $$8) {
            ComponentPath $$15 = $$14.nextFocusPath($$3);
            if ($$15 != null) {
                return $$15;
            }
        }

        return this.nextFocusPathVaguelyInDirection($$0, $$1, $$2, $$3);

    }

    /**
     * @author Keksuccino (FancyMenu)
     * @reason This is to add handling for {@link NavigatableWidget}s. Since you can't inject into interfaces, this is the only way to achieve that.
     */
    @Overwrite
    default ComponentPath nextFocusPathVaguelyInDirection(ScreenRectangle $$0, ScreenDirection $$1, GuiEventListener $$2, FocusNavigationEvent $$3) {

        LogManager.getLogger().info("########################## NEXT FOCUS PATH VAGUELY IN DIRECTION");

        ScreenAxis $$4 = $$1.getAxis();
        ScreenAxis $$5 = $$4.orthogonal();
        List<Pair<GuiEventListener, Long>> $$6 = new ArrayList<>();
        ScreenPosition $$7 = ScreenPosition.of($$4, $$0.getBoundInDirection($$1), $$0.getCenterInAxis($$5));

        List<GuiEventListener> filteredChildren = new ArrayList<>(this.children());
        filteredChildren.removeIf(guiEventListener -> (guiEventListener instanceof NavigatableWidget n) && !n.isNavigatable());

        for(GuiEventListener $$8 : filteredChildren) {
            if ($$8 != $$2) {
                ScreenRectangle $$9 = $$8.getRectangle();
                ScreenPosition $$10 = ScreenPosition.of($$4, $$9.getBoundInDirection($$1.getOpposite()), $$9.getCenterInAxis($$5));
                if ($$1.isAfter($$10.getCoordinate($$4), $$7.getCoordinate($$4))) {
                    long $$11 = Vector2i.distanceSquared($$7.x(), $$7.y(), $$10.x(), $$10.y());
                    $$6.add(Pair.of($$8, $$11));
                }
            }
        }

        $$6.sort(Comparator.comparingDouble(Pair::getSecond));

        for(Pair<GuiEventListener, Long> $$12 : $$6) {
            ComponentPath $$13 = $$12.getFirst().nextFocusPath($$3);
            if ($$13 != null) {
                return $$13;
            }
        }

        return null;

    }

    @Shadow List<? extends GuiEventListener> children();

}
