# AFMA Implementation Plan

## Purpose
AFMA stands for `Advanced FancyMenu Animation`.

AFMA is the successor to the current FMA format and is intended to solve the following problems at the same time:

- minimize final animation file size
- keep decode time low during playback
- keep memory usage close to the current streamed FMA implementation
- avoid the performance problems caused by 16-bit PNG frame inputs
- make authoring practical by providing a full in-game creator instead of hand-packed ZIP archives

This document is the implementation plan for the complete AFMA system, including format design, runtime decoder/player behavior, creator UI, encoding pipeline, optimization strategy, and rollout/migration.

## Core Goals

### Primary priorities
- file size as small as possible without turning AFMA into a full video codec
- decode time optimized for real FancyMenu playback
- memory consumption kept near the current FMA streaming model (check old FMA format for how FMA keeps frames in memory, etc.)

### Secondary priorities
- robust authoring workflow inside FancyMenu
- deterministic output and predictable playback behavior
- backward compatibility with existing FMA support
- clean future extension points without redesigning the format again

## Non-Goals
- AFMA is not meant to become a full H.264/VP9/AV1-style codec
- AFMA is not meant to require external native tools beyond `ffmpeg` (and FFMPEG only for encoding, not for decoding)
- AFMA is not meant to support arbitrary frame dependency graphs
- AFMA is not meant to keep many decoded frames in RAM

## High-Level Direction
AFMA should be designed as an advanced streamed image animation format, not as a general-purpose video codec.

The design should stay close to the current FMA mental model:

- archive-based
- PNG-backed pixel payloads
- metadata-driven timing and looping
- streaming decode
- very low retained memory

The major change is that AFMA frames are no longer required to be full-frame PNG images. AFMA will support optimized frame operations that reduce redundant pixel storage while keeping the runtime simple enough for reliable Minecraft playback.

## Chosen AFMA Feature Set
The initial AFMA version should implement the following features:

1. full + delta frame types
2. rectangular patch frames
3. periodic keyframes
4. duplicate-frame elision
8. limited rect-copy commands

The following ideas are intentionally excluded from the initial AFMA version:

- tile-based dirty maps
- multiple complex disposal modes
- arbitrary frame reference selection
- deep motion estimation / motion vectors
- transform-coded or YUV-based frame compression

This keeps the decoder fast, the encoder manageable, and memory usage close to current FMA playback.

## AFMA File Format

### File extension and type
- extension: `.afma`
- display name: `Advanced FancyMenu Animation`
- MIME type can be FancyMenu-specific, for example `image/afma`

### Container
AFMA should remain a ZIP-based archive format. This keeps tooling simple, preserves the current FMA packaging concept, and allows gradual migration from FMA internals.

### Archive layout
Recommended archive layout:

```text
metadata.json
frame_index.json
frames/
  000000.png
  000001.png
  000002.png
intro_frames/
  000000.png
  000001.png
background.png            optional, same idea as FMA if still useful
thumbnail.png             optional, creator-generated preview
```

The main difference from FMA is that not every listed frame path must mean "full replacement image". The frame index must define how each frame behaves.

### Metadata split
AFMA should continue using:

- `metadata.json` for global animation settings
- `frame_index.json` for ordered frame descriptions

This keeps metadata responsibilities separated and makes frame scanning cheap.

## Metadata Design

### `metadata.json`
`metadata.json` should contain global settings only.

Suggested fields:

```json
{
  "format": "AFMA",
  "format_version": 1,
  "canvas_width": 1920,
  "canvas_height": 1080,
  "loop_count": 0,
  "frame_time": 41,
  "frame_time_intro": 41,
  "custom_frame_times": {
    "12": 100,
    "13": 100
  },
  "custom_frame_times_intro": {
    "3": 200
  },
  "keyframe_interval": 30,
  "encoding": {
    "png_bit_depth": 8,
    "color_model": "RGBA",
    "rect_copy_enabled": true,
    "duplicate_frame_elision": true
  },
  "creator": {
    "tool": "FancyMenu AFMA Creator",
    "tool_version": "x.y.z",
    "created_at_utc": "2026-03-23T12:00:00Z"
  }
}
```

Notes:

