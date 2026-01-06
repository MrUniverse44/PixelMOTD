package me.blueslime.pixelmotd.motd;

import java.util.Locale;

public enum MoTDProtocol {
    ALWAYS_POSITIVE,
    ALWAYS_NEGATIVE(-1),
    DEFAULT;

    MoTDProtocol() {
        code = 0;
    }

    MoTDProtocol(int code) {
        this.code = code;
    }

    private int code;

    public MoTDProtocol setCode(int code) {
        this.code = code;
        return this;
    }

    public int getCode() {
        return code;
    }

    @Deprecated
    public static MoTDProtocol getFromText(String paramText, int code) {
        return fromString(paramText, code);
    }

    public static MoTDProtocol fromOther(MoTDProtocol protocol) {
        for (MoTDProtocol p : values()) {
            if (p == protocol) {
                return p;
            }
        }
        return protocol;
    }

    public static MoTDProtocol fromObject(Object object, int code) {
        if (object instanceof String) {
            return fromString(
                    (String)object,
                    code
            );
        }
        if (object instanceof Integer) {
            return fromInteger(
                    (int)object,
                    code
            );
        }
        return DEFAULT.setCode(code);
    }

    public static MoTDProtocol fromInteger(int parameter, int code) {
        return switch (parameter) {
            case 1 -> ALWAYS_POSITIVE.setCode(code);
            case 2 -> ALWAYS_NEGATIVE;
            default -> DEFAULT.setCode(code);
        };
    }

    public static MoTDProtocol fromString(String paramText, int code) {
        paramText = paramText.toLowerCase(Locale.ENGLISH);

        return switch (paramText) {
            case "always_negative", "negative", "2" -> ALWAYS_NEGATIVE;
            case "default", "0", "-1" -> DEFAULT.setCode(code);
            default -> ALWAYS_POSITIVE.setCode(code);
        };
    }
}
