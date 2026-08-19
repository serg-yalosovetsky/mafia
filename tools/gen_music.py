"""Процедурная генерация саундтрека для приложения «Мафия».

Всё синтезируется из синусов/шума numpy — никакого стороннего аудио,
значит никаких вопросов по лицензиям и крошечный размер после ogg.

Запуск:  python tools/gen_music.py
Выход:   app/src/main/res/raw/*.ogg
"""

import os
import subprocess
import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(20260819)


def t(dur):
    return np.linspace(0, dur, int(SR * dur), endpoint=False)


def sine(freq, dur, phase=0.0):
    return np.sin(2 * np.pi * freq * t(dur) + phase)


def saw(freq, dur, harmonics=8):
    out = np.zeros(int(SR * dur))
    for h in range(1, harmonics + 1):
        out += np.sin(2 * np.pi * freq * h * t(dur)) / h
    return out / np.max(np.abs(out) + 1e-9)


def env(dur, a=0.01, d=0.2, s=0.6, r=0.3):
    n = int(SR * dur)
    na, nd, nr = int(SR * a), int(SR * d), int(SR * r)
    ns = max(0, n - na - nd - nr)
    return np.concatenate(
        [
            np.linspace(0, 1, na),
            np.linspace(1, s, nd),
            np.full(ns, s),
            np.linspace(s, 0, nr),
        ]
    )[:n]


def place(buf, sig, at):
    """Подмешать sig в buf начиная с секунды at (с заворотом для лупа)."""
    n = len(buf)
    i = int(at * SR) % n
    sig = sig[:n]
    end = i + len(sig)
    if end <= n:
        buf[i:end] += sig
    else:
        head = n - i
        buf[i:] += sig[:head]
        buf[: len(sig) - head] += sig[head:]  # заворот — луп остаётся бесшовным
    return buf


def reverb(
    x, taps=((0.037, 0.35), (0.071, 0.26), (0.113, 0.19), (0.191, 0.12), (0.311, 0.08))
):
    out = x.copy()
    for delay, gain in taps:
        d = int(delay * SR)
        out[d:] += x[:-d] * gain
    return out


def lowpass(x, cutoff=1800.0):
    """Однополюсный RC-фильтр — дешёвый способ убрать песок из шума."""
    rc = 1.0 / (2 * np.pi * cutoff)
    alpha = (1.0 / SR) / (rc + 1.0 / SR)
    out = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += alpha * (x[i] - acc)
        out[i] = acc
    return out


def lowpass_fast(x, cutoff=1800.0):
    """То же, но через частотную область — на длинных буферах на порядки быстрее."""
    n = len(x)
    spec = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(n, 1.0 / SR)
    spec *= 1.0 / (1.0 + (freqs / cutoff) ** 2) ** 0.5
    return np.fft.irfft(spec, n)


def highpass_fast(x, cutoff=200.0):
    n = len(x)
    spec = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(n, 1.0 / SR)
    ratio = freqs / cutoff
    spec *= ratio / (1.0 + ratio**2) ** 0.5
    return np.fft.irfft(spec, n)


def seamless(x, fade=1.5):
    """Кроссфейд хвоста в начало: луп без щелчка на стыке."""
    nf = int(fade * SR)
    head = x[:nf].copy()
    tail = x[-nf:].copy()
    ramp = np.linspace(0, 1, nf)
    x = x[:-nf]
    x[:nf] = head * ramp + tail * (1 - ramp)
    return x


def normalize(x, peak=0.85):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def save(name, x, quality=3):
    x = normalize(x)
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    wav = os.path.join(OUT, name + ".wav")
    ogg = os.path.join(OUT, name + ".ogg")
    import wave

    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(
        [
            "ffmpeg",
            "-y",
            "-loglevel",
            "error",
            "-i",
            wav,
            "-c:a",
            "libvorbis",
            "-q:a",
            str(quality),
            "-ar",
            "44100",
            ogg,
        ],
        check=True,
    )
    os.remove(wav)
    print(f"  {name}.ogg  {os.path.getsize(ogg) / 1024:.0f} KB  ({len(x) / SR:.0f}s)")


# ---------------------------------------------------------------- НОЧЬ
def night(dur=64.0):
    """Тёмный дрон, дыхание баса, редкие капли и удары сердца."""
    n = int(SR * dur)
    x = np.zeros(n)
    base = 55.0  # A1
    for freq, amp, det in (
        (base, 0.5, 0.0),
        (base * 1.5, 0.22, 0.3),
        (base * 2, 0.16, -0.4),
        (base * 3, 0.07, 0.7),
    ):
        lfo = 1 + 0.18 * np.sin(2 * np.pi * (0.05 + det * 0.01) * t(dur))
        x += amp * np.sin(2 * np.pi * freq * t(dur) + det) * lfo

    # шелест ветра
    wind = lowpass_fast(rng.normal(0, 1, n), 420)
    wind *= 0.12 * (1 + 0.6 * np.sin(2 * np.pi * 0.035 * t(dur)))
    x += wind

    # сердцебиение, ~ раз в 2.4 с
    beat_t = np.arange(0.6, dur, 2.4)
    for bt in beat_t:
        for off, amp in ((0.0, 0.55), (0.22, 0.34)):
            d = 0.28
            thump = np.sin(
                2
                * np.pi
                * np.linspace(62, 38, int(SR * d))
                * np.linspace(0, d, int(SR * d))
            )
            thump *= np.exp(-np.linspace(0, 9, int(SR * d))) * amp
            x = place(x, thump, bt + off)

    # редкие «капли» — минорная пентатоника поверх A
    scale = [440.0, 523.25, 587.33, 659.25, 783.99]
    for k in range(int(dur / 3.1)):
        at = 1.7 + k * 3.1 + rng.uniform(-0.4, 0.4)
        f = scale[rng.integers(0, len(scale))] * (0.5 if rng.random() < 0.4 else 1.0)
        d = 2.4
        drop = (
            np.sin(2 * np.pi * f * t(d))
            * np.exp(-np.linspace(0, 6, int(SR * d)))
            * 0.16
        )
        x = place(x, reverb(drop), at)

    return seamless(x)


