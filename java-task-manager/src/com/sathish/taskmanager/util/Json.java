package com.sathish.taskmanager.util;

import com.sathish.taskmanager.exception.ValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A very small JSON helper (no external libraries needed).
 * - toJson: turns a Map or List into JSON text
 * - parseObject: reads a flat JSON object such as {"title":"Buy milk","priority":"HIGH"}
 */
public final class Json {
    private Json() { }

    // ---------- writing ----------
    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    private static void write(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof List<?> list) {
            sb.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                write(list.get(i), sb);
            }
            sb.append(']');
        } else {
            writeString(value.toString(), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
    }

    // ---------- reading (flat objects only) ----------
    public static Map<String, String> parseObject(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        if (text == null) throw new ValidationException("Request body is empty");
        String s = text.trim();
        if (s.length() < 2 || s.charAt(0) != '{' || s.charAt(s.length() - 1) != '}') {
            throw new ValidationException("Body must be a JSON object");
        }
        int[] pos = {1};
        skipSpaces(s, pos);
        if (s.charAt(pos[0]) == '}') return result;       // empty object
        while (true) {
            skipSpaces(s, pos);
            String key = readString(s, pos);
            skipSpaces(s, pos);
            expect(s, pos, ':');
            skipSpaces(s, pos);
            result.put(key, readValue(s, pos));
            skipSpaces(s, pos);
            if (pos[0] >= s.length()) throw new ValidationException("Invalid JSON: unexpected end");
            char c = s.charAt(pos[0]++);
            if (c == '}') break;
            if (c != ',') throw new ValidationException("Invalid JSON near position " + pos[0]);
        }
        return result;
    }

    private static void skipSpaces(String s, int[] pos) {
        while (pos[0] < s.length() && Character.isWhitespace(s.charAt(pos[0]))) pos[0]++;
    }

    private static void expect(String s, int[] pos, char c) {
        if (pos[0] >= s.length() || s.charAt(pos[0]) != c) {
            throw new ValidationException("Invalid JSON: expected '" + c + "'");
        }
        pos[0]++;
    }

    private static String readString(String s, int[] pos) {
        expect(s, pos, '"');
        StringBuilder sb = new StringBuilder();
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                if (pos[0] >= s.length()) break;
                char n = s.charAt(pos[0]++);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        throw new ValidationException("Invalid JSON: string is not closed");
    }

    /** Reads a string, number, true, false or null and returns it as text (null stays null). */
    private static String readValue(String s, int[] pos) {
        if (pos[0] >= s.length()) throw new ValidationException("Invalid JSON: missing value");
        if (s.charAt(pos[0]) == '"') return readString(s, pos);
        int start = pos[0];
        while (pos[0] < s.length() && ",}".indexOf(s.charAt(pos[0])) < 0) pos[0]++;
        String raw = s.substring(start, pos[0]).trim();
        return raw.equals("null") ? null : raw;
    }
}
