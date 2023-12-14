package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.datafixers.util.Pair;
import de.keksuccino.fancymenu.util.UnoptimizedMixin;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;
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

    //TODO 1.18: check how to make NavigatableWidgets work in 1.18

//    /**
//     * @author Keksuccino (FancyMenu)
//     * @reason This is to add handling for {@link NavigatableWidget}s. Since you can't inject into interfaces, this is the only way to achieve that.
//     */
//    @Overwrite
//    default ComponentPath nextFocusPathInDirection(ScreenRectangle $$0, ScreenDirection $$1, GuiEventListener $$2, FocusNavigationEvent $$3) {
//
//        ScreenAxis $$4 = $$1.getAxis();
//        ScreenAxis $$5 = $$4.orthogonal();
//        ScreenDirection $$6 = $$5.getPositive();
//        int $$7 = $$0.getBoundInDirection($$1.getOpposite());
//        List<GuiEventListener> $$8 = new ArrayList<>();
//
//        List<GuiEventListener> filteredChildren = new ArrayList<>(this.children());
//        filteredChildren.removeIf(guiEventListener -> (guiEventListener instanceof NavigatableWidget n) && (!n.isNavigatable() || !n.isFocusable()));
//
//        for(GuiEventListener $$9 : filteredChildren) {
//            if ($$9 != $$2) {
//                ScreenRectangle $$10 = $$9.getRectangle();
//                if ($$10.overlapsInAxis($$0, $$5)) {
//                    int $$11 = $$10.getBoundInDirection($$1.getOpposite());
//                    if ($$1.isAfter($$11, $$7)) {
//                        $$8.add($$9);
//                    } else if ($$11 == $$7 && $$1.isAfter($$10.getBoundInDirection($$1), $$0.getBoundInDirection($$1))) {
//                        $$8.add($$9);
//                    }
//                }
//            }
//        }
//
//        Comparator<GuiEventListener> $$12 = Comparator.comparing(
//                $$1x -> $$1x.getRectangle().getBoundInDirection($$1.getOpposite()), $$1.coordinateValueComparator()
//        );
//        Comparator<GuiEventListener> $$13 = Comparator.comparing(
//                $$1x -> $$1x.getRectangle().getBoundInDirection($$6.getOpposite()), $$6.coordinateValueComparator()
//        );
//        $$8.sort($$12.thenComparing($$13));
//
//        for(GuiEventListener $$14 : $$8) {
//            ComponentPath $$15 = $$14.nextFocusPath($$3);
//            if ($$15 != null) {
//                return $$15;
//            }
//        }
//
//    }

    @Shadow @Nullable GuiEventListener getFocused();

    @Shadow List<? extends GuiEventListener> children();

}
