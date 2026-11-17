package com.mathtutor.graphics;

import java.util.Set;
import java.util.regex.Pattern;

/** Analizador sintáctico sin evaluación, comandos ni multiplicación implícita. */
final class GraphicExpression {
    private static final Pattern NUMBER = Pattern.compile("(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");
    private static final Set<String> FUNCTIONS = Set.of("sin", "cos", "tan", "exp", "log", "sqrt", "abs");
    private final String input;
    private final boolean surface;
    private int position;
    private GraphicExpression(String input, boolean surface) { this.input = input; this.surface = surface; }
    static boolean valid(String input, boolean surface) {
        if (input == null || input.isBlank() || input.length() > 200) return false;
        var parser = new GraphicExpression(input.replaceAll("\\s", ""), surface);
        try { parser.expression(); return parser.position == parser.input.length(); }
        catch (IllegalArgumentException invalid) { return false; }
    }
    private void expression() {
        product();
        while (take('+') || take('-')) product();
    }
    private void product() {
        power();
        while (take('*') || take('/')) power();
    }
    private void power() {
        if (take('+') || take('-')) { power(); return; }
        atom();
        if (take('^')) power();
    }
    private void atom() {
        if (take('(')) { expression(); require(')'); return; }
        var matcher = NUMBER.matcher(input).region(position, input.length());
        if (matcher.lookingAt()) { position = matcher.end(); return; }
        int start = position;
        while (position < input.length() && Character.isLetter(input.charAt(position))) position++;
        String name = input.substring(start, position);
        if (name.equals("x") || (surface && name.equals("y"))) return;
        if (!FUNCTIONS.contains(name)) throw new IllegalArgumentException();
        require('('); expression(); require(')');
    }
    private boolean take(char expected) {
        if (position < input.length() && input.charAt(position) == expected) { position++; return true; }
        return false;
    }
    private void require(char expected) { if (!take(expected)) throw new IllegalArgumentException(); }
}
