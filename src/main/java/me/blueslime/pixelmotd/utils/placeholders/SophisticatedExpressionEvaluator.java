package me.blueslime.pixelmotd.utils.placeholders;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.blueslime.slimelib.impls.Implements;
import me.blueslime.slimelib.logs.SlimeLogs;

/**
 * A simple and optimized expression evaluator for conditional logic.
 * Designed to process conditions from the events configuration with minimal overhead.
 */
public class SophisticatedExpressionEvaluator {

    private final String expression;
    private final Map<String, Long> variables;
    private final SlimeLogs logger;

    private static final Map<String, Integer> OPERATOR_PRECEDENCE = new HashMap<>();
    static {
        OPERATOR_PRECEDENCE.put("+", 1);
        OPERATOR_PRECEDENCE.put("-", 1);
        OPERATOR_PRECEDENCE.put("*", 2);
        OPERATOR_PRECEDENCE.put("/", 2);
    }

    public SophisticatedExpressionEvaluator(String expression, Map<String, Long> variables) {
        this.expression = expression;
        this.variables = variables;
        this.logger = Implements.fetch(SlimeLogs.class);
    }

    public boolean evaluate() {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        try {
            if ("DEFAULT".equalsIgnoreCase(expression.trim())) {
                return false;
            }

            Pattern pattern = Pattern.compile("(<=|>=|==|!=|<|>)");
            Matcher matcher = pattern.matcher(expression);

            if (matcher.find()) {
                String operator = matcher.group();
                String left = expression.substring(0, matcher.start()).trim();
                String right = expression.substring(matcher.end()).trim();

                long leftResult = evaluateArithmetic(left);
                long rightResult = evaluateArithmetic(right);

                return switch (operator) {
                    case "<" -> leftResult < rightResult;
                    case "<=" -> leftResult <= rightResult;
                    case "==" -> leftResult == rightResult;
                    case "!=" -> leftResult != rightResult;
                    case ">" -> leftResult > rightResult;
                    case ">=" -> leftResult >= rightResult;
                    default -> false;
                };
            }
        } catch (Exception e) {
            logger.error("Error evaluating condition: '" + expression + "'. " + e.getMessage(), e);
            return false;
        }

        return false;
    }

    private long evaluateArithmetic(String arithExpression) {
        if (arithExpression.isEmpty()) {
            throw new IllegalArgumentException("Arithmetic expression is empty");
        }

        if (arithExpression.contains("&&") || arithExpression.contains("||")) {
            throw new IllegalArgumentException("Logical operators are not allowed inside arithmetic expressions. Use them to chain conditions.");
        }

        arithExpression = arithExpression.replace(" ", "");

        Pattern tokenPattern = Pattern.compile("(\\(|\\)|[+\\-*/]|[0-9]+|%[a-zA-Z_]+%)");
        Matcher matcher = tokenPattern.matcher(arithExpression);

        Stack<String> operators = new Stack<>();
        Stack<Long> values = new Stack<>();

        while (matcher.find()) {
            String token = matcher.group();

            if (token.matches("[0-9]+")) {
                values.push(Long.parseLong(token));
            } else if (token.matches("%[a-zA-Z_]+%")) {
                String varName = token.replace("%", "");
                values.push(variables.getOrDefault(varName, 0L));
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

    private void applyOperator(String operator, Stack<Long> values) {
        if (values.size() < 2) {
            throw new IllegalArgumentException("Invalid expression: Insufficient values for operator " + operator);
        }
        long b = values.pop();
        long a = values.pop();

        switch (operator) {
            case "+": values.push(a + b); break;
            case "-": values.push(a - b); break;
            case "*": values.push(a * b); break;
            case "/":
                if (b == 0) {
                    throw new IllegalArgumentException("Division by zero");
                }
                values.push(a / b);
                break;
        }
    }
}