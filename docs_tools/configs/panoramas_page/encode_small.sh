#!/bin/sh
# Encodes composed GIF frames at the 800 px the docs show them at, for GIFs where most of the screen changes in every frame,
# like a turning panorama. An optional ffmpeg select expression keeps only some frames (the rest of the time shows the kept
# frame longer), to show turning parts at 5 fps instead of 10.
# Usage: sh encode_small.sh <composed frames dir> <output gif> [gifsicle lossy, default 80] [select expression, default all]
# The Cubic Panorama GIF of the Panoramas page (clicks at frames 101 and 162 of its recording):
#   sh encode_small.sh <composed> panoramas_background.gif 100 \
#     'lte(n\,105)+between(n\,106\,161)*not(mod(n\,2))+between(n\,162\,189)*not(mod(n-162\,3))'
# The turning Title screen of the Panoramas page (4 s at 5 fps, it jumps back on restart):
#   sh encode_small.sh <composed> panoramas_title.gif 100 'between(n\,10\,49)*not(mod(n\,2))'
set -e
out="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
lossy="${3:-80}"
sel="${4:-1}"
cd "$1"
ffmpeg -loglevel error -y -framerate 10 -i c_%05d.png -vf "select='$sel',scale=800:-1:flags=lanczos,palettegen=max_colors=256:stats_mode=diff" palette.png
ffmpeg -loglevel error -y -framerate 10 -i c_%05d.png -i palette.png \
    -lavfi "[0:v]select='$sel',scale=800:-1:flags=lanczos[x];[x][1:v]paletteuse=dither=none:diff_mode=rectangle" -fps_mode vfr -loop 0 raw.gif
gifsicle -O3 --lossy="$lossy" raw.gif -o "$out"
# ffmpeg gives the last kept frame only 0.1 s: give it the delay of the frame before it
count=$(gifsicle -I "$out" | grep -c "+ image")
if [ "$count" -gt 1 ]; then
    delay=$(gifsicle -I "$out" | grep -o "delay [0-9.]*s" | tail -2 | head -1 | sed 's/delay //; s/s//')
    gifsicle -b "$out" "#0-$((count - 2))" -d"$(python3 -c "print(round($delay * 100))")" "#$((count - 1))"
fi
ls -la "$out"
