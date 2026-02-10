# FancyMenu GLSL Shader API

This document describes the GLSL runtime used by FancyMenu's:

- `GLSL` menu background
- `GLSL` element
- `GLSL` decoration overlay

It covers compile modes, multipass routing, supported uniforms, and practical shader authoring patterns.

## 1. Runtime Overview

FancyMenu renders shaders with an internal OpenGL pipeline (`#version 150`) and supports:

- single-pass shaders (`Image` pass only)
- multipass shaders (`Buffer A` / `B` / `C` / `D` + `Image`)
- Shadertoy-style entry points (`mainImage`)
- direct fragment entry points (`main`)

Shader sources are inline text fields:

- `Shader Source` (Image pass, required to render)
- `Buffer A Source` (optional)
- `Buffer B Source` (optional)
- `Buffer C Source` (optional)
- `Buffer D Source` (optional)

If Image source is empty, rendering fails with a "no source" error.

## 2. Compile Modes

FancyMenu supports three compile modes:

- `Auto`
- `Direct Fragment`
- `Shadertoy`

### 2.1 Shadertoy mode

Expected entry point:

```glsl
void mainImage(out vec4 fragColor, in vec2 fragCoord)
```

FancyMenu wraps it into `main()` and passes local area coordinates:

```glsl
mainImage(fmColor_FancyMenu, gl_FragCoord.xy - fmAreaOffset);
```

The wrapper multiplies output alpha by `fmOpacity`.

### 2.2 Direct mode

Expected entry point:

```glsl
void main()
```

Compatibility behavior:

- A `gl_FragColor` compatibility variant is attempted.
- A no-compat variant is also attempted (for modern explicit `out vec4` shaders).

In direct mode, `fmOpacity` is not automatically applied to your output. Apply it manually if needed.

### 2.3 Auto mode

Auto tries compatible variants in sequence (Shadertoy/direct) and uses the first one that compiles.

## 3. Source Preprocessing and Built-in Macros

Before compile, FancyMenu normalizes source:

- removes UTF-8 BOM
- converts CRLF/CR to LF
- strips `#version ...` lines
- strips `precision ...;` lines

Runtime-injected preamble includes:

- `#version 150`
- `in vec2 fmUv_FancyMenu` (fullscreen UV in `[0,1]`, bottom-left origin)
- `#define iGlobalTime iTime`
- `#define texture2D texture`
- `#define textureCube texture`

Note:

- If your source already declares a known uniform name (e.g. `iTime`), FancyMenu avoids injecting a duplicate declaration.
- FancyMenu still tries to upload values to that name at runtime.
- `textureCube` is only an alias macro here; `iChannel0..3` are `sampler2D` uniforms.

## 4. Pass System (Image + Buffer A-D)

FancyMenu has 5 pass slots:

- `Buffer A`
- `Buffer B`
- `Buffer C`
- `Buffer D`
- `Image` (final on-screen pass)

Behavior:

- Buffer passes only run if their source is non-empty.
- Image pass must be present to render output.
- Buffers are rendered to floating-point textures (`GL_RGBA16F`), then ping-ponged (read/write swap each frame).

### 4.1 Channel routing per pass

Each pass exposes routing for `iChannel0..3`. Per channel you can select:

- `None`
- `Resource 0`
- `Resource 1`
- `Resource 2`
- `Resource 3`
- `Buffer A`
- `Buffer B`
- `Buffer C`
- `Buffer D`

Resource channels come from the `iChannel# Resource` settings.

Defaults:

- all `iChannel` routings default to `None`
- no buffer pass is active until that buffer source is non-empty

Important:

- Routing to the same buffer pass (feedback) reads previous-frame data (ping-pong read texture).
- If a routed source is missing/inactive, a fallback texture is bound and `iChannelResolution[n].z` becomes `0.0`.

## 5. Coordinates and Area Semantics

Shaders run in an area rectangle:

- Menu background: full screen area
- GLSL element: element rectangle

Coordinate conventions:

- Pixel uniforms are in area-local pixels.
- Y origin is bottom-left for shader-facing pixel coords.
- Mouse coordinates are not clamped; values can be outside area if cursor is outside.

Special fields:

- `fmAreaOffset`: area bottom-left position in screen pixel space
- `fmAreaTopLeft`: area top-left position in screen pixel space
- `fmAreaSize`: area size in pixels

## 6. Uniform API Reference

All uniforms below are available for both background and element shaders.

## 6.1 Shadertoy-compatible uniforms

| Uniform | Type | Meaning |
|---|---|---|
| `iResolution` | `vec3` | `(areaWidthPx, areaHeightPx, 1.0)` |
| `iTime` | `float` | Accumulated shader time in seconds |
| `iTimeDelta` | `float` | Delta time of last render, affected by freeze/time scale |
| `iFrameRate` | `float` | Minecraft FPS fallback runtime FPS |
| `iFrame` | `int` | Per-runtime frame counter |
| `iMouse` | `vec4` | See details below |
| `iDate` | `vec4` | `(year, month, day, secondsOfDayWithFraction)` |
| `iSampleRate` | `float` | Constant `44100.0` |
| `iChannelTime[4]` | `float[4]` | Currently all set to `iTime` |
| `iChannelResolution[4]` | `vec3[4]` | `(width, height, validFlag)` per channel |
| `iChannel0..3` | `sampler2D` | Routed texture inputs |

### `iMouse` details

`iMouse = vec4(x, y, z, w)`

- `x`, `y`: current area-local mouse pixel position, or hold/frozen behavior if the toggle is enabled
- `z`, `w`: left-click origin
  - positive while left mouse button is held
  - negative after release

Toggle-controlled behavior:

- `Update iMouse Position Only While Holding LMB = Off`:
  - `iMouse.xy` updates continuously
