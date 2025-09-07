package me.blueslime.pixelmotd.utils.internal.players.injects;

import me.blueslime.pixelmotd.utils.internal.players.PlayerModule;

public class DefaultModule extends PlayerModule {

    public static final DefaultModule INSTANCE = new DefaultModule();

    @Override
    public int execute(int online, int selectedAmount) {
        return selectedAmount;
    }
}
