package me.blueslime.pixelmotd.color.renders;

import me.blueslime.pixelmotd.color.UniversalColorParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class BungeeRenderer implements Renderer<BaseComponent[]> {

    private static BungeeRenderer instance = null;

    public static BaseComponent[] create(String textToRender) {
        return create(UniversalColorParser.parse(textToRender));
    }

    public static BaseComponent[] create(List<UniversalColorParser.Segment> segments) {
        if (instance == null) {
            instance = new BungeeRenderer();
        }
        return instance.render(segments);
    }

    @Override
    public BaseComponent[] render(List<UniversalColorParser.Segment> segments) {
        List<BaseComponent> comps = new ArrayList<>();
        for (UniversalColorParser.Segment s : segments) {
            TextComponent tc = new TextComponent(s.text);
            if (s.color != null) try { tc.setColor(ChatColor.of(s.color.toHex())); } catch (Throwable t) {}
            tc.setBold(s.bold); tc.setItalic(s.italic); tc.setUnderlined(s.underlined); tc.setStrikethrough(s.strikethrough); tc.setObfuscated(s.obfuscated);
            comps.add(tc);
        }
        return comps.toArray(new BaseComponent[0]);
    }
}
