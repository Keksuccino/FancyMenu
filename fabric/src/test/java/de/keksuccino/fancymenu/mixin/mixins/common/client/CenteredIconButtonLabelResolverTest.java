package de.keksuccino.fancymenu.mixin.mixins.common.client;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.Nullable;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CenteredIconButtonLabelResolverTest {

    private static final Component CUSTOM_LABEL = Component.literal("custom");
    private static final Component HOVER_LABEL = Component.literal("hover");

    @ParameterizedTest
    @MethodSource("labelSelectionCases")
    void resolvesEveryLabelAndWidgetStateCombination(@Nullable Component customLabel, @Nullable Component hoverLabel, boolean hoveredOrFocused, boolean visible, boolean active, @Nullable Component expected) {
        assertSame(expected, CenteredIconButtonLabelResolver.resolve(customLabel, hoverLabel, hoveredOrFocused, visible, active));
    }

    @Test
    void preservesAnExplicitEmptyLabel() {
        Component emptyLabel = Component.empty();
        assertSame(emptyLabel, CenteredIconButtonLabelResolver.resolve(emptyLabel, HOVER_LABEL, false, true, true));
    }

    @Test
    void clearingLabelsRestoresTheIconPath() {
        assertNull(CenteredIconButtonLabelResolver.resolve(null, null, true, true, true));
    }

    private static Stream<Arguments> labelSelectionCases() {
        Stream.Builder<Arguments> cases = Stream.builder();
        for (boolean customLabelPresent : new boolean[]{false, true}) {
            for (boolean hoverLabelPresent : new boolean[]{false, true}) {
                for (boolean hoveredOrFocused : new boolean[]{false, true}) {
                    for (boolean visible : new boolean[]{false, true}) {
                        for (boolean active : new boolean[]{false, true}) {
                            Component customLabel = customLabelPresent ? CUSTOM_LABEL : null;
                            Component hoverLabel = hoverLabelPresent ? HOVER_LABEL : null;
                            Component expected = hoverLabel != null && hoveredOrFocused && visible && active ? hoverLabel : customLabel;
                            cases.add(Arguments.of(customLabel, hoverLabel, hoveredOrFocused, visible, active, expected));
                        }
                    }
                }
            }
        }
        return cases.build();
    }

}