- `format` and `format_version` are mandatory.
- `canvas_width` and `canvas_height` are mandatory.
- output payloads should always be normalized to 8-bit PNG during creation.
- custom frame times stay conceptually aligned with current FMA behavior.

### `frame_index.json`
`frame_index.json` should explicitly describe every frame and intro frame in playback order.

Suggested shape:

```json
{
  "frames": [
    {
      "type": "full",
      "path": "frames/000000.png"
    },
    {
      "type": "delta_rect",
      "path": "frames/000001.png",
      "x": 240,
      "y": 120,
      "width": 640,
      "height": 256
    },
    {
      "type": "same"
    },
    {
      "type": "copy_rect_patch",
      "copy": {
        "src_x": 0,
        "src_y": 0,
        "dst_x": 0,
        "dst_y": 8,
        "width": 1920,
        "height": 1072
      },
      "patch": {
        "path": "frames/000003.png",
        "x": 0,
        "y": 0,
        "width": 1920,
        "height": 8
      }
    }
  ],
  "intro_frames": [
    {
      "type": "full",
      "path": "intro_frames/000000.png"
    }
  ]
}
```

## AFMA Frame Operation Types

### 1. `full`
Semantics:

- replaces the entire composited canvas with a full-frame image
- must match canvas size
- acts as a keyframe

Use cases:

- first frame of intro/main sequence
- periodic recovery points
- fallback when delta representation is inefficient

### 2. `delta_rect`
Semantics:

- patch image contains only one changed rectangle
- decoder writes the patch into the composited canvas at `x/y`
- patch pixels replace the target pixels directly

Transparency:

- transparent pixels inside the patch are valid and must overwrite previous pixels
- "unchanged" is represented by being outside the patch rect, not by transparent pixels

### 3. `same`
Semantics:

- frame image data is omitted
- decoder reuses the previous composited canvas unchanged
- frame timing still applies normally

Use cases:

- paused moments
- repeated identical frames
- timing stretches without new visual data

### 4. `copy_rect_patch`
Semantics:

- decoder first copies a rectangle from one canvas region to another
- decoder then applies an optional patch image for newly exposed or changed pixels

Exact copy semantics:

- copy source pixels must be read from the current composited canvas state before the copy is applied
- overlapping source/destination regions are valid and must behave like a `memmove`-style copy, not an unsafe forward overwrite
- the optional patch is always applied after the copy step
- copy behavior must be deterministic across all loaders and platforms

Restrictions:

- support at most one `copy_rect` operation per frame in AFMA v1
- patch section is optional if copy alone reproduces the next frame exactly
- copy must reference the current composited canvas only

Use cases:

- scrolling
- panning
- credits movement
- large shifts where most pixels move intact

## Keyframe Strategy
Keyframes are full frames inserted periodically to avoid long dependency chains.

Recommended default:

- force first intro frame to be full
- force first main frame to be full
- force a full keyframe every `N` frames, where initial default should likely be between `24` and `60`

Encoder decision:

- choose interval based on playback robustness and real file-size impact
- shorter interval improves startup/recovery
- longer interval improves size but makes mid-stream reconstruction chains longer

Recommended initial default:

- `30` frames for general use

The creator can expose this as an advanced setting, with a safe default and a tooltip explaining the tradeoff.

## Image Encoding Rules

### Mandatory output normalization
All image payloads written into AFMA archives must be normalized by the creator to:

- 8-bit PNG
- RGBA if transparency is needed
- RGB if transparency is guaranteed absent and the implementation wants to optimize for that
- non-interlaced
- straight alpha, never premultiplied alpha

This is mandatory, not optional, because current performance analysis already showed that 16-bit PNG frames can severely hurt playback.

### Input handling
The creator must accept input frames that are:

- 8-bit PNG
- 16-bit PNG
- PNGs with alpha
- PNGs without alpha
- PNGs carrying ICC/gamma/sRGB-related metadata

Internally, the creator should normalize them to one working 8-bit RGBA representation before comparison and encoding decisions.

Normalization rules should explicitly include:

- compare frames only after they are normalized into one canonical straight-RGBA 8-bit byte representation
- strip or ignore input color-management metadata for AFMA payload generation so hidden transforms do not affect diff detection
- preserve visible pixel results as loaded by the creator, but do not carry source metadata that could cause cross-system decode differences

