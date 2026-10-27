package de.keksuccino.fancymenu.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.keksuccino.fancymenu.mixin.mixins.common.client.MixinOnlineServerEntry;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.gen.AccessorInfo;
import org.spongepowered.asm.mixin.injection.selectors.MatchResult;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Resolves every mixin of FancyMenu's common and Fabric mixin configs against the real Minecraft bytecode on the test classpath.
 * <p>
 * Minecraft 26.1+ is unobfuscated, so no refmap or annotation processor validates mixin target strings at build time.
 * A stale target, like a method name carried over from an older Minecraft version during a port, compiles fine and only fails
 * when the target class loads in-game, where the non-required mixin config turns it into a log warning and silently drops the feature.
 * <p>
 * The lookups mirror the rules Mixin applies while preparing a mixin (exact name and descriptor matches in the target class itself,
 * nothing inherited) and parse selectors with Mixin's own {@link MemberInfo}. Slices and injection point shifts are not evaluated.
 */
class MixinTargetResolutionTest {

    private static final List<String> MIXIN_CONFIGS = List.of("fancymenu.mixins.json", "fancymenu.fabric.mixins.json");
    private static final List<String> MIXIN_CONFIG_SECTIONS = List.of("mixins", "client", "server");
    private static final List<String> INJECTOR_ANNOTATION_PACKAGES = List.of("Lorg/spongepowered/asm/mixin/injection/", "Lcom/llamalad7/mixinextras/injector/");
    private static final List<String> OBJECT_FACTORY_PREFIXES = List.of("new", "create");
    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String OVERWRITE_DESC = "Lorg/spongepowered/asm/mixin/Overwrite;";
    private static final String ACCESSOR_DESC = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER_DESC = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String DEFAULT_SHADOW_PREFIX = "shadow$";
    private static final String CONSTRUCTOR = "<init>";

    private final Map<String, Optional<ClassNode>> classCache = new HashMap<>();

    @TestFactory
    Stream<DynamicTest> everyConfiguredMixinResolvesAllOfItsTargets() throws IOException {
        List<String> mixinClassNames = readConfiguredMixinClassNames();
        assertFalse(mixinClassNames.isEmpty(), "The mixin configs list no mixins");
        return mixinClassNames.stream().map(mixinClassName -> DynamicTest.dynamicTest(mixinClassName, () -> assertEquals(List.of(), this.findUnresolvedTargets(this.requireClass(mixinClassName)))));
    }

    @Test
    void preExtractRenameServerIconHookMethodsAreReported() {
        ClassNode mixin = readMutableClass(MixinOnlineServerEntry.class);
        setAnnotationValue(requireInjector(mixin, "onDrawIconFancyMenu"), "method", List.of("drawIcon"));
        setAnnotationValue(requireInjector(mixin, "onFillInRenderFancyMenu"), "method", List.of("renderContent"));

        List<UnresolvedTarget> unresolved = this.findUnresolvedTargets(mixin);

        assertEquals(List.of("onDrawIconFancyMenu", "onFillInRenderFancyMenu"), unresolved.stream().map(UnresolvedTarget::mixinMember).toList());
        assertEquals(List.of("drawIcon", "renderContent"), unresolved.stream().map(UnresolvedTarget::reference).toList());
    }

    @Test
    void staleInjectionPointTargetsAreReported() {
        ClassNode mixin = readMutableClass(MixinOnlineServerEntry.class);
        String staleFillTarget = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V";
        setAnnotationValue(annotationList(annotationValue(requireInjector(mixin, "onFillInRenderFancyMenu"), "at")).getFirst(), "target", staleFillTarget);

        assertEquals(List.of(new UnresolvedTarget("net/minecraft/client/gui/screens/multiplayer/ServerSelectionList$OnlineServerEntry", "onFillInRenderFancyMenu", staleFillTarget)), this.findUnresolvedTargets(mixin));
    }

