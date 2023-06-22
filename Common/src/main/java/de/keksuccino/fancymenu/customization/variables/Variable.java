package de.keksuccino.fancymenu.customization.variables;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Variable {

    @NotNull
    public final String name;
    public String value;
    public boolean resetOnLaunch = false;

    public Variable(@NotNull String name) {
        this.name = Objects.requireNonNull(name);
    }

    @NotNull
    public PropertyContainer serialize() {
        PropertyContainer c = new PropertyContainer("variable");
        c.putProperty("name", Objects.requireNonNull(this.name));
        c.putProperty("value", Objects.requireNonNullElse(this.value, ""));
        c.putProperty("reset_on_launch", "" + this.resetOnLaunch);
        return c;
    }

    @Nullable
    public static Variable deserialize(@NotNull PropertyContainer c) {
        String name = c.getValue("name");
        if (name != null) {
            Variable v = new Variable(name);
            v.value = c.getValue("value");
            String resetOnLaunch = c.getValue("reset_on_launch");
            if ((resetOnLaunch != null) && resetOnLaunch.equals("true")) {
                v.resetOnLaunch = true;
            }
            return v;
        }
        return null;
    }

}