### Recommended payload policy
For simplicity and predictability, AFMA v1 should likely write all patch/full PNGs as 8-bit RGBA.

Possible later optimization:

- allow RGB payloads for frames/patches known to have no alpha

That should be treated as optional refinement, not day-one complexity.

## Decoder/Runtime Architecture

### Current FMA behavior to preserve conceptually
The current FMA runtime is already optimized around streaming:

- low frame prefetch count
- one decoded frame prepared ahead
- one persistent `DynamicTexture`
- frame upload on demand

AFMA should preserve this streaming philosophy.

### AFMA decode model
At runtime, AFMA should keep only:

- one persistent composited canvas in memory
- one temporary decoded payload image for the current incoming frame, if needed
- one small prefetch queue of frame operations and payload descriptors
- one persistent `DynamicTexture`

This keeps memory usage close to current FMA playback.

### Playback pipeline
For each frame:

1. read the next frame descriptor from `frame_index.json`
2. if `same`, do not decode image data
3. if `full`, decode the PNG and replace the canvas
4. if `delta_rect`, decode the patch PNG and copy it into the canvas
5. if `copy_rect_patch`, apply the copy on the canvas, then decode/apply the optional patch
6. upload the changed result to the GPU

### GPU upload strategy
AFMA should not blindly keep doing only full-texture uploads.

Recommended behavior:

- `full`: full texture upload
- `same`: no upload
- `delta_rect`: partial texture upload of the dirty rect
- `copy_rect_patch`: upload the union dirty region, or full texture if that is simpler and not worse for that case

This is important because AFMA should improve both archive size and runtime cost where possible.

Implementation note:

- AFMA will need its own upload path instead of relying only on the current FMA-style `DynamicTexture.upload()` full-texture flow
- use the existing `NativeImage.upload(...)` partial-region capabilities directly for dirty-rect uploads
- full upload should remain the fallback when a partial upload would be more complex than beneficial for a given frame

### Dirty region tracking
Decoder should track the affected rectangle for each frame operation:

- `same`: none
- `full`: full canvas
- `delta_rect`: patch rect
- `copy_rect_patch`: union of copied target rect and patch rect

This dirty rectangle should drive the GL upload path.

## Memory Consumption Strategy

### Hard constraints
AFMA must not require caching many prior decoded frames.

Specifically, AFMA v1 should avoid:

- arbitrary backward frame references
- multiple reference frames
- tile atlas caches
- retained decoded keyframe caches

### Allowed retained state
Allowed retained state should be:

- current composited canvas
- current `DynamicTexture`
- current frame operation metadata
- one current decoded patch/full payload image
- small prefetch queue, similar to current FMA

### Why this is safe
This keeps AFMA aligned with the current low-memory FMA architecture while still allowing substantial file-size improvements.

## Encoder Architecture

### External dependency assumption
Assume `ffmpeg` binaries are managed through the existing FancyMenu downloader flow.

AFMA should require no additional external native tools for v1.

### Existing FFmpeg integration
Use the existing downloader/runtime integration as the source of truth for FFmpeg availability:

- `FFMPEGDownloader`
- `FFMPEGDownloaderScreen`
- `FFMPEGDownloaderScreenResult`

AFMA Creator should not require manual FFmpeg path selection in v1 if FancyMenu-managed FFmpeg is the supported path.

When AFMA Creator needs FFmpeg, it must resolve the installation through `FFMPEGDownloader` and use the installed binary paths from that installation.

### Creator entry FFmpeg gate
Opening AFMA Creator must perform an FFmpeg availability check first.

Required behavior:

- when creator is opened, check whether a valid FFmpeg installation is already available through `FFMPEGDownloader`
- if FFmpeg is already installed, continue into AFMA Creator immediately
- if FFmpeg is not installed yet, open `FFMPEGDownloaderScreen`
- downloader flow should auto-start download as it already supports
- after downloader screen closes, only continue into creator if the result is ready
- if result is `INSTALLED` or `ALREADY_AVAILABLE`, continue into AFMA Creator
- if result is `FAILED`, do not open creator; keep the user on a clear recovery path with retry/reopen support
- if result is `CANCELLED`, do not open creator; return cleanly to the previous screen without entering a half-initialized creator session
- if result is `NOT_STARTED` or otherwise unresolved, treat creator launch as aborted

