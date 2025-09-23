package me.blueslime.pixelmotd.utils.placeholders;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            Object leftResult = parseToken(left);
            Object rightResult = parseToken(right);

            return compare(leftResult, rightResult, operator);
        } else {
            Object result = parseToken(expression);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
        }
        return false;
    }

    private Object parseToken(String token) {
        token = token.trim();
        if (token.matches("true|false")) {
            return Boolean.parseBoolean(token);
        } else if (token.matches("'.*?'|\".*?\"")) {
            return token.substring(1, token.length() - 1);
        } else if (token.matches("-?\\d+")) {
            return Long.parseLong(token);
        } else if (token.matches("<[a-zA-Z_]+>")) {
            String varName = token.substring(1, token.length() - 1);
            Object value = variables.get(varName);
            if (value != null) {
                return value;
            }
            throw new IllegalArgumentException("Undefined variable: <" + varName + ">");
        } else if (hasArithmetic(token)) {
            return evaluateArithmetic(token);
        }
        throw new IllegalArgumentException("Invalid token: " + token);
    }

    private boolean hasArithmetic(String expression) {
        return expression.contains("+") || expression.contains("-") || expression.contains("*") || expression.contains("/");
    }

    private Object evaluateArithmetic(String arithExpression) {
        arithExpression = arithExpression.replace(" ", "");

        Stack<String> operators = new Stack<>();
        Stack<Object> values = new Stack<>();

        Pattern tokenPattern = Pattern.compile("(\\(|\\)|[+\\-*/]|[0-9]+|%[a-zA-Z_]+%)");
        Matcher matcher = tokenPattern.matcher(arithExpression);

        while (matcher.find()) {
            String token = matcher.group();

            if (token.matches("[0-9]+")) {
                values.push(Long.parseLong(token));
            } else if (token.matches("<[a-zA-Z_]+>")) {
                String varName = token.substring(1, token.length() - 1);
                Object value = variables.get(varName);
                if (value != null) {
                    values.push(value);
                } else {
                    throw new IllegalArgumentException("Undefined variable: <" + varName + ">");
                }
            } else if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    applyOperator(operators.pop(), values);
                }
                if (operators.isEmpty() || !operators.peek().equals("(")) {
                    throw new IllegalArgumentException("Mismatched parentheses in expression: " + arithExpression);
                }
                operators.pop();
            } else if (OPERATOR_PRECEDENCE.containsKey(token)) {
                while (!operators.isEmpty() && OPERATOR_PRECEDENCE.getOrDefault(operators.peek(), 0) >= OPERATOR_PRECEDENCE.get(token)) {
                    applyOperator(operators.pop(), values);
                }
                operators.push(token);
            } else {
                throw new IllegalArgumentException("Invalid token in expression: " + token);
            }
        }

        while (!operators.isEmpty()) {
            if (operators.peek().equals("(") || operators.peek().equals(")")) {
                throw new IllegalArgumentException("Mismatched parentheses in expression: " + arithExpression);
            }
            applyOperator(operators.pop(), values);
        }

        if (values.size() != 1) {
            throw new IllegalArgumentException("Invalid arithmetic expression format: " + arithExpression);
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
        } else if (left instanceof String && right instanceof Long || left instanceof Long && right instanceof String) {
            throw new IllegalArgumentException("Cannot compare String with Long.");
        }

        throw new IllegalArgumentException("Cannot compare mismatched types: " + left.getClass().getSimpleName() + " and " + right.getClass().getSimpleName());
    }
}