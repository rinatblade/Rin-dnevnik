#!/usr/bin/env python3
"""Скачивает 40 шрифтов с поддержкой кириллицы из репозитория google/fonts
в app/src/main/assets/fonts и пишет список fonts.txt («файл|Название»).
Запускается при сборке на GitHub Actions."""
import os
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "fonts")
TMP = os.path.join(ROOT, "_gfonts")

# (папка в google/fonts, название в приложении) — с запасом, берутся первые 40 удачных
CANDIDATES = [
    ("roboto", "Roboto"), ("opensans", "Open Sans"), ("montserrat", "Montserrat"),
    ("ptsans", "PT Sans"), ("ptserif", "PT Serif"), ("lora", "Lora"),
    ("playfairdisplay", "Playfair Display"), ("merriweather", "Merriweather"),
    ("oswald", "Oswald"), ("raleway", "Raleway"), ("notosans", "Noto Sans"),
    ("notoserif", "Noto Serif"), ("ubuntu", "Ubuntu"), ("firasans", "Fira Sans"),
    ("comfortaa", "Comfortaa"), ("pacifico", "Pacifico"), ("lobster", "Lobster"),
    ("caveat", "Caveat"), ("marckscript", "Marck Script"), ("amaticsc", "Amatic SC"),
    ("neucha", "Neucha"), ("badscript", "Bad Script"), ("underdog", "Underdog"),
    ("jura", "Jura"), ("philosopher", "Philosopher"), ("russoone", "Russo One"),
    ("rubik", "Rubik"), ("exo2", "Exo 2"), ("play", "Play"),
    ("cormorantgaramond", "Cormorant Garamond"), ("yanonekaffeesatz", "Yanone Kaffeesatz"),
    ("spectral", "Spectral"), ("arsenal", "Arsenal"), ("ptmono", "PT Mono"),
    ("kurale", "Kurale"), ("podkova", "Podkova"), ("elmessiri", "El Messiri"),
    ("tenorsans", "Tenor Sans"), ("oranienbaum", "Oranienbaum"), ("ibmplexsans", "IBM Plex Sans"),
    # запасные
    ("nunito", "Nunito"), ("manrope", "Manrope"), ("sourcesans3", "Source Sans 3"),
    ("ptsansnarrow", "PT Sans Narrow"), ("scada", "Scada"), ("alice", "Alice"),
    ("forum", "Forum"), ("firamono", "Fira Mono"), ("bellota", "Bellota"),
    ("andika", "Andika"), ("ysabeau", "Ysabeau"), ("notosansmono", "Noto Sans Mono"),
]
WANT = 40
LICENSES = ["ofl", "apache", "ufl"]


def run(*cmd, cwd=None):
    subprocess.run(cmd, cwd=cwd, check=True)


def pick_file(folder):
    ttfs = [f for f in os.listdir(folder) if f.lower().endswith(".ttf")]
    ttfs = [f for f in ttfs if "italic" not in f.lower()]
    if not ttfs:
        return None
    for f in ttfs:
        if f.endswith("-Regular.ttf"):
            return f
    for f in ttfs:
        if "[" in f:  # вариативный шрифт — берём как есть
            return f
    return sorted(ttfs)[0]


def main():
    if os.path.exists(TMP):
        shutil.rmtree(TMP)
    run("git", "clone", "--depth", "1", "--filter=blob:none", "--sparse",
        "https://github.com/google/fonts.git", TMP)
    paths = [f"{lic}/{d}" for d, _ in CANDIDATES for lic in LICENSES]
    run("git", "sparse-checkout", "set", *paths, cwd=TMP)

    os.makedirs(OUT, exist_ok=True)
    lines = []
    for d, title in CANDIDATES:
        if len(lines) >= WANT:
            break
        src = None
        for lic in LICENSES:
            folder = os.path.join(TMP, lic, d)
            if os.path.isdir(folder):
                name = pick_file(folder)
                if name:
                    src = os.path.join(folder, name)
                    break
        if not src:
            print(f"пропущен: {title} ({d})")
            continue
        dst_name = f"{d}.ttf"
        shutil.copyfile(src, os.path.join(OUT, dst_name))
        lines.append(f"{dst_name}|{title}")
        print(f"ok: {title}")

    with open(os.path.join(OUT, "fonts.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    shutil.rmtree(TMP, ignore_errors=True)
    print(f"Шрифтов: {len(lines)}")
    if len(lines) < WANT:
        print("Не хватило шрифтов!", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