The creator must never continue into FFmpeg-dependent workflows if the downloader result is not ready.

### `ffmpeg` responsibility
`ffmpeg` is used only for source ingestion and preprocessing, not for AFMA-specific decision making.

Use cases:

- decode video input to image sequence
- normalize FPS if importing from video in the future
- transcode or flatten unsupported image inputs if needed
- optionally extract thumbnails/previews
- provide probe/introspection for future import validation where useful

### FancyMenu encoder responsibility
FancyMenu must implement AFMA-specific encoding logic itself:

- frame loading
- 16-bit to 8-bit normalization
- pixel comparison
- keyframe insertion
- same-frame detection
- dirty-rect extraction
- rect-copy detection
- patch/full decision logic
- PNG output writing
- archive packing
- metadata writing

## Encoding Decision Pipeline

### Step 1: Gather and validate input
The creator should accept:

- main frame sequence as ordered full-frame PNGs
- optional intro frame sequence as ordered full-frame PNGs
- frame time
- optional per-frame custom delays
- loop count
- optional advanced settings

Validation:

- all frames in a sequence must share the same dimensions
- intro and main should match canvas dimensions
- frame counts must be non-zero where required
- custom frame times must reference valid indices

### Step 2: Normalize input frames
For every source frame:

- load source image
- convert to a normalized in-memory 8-bit RGBA representation
- strip any 16-bit precision
- strip interlacing concerns from output representation

This removes the earlier 16-bit PNG playback issue from AFMA-generated files by construction.

### Step 3: Compare against previous composited frame
For each next frame, compare it with the previous composited result and compute:

- identical or not
- minimal changed bounding box
- whether a useful rect-copy exists
- whether a full frame is preferable

### Step 4: Candidate generation
Generate candidate representations:

- `same`
- `delta_rect`
- `copy_rect_patch`
- `full`

### Step 5: Candidate scoring
Choose the best representation using a scoring model weighted toward:

1. smallest stored payload
2. lowest decode cost
3. low runtime memory complexity

Example scoring factors:

- estimated PNG output bytes
- estimated decode cost category
- patch area ratio vs full-frame area ratio
- rect-copy usefulness ratio
- whether keyframe is due

Final selection rule:

- after heuristic filtering narrows the candidate set, the encoder should perform real trial encodes for the shortlisted candidates using the exact production PNG settings
- final candidate choice should be based on actual compressed payload bytes, with decode cost and simplicity used as tie-breakers
- this is preferred because encode time is a lower priority than final AFMA size and correct representation choice

### Step 6: Keyframe enforcement
Regardless of candidate size, force `full` when:

- it is the first frame of intro/main
- keyframe interval has been reached
- dependency chain would get too long
- candidate logic fails or becomes ambiguous

### Step 7: Write payloads
Write only the payloads actually needed by chosen frame operations:

- full-frame PNGs
- delta patch PNGs
- copy-patch PNGs where needed

### Step 8: Write metadata and pack archive
After operation decisions are finalized:

- write `metadata.json`
- write `frame_index.json`
- write payload PNGs
- pack final `.afma` archive

## AFMA Optimization Heuristics

### `same` detection
Rule:

- if every pixel matches previous composited frame, emit `same`

This should have highest priority because it is the best outcome for size and decode speed.

### `delta_rect` selection
Rule:

- compute the minimal changed bounding box
- if changed area is small enough relative to full frame, prefer `delta_rect`

Suggested initial thresholds:

- if changed bounding box area is below a configurable ratio, prefer delta
- also factor in estimated PNG output size, not only area

### `copy_rect_patch` selection
Rule:

- try rect-copy only when movement between frames appears global/simple enough to detect cheaply

Good initial scope:

- detect a single translation offset between previous and next frame
- if a dominant shift is found, emit one copy rect plus one optional patch

Do not attempt:

- multiple copy commands per frame
- small object motion estimation
- complex overlapping copy graphs

### `full` fallback
Rule:

- choose `full` whenever alternative representations are not clearly better

