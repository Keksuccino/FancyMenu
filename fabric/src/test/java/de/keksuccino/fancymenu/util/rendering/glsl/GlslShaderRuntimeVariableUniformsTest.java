package de.keksuccino.fancymenu.util.rendering.glsl;

import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlslShaderRuntimeVariableUniformsTest {

    @Test
    void listVariablesGiveTheirEntryCountInsteadOfParsingTheirText() {
        GlslShaderRuntime.VariableUniformValue three = GlslShaderRuntime.toVariableUniformValue(UserVariableSnapshot.ofList("fruits", List.of("4", "5", "6"), false));
        GlslShaderRuntime.VariableUniformValue empty = GlslShaderRuntime.toVariableUniformValue(UserVariableSnapshot.ofList("empty", List.of(), false));
        GlslShaderRuntime.VariableUniformValue text = GlslShaderRuntime.toVariableUniformValue(new UserVariableSnapshot("text", "2.5", false));

        assertAll(() -> assertEquals(3.0F, three.floatValue()), () -> assertEquals(3, three.intValue()), () -> assertEquals(1, three.boolValue()), () -> assertEquals(3.0F, three.vec2X()), () -> assertEquals(3.0F, three.vec3Z()), () -> assertEquals(3.0F, three.vec4W()),
                () -> assertEquals(0.0F, empty.floatValue()), () -> assertEquals(0, empty.intValue()), () -> assertEquals(0, empty.boolValue()), () -> assertEquals(0.0F, empty.vec2Y()), () -> assertEquals(0.0F, empty.vec4W()),
                () -> assertEquals(2.5F, text.floatValue()), () -> assertEquals(2, text.intValue()));
    }

}
