package me.blueslime.pixelmotd.color;

import java.util.*;

public class UniversalColorParser {

    // Safety limits
    private static final int MAX_SEGMENTS = 50_000;
    private static final int MAX_GRADIENT_EXPANSION = 4096;

    public static class Segment {
        public final String text;
        public final Color color;
        public final boolean gradient;
        public final boolean bold, italic, underlined, strikethrough, obfuscated;
        public Segment(String text, Color color, boolean gradient, boolean bold, boolean italic, boolean underlined, boolean strikethrough, boolean obfuscated) {
            this.text = text; this.color = color; this.gradient = gradient; this.bold = bold; this.italic = italic; this.underlined = underlined; this.strikethrough = strikethrough; this.obfuscated = obfuscated;
        }
        public Segment(String text, Color color, boolean bold, boolean italic, boolean underlined, boolean strikethrough, boolean obfuscated) {
            this(text, color, false, bold, italic, underlined, strikethrough, obfuscated);
        }
        public Segment(String text, Color color) { this(text, color, false, false, false, false, false); }
        @Override public String toString(){ return String.format("[%s %s]%s", color==null?"null":color.toHex(), (bold?"B":"")+(italic?"I":"")+(underlined?"U":"")+(strikethrough?"S":""),(text==null?"":text)); }
    }

