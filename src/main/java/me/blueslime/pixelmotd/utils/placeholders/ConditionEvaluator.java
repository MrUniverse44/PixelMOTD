package me.blueslime.pixelmotd.utils.placeholders;

import me.blueslime.slimelib.impls.Implements;
import me.blueslime.slimelib.logs.SlimeLogs;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Evaluates a complex logical expression with parentheses, AND, and OR operators.
 * Uses a shunting-yard algorithm to handle operator precedence.
 */
public class ConditionEvaluator {

    private final String expression;
    private final Map<String, Object> variables;
    private final SlimeLogs logger;

    private static final ConcurrentMap<String, List<String>> TOKEN_CACHE = new ConcurrentHashMap<>();

    private static final java.util.Map<String, Integer> LOGICAL_PRECEDENCE = new java.util.HashMap<>();
    static {
        LOGICAL_PRECEDENCE.put("||", 1);
        LOGICAL_PRECEDENCE.put("&&", 2);
    }

    public ConditionEvaluator(String expression, Map<String, Object> variables) {
        this.expression = expression == null ? "" : expression.trim();
        this.variables = variables == null ? Map.of() : variables;
        this.logger = Implements.fetch(SlimeLogs.class);
    }

    public boolean evaluate() {
        if (expression.isEmpty()) {
            return false;
        }

        try {
            List<String> tokens = getTokensCached(expression);

            ArrayDeque<Boolean> values = new ArrayDeque<>();
            ArrayDeque<String> operators = new ArrayDeque<>();

            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i).trim();
                if (token.isEmpty()) continue;

                if (token.equals("(")) {
                    operators.addFirst(token);
                } else if (token.equals(")")) {
                    while (!operators.isEmpty() && !operators.peekFirst().equals("(")) {
                        applyLogicalOperator(operators.removeFirst(), values);
                    }
                    if (operators.isEmpty() || !operators.peekFirst().equals("(")) {
                        throw new IllegalArgumentException("Mismatched parentheses in condition: " + expression);
                    }
                    operators.removeFirst(); // pop "("
                } else if (LOGICAL_PRECEDENCE.containsKey(token)) {
                    while (!operators.isEmpty() && !operators.peekFirst().equals("(")
                            && LOGICAL_PRECEDENCE.getOrDefault(operators.peekFirst(), 0) >= LOGICAL_PRECEDENCE.get(token)) {
                        applyLogicalOperator(operators.removeFirst(), values);
                    }
                    operators.addFirst(token);
                } else {
                    StringBuilder sub = new StringBuilder(token);
                    while (i + 1 < tokens.size()) {
                        String next = tokens.get(i + 1);
                        if (next.equals("(") || next.equals(")") || LOGICAL_PRECEDENCE.containsKey(next)) {
                            break;
                        }
                        sub.append(' ').append(next);
                        i++;
                    }

                    SimpleExpressionEvaluator simple = new SimpleExpressionEvaluator(sub.toString(), variables);
                    values.addFirst(simple.evaluate());
                }
            }

            while (!operators.isEmpty()) {
                String op = operators.removeFirst();
                if (op.equals("(") || op.equals(")")) {
                    throw new IllegalArgumentException("Mismatched parentheses in condition: " + expression);
                }
                applyLogicalOperator(op, values);
            }

            if (values.size() != 1) {
                throw new IllegalArgumentException("Invalid condition format: " + expression);
            }

            return values.removeFirst();
        } catch (Exception e) {
            logger.error("Error evaluating condition: '" + expression + "'. " + e.getMessage(), e);
            return false;
        }
    }

    private void applyLogicalOperator(String operator, ArrayDeque<Boolean> values) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("Insufficient values for logical operator " + operator);
        }
        boolean b = values.removeFirst();
        boolean a = values.removeFirst();

        switch (operator) {
            case "&&" -> values.addFirst(a && b);
            case "||" -> values.addFirst(a || b);
            default -> throw new IllegalArgumentException("Unknown logical operator: " + operator);
        }
    }

    private List<String> getTokensCached(String expr) {
        return TOKEN_CACHE.computeIfAbsent(expr, ConditionEvaluator::tokenize);
    }

    private static List<String> tokenize(String expr) {
        List<String> result = new ArrayList<>();
        int len = expr.length();
        int i = 0;

        while (i < len) {
            char c = expr.charAt(i);

            if (Character.isWhitespace(c)) {
                i++; continue;
            }

            if (c == '(' || c == ')') {
                result.add(String.valueOf(c));
                i++; continue;
            }

            if (c == '<') {
                int j = i + 1;
                while (j < len && expr.charAt(j) != '>') j++;
                if (j < len && expr.charAt(j) == '>') {
                    result.add(expr.substring(i, j + 1));
                    i = j + 1;
                } else {
                    result.add(expr.substring(i));
                    i = len;
                }
                continue;
            }

            if (c == '\'' || c == '"') {
                char quote = c;
                int j = i + 1;
                while (j < len && expr.charAt(j) != quote) j++;
                if (j < len && expr.charAt(j) == quote) {
                    result.add(expr.substring(i, j + 1));
                    i = j + 1;
                } else {
                    // no cerró la comilla
                    result.add(expr.substring(i));
                    i = len;
                }
                continue;
            }

            if (i + 1 < len) {
                String two = expr.substring(i, i + 2);
                if (two.equals("&&") || two.equals("||") || two.equals("==") || two.equals("!=") || two.equals("<=") || two.equals(">=")) {
                    result.add(two);
                    i += 2;
                    continue;
                }
            }

            if (c == '<' || c == '>' || c == '+' || c == '-' || c == '*' || c == '/' || c == '=' || c == '!') {
                result.add(String.valueOf(c));
                i++; continue;
            }

            if (Character.isDigit(c)) {
                int j = i;
                while (j < len && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) j++;
                result.add(expr.substring(i, j));
                i = j;
                continue;
            }

            if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < len && (Character.isLetterOrDigit(expr.charAt(j)) || expr.charAt(j) == '_')) j++;
                result.add(expr.substring(i, j));
                i = j;
                continue;
            }

            result.add(String.valueOf(c));
            i++;
        }

        return result;
    }
}