This keeps decode logic predictable and avoids pathological representations.

## Rect-Copy Detection Strategy

### AFMA v1 scope
Rect-copy should be intentionally constrained.

Recommended v1 approach:

- detect only one major translation per frame
- verify that copying a rectangle from previous composited frame reconstructs most of the next frame
- patch the remaining exposed pixels with one patch PNG

Examples where this works well:

- vertical scroll
- horizontal scroll
- slow panning backgrounds
- credits movement

Detection approaches that are realistic:

- compare shifted overlap regions for a set of candidate offsets
- use heuristics based on previous successful offsets to reduce search cost
- keep offset search window bounded

The encoder can spend more CPU here because encode time is not a primary concern.

## AFMA Creator

### Creator goal
The AFMA Creator is a full in-game creation tool inside FancyMenu for producing `.afma` files from ordered image sequences.

### Required creator features

#### Input sources
- choose main animation frame directory or ordered frame list
- choose optional intro animation frame directory or ordered frame list
- support drag-and-drop / file chooser workflows if available in FancyMenu UI systems
- validate image ordering and naming

#### Timing controls
- default frame time for main frames
- default frame time for intro frames
- per-frame custom timing for main frames
- per-frame custom timing for intro frames
- preview-friendly display of effective delays

#### Looping controls
- loop count
- `<= 0` means infinite loop, matching current FMA behavior

#### Encoding controls
- output file path / save target
- keyframe interval
- optimization mode preset, with default set for best balanced output
- optional transparency handling info
- optional creator-generated thumbnail toggle
- FFmpeg availability / installation status indicator

#### Preview controls
- playback preview before export
- preview of intro -> main transition
- preview with effective custom frame timings
- estimated output summary before final save
- timeline scrubbing / frame stepping
- preview seeking that reconstructs from the nearest prior full keyframe rather than always replaying from frame `0`

#### Diagnostics
- estimated frame count
- canvas resolution
- alpha usage summary
- count of `full`, `delta_rect`, `same`, and `copy_rect_patch` frames after analysis
- estimated output size before final packing if feasible
- current FFmpeg installation source/path summary when FFmpeg-backed features are in use

#### Safety and validation
- warn if frame dimensions mismatch
- warn if input ordering is ambiguous
- warn if custom timing references invalid frames
- warn if output path would overwrite existing file
- warn when no useful optimization could be applied

#### Job execution model
- creator analysis and export must run as cancellable background jobs, never on the main client/render thread
- show progress for normalization, analysis, PNG writing, and final archive packing
- support user cancellation during long-running analysis/export
- clean up temporary files and partial outputs on cancellation or failure
- prevent concurrent exports for the same creator session unless explicitly supported later
- FFmpeg subprocess work must also run only inside these background jobs, never on the render thread
- any temporary FFmpeg extraction/intermediate files must be cleaned up on completion, cancellation, or failure
- creator job state must clearly distinguish FancyMenu analysis/export failure from FFmpeg invocation failure

### Optional creator features for later
- import from video through `ffmpeg`
- batch conversion of current FMA archives to AFMA
- compare original full-frame size vs optimized AFMA size
- manual forcing of keyframes at selected indices

## AFMA Creator UX Structure

### Recommended flow
1. attempt to open creator
2. check FFmpeg installation via `FFMPEGDownloader`
3. if missing, open `FFMPEGDownloaderScreen` and wait for a ready result
4. choose source frames
5. configure intro/main timing and loop settings
6. configure advanced encoding settings
7. run background analysis pass
8. review optimization summary
9. preview result
10. export AFMA

### Creator screen sections

#### Section 1: Source
- main frames input
- intro frames input
- frame ordering controls
- source validation status
- FFmpeg readiness status for workflows that depend on it

#### Section 2: Playback
- main frame time
- intro frame time
- per-frame custom timing editor
- loop count

#### Section 3: Optimization
- keyframe interval
- rect-copy enabled toggle
- duplicate-frame elision toggle
- optimization preset selector

#### Section 4: Output
- output file path
- overwrite behavior
- thumbnail generation

#### Section 5: Analysis/Preview
- operation distribution summary
- estimated size
- timeline preview
- seek/frame-step controls
- export action

## Suggested Optimization Presets

