"""Портреты ролей для приложения «Мафия».

Рисуются кодом (Pillow) в едином нуар-стиле: тёмная виньетка, силуэт с контровым
светом и атрибут роли. Никаких сторонних картинок — значит ни лицензий, ни веса.

Запуск: python tools/gen_art.py
Выход:  app/src/main/res/drawable-nodpi/role_*.webp
"""

import math
import os
from PIL import Image, ImageDraw, ImageFilter

S = 512  # итоговый размер
SS = 2  # суперсэмплинг
W = S * SS
OUT = os.path.join(
    os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable-nodpi"
)
os.makedirs(OUT, exist_ok=True)


def px(v):
    return int(v * SS)


def radial_background(inner, outer):
    """Мягкое пятно света в центре, уходящее в темноту по краям."""
    img = Image.new("RGB", (W, W), outer)
    d = ImageDraw.Draw(img)
    steps = 90
    for i in range(steps, 0, -1):
        f = i / steps
        r = int(W * 0.62 * f)
        t = (1 - f) ** 1.6
        color = tuple(int(outer[c] + (inner[c] - outer[c]) * t) for c in range(3))
        d.ellipse(
            [W // 2 - r, int(W * 0.52) - r, W // 2 + r, int(W * 0.52) + r],
            fill=color,
        )
    return img


def vignette(img, strength=0.85):
    mask = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse([int(W * -0.05), int(W * -0.05), int(W * 1.05), int(W * 1.05)], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(W // 8))
    dark = Image.new("RGB", (W, W), (0, 0, 0))
    return Image.composite(img, Image.blend(img, dark, strength), mask)


def grain(img, amount=6):
    """Лёгкая плёночная зернистость — иначе градиенты полосят на AMOLED."""
    import random

    random.seed(7)
    noise = Image.effect_noise((W, W), 24).convert("L")
    noise = noise.point(lambda v: 128 + (v - 128) * amount // 24)
    return Image.blend(img, Image.merge("RGB", (noise, noise, noise)), 0.06)


def new_layer():
    return Image.new("RGBA", (W, W), (0, 0, 0, 0))


def body(layer, skin, coat, rim, wide=1.0):
    """Общая основа: плечи, шея, голова и контровый свет слева."""
    d = ImageDraw.Draw(layer)
    # контровый свет — тот же силуэт, сдвинутый и размытый
    glow = new_layer()
    gd = ImageDraw.Draw(glow)
    gd.ellipse([px(80 - 10), px(320), px(432 - 10), px(600)], fill=rim + (255,))
    gd.ellipse([px(176 - 12), px(132), px(336 - 12), px(300)], fill=rim + (255,))
    glow = glow.filter(ImageFilter.GaussianBlur(px(9)))
    layer.alpha_composite(glow)

    half = 168 * wide
    d.ellipse(
        [px(256 - half), px(322), px(256 + half), px(600)], fill=coat + (255,)
    )  # плечи
    d.rectangle([px(228), px(262), px(284), px(340)], fill=skin + (255,))  # шея
    d.ellipse([px(176), px(132), px(336), px(300)], fill=skin + (255,))  # голова
    return d


def face_shadow(layer, depth=90):
    """Глаза не рисуем — лицо в тени, так персонаж читается как силуэт."""
    sh = new_layer()
    d = ImageDraw.Draw(sh)
    d.ellipse([px(186), px(150), px(326), px(292)], fill=(0, 0, 0, depth))
    sh = sh.filter(ImageFilter.GaussianBlur(px(14)))
    layer.alpha_composite(sh)


def fedora(layer, crown, band):
    d = ImageDraw.Draw(layer)
    d.ellipse([px(120), px(146), px(392), px(206)], fill=crown + (255,))  # поля
    d.polygon(
        [(px(196), px(170)), (px(206), px(96)), (px(306), px(96)), (px(316), px(170))],
        fill=crown + (255,),
    )  # тулья
    d.ellipse([px(206), px(84), px(306), px(112)], fill=crown + (255,))
    d.rectangle([px(198), px(148), px(314), px(166)], fill=band + (255,))  # лента


def cap(layer, cloth, visor, star):
    d = ImageDraw.Draw(layer)
    d.ellipse([px(150), px(150), px(362), px(196)], fill=visor + (255,))  # козырёк
    d.pieslice([px(168), px(84), px(344), px(214)], 180, 360, fill=cloth + (255,))
    d.rectangle([px(168), px(146), px(344), px(164)], fill=(20, 20, 26, 255))
    cx, cy, r = px(256), px(120), px(26)  # звезда
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        rad = r if i % 2 == 0 else r * 0.45
        pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
    d.polygon(pts, fill=star + (255,))


def hood(layer, cloth):
    d = ImageDraw.Draw(layer)
    d.ellipse([px(140), px(96), px(372), px(360)], fill=cloth + (255,))
    d.ellipse(
        [px(186), px(150), px(326), px(300)], fill=(8, 10, 9, 255)
    )  # темнота под капюшоном
    d.polygon(
        [
            (px(140), px(300)),
            (px(120), px(430)),
            (px(392), px(430)),
            (px(372), px(300)),
        ],
        fill=cloth + (255,),
    )


def hair(layer, color):
    d = ImageDraw.Draw(layer)
    d.ellipse([px(162), px(112), px(350), px(300)], fill=color + (255,))
    d.polygon(
        [
            (px(166), px(220)),
            (px(140), px(430)),
            (px(206), px(430)),
            (px(206), px(250)),
        ],
        fill=color + (255,),
    )
    d.polygon(
        [
            (px(346), px(220)),
            (px(372), px(430)),
            (px(306), px(430)),
            (px(306), px(250)),
        ],
        fill=color + (255,),
    )
    d.ellipse(
        [px(186), px(150), px(326), px(296)], fill=(214, 176, 160, 255)
    )  # лицо поверх волос


def wings(layer, c1, c2):
    w = new_layer()
    d = ImageDraw.Draw(w)
    for sx in (-1, 1):
        cx = 256 + sx * 142
        d.ellipse([px(cx - 100), px(258), px(cx + 100), px(418)], fill=c1 + (215,))
        d.ellipse([px(cx - 76), px(380), px(cx + 76), px(506)], fill=c2 + (195,))
    w = w.filter(ImageFilter.GaussianBlur(px(2)))
    layer.alpha_composite(w)


def collar(layer, color, tie=None):
    d = ImageDraw.Draw(layer)
    d.polygon(
        [
            (px(214), px(336)),
            (px(256), px(412)),
            (px(298), px(336)),
            (px(276), px(324)),
            (px(236), px(324)),
        ],
        fill=color + (255,),
    )
    if tie:
        d.polygon(
            [
                (px(246), px(360)),
                (px(266), px(360)),
                (px(276), px(470)),
                (px(256), px(492)),
                (px(236), px(470)),
            ],
            fill=tie + (255,),
        )


def cross(layer, color):
    d = ImageDraw.Draw(layer)
    d.rectangle([px(238), px(400), px(274), px(492)], fill=color + (255,))
    d.rectangle([px(210), px(428), px(302), px(464)], fill=color + (255,))


def knife(layer, blade, handle):
    k = new_layer()
    d = ImageDraw.Draw(k)
    d.polygon(
        [
            (px(372), px(300)),
            (px(404), px(316)),
            (px(352), px(470)),
            (px(330), px(452)),
        ],
        fill=blade + (255,),
    )
    d.polygon(
        [
            (px(352), px(470)),
            (px(330), px(452)),
            (px(310), px(516)),
            (px(336), px(528)),
        ],
        fill=handle + (255,),
    )
    d.line(
        [(px(378), px(312)), (px(346), px(452))], fill=(255, 255, 255, 190), width=px(3)
    )
    layer.alpha_composite(k)


def cigar(layer):
    d = ImageDraw.Draw(layer)
    d.rectangle([px(268), px(258), px(340), px(272)], fill=(72, 52, 40, 255))
    d.rectangle([px(268), px(258), px(288), px(272)], fill=(150, 130, 110, 255))
    ember = new_layer()
    ed = ImageDraw.Draw(ember)
    ed.ellipse([px(332), px(252), px(352), px(278)], fill=(255, 138, 62, 255))
    ember = ember.filter(ImageFilter.GaussianBlur(px(5)))
    layer.alpha_composite(ember)


def compose(bg_inner, bg_outer, paint):
    img = radial_background(bg_inner, bg_outer).convert("RGBA")
    layer = new_layer()
    paint(layer)
    img.alpha_composite(layer)
    img = img.convert("RGB")
    img = vignette(img)
    img = grain(img)
    return img.resize((S, S), Image.LANCZOS)


SKIN = (206, 168, 150)
SKIN_DARK = (150, 118, 104)


def civilian(layer):
    body(layer, SKIN, (74, 78, 92), (128, 148, 190))
    face_shadow(layer)
    d = ImageDraw.Draw(layer)
    d.pieslice(
        [px(172), px(112), px(340), px(232)], 180, 360, fill=(96, 102, 118, 255)
    )  # кепка
    d.ellipse([px(150), px(160), px(330), px(196)], fill=(80, 86, 100, 255))
    collar(layer, (96, 102, 118))


def doctor(layer):
    body(layer, SKIN, (232, 236, 240), (150, 210, 235))
    face_shadow(layer)
    collar(layer, (246, 248, 250))
    cross(layer, (196, 58, 66))
    d = ImageDraw.Draw(layer)
    d.arc(
        [px(196), px(330), px(316), px(452)],
        200,
        340,
        fill=(120, 130, 140, 255),
        width=px(9),
    )
    d.ellipse(
        [px(240), px(452), px(272), px(486)], fill=(120, 130, 140, 255)
    )  # стетоскоп


def sheriff(layer):
    body(layer, SKIN, (46, 58, 78), (120, 160, 220))
    face_shadow(layer)
    cap(layer, (38, 48, 66), (24, 28, 38), (232, 190, 90))
    collar(layer, (54, 68, 92))
    d = ImageDraw.Draw(layer)
    cx, cy, r = px(196), px(430), px(30)
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        rad = r if i % 2 == 0 else r * 0.45
        pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
    d.polygon(pts, fill=(232, 190, 90, 255))  # жетон


def butterfly(layer):
    wings(layer, (196, 84, 158), (128, 62, 172))
    body(layer, SKIN, (62, 34, 74), (222, 120, 190), wide=0.88)
    hair(layer, (44, 26, 40))
    face_shadow(layer, depth=70)
    d = ImageDraw.Draw(layer)
    d.polygon(
        [(px(214), px(336)), (px(256), px(430)), (px(298), px(336))],
        fill=(90, 44, 104, 255),
    )
    d.ellipse(
        [px(238), px(300), px(274), px(324)], fill=(224, 132, 196, 255)
    )  # губы-акцент? нет — брошь


def mafia(layer):
    body(layer, SKIN_DARK, (28, 28, 34), (196, 74, 84))
    face_shadow(layer, depth=120)
    fedora(layer, (24, 24, 30), (120, 30, 40))
    collar(layer, (44, 44, 52), tie=(150, 34, 44))


def don(layer):
    body(layer, SKIN_DARK, (22, 20, 26), (222, 176, 92))
    face_shadow(layer, depth=110)
    fedora(layer, (18, 16, 22), (196, 154, 72))
    collar(layer, (40, 36, 44), tie=(200, 158, 74))
    cigar(layer)
    d = ImageDraw.Draw(layer)
    d.ellipse(
        [px(300), px(470), px(330), px(500)], fill=(214, 172, 84, 255)
    )  # перстень


def maniac(layer):
    body(layer, (120, 112, 104), (48, 62, 54), (110, 226, 162))
    hood(layer, (48, 62, 54))
    knife(layer, (206, 226, 216), (46, 40, 36))


CHARACTERS = [
    ("role_civilian", (58, 62, 78), (12, 13, 18), civilian),
    ("role_doctor", (60, 84, 96), (10, 16, 20), doctor),
    ("role_sheriff", (52, 62, 88), (10, 12, 20), sheriff),
    ("role_butterfly", (92, 44, 92), (18, 8, 22), butterfly),
    ("role_mafia", (78, 26, 32), (16, 6, 8), mafia),
    ("role_don", (86, 58, 20), (18, 10, 6), don),
    ("role_maniac", (52, 96, 74), (8, 18, 13), maniac),
]

if __name__ == "__main__":
    print("Рисую портреты ролей:")
    for name, inner, outer, painter in CHARACTERS:
        img = compose(inner, outer, painter)
        path = os.path.join(OUT, name + ".webp")
        img.save(path, "WEBP", quality=88, method=6)
        print(f"  {name}.webp  {os.path.getsize(path) / 1024:.0f} KB")
    print("Готово.")
