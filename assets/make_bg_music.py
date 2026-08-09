#!/usr/bin/env python3
"""Procedurally compose the Car Jam background loop -> app/src/main/res/raw/bg_music.wav

Cheerful casual-puzzle groove: soft pads (C-Am-F-G), round bass, a plucky
pentatonic marimba melody and feather percussion. 24s at 22050 Hz mono 16-bit,
with a tiny end->start crossfade so MediaPlayer's isLooping seams perfectly.
"""
import math
import wave
import struct
import random

import numpy as np

SR = 22050
BPM = 120
BEAT = 60.0 / BPM          # 0.5 s
BAR = BEAT * 4             # 2.0 s
BARS = 12                  # 3 chord loops -> 24 s
DUR = BARS * BAR
N = int(SR * DUR)

t = np.arange(N) / SR
mix = np.zeros(N, dtype=np.float64)

# ---------------------------------------------------------------- chords ----
CHORDS = [  # (pad freqs, bass root)
    ([261.63, 329.63, 392.00], 130.81),   # C
    ([220.00, 261.63, 329.63], 110.00),   # Am
    ([174.61, 220.00, 261.63],  87.31),   # F
    ([196.00, 246.94, 293.66],  98.00),   # G
]

def add(start, dur, sig):
    i0 = int(start * SR)
    i1 = min(N, i0 + len(sig))
    sig = sig[: i1 - i0]
    if i0 < N:
        mix[i0:i1] += sig

# pads: gentle triangle-ish stack, slow swell per bar
for bar in range(BARS):
    chord = CHORDS[bar % 4][0]
    tt = np.arange(int(BAR * SR)) / SR
    env = np.minimum(tt / 0.5, 1.0) * np.minimum((BAR - tt) / 0.6, 1.0)
    env = np.clip(env, 0.0, 1.0) ** 1.5
    pad = np.zeros_like(tt)
    for f in chord:
        pad += (np.sin(2 * np.pi * f * tt)
                + 0.35 * np.sin(2 * np.pi * f * 2 * tt)
                + 0.12 * np.sin(2 * np.pi * f * 3 * tt))
        pad += 0.5 * np.sin(2 * np.pi * (f * 1.003) * tt)  # soft detune chorus
    add(bar * BAR, BAR, pad * env * 0.014)

# bass: round sine w/ one partial, roots on beats 1 & 3.5 with an octave hop
for bar in range(BARS):
    root = CHORDS[bar % 4][1]

    def pluck_sine(f, dur):
        tt = np.arange(int(dur * SR)) / SR
        env = np.exp(-tt * 5.5)
        return (np.sin(2 * np.pi * f * tt) + 0.4 * np.sin(2 * np.pi * f * 2 * tt)) * env * 0.05
    add(bar * BAR, 0.45, pluck_sine(root, 0.45))
    add(bar * BAR + 1.5 * BEAT, 0.35, pluck_sine(root * 2.0, 0.35))
    add(bar * BAR + 2.0 * BEAT, 0.5, pluck_sine(root, 0.5))

# melody: plucky marimba on C pentatonic, seeded musical random walk, ends on C5
rng = random.Random(20260809)
SCALE = [523.25, 587.33, 659.25, 783.99, 880.00, 1046.50]  # C5 D5 E5 G5 A5 C6

def marimba(f, dur=0.30):
    tt = np.arange(int(dur * SR)) / SR
    env = np.exp(-tt * 11.0)
    sig = (np.sin(2 * np.pi * f * tt)
           + 0.5 * np.sin(2 * np.pi * f * 2 * tt) * np.exp(-tt * 18)
           + 0.22 * np.sin(2 * np.pi * f * 4 * tt) * np.exp(-tt * 26))
    return sig * env * 0.030

pos = 2  # start on E5-ish
for bar in range(BARS):
    for eighth in range(8):
        when = bar * BAR + eighth * (BEAT / 2)
        last_bar = bar == BARS - 1
        if last_bar and eighth >= 6:
            note = SCALE[0] if eighth == 6 else None  # resolve home to C5
        else:
            if rng.random() < 0.18:      # breath
                note = None
            else:
                step = rng.choice([-2, -1, -1, 1, 1, 2])
                pos = max(0, min(len(SCALE) - 1, pos + step))
                note = SCALE[pos]
        if note is not None:
            vel = 1.0 if eighth % 2 == 0 else 0.7
            add(when, 0.30, marimba(note) * vel)

# sparkle answer every 4th bar
for bar in (3, 7, 11):
    for k, f in enumerate((1046.50, 1318.51, 1567.98)):
        add(bar * BAR + 3.0 * BEAT + k * 0.08, 0.25, marimba(f, 0.25) * 0.5)

# percussion: feather kick on 1 & 3, shaker noise on 8ths
nrng = np.random.default_rng(7)
for bar in range(BARS):
    for beat in (0.0, 2.0):
        tt = np.arange(int(0.12 * SR)) / SR
        f0 = 150.0 * np.exp(-tt * 30) + 55.0
        kick = np.sin(2 * np.pi * f0 * tt) * np.exp(-tt * 26) * 0.05
        add(bar * BAR + beat * BEAT, 0.12, kick)
    for eighth in range(8):
        ln = int(0.05 * SR)
        noise = nrng.standard_normal(ln)
        env = np.exp(-np.arange(ln) / SR * 70)
        hat = noise * env * (0.006 if eighth % 2 else 0.010)
        add(bar * BAR + eighth * (BEAT / 2), 0.05, hat)

# ------------------------------------------------------------- finish ------
mix = np.tanh(mix * 1.6)                    # soft limiter / glue
mix *= 0.85 / max(1e-9, np.abs(mix).max())  # headroom

# seamless loop: crossfade last 60 ms with the head
X = int(0.06 * SR)
tail = mix[-2 * X:-X].copy()
head = mix[X:2 * X].copy()
r = np.linspace(0.0, 1.0, X)
mix[-2 * X:-X] = tail * (1 - r) + head * r  # blend loop-point region

pcm = (mix * 32767).astype(np.int16)
out = "app/src/main/res/raw/bg_music.wav"
import os
os.makedirs(os.path.dirname(out), exist_ok=True)
with wave.open(out, "wb") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print("wrote", out, f"{DUR:.1f}s", f"{N*2/1024:.0f} KiB")