    @Test
    void renamedShadowFieldsAreReported() throws IOException {
        ClassNode mixin = this.readFirstSingleTargetMixin(node -> node.fields.stream().anyMatch(field -> isPlainShadow(findAnnotation(field.visibleAnnotations, field.invisibleAnnotations, SHADOW_DESC))));
        FieldNode shadowField = mixin.fields.stream().filter(field -> isPlainShadow(findAnnotation(field.visibleAnnotations, field.invisibleAnnotations, SHADOW_DESC))).findFirst().orElseThrow();
        shadowField.name = shadowField.name + "_missing";

        assertEquals(List.of(shadowField.name), this.findUnresolvedTargets(mixin).stream().map(UnresolvedTarget::mixinMember).toList());
    }

    @Test
    void renamedAccessorTargetsAreReported() throws IOException {
        this.assertRenamedTargetIsReported(ACCESSOR_DESC);
    }

    @Test
    void renamedInvokerTargetsAreReported() throws IOException {
        this.assertRenamedTargetIsReported(INVOKER_DESC);
    }

    private void assertRenamedTargetIsReported(String annotationDesc) throws IOException {
        ClassNode mixin = this.readFirstSingleTargetMixin(node -> node.methods.stream().anyMatch(method -> findAnnotation(method.visibleAnnotations, method.invisibleAnnotations, annotationDesc) != null));
        MethodNode method = mixin.methods.stream().filter(candidate -> findAnnotation(candidate.visibleAnnotations, candidate.invisibleAnnotations, annotationDesc) != null).findFirst().orElseThrow();
        AnnotationNode annotation = findAnnotation(method.visibleAnnotations, method.invisibleAnnotations, annotationDesc);
        setAnnotationValue(annotation, "value", resolveAccessorTargetName(method, annotation) + "_missing");

        assertEquals(List.of(method.name), this.findUnresolvedTargets(mixin).stream().map(UnresolvedTarget::mixinMember).toList());
    }

