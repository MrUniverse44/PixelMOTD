package me.blueslime.pixelmotd.utils.placeholders;

import me.blueslime.slimelib.impls.Implements;
import me.blueslime.slimelib.logs.SlimeLogs;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates a complex logical expression with parentheses, AND, and OR operators.
 * Uses a shunting-yard algorithm to handle operator precedence.
 */
public class ConditionEvaluator {

    private final String expression;
    private final Map<String, Object> variables;
    private final SlimeLogs logger;

    private static final Map<String, Integer> LOGICAL_PRECEDENCE = new HashMap<>();
    static {
        LOGICAL_PRECEDENCE.put("||", 1);
        LOGICAL_PRECEDENCE.put("&&", 2);
    }

    public ConditionEvaluator(String expression, Map<String, Object> variables) {
        this.expression = expression.trim();
        this.variables = variables;
        this.logger = Implements.fetch(SlimeLogs.class);
    }

    public boolean evaluate() {
        if (expression.isEmpty()) {
            return false;
        }

        try {
            Stack<Boolean> values = new Stack<>();
            Stack<String> operators = new Stack<>();

            // Pattern to match a full comparison, logical operators, and parentheses.
            // This is the core of the shunting-yard algorithm.
            Pattern tokenPattern = Pattern.compile("(\\(|\\)|&&|\\|\\||[^()&|]+)");
            Matcher matcher = tokenPattern.matcher(expression);

            while (matcher.find()) {
                String token = matcher.group().trim();

                if (token.isEmpty()) {
                    continue;
                }

                if (token.equals("(")) {
                    operators.push(token);
                } else if (token.equals(")")) {
                    while (!operators.isEmpty() && !operators.peek().equals("(")) {
                        applyLogicalOperator(operators.pop(), values);
                    }
                    if (operators.isEmpty() || !operators.peek().equals("(")) {
                        throw new IllegalArgumentException("Mismatched parentheses in condition.");
                    }
                    operators.pop();
                } else if (LOGICAL_PRECEDENCE.containsKey(token)) {
                    while (!operators.isEmpty() && LOGICAL_PRECEDENCE.getOrDefault(operators.peek(), 0) >= LOGICAL_PRECEDENCE.get(token)) {
                        applyLogicalOperator(operators.pop(), values);
                    }
                    operators.push(token);
                } else {
                    // This is a simple comparison, evaluate it.
                    SimpleExpressionEvaluator simpleEvaluator = new SimpleExpressionEvaluator(token, variables);
                    values.push(simpleEvaluator.evaluate());
                }
            }

            while (!operators.isEmpty()) {
                if (operators.peek().equals("(") || operators.peek().equals(")")) {
                    throw new IllegalArgumentException("Mismatched parentheses in condition.");
                }
                applyLogicalOperator(operators.pop(), values);
            }

            if (values.size() != 1) {
                throw new IllegalArgumentException("Invalid condition format.");
            }

            return values.pop();

        } catch (Exception e) {
            logger.error("Error evaluating condition: '" + expression + "'. " + e.getMessage(), e);
            return false;
        }
    }

    private void applyLogicalOperator(String operator, Stack<Boolean> values) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("Insufficient values for logical operator " + operator);
        }
        boolean b = values.pop();
        boolean a = values.pop();

        switch (operator) {
            case "&&": values.push(a && b); break;
            case "||": values.push(a || b); break;
        }
    }
}
