package de.keksuccino.fancymenu.util.rendering.gui;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

@FunctionalInterface
public interface Axis {

    Axis XN = Vector3f.XN::rotation;
    Axis XP = Vector3f.XP::rotation;
    Axis YN = Vector3f.YN::rotation;
    Axis YP = Vector3f.YP::rotation;
    Axis ZN = Vector3f.ZN::rotation;
    Axis ZP = Vector3f.ZP::rotation;

    static Axis of(Vector3f axis) {
        return axis::rotation;
    }

    Quaternion rotation(float f);

    default Quaternion rotationDegrees(float f) {
        return this.rotation(f * ((float)Math.PI / 180F));
    }

}
