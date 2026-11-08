package de.keksuccino.fancymenu.util.rendering;

import org.jetbrains.annotations.NotNull;

public final class IconAnimations {

    public static final IconAnimation SHORT_WIGGLE_LEFT_RIGHT = createBounceWiggle(400.0F, -1.6F, 1.4F, 1.0F);
    public static final IconAnimation SHORT_SPIN_UP = createSpinUpRotation(500.0F, 720.0F, 0.25F, 0.32F);
    public static final IconAnimation SHORT_SPIN_UP_SUBTLE = createSpinUpRotation(350.0F, 360.0F, 0.25F, 0.32F);
    public static final IconAnimation SHORT_PENDULUM_SWAY = createPendulumSway(380.0F, 1.1F, 0.3F, 0.12F);
    public static final IconAnimation SHORT_DIAGONAL_BOUNCE = createDiagonalBounce(400.0F, 0.25F, 0.6F, 0.12F, 2.5F);

    private IconAnimations() {
    }

    @NotNull
    public static IconAnimation createBounceWiggle(float durationMs, float leftPeak, float rightPeak, float tailAmplitude) {
        return new IconAnimation(durationMs, t -> new IconAnimation.Offset(
                computeBounceWiggleOffsetX(t, leftPeak, rightPeak, tailAmplitude),
                0.0F
        ));
    }

    @NotNull
    public static IconAnimation createSpinUpRotation(float durationMs, float totalDegrees, float rampFraction, float rampPortion) {
        return new IconAnimation(durationMs, t -> new IconAnimation.Offset(
                0.0F,
                0.0F,
                totalDegrees * computeSpinUpProgress(t, rampFraction, rampPortion)
        ));
    }

    @NotNull
    public static IconAnimation createPendulumSway(float durationMs, float horizontalAmplitude, float verticalAmplitude, float verticalDelayFraction) {
        return new IconAnimation(durationMs, t -> {
            float clamped = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
            float eased = easeInOutSine(clamped);
            float envelope = 1.0F - 0.2F * clamped;
            float x = (float) Math.sin(eased * Math.PI * 2.0) * horizontalAmplitude * envelope;

            float delayedT = clamped <= verticalDelayFraction ? 0.0F : (clamped - verticalDelayFraction) / (1.0F - verticalDelayFraction);
            float delayedEased = easeInOutSine(delayedT);
            float y = (float) Math.sin(delayedEased * Math.PI * 2.0) * verticalAmplitude * envelope * 0.8F;

            return new IconAnimation.Offset(x, y);
        });
    }

    @NotNull
    public static IconAnimation createDiagonalBounce(float durationMs, float horizontalAmplitude, float verticalAmplitude, float secondaryAmplitude, float rotationDegrees) {
        return new IconAnimation(durationMs, t -> {
            float clamped = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
            float eased = easeInOutSine(clamped);
            float baseX = lerp(-horizontalAmplitude, horizontalAmplitude, eased);
            float endFade = 1.0F - smoothStep((clamped - 0.75F) / 0.25F);
            float x = baseX * endFade;

            float arc = (float) Math.sin(clamped * Math.PI);
            float y = -arc * verticalAmplitude;

            float secondary = (float) Math.sin(clamped * Math.PI * 2.0F) * arc;
            y += secondary * secondaryAmplitude * 0.5F;

            float rotation = 0.0F;
            if (horizontalAmplitude != 0.0F) {
                float lean = x / horizontalAmplitude;
                rotation = lean * rotationDegrees * arc;
            }

            return new IconAnimation.Offset(x, y, rotation);
        });
    }

    private static float computeBounceWiggleOffsetX(float t, float leftPeak, float rightPeak, float tailAmplitude) {
        float clamped = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
        if (clamped < 0.18F) {
            return lerp(0.0F, leftPeak, smoothStep(clamped / 0.18F));
        }
        if (clamped < 0.32F) {
            return lerp(leftPeak, 0.0F, smoothStep((clamped - 0.18F) / 0.14F));
        }
        if (clamped < 0.50F) {
            return lerp(0.0F, rightPeak, smoothStep((clamped - 0.32F) / 0.18F));
        }
        if (clamped < 0.64F) {
            return lerp(rightPeak, 0.0F, smoothStep((clamped - 0.50F) / 0.14F));
        }
        float tailT = (clamped - 0.64F) / 0.36F;
        float decay = 1.0F - tailT;
        float amplitude = tailAmplitude * decay * decay;
        return (float) (Math.sin(tailT * Math.PI * 2.0) * amplitude);
    }

    private static float computeSpinUpProgress(float t, float rampFraction, float rampPortion) {
        float clamped = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
        float ramp = Math.max(0.05F, Math.min(rampFraction, 0.6F));
        float portion = Math.max(0.05F, Math.min(rampPortion, 0.6F));
        if (clamped <= ramp) {
            float local = clamped / ramp;
            return portion * easeInQuad(local);
        }
        float tailT = (clamped - ramp) / (1.0F - ramp);
        return portion + (1.0F - portion) * easeOutCubic(tailT);
    }

    private static float easeInQuad(float t) {
        float clamped = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
        return clamped * clamped;
    }

    private static float easeOutCubic(float t) {
        float clamped = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
        float inv = 1.0F - clamped;
        return 1.0F - inv * inv * inv;
    }

    private static float easeInOutSine(float t) {
        float clamped = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
        return (float) (0.5F - 0.5F * Math.cos(clamped * Math.PI));
    }

    private static float easeInOutCubic(float t) {
        float clamped = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
        return clamped < 0.5F
                ? 4.0F * clamped * clamped * clamped
                : 1.0F - (float) Math.pow(-2.0F * clamped + 2.0F, 3.0F) * 0.5F;
    }

    private static float smoothStep(float t) {
        float clamped = t < 0.0F ? 0.0F : Math.min(t, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }
}
