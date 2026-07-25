package de.keksuccino.fancymenu.util.rendering.glsl;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import net.minecraft.client.renderer.ShaderDefines;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the legacy loose-uniform shader dialect exposed by FancyMenu into the uniform-buffer dialect required by
 * Minecraft's backend-neutral pipeline API. The transformation stays independent of a Minecraft GPU device. Shaderc
 * preprocessing is used only when directives can alter uniforms or required entry points, leaving all other device
 * feature checks for the GPU backend that will actually compile the transformed shader.
 */
public final class GlslShaderSourceTransformer {

    public static final String UNIFORM_BLOCK_NAME = "FancyMenuUniforms";
    public static final String RENDER_AREA_UNIFORM = "fmRenderArea_FancyMenu";
    public static final String RENDER_TARGET_SIZE_UNIFORM = "fmRenderTargetSize_FancyMenu";

    private static final String GLSL_VERSION = "#version 330";
    private static final String PREPROCESS_SOURCE_NAME = "fancymenu_runtime_fragment_preprocess.glsl";
    private static final ShaderDefines VULKAN_GLOBAL_DEFINES = ShaderDefines.builder().define("gl_VertexID", "gl_VertexIndex").define("gl_InstanceID", "gl_InstanceIndex").build();
    private static final Pattern VERSION_DIRECTIVE_PATTERN = Pattern.compile("(?m)^\\s*#version\\s+.+$");
    private static final Pattern PRECISION_DIRECTIVE_PATTERN = Pattern.compile("(?m)^\\s*precision\\s+\\w+\\s+\\w+\\s*;\\s*$");
    private static final Set<String> DECLARATION_QUALIFIERS = Set.of("highp", "mediump", "lowp", "coherent", "volatile", "restrict", "readonly", "writeonly", "precise");
    private static final Set<String> RESERVED_INTERNAL_UNIFORMS = Set.of(RENDER_AREA_UNIFORM, RENDER_TARGET_SIZE_UNIFORM);
    private static final List<GlslStd140Layout.Declaration> BUILT_IN_UNIFORMS = List.of(
            declaration("vec4", RENDER_AREA_UNIFORM),
            declaration("vec2", RENDER_TARGET_SIZE_UNIFORM),
            declaration("vec3", "iResolution"),
            declaration("float", "iTime"),
            declaration("float", "iTimeDelta"),
            declaration("float", "iFrameRate"),
            declaration("int", "iFrame"),
            declaration("vec4", "iMouse"),
            declaration("vec4", "iDate"),
            declaration("float", "iSampleRate"),
            declaration("float", "iChannelTime", 4),
            declaration("vec3", "iChannelResolution", 4),
            declaration("vec2", "fmAreaOffset"),
            declaration("vec2", "fmAreaSize"),
            declaration("vec2", "fmAreaPosition"),
            declaration("vec2", "fmAreaTopLeft"),
            declaration("vec2", "fmScreenSize"),
            declaration("float", "fmGuiScale"),
            declaration("vec4", "fmMouse"),
            declaration("vec2", "fmMouseDelta"),
            declaration("ivec4", "fmMouseButtons"),
            declaration("ivec4", "fmMouseClickCount"),
            declaration("ivec4", "fmMouseReleaseCount"),
            declaration("vec2", "fmMouseScroll"),
            declaration("vec2", "fmMouseScrollTotal"),
            declaration("ivec4", "fmKeyEvent"),
            declaration("int", "fmKeyEventCount"),
            declaration("ivec4", "fmCharEvent"),
            declaration("int", "fmCharEventCount"),
            declaration("ivec4", "fmDateParts"),
            declaration("ivec4", "fmTimeParts"),
            declaration("int", "fmDayOfYear"),
            declaration("int", "fmWeekOfYear"),
            declaration("int", "fmUnixTimeSeconds"),
            declaration("int", "fmUnixTimeMilliseconds"),
            declaration("float", "fmPartialTick"),
            declaration("float", "fmGameDeltaTicks"),
            declaration("float", "fmRealtimeDeltaTicks"),
            declaration("int", "fmInWorld"),
            declaration("int", "fmIsPaused"),
            declaration("float", "fmOpacity"),
            declaration("int", "fmVariableCount")
    );
    private static final Map<String, GlslStd140Layout.Declaration> BUILT_INS_BY_NAME = indexDeclarations(BUILT_IN_UNIFORMS);
    private static final List<SamplerDeclaration> BUILT_IN_SAMPLERS = List.of(
            new SamplerDeclaration("sampler2D", "iChannel0"),
            new SamplerDeclaration("sampler2D", "iChannel1"),
            new SamplerDeclaration("sampler2D", "iChannel2"),
            new SamplerDeclaration("sampler2D", "iChannel3")
    );
    private static final Map<String, SamplerDeclaration> BUILT_IN_SAMPLERS_BY_NAME = indexSamplers(BUILT_IN_SAMPLERS);

    private GlslShaderSourceTransformer() {
    }

    @NotNull
    public static String normalizeSource(@NotNull String source) {
        String normalized = source.replace('\uFEFF', ' ');
        normalized = normalized.replace("\r\n", "\n").replace('\r', '\n');
        String masked = maskCommentsAndStrings(normalized);
        List<SourceRange> directiveRanges = new ArrayList<>();
        collectMatches(VERSION_DIRECTIVE_PATTERN.matcher(masked), directiveRanges);
        collectMatches(PRECISION_DIRECTIVE_PATTERN.matcher(masked), directiveRanges);
        return blankRanges(normalized, directiveRanges).strip();
    }

    private static void collectMatches(@NotNull Matcher matcher, @NotNull List<SourceRange> ranges) {
        while (matcher.find()) {
            ranges.add(new SourceRange(matcher.start(), matcher.end()));
        }
    }

