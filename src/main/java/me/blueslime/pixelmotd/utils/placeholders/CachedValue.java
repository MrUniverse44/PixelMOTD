package me.blueslime.pixelmotd.utils.placeholders;

public class CachedValue {
    private final String value;
    private final long expirationTime;

    public CachedValue(String value, long expirationTime) {
        this.value = value;
        this.expirationTime = expirationTime;
    }

    public CachedValue(int value, long expirationTime) {
        this.value = String.valueOf(value);
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public String getValue() {
        return value;
    }

    public int asIntValue() {
        return Integer.parseInt(value);
    }
}
