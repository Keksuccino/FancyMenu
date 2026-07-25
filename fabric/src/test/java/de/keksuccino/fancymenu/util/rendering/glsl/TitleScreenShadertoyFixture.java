package de.keksuccino.fancymenu.util.rendering.glsl;

/** Real procedural title-screen source retained as a compatibility fixture. */
final class TitleScreenShadertoyFixture {

    static final String SOURCE = """
            // ===============================
            // Fake Depth Background - Aspect Fixed
            // Procedural parallax background
            // No stretching when element is resized
            // ===============================

            const float PI = 3.14159265359;

            float hash(float n) {
                return fract(sin(n) * 43758.5453123);
            }

            float hash2(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }

            vec2 hash22(vec2 p) {
                return vec2(
                    hash2(p + vec2(13.7, 91.2)),
                    hash2(p + vec2(41.3, 17.8))
                );
            }

            float noise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);

                f = f * f * (3.0 - 2.0 * f);

                float a = hash2(i);
                float b = hash2(i + vec2(1.0, 0.0));
                float c = hash2(i + vec2(0.0, 1.0));
                float d = hash2(i + vec2(1.0, 1.0));

                return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
            }

            float fbm(vec2 p) {
                float v = 0.0;
                float a = 0.5;

                v += noise(p) * a;
                p *= 2.02;
                a *= 0.5;

                v += noise(p) * a;
                p *= 2.03;
                a *= 0.5;

                v += noise(p) * a;
                p *= 2.01;
                a *= 0.5;

                v += noise(p) * a;

                return v;
            }

            // Aspect-fixed coordinate.
            // This makes procedural shapes keep the same proportions
            // no matter how wide/tall the element becomes.
            vec2 worldCoord(vec2 fragCoord, vec2 size) {
                float s = min(size.x, size.y);
                return (fragCoord - size * 0.5) / s;
            }

            // Convert world coordinate back to a 0..1-ish screen coordinate for gradients.
            vec2 worldToView(vec2 world, vec2 size) {
                float s = min(size.x, size.y);
                return world * s / size + 0.5;
            }

            float particleLayer(vec2 world, vec2 parallax, float depth, float scale, float speed, float brightness) {
                // World-space movement, not UV-space movement.
                vec2 p = world;

                p += parallax * depth * 0.35;
                p += vec2(iTime * speed * 0.020, -iTime * speed * 0.012);

                vec2 grid = p * scale;
                vec2 cell = floor(grid);
                vec2 local = fract(grid);

                float result = 0.0;

                for (int y = -1; y <= 1; y++) {
                    for (int x = -1; x <= 1; x++) {
                        vec2 offset = vec2(float(x), float(y));
                        vec2 cid = cell + offset;

                        vec2 rnd = hash22(cid);
                        float rare = hash2(cid + 19.37);

                        float exists = step(0.58, rare);

                        vec2 starPos = offset + rnd;
                        vec2 delta = local - starPos;

                        float d = length(delta);

                        float twinkle = 0.65 + 0.35 * sin(iTime * mix(1.0, 3.5, rnd.x) + rare * 20.0);
                        float radius = mix(0.010, 0.026, depth) * mix(0.7, 1.4, rnd.y);

                        float core = exp(-pow(d / radius, 2.0));
                        float glow = exp(-pow(d / (radius * 4.0), 2.0));

                        result += exists * brightness * twinkle * (core + glow * 0.20);
                    }
                }

                return result;
            }

            void mainImage(out vec4 fragColor, in vec2 fragCoord) {
                vec2 size = iResolution.xy;
                vec2 uv = fragCoord / size;

                // Aspect-fixed world coordinate.
                // This is the important part.
                vec2 world = worldCoord(fragCoord, size);

                // Mouse parallax
                vec2 mouse = iMouse.xy;

                if (mouse.x <= 0.0 && mouse.y <= 0.0) {
                    mouse = size * 0.5;
                }

                vec2 mouseWorld = worldCoord(mouse, size);
                vec2 parallax = mouseWorld;

                float dist = length(world);

                // ===== Deep gradient background =====
                // Gradient can still use uv.y because vertical screen gradient is fine.
                vec3 topCol = vec3(0.020, 0.040, 0.085);
                vec3 botCol = vec3(0.005, 0.010, 0.025);
                vec3 col = mix(botCol, topCol, uv.y);

                // ===== Far nebula / soft clouds =====
                // Use world coordinate so clouds are not stretched.
                vec2 cloudWorld = world;

                float cloudA = fbm(cloudWorld * 2.2 + parallax * 0.08 + vec2(iTime * 0.015, -iTime * 0.010));
                float cloudB = fbm(cloudWorld * 4.8 - parallax * 0.14 + vec2(-iTime * 0.010, iTime * 0.012));

                float nebula = smoothstep(0.38, 0.86, cloudA);
                float fineNebula = smoothstep(0.48, 0.92, cloudB);

                col += vec3(0.020, 0.070, 0.150) * nebula * 0.55;
                col += vec3(0.040, 0.120, 0.230) * fineNebula * 0.20;

                // ===== Depth particle layers =====
                float farDust  = particleLayer(world, parallax, 0.12, 18.0, 0.25, 0.28);
                float midDust  = particleLayer(world, parallax, 0.35, 12.0, 0.55, 0.42);
                float nearDust = particleLayer(world, parallax, 0.75,  7.0, 1.00, 0.58);

                col += vec3(0.25, 0.48, 0.90) * farDust;
                col += vec3(0.40, 0.72, 1.00) * midDust;
                col += vec3(0.75, 0.92, 1.00) * nearDust;

                // ===== Mouse glow, aspect fixed =====
                float mouseGlow = exp(-pow(length(world - mouseWorld) / 0.35, 2.0));
                col += vec3(0.025, 0.090, 0.180) * mouseGlow * 0.35;

                // ===== Slow diagonal light sweep =====
                // Use world coordinate so sweep is not stretched.
                float sweep = sin((world.x + world.y) * 3.2 + iTime * 0.45) * 0.5 + 0.5;
                sweep = smoothstep(0.72, 1.0, sweep);
                col += vec3(0.030, 0.080, 0.160) * sweep * 0.12;

                // ===== Vignette, aspect fixed =====
                float vignette = smoothstep(0.95, 0.25, dist);
                col *= mix(0.42, 1.0, vignette);

                // ===== Very light cinematic grain =====
                float grain = hash2(fragCoord + floor(iTime * 24.0)) - 0.5;
                col += grain * 0.010;

                // Tone map
                col = 1.0 - exp(-col * 1.15);

                fragColor = vec4(col, 1.0);
            }
            """;

    private TitleScreenShadertoyFixture() {
    }

}
