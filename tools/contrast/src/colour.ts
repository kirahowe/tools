// Pure colour math: parsing, conversion, and WCAG 2.x contrast. No DOM here.

export interface Rgb {
  r: number;
  g: number;
  b: number;
}

/** Accepts #rgb, #rrggbb, rgb, or rrggbb (case-insensitive). Null if it doesn't parse. */
export function hexToRgb(hex: string): Rgb | null {
  const s = hex.trim().replace(/^#/, "");
  if (/^[0-9a-f]{3}$/i.test(s)) {
    return {
      r: parseInt(s[0] + s[0], 16),
      g: parseInt(s[1] + s[1], 16),
      b: parseInt(s[2] + s[2], 16),
    };
  }
  if (/^[0-9a-f]{6}$/i.test(s)) {
    return {
      r: parseInt(s.slice(0, 2), 16),
      g: parseInt(s.slice(2, 4), 16),
      b: parseInt(s.slice(4, 6), 16),
    };
  }
  return null;
}

/** Lowercase #rrggbb, channel values clamped to 0–255. */
export function rgbToHex(r: number, g: number, b: number): string {
  const channel = (n: number) => Math.max(0, Math.min(255, Math.round(n))).toString(16).padStart(2, "0");
  return `#${channel(r)}${channel(g)}${channel(b)}`;
}

/** h in degrees 0–360, s/l as 0–100 percentages; returns integer 0–255 RGB channels. */
export function hslToRgb(h: number, s: number, l: number): Rgb {
  const hue = ((h % 360) + 360) % 360;
  const sat = s / 100;
  const light = l / 100;

  const c = (1 - Math.abs(2 * light - 1)) * sat;
  const x = c * (1 - Math.abs(((hue / 60) % 2) - 1));
  const m = light - c / 2;

  let r1 = 0;
  let g1 = 0;
  let b1 = 0;
  if (hue < 60) {
    r1 = c; g1 = x; b1 = 0;
  } else if (hue < 120) {
    r1 = x; g1 = c; b1 = 0;
  } else if (hue < 180) {
    r1 = 0; g1 = c; b1 = x;
  } else if (hue < 240) {
    r1 = 0; g1 = x; b1 = c;
  } else if (hue < 300) {
    r1 = x; g1 = 0; b1 = c;
  } else {
    r1 = c; g1 = 0; b1 = x;
  }

  return {
    r: Math.round((r1 + m) * 255),
    g: Math.round((g1 + m) * 255),
    b: Math.round((b1 + m) * 255),
  };
}

export function linearize(channel255: number): number {
  const c = channel255 / 255;
  return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
}

/** WCAG 2.x relative luminance (0.04045 linearization threshold). */
export function relLuminance(r: number, g: number, b: number): number {
  return 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b);
}

/** (Lmax + 0.05) / (Lmin + 0.05), range ~1–21. */
export function contrastRatio(rgb1: Rgb, rgb2: Rgb): number {
  const l1 = relLuminance(rgb1.r, rgb1.g, rgb1.b);
  const l2 = relLuminance(rgb2.r, rgb2.g, rgb2.b);
  const lMax = Math.max(l1, l2);
  const lMin = Math.min(l1, l2);
  return (lMax + 0.05) / (lMin + 0.05);
}

export interface Hsl {
  h: number;
  s: number;
  l: number;
}

/** Inverse of hslToRgb: r/g/b 0–255 -> h in degrees 0–360, s/l as 0–100 percentages. */
export function rgbToHsl(r: number, g: number, b: number): Hsl {
  const rf = r / 255;
  const gf = g / 255;
  const bf = b / 255;
  const max = Math.max(rf, gf, bf);
  const min = Math.min(rf, gf, bf);
  const l = (max + min) / 2;

  if (max === min) {
    return { h: 0, s: 0, l: l * 100 };
  }

  const d = max - min;
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
  let h: number;
  if (max === rf) h = ((gf - bf) / d) % 6;
  else if (max === gf) h = (bf - rf) / d + 2;
  else h = (rf - gf) / d + 4;
  h *= 60;
  if (h < 0) h += 360;

  return { h, s: s * 100, l: l * 100 };
}
