package de.keksuccino.fancymenu.util.rendering.glsl;

import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlslShaderRuntimeTest {

    @Test
    void listVariablesGiveTheirEntryCountInsteadOfParsingTheirText() {
        GlslShaderRuntime.VariableUniformValue three = GlslShaderRuntime.toVariableUniformValue(UserVariableSnapshot.ofList("fruits", List.of("4", "5", "6"), false));
        GlslShaderRuntime.VariableUniformValue empty = GlslShaderRuntime.toVariableUniformValue(UserVariableSnapshot.ofList("empty", List.of(), false));
        GlslShaderRuntime.VariableUniformValue text = GlslShaderRuntime.toVariableUniformValue(new UserVariableSnapshot("text", "2.5", false));

        assertAll(() -> assertEquals(3.0F, three.floatValue()), () -> assertEquals(3, three.intValue()), () -> assertEquals(1, three.boolValue()), () -> assertVector(three, 3.0F),
                () -> assertEquals(0.0F, empty.floatValue()), () -> assertEquals(0, empty.intValue()), () -> assertEquals(0, empty.boolValue()), () -> assertVector(empty, 0.0F),
                () -> assertEquals(2.5F, text.floatValue()), () -> assertEquals(2, text.intValue()));
    }

    private static void assertVector(GlslShaderRuntime.VariableUniformValue value, float expected) {
        assertAll(() -> assertEquals(expected, value.vec2X()), () -> assertEquals(expected, value.vec2Y()), () -> assertEquals(expected, value.vec3X()), () -> assertEquals(expected, value.vec3Y()), () -> assertEquals(expected, value.vec3Z()),
                () -> assertEquals(expected, value.vec4X()), () -> assertEquals(expected, value.vec4Y()), () -> assertEquals(expected, value.vec4Z()), () -> assertEquals(expected, value.vec4W()));
    }

}
