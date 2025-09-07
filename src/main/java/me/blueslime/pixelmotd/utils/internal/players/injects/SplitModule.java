package me.blueslime.pixelmotd.utils.internal.players.injects;

import me.blueslime.pixelmotd.utils.internal.players.PlayerModule;

public class SplitModule extends PlayerModule {

    public static final SplitModule INSTANCE = new SplitModule();

    @Override
    public int execute(int online, int selectedAmount) {
        return online / selectedAmount;
    }
}
