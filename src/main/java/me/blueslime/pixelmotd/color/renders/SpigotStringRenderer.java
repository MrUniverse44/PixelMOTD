package me.blueslime.pixelmotd.color.renders;

import me.blueslime.pixelmotd.color.UniversalColorParser;

import java.util.List;

public class SpigotStringRenderer implements Renderer<String> {
    private static final char SECTION = '§';

    private static SpigotStringRenderer instance = null;

    public static String create(String textToRender) {
        return create(UniversalColorParser.parse(textToRender));
    }

    public static String create(List<UniversalColorParser.Segment> segments) {
        if (instance == null) {
            instance = new SpigotStringRenderer();
        }
        return instance.render(segments);
    }

    @Override
    public String render(List<UniversalColorParser.Segment> segments) {
        StringBuilder sb = new StringBuilder();
        for (UniversalColorParser.Segment s : segments) {
            if (s.color != null) sb.append(toSectionHex(s.color));
            if (s.bold) sb.append(SECTION).append('l'); if (s.italic) sb.append(SECTION).append('o'); if (s.underlined) sb.append(SECTION).append('n'); if (s.strikethrough) sb.append(SECTION).append('m'); if (s.obfuscated) sb.append(SECTION).append('k');
            sb.append(s.text);
        }
        return sb.toString();
    }
    private String toSectionHex(UniversalColorParser.Color c) {
        String hx = String.format("%02x%02x%02x", c.r(), c.g(), c.b());
        StringBuilder sb = new StringBuilder();
        sb.append(SECTION).append('x');
        for (char ch : hx.toCharArray()) {
            sb.append(SECTION).append(ch);
        }
        return sb.toString();
    }

}
