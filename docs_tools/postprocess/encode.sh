#!/bin/sh
# Usage: sh encode.sh <composed frames dir> <output gif>
set -e
# The output can be relative to where this got called from, so it gets resolved before going into the frames dir
out="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
cd "$1"
ffmpeg -loglevel error -y -framerate 10 -i c_%05d.png -vf "palettegen=max_colors=256:stats_mode=diff:reserve_transparent=1" palette.png
ffmpeg -loglevel error -y -framerate 10 -i c_%05d.png -i palette.png -lavfi "paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle" -loop 0 raw.gif
gifsicle -O3 --lossy=20 raw.gif -o "$out"
ls -la "$out"
