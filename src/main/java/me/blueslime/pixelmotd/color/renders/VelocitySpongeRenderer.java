package me.blueslime.pixelmotd.color.renders;

import me.blueslime.pixelmotd.color.UniversalColorParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public class VelocitySpongeRenderer implements Renderer<net.kyori.adventure.text.Component> {

    private static VelocitySpongeRenderer instance = null;

    public static Component create(String textToRender) {
        return create(UniversalColorParser.parse(textToRender));
    }

    public static Component create(List<UniversalColorParser.Segment> segments) {
        if (instance == null) {
            instance = new VelocitySpongeRenderer();
        }
        return instance.render(segments);
    }

    @Override
    public Component render(List<UniversalColorParser.Segment> segments) {
        Component result = Component.empty();
        for (UniversalColorParser.Segment s : segments) {
            Component part = Component.text(s.text == null ? "" : s.text);
            if (s.color != null) {
                part = part.color(TextColor.color(s.color.r(), s.color.g(), s.color.b()));
            }
            if (s.bold) part = part.decorate(TextDecoration.BOLD);
            if (s.italic) part = part.decorate(TextDecoration.ITALIC);
            if (s.underlined) part = part.decorate(TextDecoration.UNDERLINED);
            if (s.strikethrough) part = part.decorate(TextDecoration.STRIKETHROUGH);
            if (s.obfuscated) part = part.decorate(TextDecoration.OBFUSCATED);
            result = result.append(part);
        }
        return result;
    }
}
