package me.blueslime.pixelmotd.utils.internal.players;

import me.blueslime.slimelib.impls.Implements;
import me.blueslime.slimelib.logs.SlimeLogs;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public abstract class PlayerModule {

    public abstract int execute(int amount, int selectedValue);

    public int execute(int online, Object values) {
        SlimeLogs logs = Implements.fetch(SlimeLogs.class);
        List<Integer> amounts = new ArrayList<>();
        if (values instanceof String stringAmount) {
            for (String amount : stringAmount.split(";")) {
                try {
                    int intAmount = Integer.parseInt(amount.replace(" ", ""));
                    amounts.add(intAmount);
                } catch (NumberFormatException e) {
                    logs.error(amount + " is not a number. Check your motds.yml in online-max player sections to fix this issue.");
                }
            }
        } else if (values instanceof Integer[] integerArray) {
            amounts.addAll(Arrays.asList(integerArray));
        } else if (values instanceof Set<?> setValues) {
            for (Object value : setValues) {
                try {
                    int intAmount = Integer.parseInt(value.toString().replace(" ", ""));
                    amounts.add(intAmount);
                } catch (NumberFormatException e) {
                    logs.error(value + " is not a number. Check your motds.yml in online-max player sections to fix this issue.");
                }
            }
        } else if (values instanceof List<?> listValues) {
            for (Object value : listValues) {
                try {
                    int intAmount = Integer.parseInt(value.toString().replace(" ", ""));
                    amounts.add(intAmount);
                }  catch (NumberFormatException e) {
                    logs.error(value + " is not a number. Check your motds.yml in online-max player sections to fix this issue.");
                }
            }
        }
        return execute(
            online,
            generateRandomParameter(amounts)
        );
    }

    public static int generateRandomParameter(List<Integer> valueList) {
        if (valueList.isEmpty()) {
            return 0;
        }
        if (valueList.size() == 1) {
            return valueList.getFirst();
        }

        Random random = ThreadLocalRandom.current();

        return valueList.get(
                random.nextInt(
                        valueList.size()
                )
        );
    }

}
