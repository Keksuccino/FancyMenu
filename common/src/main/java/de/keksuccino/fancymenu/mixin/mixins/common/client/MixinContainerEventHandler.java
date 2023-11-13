package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.datafixers.util.Pair;
import de.keksuccino.fancymenu.util.UnoptimizedMixin;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@UnoptimizedMixin("This class overrides methods. At the moment it seems like there's no other way.")
@Mixin(ContainerEventHandler.class)
public interface MixinContainerEventHandler {

    /**
     * @author Keksuccino (FancyMenu)
     * @reason This is to add handling for {@link NavigatableWidget}s. Since you can't inject into interfaces, this is the only way to achieve that.
     */
    @Overwrite
    default ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation navigation) {

        boolean flag = navigation.forward();
        GuiEventListener guieventlistener = this.getFocused();
        List<? extends GuiEventListener> list = new ArrayList<>(this.children());
        list.removeIf(guiEventListener -> (guiEventListener instanceof NavigatableWidget n) && (!n.isFocusable() || !n.isNavigatable()));
        list.sort(Comparator.comparingInt(TabOrderedElement::getTabOrderGroup));
        int j = list.indexOf(guieventlistener);
        int i;
        if (guieventlistener != null && j >= 0) {
            i = j + (flag ? 1 : 0);
        } else if (flag) {
            i = 0;
        } else {
            i = list.size();
        }

        ListIterator<? extends GuiEventListener> listiterator = list.listIterator(i);
        BooleanSupplier booleansupplier = flag ? listiterator::hasNext : listiterator::hasPrevious;
        Supplier<? extends GuiEventListener> supplier = flag ? listiterator::next : listiterator::previous;

        while(booleansupplier.getAsBoolean()) {
            GuiEventListener guieventlistener1 = supplier.get();
            ComponentPath componentpath = guieventlistener1.nextFocusPath(navigation);
            if (componentpath != null) {
                return ComponentPath.path((ContainerEventHandler)((Object)this), componentpath);
            }
        }

        return null;

    }

    /**
     * @author Keksuccino (FancyMenu)
     * @reason This is to add handling for {@link NavigatableWidget}s. Since you can't inject into interfaces, this is the only way to achieve that.
     */
    @Overwrite
    default ComponentPath nextFocusPathInDirection(ScreenRectangle $$0, ScreenDirection $$1, GuiEventListener $$2, FocusNavigationEvent $$3) {

        ScreenAxis $$4 = $$1.getAxis();
        ScreenAxis $$5 = $$4.orthogonal();
        ScreenDirection $$6 = $$5.getPositive();
        int $$7 = $$0.getBoundInDirection($$1.getOpposite());
        List<GuiEventListener> $$8 = new ArrayList<>();

        List<GuiEventListener> filteredChildren = new ArrayList<>(this.children());
        filteredChildren.removeIf(guiEventListener -> (guiEventListener instanceof NavigatableWidget n) && (!n.isNavigatable() || !n.isFocusable()));

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

        return this.nextFocusPathVaguelyInDirectionFancyMenu($$0, $$1, $$2, $$3);

    }

    @Unique
    default ComponentPath nextFocusPathVaguelyInDirectionFancyMenu(ScreenRectangle $$0, ScreenDirection $$1, GuiEventListener $$2, FocusNavigationEvent $$3) {

        ScreenAxis $$4 = $$1.getAxis();
        ScreenAxis $$5 = $$4.orthogonal();
        List<Pair<GuiEventListener, Long>> $$6 = new ArrayList<>();
        ScreenPosition $$7 = ScreenPosition.of($$4, $$0.getBoundInDirection($$1), $$0.getCenterInAxis($$5));

        List<GuiEventListener> filteredChildren = new ArrayList<>(this.children());
        filteredChildren.removeIf(guiEventListener -> (guiEventListener instanceof NavigatableWidget n) && (!n.isNavigatable() || !n.isFocusable()));

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

    @Shadow @Nullable GuiEventListener getFocused();

    @Shadow List<? extends GuiEventListener> children();

}