    private List<UnresolvedTarget> findUnresolvedTargets(ClassNode mixin) {
        List<UnresolvedTarget> unresolved = new ArrayList<>();
        AnnotationNode mixinAnnotation = findAnnotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, MIXIN_DESC);
        List<String> targetNames = (mixinAnnotation != null) ? readMixinTargetNames(mixinAnnotation) : List.of();
        if (targetNames.isEmpty()) unresolved.add(new UnresolvedTarget(mixin.name, mixin.name, "@Mixin"));
        for (String targetName : targetNames) {
            ClassNode target = this.findClass(targetName).orElse(null);
            if (target == null) {
                unresolved.add(new UnresolvedTarget(targetName, mixin.name, "@Mixin"));
                continue;
            }
            for (FieldNode field : mixin.fields) {
                AnnotationNode shadow = findAnnotation(field.visibleAnnotations, field.invisibleAnnotations, SHADOW_DESC);
                if ((shadow != null) && !hasSpecialMemberTarget(fieldMembers(target), field.name, field.desc, shadow)) unresolved.add(new UnresolvedTarget(target.name, field.name, field.name + ":" + field.desc));
            }
            for (MethodNode method : mixin.methods) {
                for (AnnotationNode annotation : concat(method.visibleAnnotations, method.invisibleAnnotations)) {
                    checkMethodAnnotation(target, method, annotation, unresolved);
                }
            }
        }
        return unresolved;
    }

    private static void checkMethodAnnotation(ClassNode target, MethodNode method, AnnotationNode annotation, List<UnresolvedTarget> unresolved) {
        if (SHADOW_DESC.equals(annotation.desc) || OVERWRITE_DESC.equals(annotation.desc)) {
            if (!hasSpecialMemberTarget(methodMembers(target), method.name, method.desc, annotation)) unresolved.add(new UnresolvedTarget(target.name, method.name, method.name + method.desc));
        } else if (ACCESSOR_DESC.equals(annotation.desc)) {
            String fieldName = resolveAccessorTargetName(method, annotation);
            String fieldDesc = resolveAccessorFieldDesc(method);
            if (!hasAccessorTarget(target, fieldMembers(target), fieldName, fieldDesc)) unresolved.add(new UnresolvedTarget(target.name, method.name, fieldName + ":" + fieldDesc));
        } else if (INVOKER_DESC.equals(annotation.desc)) {
            boolean objectFactory = isObjectFactoryInvoker(target, method, annotation);
            String methodName = objectFactory ? CONSTRUCTOR : resolveAccessorTargetName(method, annotation);
            String methodDesc = objectFactory ? Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(method.desc)) : method.desc;
            if (!hasAccessorTarget(target, methodMembers(target), methodName, methodDesc)) unresolved.add(new UnresolvedTarget(target.name, method.name, methodName + methodDesc));
        } else if (isInjector(annotation)) {
            checkInjector(target, method, annotation, unresolved);
        }
    }

    private static void checkInjector(ClassNode target, MethodNode handler, AnnotationNode injector, List<UnresolvedTarget> unresolved) {
        // Injectors with require = 0 are optional by design, so Mixin silently skips them when nothing matches.
        if (Integer.valueOf(0).equals(annotationValue(injector, "require"))) return;
        List<MethodNode> targetMethods = new ArrayList<>();
        for (Object selector : list(annotationValue(injector, "method"))) {
            MemberInfo memberInfo = MemberInfo.parse((String) selector, null);
            List<MethodNode> matches = target.methods.stream().filter(method -> memberInfo.matches(target.name, method.name, method.desc).isExactMatch()).toList();
            if (matches.isEmpty()) unresolved.add(new UnresolvedTarget(target.name, handler.name, (String) selector));
            targetMethods.addAll(matches);
        }
        if (targetMethods.isEmpty()) return;
        for (AnnotationNode at : annotationList(annotationValue(injector, "at"))) {
            if (!hasInjectionPoint(targetMethods, at)) unresolved.add(new UnresolvedTarget(target.name, handler.name, (String) Optional.ofNullable(annotationValue(at, "target")).orElse(annotationValue(at, "value"))));
        }
    }

    private static boolean hasInjectionPoint(List<MethodNode> targetMethods, AnnotationNode at) {
        Predicate<AbstractInsnNode> matcher = createInjectionPointMatcher(at);
        if (matcher == null) return true;
        int ordinal = (annotationValue(at, "ordinal") instanceof Integer value) ? value : -1;
        for (MethodNode method : targetMethods) {
            long matchCount = StreamSupport.stream(method.instructions.spliterator(), false).filter(matcher).count();
            if (matchCount > Math.max(ordinal, 0)) return true;
        }
        return false;
    }

    /**
     * Mirrors the instruction matching of Mixin's BeforeInvoke, BeforeFieldAccess and BeforeNew injection points.
     * Returns {@code null} for structural points like HEAD, RETURN and TAIL, which exist in every target method.
     */
    @Nullable
    private static Predicate<AbstractInsnNode> createInjectionPointMatcher(AnnotationNode at) {
        String target = (String) annotationValue(at, "target");
        MemberInfo memberInfo = (target != null) ? MemberInfo.parse(target.replace('.', '/'), null) : null;
        return switch ((String) annotationValue(at, "value")) {
            case "INVOKE", "INVOKE_ASSIGN", "INVOKE_STRING" -> insn -> (insn instanceof MethodInsnNode call) && ((memberInfo == null) || memberInfo.matches(call.owner, call.name, call.desc).isExactMatch());
            case "FIELD" -> {
                int opcode = (annotationValue(at, "opcode") instanceof Integer value) ? value : -1;
                yield insn -> (insn instanceof FieldInsnNode access) && ((opcode == -1) || (access.getOpcode() == opcode)) && ((memberInfo == null) || memberInfo.matches(access.owner, access.name, access.desc).isExactMatch());
            }
            case "NEW" -> {
                String createdType = (memberInfo != null) ? memberInfo.toCtorType() : null;
                yield insn -> (insn instanceof TypeInsnNode creation) && (creation.getOpcode() == Opcodes.NEW) && ((createdType == null) || createdType.equals(creation.desc));
            }
            case null, default -> null;
        };
    }

    private static boolean hasSpecialMemberTarget(List<Member> targetMembers, String mixinMemberName, String desc, AnnotationNode annotation) {
        List<String> names = new ArrayList<>();
        String prefix = (annotationValue(annotation, "prefix") instanceof String value) ? value : DEFAULT_SHADOW_PREFIX;
        names.add(mixinMemberName.startsWith(prefix) ? mixinMemberName.substring(prefix.length()) : mixinMemberName);
        for (Object alias : list(annotationValue(annotation, "aliases"))) names.add((String) alias);
        return targetMembers.stream().anyMatch(member -> names.contains(member.name()) && member.desc().equals(desc));
    }

    /**
     * Mirrors Mixin's accessor candidate search: an exact name and descriptor match wins, otherwise a single case-insensitive match is accepted.
     */
    private static boolean hasAccessorTarget(ClassNode target, List<Member> targetMembers, String name, String desc) {
        MemberInfo memberInfo = new MemberInfo(name, null, desc);
        List<MatchResult> matches = targetMembers.stream().map(member -> memberInfo.matches(target.name, member.name(), member.desc())).filter(MatchResult::isMatch).toList();
        return matches.stream().anyMatch(MatchResult::isExactMatch) || (matches.size() == 1);
    }

    private static String resolveAccessorTargetName(MethodNode method, AnnotationNode annotation) {
        if ((annotationValue(annotation, "value") instanceof String specifiedName) && !specifiedName.isEmpty()) {
            String parsedName = MemberInfo.parse(specifiedName, null).getName();
            return (parsedName != null) ? parsedName : specifiedName;
        }
        AccessorInfo.AccessorName inflectedName = AccessorInfo.AccessorName.of(method.name);
        return (inflectedName != null) ? inflectedName.name : method.name;
    }

    private static String resolveAccessorFieldDesc(MethodNode accessor) {
        Type[] argumentTypes = Type.getArgumentTypes(accessor.desc);
        return (argumentTypes.length == 0) ? Type.getReturnType(accessor.desc).getDescriptor() : argumentTypes[0].getDescriptor();
    }

    private static boolean isObjectFactoryInvoker(ClassNode target, MethodNode invoker, AnnotationNode annotation) {
        if (CONSTRUCTOR.equals(annotationValue(annotation, "value"))) return true;
        if (!Type.getReturnType(invoker.desc).getDescriptor().equals("L" + target.name + ";")) return false;
        AccessorInfo.AccessorName accessorName = AccessorInfo.AccessorName.of(invoker.name, false);
        return (accessorName != null) && OBJECT_FACTORY_PREFIXES.contains(accessorName.prefix);
    }

    private static boolean isInjector(AnnotationNode annotation) {
        return INJECTOR_ANNOTATION_PACKAGES.stream().anyMatch(annotation.desc::startsWith) && (annotationValue(annotation, "method") != null);
    }

    private static boolean isPlainShadow(@Nullable AnnotationNode shadow) {
        return (shadow != null) && list(annotationValue(shadow, "aliases")).isEmpty();
    }

    private static List<String> readMixinTargetNames(AnnotationNode mixin) {
        List<String> targetNames = new ArrayList<>();
        for (Object type : list(annotationValue(mixin, "value"))) targetNames.add(((Type) type).getInternalName());
        for (Object name : list(annotationValue(mixin, "targets"))) targetNames.add(((String) name).replace('.', '/'));
        return targetNames;
    }

    private static List<String> readConfiguredMixinClassNames() throws IOException {
        List<String> mixinClassNames = new ArrayList<>();
        for (String configName : MIXIN_CONFIGS) {
            JsonObject config = JsonParser.parseString(readResource(configName)).getAsJsonObject();
            String mixinPackage = config.get("package").getAsString();
            for (String section : MIXIN_CONFIG_SECTIONS) {
                JsonArray entries = config.getAsJsonArray(section);
                if (entries == null) continue;
                for (JsonElement entry : entries) mixinClassNames.add(mixinPackage + "." + entry.getAsString());
            }
        }
        return mixinClassNames;
    }

    private ClassNode readFirstSingleTargetMixin(Predicate<ClassNode> filter) throws IOException {
        for (String mixinClassName : readConfiguredMixinClassNames()) {
            ClassNode mixin = this.requireClass(mixinClassName);
            AnnotationNode mixinAnnotation = findAnnotation(mixin.visibleAnnotations, mixin.invisibleAnnotations, MIXIN_DESC);
            if ((mixinAnnotation != null) && (readMixinTargetNames(mixinAnnotation).size() == 1) && filter.test(mixin)) return readClassFromClasspath(mixin.name).orElseThrow();
        }
        throw new IllegalStateException("No configured mixin matches the filter");
    }

    private ClassNode requireClass(String className) {
        return this.findClass(className.replace('.', '/')).orElseThrow(() -> new IllegalStateException("Class not found on the test classpath: " + className));
    }

    private Optional<ClassNode> findClass(String internalName) {
        return this.classCache.computeIfAbsent(internalName, MixinTargetResolutionTest::readClassFromClasspath);
    }

    private static ClassNode readMutableClass(Class<?> type) {
        return readClassFromClasspath(Type.getInternalName(type)).orElseThrow();
    }

    private static Optional<ClassNode> readClassFromClasspath(String internalName) {
        try (InputStream in = MixinTargetResolutionTest.class.getClassLoader().getResourceAsStream(internalName + ".class")) {
            if (in == null) return Optional.empty();
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return Optional.of(node);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String readResource(String name) throws IOException {
        try (InputStream in = MixinTargetResolutionTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "Resource not found on the test classpath: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static AnnotationNode requireInjector(ClassNode mixin, String handlerName) {
        MethodNode handler = mixin.methods.stream().filter(method -> method.name.equals(handlerName)).findFirst().orElseThrow();
        return concat(handler.visibleAnnotations, handler.invisibleAnnotations).stream().filter(MixinTargetResolutionTest::isInjector).findFirst().orElseThrow();
    }

    private static List<Member> fieldMembers(ClassNode node) {
        return node.fields.stream().map(field -> new Member(field.name, field.desc)).toList();
    }

    private static List<Member> methodMembers(ClassNode node) {
        return node.methods.stream().map(method -> new Member(method.name, method.desc)).toList();
    }

    @Nullable
    private static AnnotationNode findAnnotation(@Nullable List<AnnotationNode> visible, @Nullable List<AnnotationNode> invisible, String desc) {
        return concat(visible, invisible).stream().filter(annotation -> annotation.desc.equals(desc)).findFirst().orElse(null);
    }

    private static List<AnnotationNode> concat(@Nullable List<AnnotationNode> visible, @Nullable List<AnnotationNode> invisible) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (visible != null) annotations.addAll(visible);
        if (invisible != null) annotations.addAll(invisible);
        return annotations;
    }

    /**
     * ASM stores annotation attributes as a flat name/value list and omits attributes that use their default value.
     */
    @Nullable
    private static Object annotationValue(AnnotationNode annotation, String key) {
        if (annotation.values == null) return null;
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static void setAnnotationValue(AnnotationNode annotation, String key, Object value) {
        if (annotation.values == null) annotation.values = new ArrayList<>();
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) {
                annotation.values.set(i + 1, value);
                return;
            }
        }
        annotation.values.add(key);
        annotation.values.add(value);
    }

    private static List<?> list(@Nullable Object annotationValue) {
        return (annotationValue instanceof List<?> values) ? values : List.of();
    }

    /**
     * Annotation attributes declared as arrays are stored as lists, but single-value attributes (like {@code ModifyArg#at}) as a plain node.
     */
    private static List<AnnotationNode> annotationList(@Nullable Object annotationValue) {
        if (annotationValue instanceof AnnotationNode node) return List.of(node);
        return list(annotationValue).stream().map(AnnotationNode.class::cast).toList();
    }

    private record Member(String name, String desc) {
    }

    private record UnresolvedTarget(String targetClass, String mixinMember, String reference) {
    }

}
