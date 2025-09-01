package me.blueslime.pixelmotd.utils.placeholders;

import java.util.Map;
import java.util.Stack;

/**
 * A simple and optimized expression evaluator for conditional logic.
 * Designed to process conditions from the events configuration with minimal overhead.
 */
public class ExpressionEvaluator {

    private final String expression;
    private final Map<String, Long> variables;

    public ExpressionEvaluator(String expression, Map<String, Long> variables) {
        this.expression = expression;
        this.variables = variables;
    }

    public boolean evaluate() {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        String[] tokens = expression.replace("AND", "&&").replace("OR","||").split("\\s+|(?<=[><=!&|])|(?=[><=!&|])");

        Stack<Boolean> values = new Stack<>();
        Stack<String> operators = new Stack<>();

        // This is a simplified evaluator that processes tokens sequentially
        // based on the expected structure of your conditions.
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) continue;

            if (isLogicalOperator(token)) {
                operators.push(token);
            } else {
                long value1;
                try {
                    value1 = Long.parseLong(token);
                } catch (NumberFormatException e) {
                    value1 = variables.getOrDefault(token.replace("%", ""), 0L);
                }

                if (i + 2 < tokens.length) {
                    String operator = tokens[i + 1].trim();
                    String value2Token = tokens[i + 2].trim();
                    long value2;
                    try {
                        value2 = Long.parseLong(value2Token);
                    } catch (NumberFormatException e) {
                        value2 = variables.getOrDefault(value2Token.replace("%", ""), 0L);
                    }

                    boolean result = switch (operator) {
                        case ">=" -> value1 >= value2;
                        case "<=" -> value1 <= value2;
                        case ">" -> value1 > value2;
                        case "<" -> value1 < value2;
                        case "==" -> value1 == value2;
                        case "!=" -> value1 != value2;
                        default -> false;
                    };
                    values.push(result);
                    i += 2;
                }
            }
        }

        if (values.isEmpty()) return false;

        boolean finalResult = values.pop();
        while (!operators.isEmpty()) {
            String operator = operators.pop();
            boolean nextValue = values.pop();
            if ("&&".equals(operator)) {
                finalResult = finalResult && nextValue;
            } else if ("||".equals(operator)) {
                finalResult = finalResult || nextValue;
            }
        }
        return finalResult;
    }

    private boolean isLogicalOperator(String token) {
        return token.equals("&&") || token.equals("||");
    }
}
