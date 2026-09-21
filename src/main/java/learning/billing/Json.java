package learning.billing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private final String source;
    private int position;

    private Json(String source) { this.source = source; }

    static Object parse(String source) {
        Json parser = new Json(source);
        Object value = parser.value();
        parser.space();
        if (parser.position != source.length()) throw parser.bad("Trailing JSON content");
        return value;
    }

    static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) return quote(text);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (out.length() > 1) out.append(',');
                out.append(quote(entry.getKey().toString())).append(':').append(stringify(entry.getValue()));
            }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> values) {
            StringBuilder out = new StringBuilder("[");
            for (Object item : values) {
                if (out.length() > 1) out.append(',');
                out.append(stringify(item));
            }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    private Object value() {
        space();
        if (position >= source.length()) throw bad("Expected a value");
        return switch (source.charAt(position)) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", true);
            case 'f' -> literal("false", false);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        position++;
        Map<String, Object> result = new LinkedHashMap<>();
        space();
        if (take('}')) return result;
        do {
            space();
            String key = string();
            space();
            require(':');
            result.put(key, value());
            space();
        } while (take(','));
        require('}');
        return result;
    }

    private List<Object> array() {
        position++;
        List<Object> result = new ArrayList<>();
        space();
        if (take(']')) return result;
        do { result.add(value()); space(); } while (take(','));
        require(']');
        return result;
    }

    private String string() {
        require('"');
        StringBuilder out = new StringBuilder();
        while (position < source.length()) {
            char c = source.charAt(position++);
            if (c == '"') return out.toString();
            if (c != '\\') { out.append(c); continue; }
            if (position >= source.length()) throw bad("Incomplete escape");
            char escaped = source.charAt(position++);
            switch (escaped) {
                case '"', '\\', '/' -> out.append(escaped);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (position + 4 > source.length()) throw bad("Incomplete unicode escape");
                    out.append((char) Integer.parseInt(source.substring(position, position + 4), 16));
                    position += 4;
                }
                default -> throw bad("Invalid escape");
            }
        }
        throw bad("Unclosed string");
    }

    private Object number() {
        int start = position;
        while (position < source.length() && "-+0123456789.eE".indexOf(source.charAt(position)) >= 0) position++;
        String token = source.substring(start, position);
        try { return token.matches("-?\\d+") ? Long.parseLong(token) : Double.parseDouble(token); }
        catch (NumberFormatException ex) { throw bad("Invalid number"); }
    }

    private Object literal(String text, Object value) {
        if (!source.startsWith(text, position)) throw bad("Invalid literal");
        position += text.length();
        return value;
    }

    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c < 32 ? String.format("\\u%04x", (int) c) : c);
            }
        }
        return out.append('"').toString();
    }

    private void space() { while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++; }
    private boolean take(char expected) { if (position < source.length() && source.charAt(position) == expected) { position++; return true; } return false; }
    private void require(char expected) { if (!take(expected)) throw bad("Expected " + expected); }
    private IllegalArgumentException bad(String message) { return new IllegalArgumentException(message + " at " + position); }
}
