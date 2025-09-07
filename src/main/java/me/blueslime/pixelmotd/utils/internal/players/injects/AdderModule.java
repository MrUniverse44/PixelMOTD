package me.blueslime.pixelmotd.utils.internal.players.injects;

import me.blueslime.pixelmotd.utils.internal.players.PlayerModule;

public class AdderModule extends PlayerModule {

    public static final AdderModule INSTANCE = new AdderModule();

    @Override
    public int execute(int online, int selectedAmount) {
        return online + selectedAmount;
    }
}
