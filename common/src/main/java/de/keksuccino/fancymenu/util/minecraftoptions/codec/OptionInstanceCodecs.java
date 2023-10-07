package de.keksuccino.fancymenu.util.minecraftoptions.codec;

import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;

public class OptionInstanceCodecs {

    public static final OptionInstanceCodec<String> STRING_CODEC = new OptionInstanceCodec<>(String.class, consumes -> consumes, consumes -> consumes);
    public static final OptionInstanceCodec<Boolean> BOOLEAN_CODEC = new OptionInstanceCodec<>(Boolean.class, consumes -> "" + consumes.booleanValue(), consumes -> consumes.equalsIgnoreCase("true"));
    public static final OptionInstanceCodec<Integer> INTEGER_CODEC = new OptionInstanceCodec<>(Integer.class, consumes -> "" + consumes, consumes -> MathUtils.isInteger(consumes) ? Integer.parseInt(consumes) : null);
    public static final OptionInstanceCodec<Double> DOUBLE_CODEC = new OptionInstanceCodec<>(Double.class, consumes -> "" + consumes, consumes -> MathUtils.isDouble(consumes) ? Double.parseDouble(consumes) : null);
    public static final OptionInstanceCodec<Float> FLOAT_CODEC = new OptionInstanceCodec<>(Float.class, consumes -> "" + consumes, consumes -> MathUtils.isFloat(consumes) ? Float.parseFloat(consumes) : null);
    public static final OptionInstanceCodec<Long> LONG_CODEC = new OptionInstanceCodec<>(Long.class, consumes -> "" + consumes, consumes -> MathUtils.isLong(consumes) ? Long.parseLong(consumes) : null);
    public static final OptionInstanceCodec<CloudStatus> CLOUD_STATUS_CODEC = new OptionInstanceCodec<>(CloudStatus.class,
            consumes -> switch (consumes) {
                case FANCY -> "true";
                case FAST -> "fast";
                default -> "false";
            },
            consumes -> switch (consumes) {
                case "true" -> CloudStatus.FANCY;
                case "fast" -> CloudStatus.FAST;
                default -> CloudStatus.OFF;
            });
    public static final OptionInstanceCodec<GraphicsStatus> GRAPHICS_STATUS_CODEC = new OptionInstanceCodec<>(GraphicsStatus.class, consumes -> "" + consumes.getId(), consumes -> MathUtils.isInteger(consumes) ? GraphicsStatus.byId(Integer.parseInt(consumes)) : null);
    public static final OptionInstanceCodec<PrioritizeChunkUpdates> PRIORITIZE_CHUNK_UPDATES_CODEC = new OptionInstanceCodec<>(PrioritizeChunkUpdates.class, consumes -> "" + consumes.getId(), consumes -> MathUtils.isInteger(consumes) ? PrioritizeChunkUpdates.byId(Integer.parseInt(consumes)) : null);
    public static final OptionInstanceCodec<ChatVisiblity> CHAT_VISIBILITY_CODEC = new OptionInstanceCodec<>(ChatVisiblity.class, consumes -> "" + consumes.getId(), consumes -> MathUtils.isInteger(consumes) ? ChatVisiblity.byId(Integer.parseInt(consumes)) : null);
    public static final OptionInstanceCodec<HumanoidArm> HUMANOID_ARM_CODEC = new OptionInstanceCodec<>(HumanoidArm.class, consumes -> (consumes == HumanoidArm.LEFT) ? "left" : "right", consumes -> "left".equals(consumes) ? HumanoidArm.LEFT : HumanoidArm.RIGHT);

    public static void registerAll() {

        OptionInstanceCodecRegistry.register(STRING_CODEC);
        OptionInstanceCodecRegistry.register(BOOLEAN_CODEC);
        OptionInstanceCodecRegistry.register(INTEGER_CODEC);
        OptionInstanceCodecRegistry.register(DOUBLE_CODEC);
        OptionInstanceCodecRegistry.register(FLOAT_CODEC);
        OptionInstanceCodecRegistry.register(LONG_CODEC);
        OptionInstanceCodecRegistry.register(CLOUD_STATUS_CODEC);
        OptionInstanceCodecRegistry.register(GRAPHICS_STATUS_CODEC);
        OptionInstanceCodecRegistry.register(PRIORITIZE_CHUNK_UPDATES_CODEC);
        OptionInstanceCodecRegistry.register(CHAT_VISIBILITY_CODEC);
        OptionInstanceCodecRegistry.register(HUMANOID_ARM_CODEC);

    }

}