# ---------------------------------------------------------------- ДЕНЬ
def day(dur=60.0):
    """Дневное обсуждение: спокойный пульсирующий пэд Am–F–C–G."""
    n = int(SR * dur)
    x = np.zeros(n)
    chords = [
        [220.00, 261.63, 329.63],  # Am
        [174.61, 220.00, 261.63],  # F
        [261.63, 329.63, 392.00],  # C
        [196.00, 246.94, 293.66],  # G
    ]
    bar = 4.0
    for i in range(int(dur / bar) + 1):
        ch = chords[i % len(chords)]
        for f in ch:
            d = bar * 1.4
            v = saw(f, d, 6) * env(d, a=0.6, d=1.0, s=0.5, r=1.6) * 0.16
            v += (
                np.sin(2 * np.pi * f * 0.5 * t(d))
                * env(d, a=0.7, d=1.0, s=0.45, r=1.6)
                * 0.10
            )
            x = place(x, v, i * bar)
        # мягкий пульс на долю
        for b in range(4):
            d = 0.5
            p = np.sin(
                2
                * np.pi
                * np.linspace(120, 60, int(SR * d))
                * np.linspace(0, d, int(SR * d))
            )
            p *= np.exp(-np.linspace(0, 14, int(SR * d))) * (0.22 if b == 0 else 0.10)
            x = place(x, p, i * bar + b)

    x = lowpass_fast(x, 3200)
    return seamless(x)


# ---------------------------------------------------------------- ГОЛОСОВАНИЕ
def vote(dur=48.0):
    """Тиканье часов и нарастающее напряжение — фон голосования."""
    n = int(SR * dur)
    x = np.zeros(n)
    drone = np.sin(2 * np.pi * 73.42 * t(dur)) * 0.34  # D2
    drone += np.sin(2 * np.pi * 110.0 * t(dur)) * 0.18
    drone += np.sin(2 * np.pi * 155.56 * t(dur)) * 0.10  # тритон — беспокойство
    x += drone * (0.6 + 0.4 * np.linspace(0, 1, n))

    for i in range(int(dur)):
        d = 0.12
        tick = highpass_fast(rng.normal(0, 1, int(SR * d)), 2600)
        tick *= np.exp(-np.linspace(0, 30, int(SR * d))) * (
            0.26 if i % 2 == 0 else 0.18
        )
        x = place(x, tick, float(i))

    for i in range(int(dur / 8)):
        d = 1.6
        swell = (
            np.sin(2 * np.pi * 146.83 * t(d))
            * np.sin(np.linspace(0, np.pi, int(SR * d)))
            * 0.22
        )
        x = place(x, reverb(swell), i * 8.0 + 6.0)

    return seamless(x)


# ---------------------------------------------------------------- СИГНАЛЫ
def gong(dur=3.4):
    """Город засыпает."""
    x = np.zeros(int(SR * dur))
    for f, a in ((98.0, 1.0), (146.8, 0.5), (233.1, 0.3), (392.0, 0.16), (587.3, 0.08)):
        x += (
            np.sin(2 * np.pi * f * t(dur))
            * np.exp(-np.linspace(0, 5 + f / 200, int(SR * dur)))
            * a
        )
    return reverb(x)


def morning(dur=2.6):
    """Город просыпается — восходящий аккорд."""
    x = np.zeros(int(SR * dur))
    for i, f in enumerate((261.63, 329.63, 392.00, 523.25)):
        d = dur - i * 0.12
        v = (
            np.sin(2 * np.pi * f * t(d))
            * np.exp(-np.linspace(0, 4, int(SR * d)))
            * (0.5 - i * 0.06)
        )
        v += (
            np.sin(2 * np.pi * f * 2 * t(d))
            * np.exp(-np.linspace(0, 7, int(SR * d)))
            * 0.12
        )
        x = place(x, v, i * 0.12)
    return reverb(x)


def timeup(dur=1.4):
    """Время речи вышло."""
    x = np.zeros(int(SR * dur))
    for i in range(3):
        d = 0.22
        v = (
            np.sin(2 * np.pi * 880.0 * t(d))
            * env(d, a=0.005, d=0.05, s=0.3, r=0.15)
            * 0.6
        )
        x = place(x, v, i * 0.24)
    return reverb(x)


def shot(dur=2.2):
    """Ночной выстрел — объявление жертвы."""
    n = int(SR * dur)
    x = highpass_fast(rng.normal(0, 1, n), 300) * np.exp(-np.linspace(0, 16, n)) * 0.9
    x += (
        np.sin(2 * np.pi * np.linspace(90, 40, n) * t(dur))
        * np.exp(-np.linspace(0, 10, n))
        * 0.6
    )
    return reverb(x)


if __name__ == "__main__":
    print("Генерирую саундтрек:")
    save("music_night", night())
    save("music_day", day())
    save("music_vote", vote())
    save("sfx_gong", gong(), quality=2)
    save("sfx_morning", morning(), quality=2)
    save("sfx_timeup", timeup(), quality=2)
    save("sfx_shot", shot(), quality=2)
    print("Готово.")
