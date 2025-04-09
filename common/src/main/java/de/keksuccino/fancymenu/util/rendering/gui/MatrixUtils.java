package de.keksuccino.fancymenu.util.rendering.gui;

import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinMatrix4f;
import org.joml.Matrix4f;

public class MatrixUtils {

    public static Matrix4f convertToJoml(com.mojang.math.Matrix4f mojangMatrix) {
        // Cast the Mojang matrix to your accessor interface
        IMixinMatrix4f accessor = (IMixinMatrix4f) mojangMatrix;

        // Now use the accessor methods to get the fields
        // Remember: Mojang mij = row i, col j. JOML constructor wants column-major order.
        return new Matrix4f(
                accessor.get_M00_FancyMenu(), accessor.get_M10_FancyMenu(), accessor.get_M20_FancyMenu(), accessor.get_M30_FancyMenu(), // JOML Col 0 <-- Mojang Col 0
                accessor.get_M01_FancyMenu(), accessor.get_M11_FancyMenu(), accessor.get_M21_FancyMenu(), accessor.get_M31_FancyMenu(), // JOML Col 1 <-- Mojang Col 1
                accessor.get_M02_FancyMenu(), accessor.get_M12_FancyMenu(), accessor.get_M22_FancyMenu(), accessor.get_M32_FancyMenu(), // JOML Col 2 <-- Mojang Col 2
                accessor.get_M03_FancyMenu(), accessor.get_M13_FancyMenu(), accessor.get_M23_FancyMenu(), accessor.get_M33_FancyMenu()  // JOML Col 3 <-- Mojang Col 3
        );
    }

    // Example using the converted Mojang utility methods from the previous response
    public static boolean isMatrixIdentityMojang(com.mojang.math.Matrix4f mojangMatrix) {
        IMixinMatrix4f accessor = (IMixinMatrix4f) mojangMatrix;
        return accessor.get_M00_FancyMenu() == 1.0f && accessor.get_M01_FancyMenu() == 0.0f && accessor.get_M02_FancyMenu() == 0.0f && accessor.get_M03_FancyMenu() == 0.0f &&
                accessor.get_M10_FancyMenu() == 0.0f && accessor.get_M11_FancyMenu() == 1.0f && accessor.get_M12_FancyMenu() == 0.0f && accessor.get_M13_FancyMenu() == 0.0f &&
                accessor.get_M20_FancyMenu() == 0.0f && accessor.get_M21_FancyMenu() == 0.0f && accessor.get_M22_FancyMenu() == 1.0f && accessor.get_M23_FancyMenu() == 0.0f &&
                accessor.get_M30_FancyMenu() == 0.0f && accessor.get_M31_FancyMenu() == 0.0f && accessor.get_M32_FancyMenu() == 0.0f && accessor.get_M33_FancyMenu() == 1.0f;
    }

}