    @NotNull
    public static List<FragmentVariant> buildFragmentVariants(@NotNull String source, @NotNull GlslShaderRuntime.CompileMode compileMode, boolean forceShadertoyCompatibility, @NotNull BackendCoordinates backendCoordinates, @NotNull PassKind passKind) {
        String normalizedSource = normalizeSource(source);
        UniformTransformation uniformTransformation = transformUniformsSelectively(normalizedSource, compileMode, backendCoordinates);
        String transformedUserSource = uniformTransformation.source();
        String entryPointSource = maskCommentsAndStrings(transformedUserSource);
        boolean hasMainImage = containsFunction(entryPointSource, "mainImage");
        boolean hasMain = containsFunction(entryPointSource, "main");
        boolean referencesGlFragColor = containsIdentifier(entryPointSource, "gl_FragColor");
        boolean definesOutColor = Pattern.compile("(?m)^\\s*(?:layout\\s*\\([^)]*\\)\\s*)?out\\s+vec4\\s+\\w+").matcher(entryPointSource).find();

        List<VariantSource> sources = new ArrayList<>();
        if (compileMode == GlslShaderRuntime.CompileMode.SHADERTOY) {
            if (hasMainImage) {
                sources.add(new VariantSource("shadertoy", buildShadertoyFragment(transformedUserSource, uniformTransformation, backendCoordinates, passKind, hasMain)));
            }
        } else {
            if (compileMode == GlslShaderRuntime.CompileMode.AUTO && forceShadertoyCompatibility && hasMainImage) {
                sources.add(new VariantSource("shadertoy", buildShadertoyFragment(transformedUserSource, uniformTransformation, backendCoordinates, passKind, hasMain)));
            }
            if ((compileMode == GlslShaderRuntime.CompileMode.DIRECT || compileMode == GlslShaderRuntime.CompileMode.AUTO) && hasMain) {
                if (definesOutColor && !referencesGlFragColor) {
                    sources.add(new VariantSource("direct_no_compat", buildDirectFragment(transformedUserSource, uniformTransformation, backendCoordinates, passKind, false)));
                    sources.add(new VariantSource("direct_glfragcolor_compat", buildDirectFragment(transformedUserSource, uniformTransformation, backendCoordinates, passKind, true)));
                } else {
                    sources.add(new VariantSource("direct_glfragcolor_compat", buildDirectFragment(transformedUserSource, uniformTransformation, backendCoordinates, passKind, true)));
                    sources.add(new VariantSource("direct_no_compat", buildDirectFragment(transformedUserSource, uniformTransformation, backendCoordinates, passKind, false)));
                }
            }
            if (compileMode == GlslShaderRuntime.CompileMode.AUTO && !forceShadertoyCompatibility && hasMainImage) {
                sources.add(new VariantSource("shadertoy", buildShadertoyFragment(transformedUserSource, uniformTransformation, backendCoordinates, passKind, hasMain)));
            }
        }

        if ((!hasMainImage && !hasMain) || sources.isEmpty()) {
            return List.of();
        }

        String vertexSource = buildVertexSource(uniformTransformation.blockDeclaration(), backendCoordinates, passKind);
        CoordinateConvention coordinateConvention = CoordinateConvention.forPass(backendCoordinates, passKind);
        List<FragmentVariant> variants = new ArrayList<>(sources.size());
        for (VariantSource variantSource : sources) {
            String identity = contentIdentity(compileMode.name(), Boolean.toString(forceShadertoyCompatibility), backendCoordinates.name(), passKind.name(), variantSource.label(), vertexSource, variantSource.source());
            variants.add(new FragmentVariant(variantSource.label(), variantSource.source(), vertexSource, uniformTransformation.layout(), uniformTransformation.activeSamplerNames(), identity, coordinateConvention));
        }
        return List.copyOf(variants);
    }

    @NotNull
    private static UniformTransformation transformUniformsSelectively(@NotNull String source, @NotNull GlslShaderRuntime.CompileMode compileMode, @NotNull BackendCoordinates backendCoordinates) {
        if (requiresPreprocessing(source, compileMode)) {
            return transformUniforms(preprocessSource(source, backendCoordinates));
        }
        try {
            return transformUniforms(source);
        } catch (ShaderTransformException rawFailure) {
            // An opaque macro form can evade the conservative syntax scan but reveal itself when the raw uniform
            // parser rejects the declaration. Preserve that original diagnostic if unrelated device directives make
            // Shaderc's speculative preprocessing fail before it can provide a structurally useful result.
            try {
                return transformUniforms(preprocessSource(source, backendCoordinates));
            } catch (ShaderTransformException fallbackFailure) {
                if (fallbackFailure.diagnostics().stream().anyMatch(diagnostic -> diagnostic.startsWith(backendCoordinates == BackendCoordinates.VULKAN ? "Vulkan shader preprocessing failed:" : "OpenGL shader preprocessing failed:"))) {
                    throw rawFailure;
                }
                throw fallbackFailure;
            }
        }
    }

