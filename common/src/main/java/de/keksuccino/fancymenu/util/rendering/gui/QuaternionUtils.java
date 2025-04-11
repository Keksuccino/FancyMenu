package de.keksuccino.fancymenu.util.rendering.gui;

import com.mojang.math.Quaternion;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class QuaternionUtils {

    @NotNull
    public static Quaternion toMojangQuaternion(@NotNull Quaternionf jomlQuaternionf) {
        return new Quaternion(
                jomlQuaternionf.x(), // Corresponds to Mojang's i
                jomlQuaternionf.y(), // Corresponds to Mojang's j
                jomlQuaternionf.z(), // Corresponds to Mojang's k
                jomlQuaternionf.w()  // Corresponds to Mojang's r
        );
    }

}
