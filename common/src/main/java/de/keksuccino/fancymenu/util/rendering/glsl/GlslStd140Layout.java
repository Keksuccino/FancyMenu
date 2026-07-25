package de.keksuccino.fancymenu.util.rendering.glsl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small std140 layout calculator used by dynamically authored FancyMenu shaders.
 *
 * <p>The GPU backends only expose uniform-buffer binding through the public rendering API. Keeping the layout
 * calculation here, instead of relying on a backend's reflection result, makes the exact same byte layout available
 * before either OpenGL or Vulkan compiles the shader.</p>
 */
public final class GlslStd140Layout {

    /**
     * Conservative block-size ceiling shared by the OpenGL and Vulkan paths. Both APIs guarantee at least 16 KiB
     * for one uniform block/range; accepting more would make a user shader device-dependent and could allow a
     * pathological array declaration to reach native allocation code. Do not raise this without adding explicit
     * per-device limit handling for both backends.
     */
    static final int MAX_UNIFORM_BLOCK_SIZE = 16 * 1024;

    private static final Pattern VECTOR_TYPE_PATTERN = Pattern.compile("([biu]?vec)([2-4])");
    private static final Pattern MATRIX_TYPE_PATTERN = Pattern.compile("mat([2-4])(?:x([2-4]))?");

    private final List<Member> members;
    private final Map<String, Member> membersByName;
    private final int size;

    private GlslStd140Layout(@NotNull List<Member> members, int size) {
        this.members = List.copyOf(members);
        Map<String, Member> indexedMembers = new LinkedHashMap<>();
        for (Member member : members) {
            indexedMembers.put(member.name(), member);
        }
        this.membersByName = Collections.unmodifiableMap(indexedMembers);
        this.size = size;
    }

    @NotNull
    public static GlslStd140Layout create(@NotNull List<Declaration> declarations) {
        List<Member> members = new ArrayList<>(declarations.size());
        Map<String, Declaration> declarationsByName = new LinkedHashMap<>();
        int offset = 0;

        for (Declaration declaration : declarations) {
            Declaration previous = declarationsByName.putIfAbsent(declaration.name(), declaration);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate uniform declaration '" + declaration.name() + "'.");
            }

            TypeInfo type = TypeInfo.parse(declaration.type());
            boolean array = declaration.arrayLength() > 0;
            int alignment = array ? 16 : type.alignment();
            int arrayStride = array ? roundUp(type.size(), 16) : 0;
            long memberSize = array ? (long) arrayStride * declaration.arrayLength() : type.size();
            int memberOffset = roundUp(offset, alignment);
            long unpaddedBlockSize = memberOffset + memberSize;
            long paddedBlockSize = roundUp(unpaddedBlockSize, 16);
            if (paddedBlockSize > MAX_UNIFORM_BLOCK_SIZE) {
                throw new IllegalArgumentException("Uniform block exceeds FancyMenu's cross-backend limit of " + MAX_UNIFORM_BLOCK_SIZE + " bytes after declaration '" + declaration.name() + "' (requires " + paddedBlockSize + " bytes).");
            }
            members.add(new Member(declaration.type(), declaration.name(), memberOffset, (int) memberSize, alignment, declaration.arrayLength(), arrayStride, type.scalarKind(), type.columns(), type.rows()));
            offset = (int) unpaddedBlockSize;
        }

