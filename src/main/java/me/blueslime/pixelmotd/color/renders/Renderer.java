package me.blueslime.pixelmotd.color.renders;

import me.blueslime.pixelmotd.color.UniversalColorParser;

import java.util.List;

public interface Renderer<T> {
    T render(List<UniversalColorParser.Segment> segments);
}
