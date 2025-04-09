package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Matrix4f.class)
public interface IMixinMatrix4f {

    @Accessor("m00") float get_M00_FancyMenu();
    @Accessor("m01") float get_M01_FancyMenu();
    @Accessor("m02") float get_M02_FancyMenu();
    @Accessor("m03") float get_M03_FancyMenu();

    @Accessor("m10") float get_M10_FancyMenu();
    @Accessor("m11") float get_M11_FancyMenu();
    @Accessor("m12") float get_M12_FancyMenu();
    @Accessor("m13") float get_M13_FancyMenu();

    @Accessor("m20") float get_M20_FancyMenu();
    @Accessor("m21") float get_M21_FancyMenu();
    @Accessor("m22") float get_M22_FancyMenu();
    @Accessor("m23") float get_M23_FancyMenu();

    @Accessor("m30") float get_M30_FancyMenu();
    @Accessor("m31") float get_M31_FancyMenu();
    @Accessor("m32") float get_M32_FancyMenu();
    @Accessor("m33") float get_M33_FancyMenu();

}