- `... = On`:
  - `iMouse.xy` updates only while LMB is down, then remains at last held position

Default value:

- `Off` (continuous updates)

## 6.2 FancyMenu-specific uniforms

| Uniform | Type | Meaning |
|---|---|---|
| `fmAreaOffset` | `vec2` | Area bottom-left pixel offset in screen space |
| `fmAreaSize` | `vec2` | Area size in pixels |
| `fmAreaPosition` | `vec2` | Same as `fmAreaOffset` |
| `fmAreaTopLeft` | `vec2` | Area top-left pixel offset in screen space |
| `fmScreenSize` | `vec2` | Full screen size in pixels |
| `fmGuiScale` | `float` | Current GUI scale |
| `fmMouse` | `vec4` | `(localPxX, localPxYBottom, localNormX, localNormY)` |
| `fmMouseDelta` | `vec2` | Mouse delta in area pixels (Y is inverted to shader-up) |
| `fmMouseButtons` | `ivec4` | Button down states for buttons `0..3` |
| `fmMouseClickCount` | `ivec4` | Cumulative press counts for buttons `0..3` |
| `fmMouseReleaseCount` | `ivec4` | Cumulative release counts for buttons `0..3` |
| `fmMouseScroll` | `vec2` | Scroll delta since this runtime's previous render |
| `fmMouseScrollTotal` | `vec2` | Cumulative scroll totals |
| `fmKeyEvent` | `ivec4` | `(lastKeyCode, lastScanCode, lastModifiers, lastAction)` |
| `fmKeyEventCount` | `int` | Cumulative key event counter |
| `fmCharEvent` | `ivec4` | `(lastCodePoint, lastModifiers, 0, 0)` |
| `fmCharEventCount` | `int` | Cumulative char-typed event counter |
| `fmDateParts` | `ivec4` | `(year, month, day, dayOfWeekIso1To7)` |
| `fmTimeParts` | `ivec4` | `(hour, minute, second, millisecondPart0To999)` |
| `fmDayOfYear` | `int` | Day of year |
| `fmWeekOfYear` | `int` | ISO week of year |
| `fmUnixTimeSeconds` | `int` | Unix epoch seconds |
| `fmUnixTimeMilliseconds` | `int` | Millisecond part of current time (`0..999`) |
| `fmPartialTick` | `float` | Current partial tick |
| `fmGameDeltaTicks` | `float` | Minecraft game delta ticks |
| `fmRealtimeDeltaTicks` | `float` | Minecraft realtime delta ticks |
| `fmInWorld` | `int` | `1` when in a world, else `0` |
| `fmIsPaused` | `int` | `1` when paused, else `0` |
| `fmOpacity` | `float` | Effective opacity multiplier (`0..1`) |

Key action values (`fmKeyEvent.w`):

- `0` = release
- `1` = press
- `2` = repeat

## 7. Input Tracking Model

FancyMenu tracks input globally and snapshots it per render:

- mouse move/drag
- mouse press/release
- scroll
- key press/release/repeat
- char typed

Mouse robustness:

- Runtime reconciles button states with GLFW polling each frame to prevent stuck-button states.

If `Pass Input Events To Shader` is disabled:

- input uniforms are reset to neutral values each frame
- counters and events are zeroed in shader-visible data

## 8. Texture Input Details

Resource channels (`iChannel# Resource`) expect 2D textures.

Texture state per channel:

- valid resource: bound texture, real width/height, `iChannelResolution[n].z = 1.0`
- missing/inactive/None: fallback texture, `iChannelResolution[n].xyz = (0,0,0)`

Buffer textures:

- internal format: `RGBA16F` (floating point)
- filtering: linear
- wrap: clamp-to-edge

This is suitable for multipass data (including values outside `[0,1]`).

## 9. Rendering and Blending Notes

- Buffer passes render offscreen without blending.
- Final Image pass uses the `Enable Blending` setting for compositing.
- Shadertoy wrapper applies `fmOpacity` to alpha automatically.
- Direct shaders should apply `fmOpacity` manually if required.

## 10. Practical Templates

## 10.1 Minimal Shadertoy-style shader

```glsl
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    vec3 col = vec3(uv, 0.5 + 0.5 * sin(iTime));
    fragColor = vec4(col, 1.0);
}
```

## 10.2 Minimal direct fragment shader

```glsl
out vec4 FragColor;

void main() {
    vec2 uv = gl_FragCoord.xy - fmAreaOffset;
    uv /= iResolution.xy;

    vec3 col = vec3(uv.x, uv.y, 0.5 + 0.5 * sin(iTime));
    FragColor = vec4(col, fmOpacity);
}
```

## 10.3 Minimal feedback multipass

### Buffer A source

Route: `Buffer A iChannel0 Input -> Buffer A`

```glsl
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    vec4 prev = texture(iChannel0, uv);
    vec4 seed = vec4(uv, 0.0, 1.0);
    fragColor = mix(prev, seed, 0.02);
}
```

### Image source

Route: `Image iChannel0 Input -> Buffer A`

```glsl
void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord / iResolution.xy;
    fragColor = texture(iChannel0, uv);
}
```

## 11. Troubleshooting Checklist

- No output:
  - verify Image source is non-empty
  - verify compile mode matches your entry point (`mainImage` vs `main`)
- Purple/invalid textures:
  - verify resource bindings and channel routing
  - check `iChannelResolution[n].z` (`0.0` means invalid/unavailable)
- Wrong coordinates in direct shader:
  - use `gl_FragCoord.xy - fmAreaOffset` for local area coords
- Drag behavior wrong:
  - use the `Update iMouse Position Only While Holding LMB` toggle
- Opacity not applied in direct shader:
  - multiply alpha by `fmOpacity` yourself