    /**
     * Shaderc's OpenGL preprocessor advertises extensions independently of the active OpenGL driver. Feeding every
     * shader through it can therefore select a device-specific branch which the real backend cannot compile. This
     * scan does not evaluate a single conditional expression; it only detects whether directives structurally touch
     * uniforms or entry points whose concrete shape FancyMenu must know before it can build a pipeline.
     */
    private static boolean requiresPreprocessing(@NotNull String source, @NotNull GlslShaderRuntime.CompileMode compileMode) {
        List<PreprocessorDirective> directives = findPreprocessorDirectives(source);
        if (directives.isEmpty()) {
            return false;
        }
        if (hasMalformedConditionalStructure(directives)) {
            return true;
        }

        Map<String, List<MacroDefinition>> macroDefinitions = findMacroDefinitions(directives);
        Set<String> macroNames = macroDefinitions.keySet();
        List<Token> tokens = tokenize(source);
        for (SourceRange uniformRange : findRawUniformDeclarationRanges(source, tokens, directives)) {
            if (conditionalDepthAt(directives, uniformRange.start()) > 0 || rangeReferencesAnyMacro(tokens, uniformRange, macroNames)) {
                return true;
            }
        }

        Set<String> uniformGeneratingMacros = resolveStructuralMacros(macroDefinitions, Set.of("uniform"));
        if (referencesAnyMacroOutsideDirectives(tokens, directives, uniformGeneratingMacros)) {
            return true;
        }

        Set<String> requiredEntryNames = compileMode == GlslShaderRuntime.CompileMode.DIRECT ? Set.of("main") : Set.of("main", "mainImage");
        Set<String> entryGeneratingMacros = resolveEntryGeneratingMacros(macroDefinitions, requiredEntryNames);
        if (referencesAnyMacroOutsideDirectives(tokens, directives, entryGeneratingMacros)) {
            return true;
        }

        Set<String> voidGeneratingMacros = resolveStructuralMacros(macroDefinitions, Set.of("void"));
        for (int i = 0; i + 1 < tokens.size(); i++) {
            Token macro = tokens.get(i);
            if (!macroNames.contains(macro.text()) || isInPreprocessorDirective(directives, macro.start()) || !tokens.get(i + 1).text().equals("(")) {
                continue;
            }
            int closingParenthesis = findClosingToken(tokens, i + 1, "(", ")");
            if (closingParenthesis < 0) {
                continue;
            }
            boolean suppliesUniform = rangeContainsAnyIdentifier(tokens, i + 2, closingParenthesis, Set.of("uniform"), uniformGeneratingMacros);
            boolean suppliesEntryName = rangeContainsAnyIdentifier(tokens, i + 2, closingParenthesis, requiredEntryNames, entryGeneratingMacros);
            boolean suppliesVoid = rangeContainsAnyIdentifier(tokens, i + 2, closingParenthesis, Set.of("void"), voidGeneratingMacros);
            if (suppliesUniform || suppliesEntryName && suppliesVoid) {
                return true;
            }
        }
        for (int i = 0; i + 2 < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (isInPreprocessorDirective(directives, token.start())) {
                continue;
            }
            Token name = tokens.get(i + 1);
            Token openingParenthesis = tokens.get(i + 2);
            if (token.text().equals("void") && openingParenthesis.text().equals("(")) {
                if (requiredEntryNames.contains(name.text()) && (conditionalDepthAt(directives, token.start()) > 0 || macroNames.contains(name.text()))) {
                    return true;
                }
                if (macroNames.contains(name.text()) && entryGeneratingMacros.contains(name.text())) {
                    return true;
                }
            }
            if (!voidGeneratingMacros.contains(token.text())) {
                continue;
            }
            if (requiredEntryNames.contains(name.text()) && openingParenthesis.text().equals("(")) {
                return true;
            }
            if (!name.text().equals("(")) {
                continue;
            }
            int closingParenthesis = findClosingToken(tokens, i + 1, "(", ")");
            if (closingParenthesis > i + 1 && rangeContainsAnyIdentifier(tokens, i + 2, closingParenthesis, requiredEntryNames, entryGeneratingMacros)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static List<PreprocessorDirective> findPreprocessorDirectives(@NotNull String source) {
        String masked = maskCommentsAndStrings(source);
        List<PreprocessorDirective> directives = new ArrayList<>();
        int lineStart = 0;
        while (lineStart < masked.length()) {
            int lineEnd = masked.indexOf('\n', lineStart);
            if (lineEnd < 0) {
                lineEnd = masked.length();
            }
            int firstContent = lineStart;
            while (firstContent < lineEnd && Character.isWhitespace(masked.charAt(firstContent))) {
                firstContent++;
            }
            if (firstContent >= lineEnd || masked.charAt(firstContent) != '#') {
                lineStart = lineEnd < masked.length() ? lineEnd + 1 : masked.length();
                continue;
            }

            int logicalEnd = lineEnd;
            while (logicalEnd < masked.length() && logicalEnd > lineStart && masked.charAt(logicalEnd - 1) == '\\') {
                int nextLineEnd = masked.indexOf('\n', logicalEnd + 1);
                logicalEnd = nextLineEnd < 0 ? masked.length() : nextLineEnd;
            }
            int keywordStart = firstContent + 1;
            while (keywordStart < logicalEnd && Character.isWhitespace(masked.charAt(keywordStart))) {
                keywordStart++;
            }
            int keywordEnd = keywordStart;
            while (keywordEnd < logicalEnd && isIdentifierPart(masked.charAt(keywordEnd))) {
                keywordEnd++;
            }
            String name = masked.substring(keywordStart, keywordEnd);
            directives.add(new PreprocessorDirective(name, firstContent, logicalEnd, masked.substring(keywordEnd, logicalEnd)));
            lineStart = logicalEnd < masked.length() ? logicalEnd + 1 : masked.length();
        }
        return List.copyOf(directives);
    }

    private static boolean hasMalformedConditionalStructure(@NotNull List<PreprocessorDirective> directives) {
        Deque<Boolean> elseBranches = new ArrayDeque<>();
        for (PreprocessorDirective directive : directives) {
            switch (directive.name()) {
                case "if" -> {
                    if (directive.body().isBlank()) {
                        return true;
                    }
                    elseBranches.push(false);
                }
                case "ifdef", "ifndef" -> {
                    if (firstIdentifier(directive.body()) == null) {
                        return true;
                    }
                    elseBranches.push(false);
                }
                case "elif" -> {
                    if (elseBranches.isEmpty() || elseBranches.peek() || directive.body().isBlank()) {
                        return true;
                    }
                }
                case "else" -> {
                    if (elseBranches.isEmpty() || elseBranches.pop()) {
                        return true;
                    }
                    elseBranches.push(true);
                }
                case "endif" -> {
                    if (elseBranches.isEmpty()) {
                        return true;
                    }
                    elseBranches.pop();
                }
            }
        }
        return !elseBranches.isEmpty();
    }

    private static int conditionalDepthAt(@NotNull List<PreprocessorDirective> directives, int offset) {
        int depth = 0;
        for (PreprocessorDirective directive : directives) {
            if (directive.start() >= offset) {
                break;
            }
            if (directive.name().equals("if") || directive.name().equals("ifdef") || directive.name().equals("ifndef")) {
                depth++;
            } else if (directive.name().equals("endif")) {
                depth = Math.max(0, depth - 1);
            }
        }
        return depth;
    }

    @NotNull
    private static Map<String, List<MacroDefinition>> findMacroDefinitions(@NotNull List<PreprocessorDirective> directives) {
        Map<String, List<MacroDefinition>> definitions = new LinkedHashMap<>();
        for (PreprocessorDirective directive : directives) {
            if (!directive.name().equals("define")) {
                continue;
            }
            MacroDefinition definition = parseMacroDefinition(directive.body());
            if (definition != null) {
                definitions.computeIfAbsent(definition.name(), ignored -> new ArrayList<>()).add(definition);
            }
        }
        Map<String, List<MacroDefinition>> immutableDefinitions = new LinkedHashMap<>();
        for (Map.Entry<String, List<MacroDefinition>> entry : definitions.entrySet()) {
            immutableDefinitions.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(immutableDefinitions);
    }

    private static MacroDefinition parseMacroDefinition(@NotNull String body) {
        int nameStart = skipDirectiveWhitespace(body, 0);
        if (nameStart >= body.length() || !isIdentifierStart(body.charAt(nameStart))) {
            return null;
        }
        int nameEnd = nameStart + 1;
        while (nameEnd < body.length() && isIdentifierPart(body.charAt(nameEnd))) {
            nameEnd++;
        }
        String name = body.substring(nameStart, nameEnd);
        boolean functionLike = nameEnd < body.length() && body.charAt(nameEnd) == '(';
        Set<String> parameters = new LinkedHashSet<>();
        int replacementStart = nameEnd;
        if (functionLike) {
            int closingParenthesis = findClosingCharacter(body, nameEnd, '(', ')');
            if (closingParenthesis < 0) {
                return new MacroDefinition(name, Set.of(), Set.of());
            }
            for (Token token : tokenize(body.substring(nameEnd + 1, closingParenthesis))) {
                if (token.identifier()) {
                    parameters.add(token.text());
                }
            }
            replacementStart = closingParenthesis + 1;
        }
        Set<String> replacementIdentifiers = new LinkedHashSet<>();
        for (Token token : tokenize(body.substring(replacementStart))) {
            if (token.identifier()) {
                replacementIdentifiers.add(token.text());
            }
        }
        return new MacroDefinition(name, Set.copyOf(parameters), Set.copyOf(replacementIdentifiers));
    }

    private static int skipDirectiveWhitespace(@NotNull String source, int start) {
        int index = start;
        while (index < source.length() && (Character.isWhitespace(source.charAt(index)) || source.charAt(index) == '\\')) {
            index++;
        }
        return index;
    }

    private static int findClosingCharacter(@NotNull String source, int openingIndex, char opening, char closing) {
        int depth = 0;
        for (int i = openingIndex; i < source.length(); i++) {
            if (source.charAt(i) == opening) {
                depth++;
            } else if (source.charAt(i) == closing && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static String firstIdentifier(@NotNull String source) {
        for (Token token : tokenize(source)) {
            if (token.identifier()) {
                return token.text();
            }
        }
        return null;
    }

    @NotNull
    private static Set<String> resolveStructuralMacros(@NotNull Map<String, List<MacroDefinition>> definitions, @NotNull Set<String> structuralTokens) {
        Set<String> resolved = new LinkedHashSet<>();
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, List<MacroDefinition>> entry : definitions.entrySet()) {
                if (!resolved.contains(entry.getKey()) && entry.getValue().stream().anyMatch(definition -> definition.referencesAny(structuralTokens, resolved))) {
                    changed |= resolved.add(entry.getKey());
                }
            }
        } while (changed);
        return Set.copyOf(resolved);
    }

    @NotNull
    private static Set<String> resolveEntryGeneratingMacros(@NotNull Map<String, List<MacroDefinition>> definitions, @NotNull Set<String> requiredEntryNames) {
        Set<String> resolved = new LinkedHashSet<>();
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, List<MacroDefinition>> entry : definitions.entrySet()) {
                if (resolved.contains(entry.getKey())) {
                    continue;
                }
                for (MacroDefinition definition : entry.getValue()) {
                    boolean directEntry = definition.replacementIdentifiers().stream().anyMatch(requiredEntryNames::contains) && (definition.replacementIdentifiers().contains("void") || definition.replacementIdentifiers().size() == 1);
                    if (directEntry || definition.referencesAny(Set.of(), resolved)) {
                        changed |= resolved.add(entry.getKey());
                        break;
                    }
                }
            }
        } while (changed);
        return Set.copyOf(resolved);
    }

    @NotNull
    private static List<SourceRange> findRawUniformDeclarationRanges(@NotNull String source, @NotNull List<Token> tokens, @NotNull List<PreprocessorDirective> directives) {
        List<SourceRange> ranges = new ArrayList<>();
        int braceDepth = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (isInPreprocessorDirective(directives, token.start())) {
                continue;
            }
            if (token.text().equals("{")) {
                braceDepth++;
                continue;
            }
            if (token.text().equals("}")) {
                braceDepth = Math.max(0, braceDepth - 1);
                continue;
            }
            if (braceDepth != 0 || !token.text().equals("uniform")) {
                continue;
            }
            int endTokenIndex = findDeclarationEnd(tokens, i + 1);
            int end = endTokenIndex < 0 ? source.length() : tokens.get(endTokenIndex).end();
            ranges.add(new SourceRange(token.start(), end));
            if (endTokenIndex >= 0) {
                i = endTokenIndex;
            }
        }
        return List.copyOf(ranges);
    }

    private static boolean rangeReferencesAnyMacro(@NotNull List<Token> tokens, @NotNull SourceRange range, @NotNull Set<String> macroNames) {
        if (macroNames.isEmpty()) {
            return false;
        }
        for (Token token : tokens) {
            if (token.start() >= range.end()) {
                break;
            }
            if (token.start() >= range.start() && token.identifier() && macroNames.contains(token.text())) {
                return true;
            }
        }
        return false;
    }

    private static boolean referencesAnyMacroOutsideDirectives(@NotNull List<Token> tokens, @NotNull List<PreprocessorDirective> directives, @NotNull Set<String> macroNames) {
        if (macroNames.isEmpty()) {
            return false;
        }
        for (Token token : tokens) {
            if (token.identifier() && macroNames.contains(token.text()) && !isInPreprocessorDirective(directives, token.start())) {
                return true;
            }
        }
        return false;
    }

    private static boolean rangeContainsAnyIdentifier(@NotNull List<Token> tokens, int startIndex, int endIndex, @NotNull Set<String> directNames, @NotNull Set<String> macroNames) {
        for (int i = startIndex; i < endIndex; i++) {
            String text = tokens.get(i).text();
            if (directNames.contains(text) || macroNames.contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInPreprocessorDirective(@NotNull List<PreprocessorDirective> directives, int offset) {
        for (PreprocessorDirective directive : directives) {
            if (offset < directive.start()) {
                return false;
            }
            if (offset < directive.end()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves macros and conditional branches only after the structural scan proves that their concrete result is
     * required for uniform layout or wrapper selection. The options and Vulkan compatibility defines intentionally
     * mirror Minecraft 26.2's {@code GlslCompiler}.
     */
    @NotNull
    private static String preprocessSource(@NotNull String source, @NotNull BackendCoordinates backendCoordinates) {
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        if (compiler == 0L || options == 0L) {
            if (options != 0L) {
                Shaderc.shaderc_compile_options_release(options);
            }
            if (compiler != 0L) {
                Shaderc.shaderc_compiler_release(compiler);
            }
            throw preprocessingFailure(backendCoordinates, "Shaderc could not initialize.");
        }

        try {
            if (backendCoordinates == BackendCoordinates.VULKAN) {
                Shaderc.shaderc_compile_options_set_target_env(options, Shaderc.shaderc_target_env_vulkan, Shaderc.shaderc_env_version_vulkan_1_2);
            } else {
                Shaderc.shaderc_compile_options_set_target_env(options, Shaderc.shaderc_target_env_opengl, Shaderc.shaderc_env_version_opengl_4_5);
            }
            Shaderc.shaderc_compile_options_set_auto_bind_uniforms(options, true);
            Shaderc.shaderc_compile_options_set_auto_map_locations(options, true);
            Shaderc.shaderc_compile_options_set_generate_debug_info(options);
            Shaderc.shaderc_compile_options_set_optimization_level(options, Shaderc.shaderc_optimization_level_zero);

            String compilerSource = GLSL_VERSION + "\n" + source;
            if (backendCoordinates == BackendCoordinates.VULKAN) {
                compilerSource = GlslPreprocessor.injectDefines(compilerSource, VULKAN_GLOBAL_DEFINES);
            }

            long result = Shaderc.shaderc_compile_into_preprocessed_text(compiler, compilerSource, Shaderc.shaderc_fragment_shader, PREPROCESS_SOURCE_NAME, "main", options);
            if (result == 0L) {
                throw preprocessingFailure(backendCoordinates, "Shaderc returned no preprocessing result.");
            }
            try {
                int status = Shaderc.shaderc_result_get_compilation_status(result);
                if (status != Shaderc.shaderc_compilation_status_success) {
                    String message = Shaderc.shaderc_result_get_error_message(result);
                    throw preprocessingFailure(backendCoordinates, message == null || message.isBlank() ? "Unknown Shaderc preprocessing failure." : message.strip());
                }
                ByteBuffer preprocessedBytes = Shaderc.shaderc_result_get_bytes(result);
                if (preprocessedBytes == null) {
                    throw preprocessingFailure(backendCoordinates, "Shaderc returned no preprocessed source.");
                }
                return normalizeSource(MemoryUtil.memUTF8(preprocessedBytes));
            } finally {
                Shaderc.shaderc_result_release(result);
            }
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }

    @NotNull
    private static ShaderTransformException preprocessingFailure(@NotNull BackendCoordinates backendCoordinates, @NotNull String detail) {
        String backendName = backendCoordinates == BackendCoordinates.VULKAN ? "Vulkan" : "OpenGL";
        return new ShaderTransformException(List.of(backendName + " shader preprocessing failed: " + detail));
    }

    @NotNull
    public static String contentIdentity(@NotNull String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                byte[] bytes = part.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("The Java runtime does not provide SHA-256.", ex);
        }
    }

    @NotNull
    private static UniformTransformation transformUniforms(@NotNull String source) {
        List<Token> tokens = tokenize(source);
        List<ParsedUniformDeclaration> parsedDeclarations = parseUniformDeclarations(source, tokens);
        List<String> diagnostics = new ArrayList<>();
        Map<String, GlslStd140Layout.Declaration> looseUniforms = new LinkedHashMap<>();
        Map<String, SamplerDeclaration> samplers = new LinkedHashMap<>();
        List<SourceRange> allDeclarationRanges = new ArrayList<>();

        for (ParsedUniformDeclaration parsed : parsedDeclarations) {
            allDeclarationRanges.add(parsed.range());
            if (parsed.uniformBlock()) {
                diagnostics.add("Legacy uniform block '" + parsed.type() + "' is not supported; declare its members as loose uniforms so FancyMenu can merge them into " + UNIFORM_BLOCK_NAME + ".");
                continue;
            }
            if (parsed.sampler()) {
                for (ParsedDeclarator declarator : parsed.declarators()) {
                    if (declarator.arrayLength() > 0) {
                        diagnostics.add("Sampler array '" + declarator.name() + "' is not supported by Minecraft's public bind-group API.");
                        continue;
                    }
                    SamplerDeclaration declaration = new SamplerDeclaration(parsed.type(), declarator.name());
                    SamplerDeclaration previous = samplers.putIfAbsent(declarator.name(), declaration);
                    if (previous != null) {
                        diagnostics.add("Duplicate sampler uniform declaration '" + declarator.name() + "'.");
                    }
                }
                continue;
            }

            for (ParsedDeclarator declarator : parsed.declarators()) {
                GlslStd140Layout.Declaration declaration = new GlslStd140Layout.Declaration(parsed.type(), declarator.name(), declarator.arrayLength());
                GlslStd140Layout.Declaration previous = looseUniforms.putIfAbsent(declarator.name(), declaration);
                if (previous != null) {
                    diagnostics.add("Duplicate loose uniform declaration '" + declarator.name() + "'.");
                }
            }
        }

        List<GlslStd140Layout.Declaration> blockDeclarations = new ArrayList<>(BUILT_IN_UNIFORMS.size() + looseUniforms.size());
        blockDeclarations.addAll(BUILT_IN_UNIFORMS);
        for (GlslStd140Layout.Declaration declaration : looseUniforms.values()) {
            if (RESERVED_INTERNAL_UNIFORMS.contains(declaration.name())) {
                diagnostics.add("Uniform name '" + declaration.name() + "' is reserved by FancyMenu's GPU runtime.");
                continue;
            }
            if (BUILT_IN_SAMPLERS_BY_NAME.containsKey(declaration.name())) {
                diagnostics.add("Uniform '" + declaration.name() + "' is a reserved sampler and must be declared as sampler2D, not " + declaration.type() + ".");
                continue;
            }
            GlslStd140Layout.Declaration builtIn = BUILT_INS_BY_NAME.get(declaration.name());
            if (builtIn != null) {
                if (!sameDeclaration(builtIn, declaration)) {
                    diagnostics.add("Uniform '" + declaration.name() + "' must be declared as " + builtIn.toShaderDeclaration() + " but was declared as " + declaration.toShaderDeclaration());
                }
                continue;
            }
            validateVariableUniform(declaration, diagnostics);
            blockDeclarations.add(declaration);
        }

        for (SamplerDeclaration builtIn : BUILT_IN_SAMPLERS) {
            SamplerDeclaration declared = samplers.get(builtIn.name());
            if (declared != null && !declared.type().equals(builtIn.type())) {
                diagnostics.add("Sampler uniform '" + builtIn.name() + "' must be declared as " + builtIn.type() + " but was declared as " + declared.type() + ".");
            }
        }
        for (SamplerDeclaration sampler : samplers.values()) {
            if (!sampler.type().equals("sampler2D")) {
                diagnostics.add("Sampler uniform '" + sampler.name() + "' uses unsupported type " + sampler.type() + "; only sampler2D is supported.");
            }
        }

        GlslStd140Layout layout = null;
        try {
            layout = GlslStd140Layout.create(blockDeclarations);
        } catch (IllegalArgumentException ex) {
            diagnostics.add(ex.getMessage());
        }
        if (!diagnostics.isEmpty() || layout == null) {
            throw new ShaderTransformException(diagnostics);
        }

        String sourceWithoutDeclarations = blankRanges(source, allDeclarationRanges);
        String transformedSource = sourceWithoutDeclarations;
        Set<String> activeSamplerNames = new LinkedHashSet<>();
        for (SamplerDeclaration builtIn : BUILT_IN_SAMPLERS) {
            if (containsIdentifier(maskCommentsAndStrings(sourceWithoutDeclarations), builtIn.name())) {
                activeSamplerNames.add(builtIn.name());
            }
        }
        for (SamplerDeclaration sampler : samplers.values()) {
            if (containsIdentifier(maskCommentsAndStrings(sourceWithoutDeclarations), sampler.name())) {
                activeSamplerNames.add(sampler.name());
            }
        }

        String blockDeclaration = buildUniformBlock(blockDeclarations);
        String samplerDeclarations = buildActiveSamplerDeclarations(activeSamplerNames, samplers);
        return new UniformTransformation(transformedSource, blockDeclaration, samplerDeclarations, layout, List.copyOf(activeSamplerNames));
    }

    @NotNull
    private static String buildShadertoyFragment(@NotNull String source, @NotNull UniformTransformation uniforms, @NotNull BackendCoordinates backendCoordinates, @NotNull PassKind passKind, boolean hasDirectMain) {
        StringBuilder fragment = buildFragmentPrefix(uniforms, backendCoordinates, passKind, true);
        if (hasDirectMain) {
            // A compatibility shader may deliberately provide both entry points. The unused Direct body must be
            // removed, not merely renamed: references to legacy gl_FragColor remain illegal anywhere in a translation
            // unit that declares the Shadertoy wrapper's user output, even when the renamed function is never called.
            source = blankTopLevelFunctionDefinition(source, "main");
        }
        fragment.append(source).append('\n');
        fragment.append("\nvoid main() {\n");
        fragment.append("    vec4 fmColor_FancyMenu = vec4(0.0);\n");
        fragment.append("    mainImage(fmColor_FancyMenu, gl_FragCoord.xy - fmAreaOffset);\n");
        if (passKind == PassKind.IMAGE) {
            fragment.append("    fmOutputColor_FancyMenu = vec4(fmColor_FancyMenu.rgb, fmColor_FancyMenu.a * fmOpacity);\n");
        } else {
            // Buffer alpha is often persistent shader data. Opacity belongs to the final Image composite only.
            fragment.append("    fmOutputColor_FancyMenu = fmColor_FancyMenu;\n");
        }
        fragment.append("}\n");
        return fragment.toString();
    }

    @NotNull
    private static String blankTopLevelFunctionDefinition(@NotNull String source, @NotNull String functionName) {
        List<Token> tokens = tokenize(source);
        List<SourceRange> ranges = new ArrayList<>();
        int braceDepth = 0;
        for (int i = 0; i + 3 < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.text().equals("{")) {
                braceDepth++;
                continue;
            }
            if (token.text().equals("}")) {
                braceDepth = Math.max(0, braceDepth - 1);
                continue;
            }
            if (braceDepth != 0 || !token.text().equals("void") || !tokens.get(i + 1).text().equals(functionName) || !tokens.get(i + 2).text().equals("(") || isPreprocessorToken(source, token.start())) {
                continue;
            }

            int closingParenthesis = findClosingToken(tokens, i + 2, "(", ")");
            if (closingParenthesis < 0 || closingParenthesis + 1 >= tokens.size() || !tokens.get(closingParenthesis + 1).text().equals("{")) {
                continue;
            }
            int closingBrace = findClosingToken(tokens, closingParenthesis + 1, "{", "}");
            if (closingBrace < 0) {
                continue;
            }
            ranges.add(new SourceRange(token.start(), tokens.get(closingBrace).end()));
            i = closingBrace;
        }
        return blankRanges(source, ranges);
    }

    private static int findClosingToken(@NotNull List<Token> tokens, int openingIndex, @NotNull String openingToken, @NotNull String closingToken) {
        int depth = 0;
        for (int i = openingIndex; i < tokens.size(); i++) {
            String text = tokens.get(i).text();
            if (text.equals(openingToken)) {
                depth++;
            } else if (text.equals(closingToken) && --depth == 0) {
                return i;
            }
        }
        return -1;
    }

    @NotNull
    private static String buildDirectFragment(@NotNull String source, @NotNull UniformTransformation uniforms, @NotNull BackendCoordinates backendCoordinates, @NotNull PassKind passKind, boolean withGlFragColorCompat) {
        StringBuilder fragment = buildFragmentPrefix(uniforms, backendCoordinates, passKind, withGlFragColorCompat);
        if (withGlFragColorCompat) {
            fragment.append("#define gl_FragColor fmOutputColor_FancyMenu\n");
        }
        fragment.append(source).append('\n');
        return fragment.toString();
    }

    @NotNull
    private static StringBuilder buildFragmentPrefix(@NotNull UniformTransformation uniforms, @NotNull BackendCoordinates backendCoordinates, @NotNull PassKind passKind, boolean declareOutput) {
        StringBuilder fragment = new StringBuilder();
        fragment.append(GLSL_VERSION).append('\n');
        fragment.append("in vec2 fmUv_FancyMenu;\n");
        if (declareOutput) {
            fragment.append("out vec4 fmOutputColor_FancyMenu;\n");
        }
        fragment.append("#define iGlobalTime iTime\n");
        fragment.append("#define texture2D texture\n");
        fragment.append("#define textureCube texture\n");
        fragment.append(uniforms.samplerDeclarations());
        fragment.append(uniforms.blockDeclaration());
        fragment.append(buildFragCoordCompatibilityFunction(backendCoordinates, passKind));
        fragment.append("#define gl_FragCoord fmGlFragCoord_FancyMenu()\n");
        return fragment;
    }

    @NotNull
    private static String buildFragCoordCompatibilityFunction(@NotNull BackendCoordinates backendCoordinates, @NotNull PassKind passKind) {
        if (backendCoordinates == BackendCoordinates.VULKAN && passKind == PassKind.IMAGE) {
            return "vec4 fmGlFragCoord_FancyMenu() { return vec4(gl_FragCoord.x, " + RENDER_TARGET_SIZE_UNIFORM + ".y - gl_FragCoord.y, gl_FragCoord.z, gl_FragCoord.w); }\n";
        }
        return "vec4 fmGlFragCoord_FancyMenu() { return gl_FragCoord; }\n";
    }

    @NotNull
    static String buildVertexSource(@NotNull String blockDeclaration, @NotNull BackendCoordinates backendCoordinates, @NotNull PassKind passKind) {
        StringBuilder vertex = new StringBuilder();
        vertex.append(GLSL_VERSION).append('\n');
        vertex.append(blockDeclaration);
        vertex.append("out vec2 fmUv_FancyMenu;\n");
        vertex.append("void main() {\n");
        if (backendCoordinates == BackendCoordinates.VULKAN && passKind == PassKind.IMAGE) {
            // Minecraft's Vulkan pipeline declares clockwise fronts for its positive-height viewport. The image-space
            // Y inversion below flips winding once, so reverse the logical quad here to preserve legacy gl_FrontFacing.
            vertex.append("    const vec2 fmVertices[6] = vec2[6](vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(1.0, 0.0), vec2(0.0, 0.0), vec2(0.0, 1.0), vec2(1.0, 1.0));\n");
        } else {
            vertex.append("    const vec2 fmVertices[6] = vec2[6](vec2(0.0, 0.0), vec2(1.0, 0.0), vec2(1.0, 1.0), vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(0.0, 1.0));\n");
        }
        vertex.append("    vec2 fmUv = fmVertices[gl_VertexID];\n");
        vertex.append("    vec2 fmLogicalPixel = " + RENDER_AREA_UNIFORM + ".xy + fmUv * " + RENDER_AREA_UNIFORM + ".zw;\n");
        if (backendCoordinates == BackendCoordinates.VULKAN && passKind == PassKind.IMAGE) {
            vertex.append("    vec2 fmPixel = vec2(fmLogicalPixel.x, " + RENDER_TARGET_SIZE_UNIFORM + ".y - fmLogicalPixel.y);\n");
        } else {
            vertex.append("    vec2 fmPixel = fmLogicalPixel;\n");
        }
        vertex.append("    gl_Position = vec4(fmPixel / " + RENDER_TARGET_SIZE_UNIFORM + " * 2.0 - 1.0, 0.0, 1.0);\n");
        // The internal render area is clipped to the target, while these public UVs historically span the element's
        // complete rectangle. Remapping the clipped vertex positions preserves that contract for off-screen elements.
        vertex.append("    fmUv_FancyMenu = (fmLogicalPixel - fmAreaOffset) / fmAreaSize;\n");
        vertex.append("}\n");
        return vertex.toString();
    }

    @NotNull
    private static String buildUniformBlock(@NotNull List<GlslStd140Layout.Declaration> declarations) {
        StringBuilder block = new StringBuilder("layout(std140) uniform ").append(UNIFORM_BLOCK_NAME).append(" {\n");
        for (GlslStd140Layout.Declaration declaration : declarations) {
            block.append("    ").append(declaration.toShaderDeclaration()).append('\n');
        }
        block.append("};\n");
        return block.toString();
    }

    @NotNull
    private static String buildActiveSamplerDeclarations(@NotNull Set<String> activeSamplerNames, @NotNull Map<String, SamplerDeclaration> declaredSamplers) {
        StringBuilder declarations = new StringBuilder();
        for (String samplerName : activeSamplerNames) {
            SamplerDeclaration sampler = BUILT_IN_SAMPLERS_BY_NAME.get(samplerName);
            if (sampler == null) {
                sampler = declaredSamplers.get(samplerName);
            }
            if (sampler != null) {
                declarations.append("uniform ").append(sampler.type()).append(' ').append(sampler.name()).append(";\n");
            }
        }
        return declarations.toString();
    }

    private static boolean containsFunction(@NotNull String source, @NotNull String functionName) {
        return Pattern.compile("(?m)^\\s*void\\s+" + Pattern.quote(functionName) + "\\s*\\(").matcher(source).find();
    }

    private static boolean containsIdentifier(@NotNull String source, @NotNull String identifier) {
        return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(identifier) + "(?![A-Za-z0-9_])").matcher(source).find();
    }

    @NotNull
    private static List<ParsedUniformDeclaration> parseUniformDeclarations(@NotNull String source, @NotNull List<Token> tokens) {
        List<ParsedUniformDeclaration> declarations = new ArrayList<>();
        int braceDepth = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.text().equals("{")) {
                braceDepth++;
                continue;
            }
            if (token.text().equals("}")) {
                braceDepth = Math.max(0, braceDepth - 1);
                continue;
            }
            if (braceDepth != 0 || !token.text().equals("uniform") || isPreprocessorToken(source, token.start())) {
                continue;
            }

            int endTokenIndex = findDeclarationEnd(tokens, i + 1);
            if (endTokenIndex < 0) {
                throw new ShaderTransformException(List.of("Unterminated uniform declaration at source offset " + token.start() + "."));
            }
            int start = findLayoutQualifierStart(source, tokens, i);
            ParsedUniformDeclaration declaration = parseUniformDeclarationTokens(tokens.subList(i + 1, endTokenIndex), new SourceRange(start, tokens.get(endTokenIndex).end()));
            declarations.add(declaration);
            i = endTokenIndex;
        }
        return declarations;
    }

    private static int findDeclarationEnd(@NotNull List<Token> tokens, int startIndex) {
        int nestedBraces = 0;
        for (int i = startIndex; i < tokens.size(); i++) {
            String text = tokens.get(i).text();
            if (text.equals("{")) {
                nestedBraces++;
            } else if (text.equals("}")) {
                nestedBraces--;
            } else if (text.equals(";") && nestedBraces == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int findLayoutQualifierStart(@NotNull String source, @NotNull List<Token> tokens, int uniformTokenIndex) {
        if (uniformTokenIndex < 3 || !tokens.get(uniformTokenIndex - 1).text().equals(")")) {
            return tokens.get(uniformTokenIndex).start();
        }
        int depth = 0;
        for (int i = uniformTokenIndex - 1; i >= 0; i--) {
            String text = tokens.get(i).text();
            if (text.equals(")")) {
                depth++;
            } else if (text.equals("(")) {
                depth--;
                if (depth == 0 && i > 0 && tokens.get(i - 1).text().equals("layout") && onlyWhitespaceOrComments(source, tokens.get(uniformTokenIndex - 1).end(), tokens.get(uniformTokenIndex).start())) {
                    return tokens.get(i - 1).start();
                }
            }
        }
        return tokens.get(uniformTokenIndex).start();
    }

    @NotNull
    private static ParsedUniformDeclaration parseUniformDeclarationTokens(@NotNull List<Token> tokens, @NotNull SourceRange range) {
        if (tokens.isEmpty()) {
            throw new ShaderTransformException(List.of("Empty uniform declaration at source offset " + range.start() + "."));
        }
        int index = 0;
        while (index < tokens.size() && DECLARATION_QUALIFIERS.contains(tokens.get(index).text())) {
            index++;
        }
        if (index >= tokens.size() || !tokens.get(index).identifier()) {
            throw new ShaderTransformException(List.of("Unable to determine uniform type at source offset " + range.start() + "."));
        }
        String type = tokens.get(index++).text();
        if (index < tokens.size() && tokens.get(index).text().equals("{")) {
            return new ParsedUniformDeclaration(type, List.of(), false, true, range);
        }
        boolean sampler = type.contains("sampler");
        if (type.startsWith("image") || type.startsWith("iimage") || type.startsWith("uimage") || type.equals("atomic_uint")) {
            throw new ShaderTransformException(List.of("Opaque uniform type '" + type + "' is not supported by FancyMenu's runtime bind group."));
        }

        List<ParsedDeclarator> declarators = new ArrayList<>();
        while (index < tokens.size()) {
            Token nameToken = tokens.get(index++);
            if (!nameToken.identifier()) {
                throw new ShaderTransformException(List.of("Expected a uniform name after type '" + type + "' at source offset " + range.start() + "."));
            }
            int arrayLength = 0;
            if (index < tokens.size() && tokens.get(index).text().equals("[")) {
                index++;
                if (index >= tokens.size() || !tokens.get(index).number()) {
                    throw new ShaderTransformException(List.of("Uniform array '" + nameToken.text() + "' must use a positive integer literal length."));
                }
                try {
                    arrayLength = Integer.parseInt(tokens.get(index++).text());
                } catch (NumberFormatException ex) {
                    throw new ShaderTransformException(List.of("Uniform array '" + nameToken.text() + "' has an invalid length."));
                }
                if (arrayLength <= 0 || index >= tokens.size() || !tokens.get(index).text().equals("]")) {
                    throw new ShaderTransformException(List.of("Uniform array '" + nameToken.text() + "' must use a positive integer literal length."));
                }
                index++;
            }
            declarators.add(new ParsedDeclarator(nameToken.text(), arrayLength));
            if (index >= tokens.size()) {
                break;
            }
            if (!tokens.get(index).text().equals(",")) {
                throw new ShaderTransformException(List.of("Unsupported syntax in uniform declaration for '" + nameToken.text() + "'."));
            }
            index++;
        }
        return new ParsedUniformDeclaration(type, List.copyOf(declarators), sampler, false, range);
    }

    @NotNull
    private static List<Token> tokenize(@NotNull String source) {
        String masked = maskCommentsAndStrings(source);
        List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < masked.length();) {
            char c = masked.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (isIdentifierStart(c)) {
                int start = i++;
                while (i < masked.length() && isIdentifierPart(masked.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(masked.substring(start, i), start, i, true, false));
                continue;
            }
            if (Character.isDigit(c)) {
                int start = i++;
                while (i < masked.length() && Character.isDigit(masked.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(masked.substring(start, i), start, i, false, true));
                continue;
            }
            tokens.add(new Token(Character.toString(c), i, i + 1, false, false));
            i++;
        }
        return tokens;
    }

    @NotNull
    private static String maskCommentsAndStrings(@NotNull String source) {
        char[] masked = source.toCharArray();
        boolean lineComment = false;
        boolean blockComment = false;
        boolean string = false;
        char quote = 0;
        for (int i = 0; i < masked.length; i++) {
            char c = masked[i];
            char next = i + 1 < masked.length ? masked[i + 1] : 0;
            if (lineComment) {
                if (c == '\n') {
                    lineComment = false;
                } else {
                    masked[i] = ' ';
                }
                continue;
            }
            if (blockComment) {
                if (c == '*' && next == '/') {
                    masked[i] = ' ';
                    masked[++i] = ' ';
                    blockComment = false;
                } else if (c != '\n') {
                    masked[i] = ' ';
                }
                continue;
            }
            if (string) {
                if (c == '\\' && next != 0) {
                    masked[i] = ' ';
                    if (masked[i + 1] != '\n') {
                        masked[i + 1] = ' ';
                    }
                    i++;
                } else if (c == quote) {
                    masked[i] = ' ';
                    string = false;
                } else if (c != '\n') {
                    masked[i] = ' ';
                }
                continue;
            }
            if (c == '/' && next == '/') {
                masked[i] = ' ';
                masked[i + 1] = ' ';
                i++;
                lineComment = true;
            } else if (c == '/' && next == '*') {
                masked[i] = ' ';
                masked[i + 1] = ' ';
                i++;
                blockComment = true;
            } else if (c == '"' || c == '\'') {
                masked[i] = ' ';
                string = true;
                quote = c;
            }
        }
        return new String(masked);
    }

    @NotNull
    private static String blankRanges(@NotNull String source, @NotNull List<SourceRange> ranges) {
        if (ranges.isEmpty()) {
            return source;
        }
        char[] result = source.toCharArray();
        List<SourceRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(SourceRange::start));
        for (SourceRange range : sorted) {
            for (int i = Math.max(0, range.start()); i < Math.min(result.length, range.end()); i++) {
                if (result[i] != '\n') {
                    result[i] = ' ';
                }
            }
        }
        return new String(result);
    }

    private static boolean isPreprocessorToken(@NotNull String source, int offset) {
        int lineStart = source.lastIndexOf('\n', Math.max(0, offset - 1)) + 1;
        while (lineStart >= 2 && source.charAt(lineStart - 2) == '\\') {
            lineStart = source.lastIndexOf('\n', lineStart - 2) + 1;
        }
        for (int i = lineStart; i < offset; i++) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            return c == '#';
        }
        return false;
    }

    private static boolean onlyWhitespaceOrComments(@NotNull String source, int start, int end) {
        String masked = maskCommentsAndStrings(source.substring(start, end));
        for (int i = 0; i < masked.length(); i++) {
            if (!Character.isWhitespace(masked.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentifierStart(char c) {
        return c == '_' || Character.isLetter(c);
    }

    private static boolean isIdentifierPart(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    private static boolean sameDeclaration(@NotNull GlslStd140Layout.Declaration first, @NotNull GlslStd140Layout.Declaration second) {
        return first.type().equals(second.type()) && first.arrayLength() == second.arrayLength();
    }

    private static void validateVariableUniform(@NotNull GlslStd140Layout.Declaration declaration, @NotNull List<String> diagnostics) {
        String expectedType = null;
        boolean integerOrBoolean = false;
        if (declaration.name().startsWith("fmVarFloat_")) {
            expectedType = "float";
        } else if (declaration.name().startsWith("fmVarInt_")) {
            expectedType = "int";
        } else if (declaration.name().startsWith("fmVarBool_") || declaration.name().startsWith("fmVarExists_")) {
            integerOrBoolean = true;
        } else if (declaration.name().startsWith("fmVarVec2_")) {
            expectedType = "vec2";
        } else if (declaration.name().startsWith("fmVarVec3_")) {
            expectedType = "vec3";
        } else if (declaration.name().startsWith("fmVarVec4_")) {
            expectedType = "vec4";
        }
        if (expectedType != null && !declaration.type().equals(expectedType)) {
            diagnostics.add("FancyMenu variable uniform '" + declaration.name() + "' must use type " + expectedType + " but was declared as " + declaration.type() + ".");
        } else if (integerOrBoolean && !declaration.type().equals("int") && !declaration.type().equals("bool")) {
            diagnostics.add("FancyMenu variable uniform '" + declaration.name() + "' must use type int or bool but was declared as " + declaration.type() + ".");
        }
    }

    @NotNull
    private static GlslStd140Layout.Declaration declaration(@NotNull String type, @NotNull String name) {
        return declaration(type, name, 0);
    }

    @NotNull
    private static GlslStd140Layout.Declaration declaration(@NotNull String type, @NotNull String name, int arrayLength) {
        return new GlslStd140Layout.Declaration(type, name, arrayLength);
    }

    @NotNull
    private static Map<String, GlslStd140Layout.Declaration> indexDeclarations(@NotNull List<GlslStd140Layout.Declaration> declarations) {
        Map<String, GlslStd140Layout.Declaration> indexed = new HashMap<>();
        for (GlslStd140Layout.Declaration declaration : declarations) {
            indexed.put(declaration.name(), declaration);
        }
        return Map.copyOf(indexed);
    }

    @NotNull
    private static Map<String, SamplerDeclaration> indexSamplers(@NotNull List<SamplerDeclaration> declarations) {
        Map<String, SamplerDeclaration> indexed = new HashMap<>();
        for (SamplerDeclaration declaration : declarations) {
            indexed.put(declaration.name(), declaration);
        }
        return Map.copyOf(indexed);
    }

    public enum BackendCoordinates {
        OPENGL,
        VULKAN
    }

    public enum PassKind {
        BUFFER,
        IMAGE
    }

    public record CoordinateConvention(@NotNull BackendCoordinates backend, @NotNull PassKind passKind, boolean rawFragCoordUsesBottomOrigin, boolean uvZeroStoredAtTextureVZero, boolean imageFragCoordIsAbsolute) {

        @NotNull
        public static CoordinateConvention forPass(@NotNull BackendCoordinates backend, @NotNull PassKind passKind) {
            boolean bottomOrigin = backend == BackendCoordinates.OPENGL || passKind == PassKind.IMAGE;
            return new CoordinateConvention(backend, passKind, bottomOrigin, true, passKind == PassKind.IMAGE);
        }
    }

    public record FragmentVariant(@NotNull String label, @NotNull String source, @NotNull String vertexSource, @NotNull GlslStd140Layout uniformLayout, @NotNull List<String> activeSamplerNames, @NotNull String identity, @NotNull CoordinateConvention coordinateConvention) {
    }

    public static final class ShaderTransformException extends IllegalArgumentException {

        private final List<String> diagnostics;

        private ShaderTransformException(@NotNull List<String> diagnostics) {
            super(String.join("\n", diagnostics));
            this.diagnostics = List.copyOf(diagnostics);
        }

        @NotNull
        public List<String> diagnostics() {
            return this.diagnostics;
        }
    }

    private record SamplerDeclaration(@NotNull String type, @NotNull String name) {
    }

    private record UniformTransformation(@NotNull String source, @NotNull String blockDeclaration, @NotNull String samplerDeclarations, @NotNull GlslStd140Layout layout, @NotNull List<String> activeSamplerNames) {
    }

    private record VariantSource(@NotNull String label, @NotNull String source) {
    }

    private record ParsedUniformDeclaration(@NotNull String type, @NotNull List<ParsedDeclarator> declarators, boolean sampler, boolean uniformBlock, @NotNull SourceRange range) {
    }

    private record ParsedDeclarator(@NotNull String name, int arrayLength) {
    }

    private record PreprocessorDirective(@NotNull String name, int start, int end, @NotNull String body) {
    }

    private record MacroDefinition(@NotNull String name, @NotNull Set<String> parameters, @NotNull Set<String> replacementIdentifiers) {

        private boolean referencesAny(@NotNull Set<String> directIdentifiers, @NotNull Set<String> macroIdentifiers) {
            for (String identifier : this.replacementIdentifiers) {
                if (!this.parameters.contains(identifier) && (directIdentifiers.contains(identifier) || macroIdentifiers.contains(identifier))) {
                    return true;
                }
            }
            return false;
        }
    }

    private record SourceRange(int start, int end) {
    }

    private record Token(@NotNull String text, int start, int end, boolean identifier, boolean number) {
    }
}
