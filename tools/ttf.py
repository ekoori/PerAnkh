"""Just enough of a TrueType reader for this project's build tools.

Reads the cmap (formats 4 and 12), the per-glyph ink boxes from glyf/loca, advance widths, and
the name table. Written instead of depending on fontTools so the generators run on a bare Python
install; it only has to understand the two fonts the app ships.
"""
from __future__ import annotations

import struct
from dataclasses import dataclass


@dataclass(frozen=True)
class Box:
    x_min: int
    y_min: int
    x_max: int
    y_max: int


class Font:
    def __init__(self, path: str):
        with open(path, "rb") as f:
            self.data = f.read()
        num_tables = struct.unpack(">H", self.data[4:6])[0]
        self.tables: dict[str, tuple[int, int]] = {}
        for i in range(num_tables):
            rec = self.data[12 + 16 * i: 28 + 16 * i]
            tag = rec[0:4].decode("latin-1")
            _, offset, length = struct.unpack(">III", rec[4:16])
            self.tables[tag] = (offset, length)

        head = self._table("head")
        self.units_per_em = struct.unpack(">H", head[18:20])[0]
        self.index_to_loc_format = struct.unpack(">h", head[50:52])[0]
        self.num_glyphs = struct.unpack(">H", self._table("maxp")[4:6])[0]

        hhea = self._table("hhea")
        self.ascender, self.descender = struct.unpack(">hh", hhea[4:8])
        self.num_h_metrics = struct.unpack(">H", hhea[34:36])[0]

        self.cmap = self._read_cmap()

    def _table(self, tag: str) -> bytes:
        offset, length = self.tables[tag]
        return self.data[offset: offset + length]

    def _read_cmap(self) -> dict[int, int]:
        cmap = self._table("cmap")
        n = struct.unpack(">H", cmap[2:4])[0]
        best: dict[int, int] = {}
        for i in range(n):
            platform, encoding, offset = struct.unpack(">HHI", cmap[4 + 8 * i: 12 + 8 * i])
            if platform not in (0, 3):
                continue
            fmt = struct.unpack(">H", cmap[offset: offset + 2])[0]
            if fmt == 4:
                mapping = self._cmap4(cmap, offset)
            elif fmt == 12:
                mapping = self._cmap12(cmap, offset)
            else:
                continue
            if len(mapping) > len(best):
                best = mapping
        return best

    @staticmethod
    def _cmap4(cmap: bytes, offset: int) -> dict[int, int]:
        seg_x2 = struct.unpack(">H", cmap[offset + 6: offset + 8])[0]
        segs = seg_x2 // 2
        ends_at = offset + 14
        starts_at = ends_at + seg_x2 + 2
        deltas_at = starts_at + seg_x2
        ranges_at = deltas_at + seg_x2
        out: dict[int, int] = {}
        for s in range(segs):
            end = struct.unpack(">H", cmap[ends_at + 2 * s: ends_at + 2 * s + 2])[0]
            start = struct.unpack(">H", cmap[starts_at + 2 * s: starts_at + 2 * s + 2])[0]
            delta = struct.unpack(">h", cmap[deltas_at + 2 * s: deltas_at + 2 * s + 2])[0]
            range_offset = struct.unpack(">H", cmap[ranges_at + 2 * s: ranges_at + 2 * s + 2])[0]
            for c in range(start, end + 1):
                if c == 0xFFFF:
                    continue
                if range_offset == 0:
                    g = (c + delta) & 0xFFFF
                else:
                    at = ranges_at + 2 * s + range_offset + 2 * (c - start)
                    g = struct.unpack(">H", cmap[at: at + 2])[0]
                    if g:
                        g = (g + delta) & 0xFFFF
                if g:
                    out[c] = g
        return out

    @staticmethod
    def _cmap12(cmap: bytes, offset: int) -> dict[int, int]:
        groups = struct.unpack(">I", cmap[offset + 12: offset + 16])[0]
        out: dict[int, int] = {}
        for i in range(groups):
            at = offset + 16 + 12 * i
            start, end, glyph = struct.unpack(">III", cmap[at: at + 12])
            for c in range(start, end + 1):
                out[c] = glyph + (c - start)
        return out

    def glyph_box(self, glyph: int) -> Box | None:
        loca = self._table("loca")
        if self.index_to_loc_format == 0:
            a, b = struct.unpack(">HH", loca[2 * glyph: 2 * glyph + 4])
            a, b = a * 2, b * 2
        else:
            a, b = struct.unpack(">II", loca[4 * glyph: 4 * glyph + 8])
        if a == b:
            return None  # empty glyph (a space)
        glyf_offset, _ = self.tables["glyf"]
        header = self.data[glyf_offset + a: glyf_offset + a + 10]
        _, x_min, y_min, x_max, y_max = struct.unpack(">hhhhh", header)
        return Box(x_min, y_min, x_max, y_max)

    def advance(self, glyph: int) -> int:
        hmtx = self._table("hmtx")
        i = min(glyph, self.num_h_metrics - 1)
        return struct.unpack(">H", hmtx[4 * i: 4 * i + 2])[0]

    def names(self) -> dict[int, str]:
        name = self._table("name")
        count, string_offset = struct.unpack(">HH", name[2:6])
        out: dict[int, str] = {}
        for i in range(count):
            platform, encoding, lang, name_id, length, offset = struct.unpack(
                ">HHHHHH", name[6 + 12 * i: 18 + 12 * i]
            )
            raw = name[string_offset + offset: string_offset + offset + length]
            if platform == 3 and lang == 0x409:
                out[name_id] = raw.decode("utf-16-be", errors="replace")
            elif platform == 1 and name_id not in out:
                out[name_id] = raw.decode("latin-1", errors="replace")
        return out
