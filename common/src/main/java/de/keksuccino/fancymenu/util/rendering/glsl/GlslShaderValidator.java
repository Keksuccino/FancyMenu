package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.renderpearl.api.pipeline.ShaderType;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Recovers actionable diagnostics when Minecraft's public pipeline API only reports an invalid pipeline. The options
 * mirror Minecraft's Vulkan compiler closely so a message shown in FancyMenu points at the transformed source that the
 * backend actually consumes.
 */
final class GlslShaderValidator {


    private GlslShaderValidator() {
    }

    @NotNull
    static ValidationResult validate(@NotNull String vertexSource, @NotNull String fragmentSource) {
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        if (compiler == 0L || options == 0L) {
            if (options != 0L) {
                Shaderc.shaderc_compile_options_release(options);
            }
            if (compiler != 0L) {
                Shaderc.shaderc_compiler_release(compiler);
            }
            return new ValidationResult(List.of("Shaderc could not initialize; consult the game log for backend compiler diagnostics."));
        }

        try {
            Shaderc.shaderc_compile_options_set_target_env(options, Shaderc.shaderc_target_env_vulkan, Shaderc.shaderc_env_version_vulkan_1_2);
            Shaderc.shaderc_compile_options_set_auto_bind_uniforms(options, true);
            Shaderc.shaderc_compile_options_set_generate_debug_info(options);
            Shaderc.shaderc_compile_options_set_optimization_level(options, Shaderc.shaderc_optimization_level_zero);

            Shaderc.shaderc_compile_options_add_macro_definition(options, "gl_VertexID", "gl_VertexIndex");
            Shaderc.shaderc_compile_options_add_macro_definition(options, "gl_InstanceID", "gl_InstanceIndex");

            List<String> diagnostics = new ArrayList<>();
            validateStage(compiler, options, "vertex", vertexSource, ShaderType.VERTEX, diagnostics);
            validateStage(compiler, options, "fragment", fragmentSource, ShaderType.FRAGMENT, diagnostics);
            return new ValidationResult(List.copyOf(diagnostics));
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }

    private static void validateStage(long compiler, long options, @NotNull String label, @NotNull String source, @NotNull ShaderType type, @NotNull List<String> diagnostics) {
        ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, false);
        ByteBuffer filenameBuffer = MemoryUtil.memUTF8("fancymenu_runtime_" + label + ".glsl");
        ByteBuffer entryPointBuffer = MemoryUtil.memUTF8("main");
        long result = 0L;
        try {
            int shaderKind = type == ShaderType.FRAGMENT ? Shaderc.shaderc_fragment_shader : Shaderc.shaderc_vertex_shader;
            result = Shaderc.shaderc_compile_into_spv(compiler, sourceBuffer, shaderKind, filenameBuffer, entryPointBuffer, options);
            if (result == 0L) {
                diagnostics.add(label + " shader: Shaderc returned no compilation result.");
                return;
            }
            int status = Shaderc.shaderc_result_get_compilation_status(result);
            if (status != Shaderc.shaderc_compilation_status_success) {
                String message = Shaderc.shaderc_result_get_error_message(result);
                diagnostics.add(label + " shader: " + (message == null || message.isBlank() ? "unknown Shaderc compilation failure" : message.strip()));
            }
        } finally {
            if (result != 0L) {
                Shaderc.shaderc_result_release(result);
            }
            MemoryUtil.memFree(entryPointBuffer);
            MemoryUtil.memFree(filenameBuffer);
            MemoryUtil.memFree(sourceBuffer);
        }
    }

    record ValidationResult(@NotNull List<String> diagnostics) {

        boolean valid() {
            return this.diagnostics.isEmpty();
        }
    }
}
