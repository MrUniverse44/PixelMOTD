package me.blueslime.pixelmotd.utils.placeholders;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates a single boolean expression with arithmetic and parentheses.
 * E.g., "(5 * 2) > 9" or "client_protocol >= (735 - 1)".
 */
public class SimpleExpressionEvaluator {

    private final String expression;
    private final Map<String, Object> variables;

    private static final Map<String, Integer> OPERATOR_PRECEDENCE = new HashMap<>();
    static {
        OPERATOR_PRECEDENCE.put("+", 1);
        OPERATOR_PRECEDENCE.put("-", 1);
        OPERATOR_PRECEDENCE.put("*", 2);
        OPERATOR_PRECEDENCE.put("/", 2);
    }

    public SimpleExpressionEvaluator(String expression, Map<String, Object> variables) {
        this.expression = expression.trim();
        this.variables = variables;
    }

    public boolean evaluate() {
        if (expression.isEmpty()) {
            return false;
        }

        Pattern pattern = Pattern.compile("(<=|>=|==|!=|<|>)");
        Matcher matcher = pattern.matcher(expression);

        if (matcher.find()) {
            String operator = matcher.group();
            String left = expression.substring(0, matcher.start()).trim();
            String right = expression.substring(matcher.end()).trim();

            Object leftResult = evaluateArithmetic(left);
            Object rightResult = evaluateArithmetic(right);

            return compare(leftResult, rightResult, operator);
        } else {
            Object result = evaluateArithmetic(expression);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }

        return false;
    }

    private Object evaluateArithmetic(String arithExpression) {
        arithExpression = arithExpression.replace(" ", "");

        Stack<String> operators = new Stack<>();
        Stack<Object> values = new Stack<>();

        // Pattern to match numbers, variables, and operators.
        Pattern tokenPattern = Pattern.compile("(\\(|\\)|[+\\-*/]|true|false|\\d+|'.*?'|\".*?\"|<[a-zA-Z_]+>)");
        Matcher matcher = tokenPattern.matcher(arithExpression);

        while (matcher.find()) {
            String token = matcher.group();

            if (token.matches("\\d+")) {
                values.push(Long.parseLong(token));
            } else if (token.matches("true|false")) {
                values.push(Boolean.parseBoolean(token));
            } else if (token.matches("'.*?'|\".*?\"")) {
                values.push(token.substring(1, token.length() - 1));
            } else if (token.matches("<[a-zA-Z_]+>")) {
                String varName = token.substring(1, token.length() - 1);
                values.push(variables.getOrDefault(varName, 0L));
            } else if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    applyOperator(operators.pop(), values);
                }
                if (operators.isEmpty() || !operators.peek().equals("(")) {
                    throw new IllegalArgumentException("Mismatched parentheses.");
                }
                operators.pop();
            } else if (OPERATOR_PRECEDENCE.containsKey(token)) {
                while (!operators.isEmpty() && OPERATOR_PRECEDENCE.getOrDefault(operators.peek(), 0) >= OPERATOR_PRECEDENCE.get(token)) {
                    applyOperator(operators.pop(), values);
                }
                operators.push(token);
            } else {
                throw new IllegalArgumentException("Invalid token: " + token);
            }
        }

        while (!operators.isEmpty()) {
            applyOperator(operators.pop(), values);
        }

        if (values.size() != 1) {
            throw new IllegalArgumentException("Invalid expression format.");
        }

        return values.pop();
    }

    private void applyOperator(String operator, Stack<Object> values) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("Insufficient values for operator " + operator);
        }
        Object b = values.pop();
        Object a = values.pop();

        if (a instanceof Long && b instanceof Long) {
            long valB = (Long) b;
            long valA = (Long) a;
            switch (operator) {
                case "+": values.push(valA + valB); return;
                case "-": values.push(valA - valB); return;
                case "*": values.push(valA * valB); return;
                case "/":
                    if (valB == 0) {
                        throw new IllegalArgumentException("Division by zero");
                    }
                    values.push(valA / valB); return;
            }
        }
        throw new IllegalArgumentException("Invalid operation between types: " + a.getClass().getSimpleName() + " and " + b.getClass().getSimpleName());
    }

    private boolean compare(Object left, Object right, String operator) {
        if (left instanceof Long && right instanceof Long) {
            long l = (Long) left;
            long r = (Long) right;
            return switch (operator) {
                case "<" -> l < r;
                case "<=" -> l <= r;
                case "==" -> l == r;
                case "!=" -> l != r;
                case ">" -> l > r;
                case ">=" -> l >= r;
                default -> false;
            };
        } else if (left instanceof String l && right instanceof String r) {
            return switch (operator) {
                case "==" -> l.equalsIgnoreCase(r);
                case "!=" -> !l.equalsIgnoreCase(r);
                default -> false;
            };
        } else if (left instanceof Boolean && right instanceof Boolean) {
            boolean l = (Boolean) left;
            boolean r = (Boolean) right;
            return switch (operator) {
                case "==" -> l == r;
                case "!=" -> l != r;
                default -> false;
            };
        }
        throw new IllegalArgumentException("Cannot compare mismatched types: " + left.getClass().getSimpleName() + " and " + right.getClass().getSimpleName());
    }
}