### Balanced
Default preset.

Goals:

- best overall compromise of file size, decode time, and simplicity

Suggested behavior:

- normal keyframe interval
- `same` enabled
- `delta_rect` enabled
- `copy_rect_patch` enabled

### Smallest File
Goals:

- minimize final archive size more aggressively

Suggested behavior:

- longer keyframe interval
- more willingness to choose delta/copy forms
- still no format changes that hurt memory model

### Fastest Decode
Goals:

- reduce runtime work further

Suggested behavior:

- shorter keyframe interval
- less aggressive rect-copy use if it does not materially help
- prefer `full` over marginally better delta forms

The creator should still expose advanced manual settings beyond presets.

## AFMA Runtime Support Plan

### New file type registration
Add a new file type for `.afma` alongside existing `.fma`.

Do not replace FMA support.

### New decoder and texture classes
Recommended new classes:

- `AfmaDecoder`
- `AfmaTexture`
- AFMA-specific frame descriptor/data classes

The old FMA classes should be used as conceptual structure, not overloaded with AFMA complexity if that would make them harder to maintain.

### Shared utilities
Some utilities can be shared between FMA and AFMA:

- archive spooling/indexing helpers
- metadata parsing helpers
- PNG header inspection / normalization helpers
- path normalization helpers

### Decoder startup behavior
When loading AFMA:

- read metadata
- read frame index
- validate first frame
- initialize composited canvas size
- initialize streaming state
- begin normal streamed playback

Preview/seek behavior should follow the same reconstruction rules:

- sequential playback continues normally from the current composited canvas
- random access for preview/debugging should restart from the nearest prior full keyframe
- AFMA v1 should not require retaining historical decoded keyframes in memory just to support seeking

## Backward Compatibility and Migration

### Existing FMA support
Keep current FMA support intact.

AFMA is additive, not a replacement-only change.

### Migration path
Potential migration tools for later:

- FMA -> AFMA converter in creator
- AFMA export from existing PNG frame folders
- AFMA import from future video source pipeline

### Interoperability
Creator should allow users to create AFMA from the exact frame sequences they previously used for hand-packed FMA archives.

## Error Handling

### Creator-time errors
- invalid source frame ordering
- missing frames
- mismatched dimensions
- invalid custom frame times
- save path errors
- archive write failure
- FFmpeg not installed / installation unavailable when creator is opened
- FFmpeg download failed before creator launch
- FFmpeg download cancelled before creator launch
- `ffmpeg` process failure for FFmpeg-backed creator workflows
- invalid or missing FFmpeg binary path returned from downloader installation
- FFmpeg subprocess timeout or abnormal exit

### Runtime AFMA load errors
- missing metadata/index
- invalid frame op descriptors
- missing referenced payload files
- out-of-bounds patch/copy coordinates
- invalid first frame not being reconstructible

### Failure policy
Creator:

- fail early and explain exactly what is invalid
- if creator launch was blocked by FFmpeg download failure/cancellation, exit cleanly without opening a broken creator state
- provide a direct retry/reopen path into `FFMPEGDownloaderScreen` when launch was blocked by missing FFmpeg

Runtime:

- log clear AFMA-specific errors
- fail gracefully instead of crashing the game

## Testing and Validation Plan

### Functional tests
- intro-only AFMA
- main-only AFMA
- intro + main AFMA
- custom frame timing on intro and main
- finite loop count
- infinite loop count
- `same` frames
- `delta_rect` frames
- `copy_rect_patch` frames
- mixed-operation timeline

### Performance tests
- compare AFMA vs equivalent full-frame FMA on:
  - archive size
  - startup time
  - steady-state playback smoothness
  - RAM usage
  - GC behavior

### Regression tests
- transparency preservation
- straight-alpha preservation after normalization
- overlapping `copy_rect_patch` correctness
- first-frame correctness
- intro -> main transition correctness
- loop restart correctness
- custom timing correctness
- partial upload correctness
- preview seek correctness from nearest keyframe
- normalized-input equivalence for 8-bit vs 16-bit source PNGs

## Implementation Phases

### Phase 1: AFMA spec and runtime core
- define final JSON schema
- implement AFMA archive reader
- implement frame op descriptors
- implement composited canvas logic
- implement `full`, `delta_rect`, `same`
- implement full and partial GPU upload paths

