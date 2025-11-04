package de.keksuccino.fancymenu.util.rendering.gui;

import org.joml.Matrix4f;

public class MatrixUtils {

    public static Matrix4f convertToJoml(com.mojang.math.Matrix4f mojangMatrix) {
        // Cast the Mojang matrix to the accessor interface
        GuiMatrix4f accessor = new GuiMatrix4f(mojangMatrix);
        // Remember: Mojang mij = row i, col j. JOML constructor wants column-major order.
        return new Matrix4f(
                accessor.getM00(), accessor.getM10(), accessor.getM20(), accessor.getM30(), // JOML Col 0 <-- Mojang Col 0
                accessor.getM01(), accessor.getM11(), accessor.getM21(), accessor.getM31(), // JOML Col 1 <-- Mojang Col 1
                accessor.getM02(), accessor.getM12(), accessor.getM22(), accessor.getM32(), // JOML Col 2 <-- Mojang Col 2
                accessor.getM03(), accessor.getM13(), accessor.getM23(), accessor.getM33()  // JOML Col 3 <-- Mojang Col 3
        );
    }

    public static boolean isMatrixIdentityMojang(com.mojang.math.Matrix4f mojangMatrix) {
        GuiMatrix4f accessor = new GuiMatrix4f(mojangMatrix);
        return accessor.getM00() == 1.0f && accessor.getM01() == 0.0f && accessor.getM02() == 0.0f && accessor.getM03() == 0.0f &&
                accessor.getM10() == 0.0f && accessor.getM11() == 1.0f && accessor.getM12() == 0.0f && accessor.getM13() == 0.0f &&
                accessor.getM20() == 0.0f && accessor.getM21() == 0.0f && accessor.getM22() == 1.0f && accessor.getM23() == 0.0f &&
                accessor.getM30() == 0.0f && accessor.getM31() == 0.0f && accessor.getM32() == 0.0f && accessor.getM33() == 1.0f;
    }

}
