package me.blueslime.pixelmotd.motd.setup;

public class MotdSetup {
    private final boolean userIsBlacklisted;
    private final String domain;
    private final String user;
    private final int code;

    public MotdSetup(boolean userIsBlacklisted, String domain, String user, int code) {
        this.userIsBlacklisted = userIsBlacklisted;
        this.domain = domain;
        this.user = user;
        this.code = code;
    }


    public boolean isUserBlacklisted() {
        return userIsBlacklisted;
    }

    public String getDomain() {
        return domain;
    }

    public String getUser() {
        return user;
    }

    public int getCode() {
        return code;
    }
}
