package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ActiveEffectPlaceholderTest {

    @BeforeAll
    static void bootStrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void effectsAreIndexedInReverseNaturalOrder() {
        MobEffectInstance shorterEffect = new MobEffectInstance(MobEffects.SPEED, 200);
        MobEffectInstance longerEffect = new MobEffectInstance(MobEffects.HASTE, 600);
        List<MobEffectInstance> effects = List.of(shorterEffect, longerEffect);

        assertSame(longerEffect, ActiveEffectPlaceholder.getEffectAt(effects, 0));
        assertSame(shorterEffect, ActiveEffectPlaceholder.getEffectAt(effects, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -2, Integer.MIN_VALUE, 2, Integer.MAX_VALUE})
    void indexesOutsideOfTheActiveEffectsReturnNoEffect(int index) {
        List<MobEffectInstance> effects = List.of(new MobEffectInstance(MobEffects.SPEED, 200), new MobEffectInstance(MobEffects.HASTE, 600));

        assertNull(ActiveEffectPlaceholder.getEffectAt(effects, index));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void noActiveEffectsReturnNoEffect(int index) {
        assertNull(ActiveEffectPlaceholder.getEffectAt(List.of(), index));
    }

}