    public record Color(int r, int g, int b) {
        public static Color fromHex(String hex) {
            if (hex == null) throw new IllegalArgumentException("hex == null");
            String h = hex.replace("#", "");
            if (h.length() == 3) {
                h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
            }
            if (h.length() != 6) throw new IllegalArgumentException("Invalid hex color: " + hex);
            int v = Integer.parseInt(h, 16);
            return new Color((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF);
        }

        public String toHex() {
            return String.format("#%02x%02x%02x", r, g, b);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Color(int r1, int g1, int b1))) return false;
            return r1 == r && g1 == g && b1 == b;
        }
    }

    // MiniMessage-like named colors -> legacy code
    private static final Map<String, Character> NAME_TO_LEGACY = new HashMap<>();
    static {
        NAME_TO_LEGACY.put("black", '0'); NAME_TO_LEGACY.put("darkgreen",'2');
        NAME_TO_LEGACY.put("darkred", '4'); NAME_TO_LEGACY.put("gold", '6'); NAME_TO_LEGACY.put("gray", '7');
        NAME_TO_LEGACY.put("dark_gray", '8'); NAME_TO_LEGACY.put("blue", '9'); NAME_TO_LEGACY.put("green", 'a'); NAME_TO_LEGACY.put("aqua", 'b');
        NAME_TO_LEGACY.put("red", 'c'); NAME_TO_LEGACY.put("light_purple", 'd'); NAME_TO_LEGACY.put("yellow", 'e'); NAME_TO_LEGACY.put("white", 'f');
        // additional common aliases
        NAME_TO_LEGACY.put("dark_blue",'1'); NAME_TO_LEGACY.put("dark_green",'2'); NAME_TO_LEGACY.put("dark_aqua",'3'); NAME_TO_LEGACY.put("dark_purple",'5');
    }

    private static Color legacyCodeToColor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> new Color(0, 0, 0);
            case '1' -> new Color(0, 0, 170);
            case '2' -> new Color(0, 170, 0);
            case '3' -> new Color(0, 170, 170);
            case '4' -> new Color(170, 0, 0);
            case '5' -> new Color(170, 0, 170);
            case '6' -> new Color(255, 170, 0);
            case '7' -> new Color(170, 170, 170);
            case '8' -> new Color(85, 85, 85);
            case '9' -> new Color(85, 85, 255);
            case 'a' -> new Color(85, 255, 85);
            case 'b' -> new Color(85, 255, 255);
            case 'c' -> new Color(255, 85, 85);
            case 'd' -> new Color(255, 85, 255);
            case 'e' -> new Color(255, 255, 85);
            case 'f' -> new Color(255, 255, 255);
            default -> null;
        };
    }

    // ---------------- Parsing ----------------
    public static List<Segment> parse(String input) {
        List<Segment> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        Color curColor = null; boolean bold=false, italic=false, under=false, strike=false, obf=false;
        int segmentsCreated = 0;
        int i=0, len = input.length();
        while (i < len) {
            if (segmentsCreated > MAX_SEGMENTS) { cur.append(input.substring(i)); break; }
            char ch = input.charAt(i);
            if (ch == '&') {
                // escaped && => literal &
                if (i+1 < len && input.charAt(i+1) == '&') { cur.append('&'); i += 2; continue; }

                // try &x hex-with-separators
                ParseHexResult ph = tryParseAmpersandHex(input, i);
                if (ph != null) {
                    if (!cur.isEmpty()) { out.add(new Segment(cur.toString(), curColor, false, bold, italic, under, strike, obf)); cur.setLength(0); segmentsCreated++; }
                    curColor = ph.color; i = ph.newIndex; continue;
                }

                // try '&#' inline hex (do NOT consume following &)
                if (i+1 < len && input.charAt(i+1) == '#') {
                    if (!cur.isEmpty()) { out.add(new Segment(cur.toString(), curColor, false, bold, italic, under, strike, obf)); cur.setLength(0); segmentsCreated++; }
                    int j = i+2; StringBuilder hx = new StringBuilder();
                    while (j < len && isHexChar(input.charAt(j)) && hx.length() < 6) { hx.append(input.charAt(j)); j++; }
                    if (hx.length() >= 3) { curColor = Color.fromHex(hx.toString()); i = j; continue; }
                    // malformed: fallthrough and treat as literal
                }

                // &name& style like &gold&
                int j = i + 1;
                while (j < len && (Character.isLetter(input.charAt(j)) || input.charAt(j) == '_' || input.charAt(j) == '-')) {
                    j++;
                }
                if (j < len && j > i + 1 && input.charAt(j) == '&') {
                    String name = input.substring(i + 1, j).toLowerCase();
                    Character code = NAME_TO_LEGACY.get(name);
                    if (code != null) {
                        if (!cur.isEmpty()) {
                            out.add(new Segment(cur.toString(), curColor, false, bold, italic, under, strike, obf));
                            cur.setLength(0);
                            segmentsCreated++;
                        }
                        curColor = legacyCodeToColor(code);
                        i = j + 1;
                        continue;
                    }
                }

                // single char codes: &l &o &n &m &k &r or legacy colors &a
                if (i+1 < len) {
                    if (!cur.isEmpty()) { out.add(new Segment(cur.toString(), curColor, false, bold, italic, under, strike, obf)); cur.setLength(0); segmentsCreated++; }
                    char code = Character.toLowerCase(input.charAt(i+1)); i += 2;
                    switch (code) {
                        case 'k': obf = true; break; case 'l': bold = true; break; case 'm': strike = true; break; case 'n': under = true; break; case 'o': italic = true; break;
                        case 'r': curColor = null; bold = italic = under = strike = obf = false; break;
                        default:
                            Color c = legacyCodeToColor(code);
                            if (c != null) { curColor = c; } else { cur.append('&').append(code); }
                            break;
                    }
                    continue;
                }
                // single trailing &
                cur.append('&'); i++; continue;
            }

            // Angle tags: <#hex> ... </#>, <GRADIENT:hex[,hex...]>, <RAINBOW>, <red> etc.
            if (ch == '<') {
                // try <#HEX>
                if (i+1 < len && input.charAt(i+1) == '#') {
                    // flush
                    if (!cur.isEmpty()) { out.add(new Segment(cur.toString(), curColor, false, bold, italic, under, strike, obf)); cur.setLength(0); segmentsCreated++; }
                    int j = i+2; StringBuilder hx = new StringBuilder();
                    while (j < len && isHexChar(input.charAt(j)) && hx.length() < 6) { hx.append(input.charAt(j)); j++; }
                    if (j < len && input.charAt(j) == '>') {
                        j++;
                        int close = input.indexOf("</#>", j);
                        int closeAlt = input.indexOf("</#", j);
                        if (close == -1 && closeAlt == -1) {
                            String body = input.substring(j);
                            out.addAll(applyColorOverrideToParsed(body, Color.fromHex(hx.toString()), bold, italic, under, strike, obf));
                            return mergeSegments(out);
                        }
                        int closeIndex = (close != -1) ? close : closeAlt;
                        String body = input.substring(j, closeIndex);
                        out.addAll(applyColorOverrideToParsed(body, Color.fromHex(hx.toString()), bold, italic, under, strike, obf));
                        int closeEnd = input.indexOf('>', closeIndex); i = (closeEnd == -1) ? len : closeEnd+1; continue;
                    }
                }

                if (matchesAtIgnoreCase(input, i, "<GRADIENT:", true)) {
                    int colon = i + "<GRADIENT:".length();
                    int j = colon;
                    StringBuilder token = new StringBuilder();
                    while (j < len && input.charAt(j) != '>') { token.append(input.charAt(j)); j++; }
                    if (j < len && input.charAt(j) == '>') {
                        j++;
                        String tok = token.toString();
                        String[] parts = tok.split("[,;|]");
                        List<Color> stops = new ArrayList<>();
                        for (String p : parts) {
                            String p2 = p.trim().replace("#", "");
                            if (p2.length() >= 3 && p2.length() <= 6) {
                                stops.add(Color.fromHex(p2));
                            }
                        }
                        if (stops.isEmpty()) { i = j; continue; }

                        int closeIdx = indexOfIgnoreCase(input, "</GRADIENT:", j);
                        String endHex = null;
                        int closeStart = -1, closeEnd = -1;
                        if (closeIdx != -1) {
                            int after = closeIdx + "</GRADIENT:".length();
                            int k = after;
                            StringBuilder hx2 = new StringBuilder();
                            while (k < len && isHexChar(input.charAt(k)) && hx2.length() < 6) { hx2.append(input.charAt(k)); k++; }
                            if (k < len && input.charAt(k) == '>') {
                                closeStart = closeIdx; closeEnd = k + 1;
                                endHex = hx2.toString();
                            }
                        }

                        if (closeStart == -1) {
                            int close2 = indexOfIgnoreCase(input, "</GRADIENT>", j);
                            if (close2 != -1) { closeStart = close2; closeEnd = close2 + "</GRADIENT>".length(); }
                        }

                        if (endHex != null && endHex.length() >= 3) {
                            // Append endHex as final stop if it's not already equal to the last stop
                            Color endColor = Color.fromHex(endHex);
                            if (stops.isEmpty() || !Objects.equals(stops.get(stops.size()-1), endColor)) {
                                stops.add(endColor);
                            }
                        }

                        if (closeStart == -1) {
                            String body = input.substring(j);
                            out.addAll(expandMultiStopGradientFromParsed(body, stops, bold, italic, under, strike, obf));
                            return mergeSegments(out);
                        } else {
                            String body = input.substring(j, closeStart);
                            out.addAll(expandMultiStopGradientFromParsed(body, stops, bold, italic, under, strike, obf));
                            i = closeEnd;
                            continue;
                        }
                    }
                }

                // <RAINBOW>...
                if (matchesAtIgnoreCase(input, i, "<RAINBOW", true) || matchesAtIgnoreCase(input, i, "<rainbow", true)) {
                    int openClose = input.indexOf('>', i); int afterOpen = (openClose == -1) ? i+1 : openClose+1; int close = indexOfIgnoreCase(input, "</RAINBOW>", afterOpen);
                    if (close == -1) { String body = input.substring(afterOpen); out.addAll(expandRainbowFromParsed(body, bold, italic, under, strike, obf)); return mergeSegments(out); }
                    String body = input.substring(afterOpen, close); out.addAll(expandRainbowFromParsed(body, bold, italic, under, strike, obf)); i = close + "</RAINBOW>".length(); continue;
                }

                // named mini-message tags like <red>text</red>
                String tag = readTagName(input, i);
                if (tag != null) {
                    Character code = NAME_TO_LEGACY.get(tag.toLowerCase());
                    if (code != null) {
                        int openEnd = input.indexOf('>', i); if (openEnd == -1) { i++; continue; }
                        int close = indexOfIgnoreCase(input, "</"+tag+">", openEnd+1);
                        if (close == -1) { String body = input.substring(openEnd+1); out.addAll(applyColorOverrideToParsed(body, legacyCodeToColor(code), bold, italic, under, strike, obf)); return mergeSegments(out); }
                        String body = input.substring(openEnd+1, close);
                        out.addAll(applyColorOverrideToParsed(body, legacyCodeToColor(code), bold, italic, under, strike, obf)); i = close + tag.length() + 3; continue;
                    }
                }
            }

            // default char
            cur.append(ch); i++;
        }
        if (!cur.isEmpty()) out.add(new Segment(cur.toString(), curColor, false, bold, italic, under, strike, obf));
        return mergeSegments(out);
    }

    // ---------------- parsing helper ----------------
    private static class ParseHexResult { final Color color; final int newIndex; ParseHexResult(Color color,int newIndex){this.color=color;this.newIndex=newIndex;} }

    private static ParseHexResult tryParseAmpersandHex(String input, int idx) {
        int len = input.length();
        if (idx + 1 >= len) return null;
        char x = input.charAt(idx + 1);
        if (!(x == 'x' || x == 'X')) return null;

        int pos = idx + 2;

        if (pos < len && input.charAt(pos) == '&') {
            int p = pos;
            StringBuilder hx = new StringBuilder(6);
            for (int k = 0; k < 6; k++) {
                if (p >= len || input.charAt(p) != '&') return null;
                p++; // skip '&'
                if (p >= len) return null;
                char hc = input.charAt(p);
                if (!isHexChar(hc)) return null;
                hx.append(hc);
                p++;
            }
            return new ParseHexResult(Color.fromHex(hx.toString()), p);
        }

        if (pos + 6 <= len) {
            boolean ok = true;
            for (int k = 0; k < 6; k++) {
                if (!isHexChar(input.charAt(pos + k))) { ok = false; break; }
            }
            if (ok) {
                String hx = input.substring(pos, pos + 6);
                return new ParseHexResult(Color.fromHex(hx), pos + 6);
            }
        }

        return null;
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
    @SuppressWarnings("unused")
    private static boolean matchesAtIgnoreCase(String input, int idx, String prefix, boolean unused) { if (idx+prefix.length()>input.length()) return false; return input.substring(idx, idx+prefix.length()).equalsIgnoreCase(prefix); }
    private static int indexOfIgnoreCase(String s, String sub, int from) { return s.toLowerCase().indexOf(sub.toLowerCase(), from); }

    private static String readTagName(String input, int idx) {
        // read tag name after '<'
        if (input.charAt(idx) != '<') return null; int j = idx+1; StringBuilder b=new StringBuilder(); while (j<input.length()) { char c=input.charAt(j); if (Character.isLetter(c) || c=='_' || c=='-') { b.append(c); j++; } else break; }
        return b.isEmpty() ?null:b.toString();
    }

    // parse content and override colors
    private static List<Segment> applyColorOverrideToParsed(String body, Color color, boolean bold, boolean italic, boolean under, boolean strike, boolean obf) {
        List<Segment> parsed = parse(body);
        List<Segment> out = new ArrayList<>();
        for (Segment s : parsed) {
            for (int i=0;i<s.text.length();i++) {
                out.add(new Segment(String.valueOf(s.text.charAt(i)), color, s.bold||bold, s.italic||italic, s.underlined||under, s.strikethrough||strike, s.obfuscated||obf));
            }
        }
        return mergeSegments(out);
    }

    private static List<Segment> expandMultiStopGradientFromParsed(String body, List<Color> stops, boolean bold, boolean italic, boolean under, boolean strike, boolean obf) {
        // parse body preserving styles
        List<Segment> inner = parse(body);

        class CE { char ch; boolean b,i,u,s,k; }

        List<CE> chars = new ArrayList<>();
        for (Segment s : inner) {
            for (int j = 0; j < s.text.length(); j++) {
                CE e = new CE();
                e.ch = s.text.charAt(j);
                e.b = s.bold || bold;
                e.i = s.italic || italic;
                e.u = s.underlined || under;
                e.s = s.strikethrough || strike;
                e.k = s.obfuscated || obf;
                chars.add(e);
            }
        }

        int n = chars.size();
        if (n == 0) return Collections.emptyList();

        int numStops = stops == null ? 0 : stops.size();
        if (numStops == 0) {
            // no stops provided -> fallback: return body as default color (null)
            return Collections.singletonList(new Segment(body, null, bold, italic, under, strike, obf));
        }

        List<Segment> out = new ArrayList<>(n);

        if (numStops == 1) {
            // Single stop: everything gets the same color
            Color c = stops.get(0);
            for (int idx = 0; idx < n; idx++) {
                CE ce = chars.get(idx);
                out.add(new Segment(String.valueOf(ce.ch), c, true, ce.b, ce.i, ce.u, ce.s, ce.k));
            }
            return mergeSegments(out);
        }

        for (int idx = 0; idx < n; idx++) {
            double tGlobal = (double) idx / Math.max(1, n - 1);               // in [0,1]
            double scaled = tGlobal * (numStops - 1);                         // in [0, numStops-1]
            int left = (int) Math.floor(scaled);
            if (left < 0) left = 0;
            if (left > numStops - 2) left = numStops - 2;
            double localT = scaled - left;                                    // in [0,1]
            Color a = stops.get(left);
            Color b = stops.get(left + 1);
            Color c = lerpColor(a, b, localT);
            CE ce = chars.get(idx);
            out.add(new Segment(String.valueOf(ce.ch), c, true, ce.b, ce.i, ce.u, ce.s, ce.k));
        }

        return mergeSegments(out);
    }

    private static List<Segment> expandGradientFromParsed(String body, Color a, Color b, boolean bold, boolean italic, boolean under, boolean strike, boolean obf) {
        return expandMultiStopGradientFromParsed(body, Arrays.asList(a,b), bold, italic, under, strike, obf);
    }

    private static List<Segment> expandRainbowFromParsed(String body, boolean bold, boolean italic, boolean under, boolean strike, boolean obf) {
        List<Segment> inner = parse(body);
        class CE { char ch; boolean b,i,u,s,k; }
        List<CE> chars = new ArrayList<>();
        for (Segment s : inner) for (int j=0;j<s.text.length();j++){ CE e=new CE(); e.ch=s.text.charAt(j); e.b=s.bold||bold; e.i=s.italic||italic; e.u=s.underlined||under; e.s=s.strikethrough||strike; e.k=s.obfuscated||obf; chars.add(e); }
        int n = chars.size(); if (n==0) return Collections.emptyList();
        if (n > MAX_GRADIENT_EXPANSION) {
            int chunk = (int)Math.ceil((double)n / MAX_GRADIENT_EXPANSION);
            List<Segment> out = new ArrayList<>();
            for (int start=0; start<n; start+=chunk) {
                int end = Math.min(n, start+chunk);
                double t = (double)start / Math.max(1, n-1);
                Color c = hsvToRgb(t,1.0,1.0);
                StringBuilder sb = new StringBuilder(); boolean B=false,O=false,U=false,S=false,K=false;
                for (int k=start;k<end;k++) { sb.append(chars.get(k).ch); CE ce = chars.get(k); B = B||ce.b; O = O||ce.i; U = U||ce.u; S = S||ce.s; K = K||ce.k; }
                out.add(new Segment(sb.toString(), c, true, B, O, U, S, K));
            }
            return out;
        }
        List<Segment> out = new ArrayList<>();
        for (int idx=0; idx<n; idx++) {
            double t = (double)idx / Math.max(1, n-1);
            Color c = hsvToRgb(t,1.0,1.0); CE ce = chars.get(idx);
            out.add(new Segment(String.valueOf(ce.ch), c, true, ce.b, ce.i, ce.u, ce.s, ce.k));
        }
        return mergeSegments(out);
    }

    // ---------------- Utilities ----------------
    private static Color lerpColor(Color a, Color b, double t) {
        int r = (int)Math.round(a.r + (b.r - a.r) * t);
        int g = (int)Math.round(a.g + (b.g - a.g) * t);
        int bl = (int)Math.round(a.b + (b.b - a.b) * t);
        return new Color(clamp(r), clamp(g), clamp(bl));
    }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private static Color hsvToRgb(double h, double s, double v) {
        double r=0,g=0,b=0; int i = (int)Math.floor(h*6); double f = h*6 - i; double p = v*(1-s); double q = v*(1-f*s); double t = v*(1-(1-f)*s);
        switch (i%6) { case 0: r=v; g=t; b=p; break; case 1: r=q; g=v; b=p; break; case 2: r=p; g=v; b=t; break; case 3: r=p; g=q; b=v; break; case 4: r=t; g=p; b=v; break; default: r=v; g=p; b=q; break; }
        return new Color((int)Math.round(r*255),(int)Math.round(g*255),(int)Math.round(b*255));
    }

    private static List<Segment> mergeSegments(List<Segment> in) {
        if (in.isEmpty()) return in; List<Segment> out = new ArrayList<>(); Segment cur = in.get(0);
        for (int i=1;i<in.size();i++) { Segment s=in.get(i); if (Objects.equals(cur.color,s.color) && cur.bold==s.bold && cur.italic==s.italic && cur.underlined==s.underlined && cur.strikethrough==s.strikethrough && cur.obfuscated==s.obfuscated && cur.gradient==s.gradient) { cur = new Segment(cur.text + s.text, cur.color, cur.gradient, cur.bold, cur.italic, cur.underlined, cur.strikethrough, cur.obfuscated); } else { out.add(cur); cur = s; } } out.add(cur); return out;
    }

}