        return new GlslStd140Layout(members, roundUp(offset, 16));
    }

    @NotNull
    public List<Member> members() {
        return this.members;
    }

    @Nullable
    public Member member(@NotNull String name) {
        return this.membersByName.get(name);
    }

    public int size() {
        return this.size;
    }

    @NotNull
    public ByteBuffer createBuffer() {
        return ByteBuffer.allocateDirect(this.size).order(ByteOrder.nativeOrder());
    }

    @NotNull
    public ByteBuffer pack(@NotNull Map<String, ?> values) {
        ByteBuffer buffer = this.createBuffer();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof float[] floats) {
                this.writeFloats(buffer, entry.getKey(), floats);
            } else if (value instanceof int[] integers) {
                this.writeInts(buffer, entry.getKey(), integers);
            } else if (value instanceof Number number) {
                Member member = this.requireMember(entry.getKey());
                if (member.scalarKind() == ScalarKind.FLOAT) {
                    this.writeFloats(buffer, entry.getKey(), number.floatValue());
                } else {
                    this.writeInts(buffer, entry.getKey(), number.intValue());
                }
            } else if (value instanceof Boolean bool) {
                this.writeInts(buffer, entry.getKey(), bool ? 1 : 0);
            } else if (value != null) {
                throw new IllegalArgumentException("Unsupported value for uniform '" + entry.getKey() + "': " + value.getClass().getName());
            }
        }
        buffer.position(0);
        buffer.limit(this.size);
        return buffer;
    }

    public void writeFloats(@NotNull ByteBuffer buffer, @NotNull String name, float... values) {
        Member member = this.requireMember(name);
        if (member.scalarKind() != ScalarKind.FLOAT) {
            throw new IllegalArgumentException("Uniform '" + name + "' is not floating-point (declared " + member.type() + ").");
        }
        this.writeComponents(buffer, member, values, null);
    }

    public void writeInts(@NotNull ByteBuffer buffer, @NotNull String name, int... values) {
        Member member = this.requireMember(name);
        if (member.scalarKind() == ScalarKind.FLOAT) {
            throw new IllegalArgumentException("Uniform '" + name + "' is not integer-compatible (declared " + member.type() + ").");
        }
        this.writeComponents(buffer, member, null, values);
    }

    private void writeComponents(@NotNull ByteBuffer buffer, @NotNull Member member, @Nullable float[] floats, @Nullable int[] integers) {
        int providedCount = floats != null ? floats.length : integers != null ? integers.length : 0;
        int componentCount = member.columns() * member.rows();
        int elementCount = member.arrayLength() > 0 ? member.arrayLength() : 1;
        int expectedCount = componentCount * elementCount;
        if (providedCount > expectedCount) {
            throw new IllegalArgumentException("Too many values for uniform '" + member.name() + "': expected at most " + expectedCount + ", got " + providedCount + ".");
        }

        int valueIndex = 0;
        for (int elementIndex = 0; elementIndex < elementCount && valueIndex < providedCount; elementIndex++) {
            int elementOffset = member.offset() + (member.arrayLength() > 0 ? elementIndex * member.arrayStride() : 0);
            for (int column = 0; column < member.columns() && valueIndex < providedCount; column++) {
                int columnOffset = elementOffset + (member.columns() > 1 ? column * 16 : 0);
                for (int row = 0; row < member.rows() && valueIndex < providedCount; row++) {
                    int componentOffset = columnOffset + row * Integer.BYTES;
                    if (floats != null) {
                        buffer.putFloat(componentOffset, floats[valueIndex]);
                    } else {
                        buffer.putInt(componentOffset, integers[valueIndex]);
                    }
                    valueIndex++;
                }
            }
        }
    }

    @NotNull
    private Member requireMember(@NotNull String name) {
        Member member = this.membersByName.get(name);
        if (member == null) {
            throw new IllegalArgumentException("Unknown uniform '" + name + "'.");
        }
        return member;
    }

    private static int roundUp(int value, int alignment) {
        return (int) roundUp((long) value, alignment);
    }

    private static long roundUp(long value, int alignment) {
        return value == 0 ? 0 : (value + alignment - 1) / alignment * alignment;
    }

    public enum ScalarKind {
        FLOAT,
        SIGNED_INT,
        UNSIGNED_INT,
        BOOLEAN
    }

    public record Declaration(@NotNull String type, @NotNull String name, int arrayLength) {

        public Declaration {
            if (type.isBlank()) {
                throw new IllegalArgumentException("Uniform type cannot be blank.");
            }
            if (name.isBlank()) {
                throw new IllegalArgumentException("Uniform name cannot be blank.");
            }
            if (arrayLength < 0) {
                throw new IllegalArgumentException("Uniform array length cannot be negative.");
            }
        }

        @NotNull
        public String toShaderDeclaration() {
            return this.type + " " + this.name + (this.arrayLength > 0 ? "[" + this.arrayLength + "]" : "") + ";";
        }
    }

    public record Member(@NotNull String type, @NotNull String name, int offset, int size, int alignment, int arrayLength, int arrayStride, @NotNull ScalarKind scalarKind, int columns, int rows) {

        public int elementCount() {
            return this.arrayLength > 0 ? this.arrayLength : 1;
        }
    }

    private record TypeInfo(@NotNull ScalarKind scalarKind, int columns, int rows, int alignment, int size) {

        @NotNull
        private static TypeInfo parse(@NotNull String rawType) {
            String type = rawType.trim();
            return switch (type) {
                case "float" -> new TypeInfo(ScalarKind.FLOAT, 1, 1, 4, 4);
                case "int" -> new TypeInfo(ScalarKind.SIGNED_INT, 1, 1, 4, 4);
                case "uint" -> new TypeInfo(ScalarKind.UNSIGNED_INT, 1, 1, 4, 4);
                case "bool" -> new TypeInfo(ScalarKind.BOOLEAN, 1, 1, 4, 4);
                default -> parseComposite(type);
            };
        }

        @NotNull
        private static TypeInfo parseComposite(@NotNull String type) {
            Matcher vectorMatcher = VECTOR_TYPE_PATTERN.matcher(type);
            if (vectorMatcher.matches()) {
                int rows = Integer.parseInt(vectorMatcher.group(2));
                ScalarKind scalarKind = switch (vectorMatcher.group(1)) {
                    case "vec" -> ScalarKind.FLOAT;
                    case "ivec" -> ScalarKind.SIGNED_INT;
                    case "uvec" -> ScalarKind.UNSIGNED_INT;
                    case "bvec" -> ScalarKind.BOOLEAN;
                    default -> throw new IllegalArgumentException("Unsupported std140 vector type '" + type + "'.");
                };
                int alignment = rows == 2 ? 8 : 16;
                // A three-component vector has 16-byte base alignment but only 12 occupied bytes. A following scalar
                // may legally occupy the fourth lane; arrays still round their element stride up to 16 below.
                int size = rows * Integer.BYTES;
                return new TypeInfo(scalarKind, 1, rows, alignment, size);
            }

            Matcher matrixMatcher = MATRIX_TYPE_PATTERN.matcher(type);
            if (matrixMatcher.matches()) {
                int columns = Integer.parseInt(matrixMatcher.group(1));
                int rows = matrixMatcher.group(2) == null ? columns : Integer.parseInt(matrixMatcher.group(2));
                return new TypeInfo(ScalarKind.FLOAT, columns, rows, 16, columns * 16);
            }

            throw new IllegalArgumentException("Unsupported std140 uniform type '" + type + "'. Supported types are float/int/uint/bool, their 2-4 component vectors, and float matrices.");
        }
    }
}