### Phase 2: rect-copy support
- implement `copy_rect_patch`
- implement dirty-region union logic
- validate scrolling/panning cases

### Phase 3: creator core
- implement source frame intake
- implement timing and loop configuration
- implement frame normalization to 8-bit RGBA
- implement candidate analysis and scoring
- implement trial-encode validation for shortlisted candidates
- implement AFMA archive writer
- implement background job execution, progress reporting, cancellation, and temp cleanup

### Phase 4: creator UX and preview
- build creator UI (This should be a normal Vanilla Minecraft `Screen`, similar to the `LayoutEditorScreen`, NOT a PipWindow, Dialog, etc.!)
- add analysis summary
- add preview playback
- add preview seek/frame-step behavior from nearest prior keyframe
- add output validation and overwrite handling

### Phase 5: refinement and profiling
- profile decode time
- tune keyframe interval defaults
- tune rect-copy heuristics
- tune delta thresholds
- tune partial upload behavior

## Recommended Class/Subsystem Breakdown

### Runtime
- `AfmaDecoder`
- `AfmaTexture`
- `AfmaMetadata`
- `AfmaFrameIndex`
- `AfmaFrameDescriptor`
- `AfmaFrameOperationType`
- `AfmaPatchRegion`
- `AfmaCopyRect`

### Creator
- `AfmaCreatorEntryGate` or equivalent launch coordinator that checks `FFMPEGDownloader` before opening the creator
- `AfmaCreatorScreen`
- `AfmaCreatorState`
- `AfmaFfmpegBridge` or equivalent wrapper that resolves the installed FFmpeg/FFprobe binaries from `FFMPEGDownloader`
- `AfmaFfmpegProcessRunner` or equivalent terminal/process bridge for invoking FFmpeg in background jobs
- `AfmaSourceSequence`
- `AfmaEncodeOptions`
- `AfmaEncodeAnalyzer`
- `AfmaEncodePlanner`
- `AfmaEncodeJob`
- `AfmaEncodeProgress`
- `AfmaArchiveWriter`
- `AfmaFrameNormalizer`
- `AfmaRectCopyDetector`
- `AfmaPreviewController`

### Shared helpers
- PNG normalization/writer helpers
- archive I/O helpers
- frame ordering utilities
- timing validation utilities

## Important Design Rules

### Rule 1
AFMA v1 must stay single-reference and streaming-friendly.

### Rule 2
Every AFMA-created payload image must be 8-bit PNG to eliminate 16-bit playback regressions.

### Rule 3
The first frame of every independently decodable section must be a full frame.

### Rule 4
`same` should always beat every other representation if exact.

### Rule 5
Rect-copy should remain intentionally limited to one simple copy operation per frame in AFMA v1.

### Rule 6
When in doubt, the encoder should prefer the simpler representation unless the more complex one gives a clear file-size win.

### Rule 7
All AFMA comparison and diff logic must operate on one canonical normalized straight-RGBA 8-bit representation.

### Rule 8
`copy_rect_patch` behavior must be defined as overlap-safe, deterministic copy-then-patch semantics.

### Rule 9
The creator must always use background jobs with progress/cancellation for analysis and export.

### Rule 10
AFMA Creator launch must be gated on a ready FFmpeg installation from `FFMPEGDownloader`; failed or cancelled download flows must never leave the creator partially opened.

## Final Recommendation
AFMA v1 should be implemented as a streamed ZIP+PNG animation format with:

- full keyframes
- single-rectangle delta patches
- duplicate-frame reuse
- limited rect-copy + patch frames
- mandatory 8-bit PNG normalization
- periodic keyframes
- partial GPU uploads where beneficial

The creator should be a first-class in-game FancyMenu tool for building AFMA files from intro/main full-frame PNG sequences with:

- default and per-frame timing controls
- loop configuration
- optimization settings
- analysis summary
- preview
- deterministic export

This design gives AFMA the best practical balance of:

- smaller files than current FMA
- better runtime decode characteristics than naive full-frame storage
- memory usage still close to the current streamed FMA path
- a realistic implementation scope for FancyMenu
