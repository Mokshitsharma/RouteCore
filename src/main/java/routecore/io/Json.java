package routecore.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A ~120-line recursive-descent JSON reader, so the project has <b>zero runtime
 * dependencies</b> even for loading data files.
 *
 * <p>Supports the full JSON grammar for reading: objects, arrays, strings (with
 * {@code \" \\ \/ \b \f \n \r \t \\uXXXX} escapes), numbers (parsed to
 * {@code Double}), {@code true}/{@code false}/{@code null}. Not a validator and not
 * a writer — just enough to parse {@code data/*.json}.
 *
 * <p>Returned Java types: {@code Map<String,Object>}, {@code List<Object>},
 * {@code String}, {@code Double}, {@code Boolean}, {@code null}.
 */
public final class Json {

    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        Json p = new Json(text);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) {
            throw p.err("Trailing content after JSON value");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object at top level");
        }
        return (Map<String, Object>) v;
    }

    private Object value() {
        char c = peek();
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': expect("true"); return Boolean.TRUE;
            case 'f': expect("false"); return Boolean.FALSE;
            case 'n': expect("null"); return null;
            default:
                if (c == '-' || (c >= '0' && c <= '9')) return number();
                throw err("Unexpected character '" + c + "'");
        }
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; // {
        ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws();
            String key = string();
            ws();
            if (s.charAt(i++) != ':') throw err("Expected ':' in object");
            ws();
            m.put(key, value());
            ws();
            char c = s.charAt(i++);
            if (c == ',') continue;
            if (c == '}') return m;
            throw err("Expected ',' or '}' in object");
        }
    }

    private List<Object> array() {
        List<Object> a = new ArrayList<>();
        i++; // [
        ws();
        if (peek() == ']') { i++; return a; }
        while (true) {
            ws();
            a.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ',') continue;
            if (c == ']') return a;
            throw err("Expected ',' or ']' in array");
        }
    }

    private String string() {
        if (s.charAt(i++) != '"') throw err("Expected string");
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: throw err("Bad escape '\\" + e + "'");
                }
            } else {
                sb.append(c);
            }
        }
    }

    private Double number() {
        int start = i;
        if (peek() == '-') i++;
        while (i < s.length() && "0123456789+-.eE".indexOf(s.charAt(i)) >= 0) i++;
        return Double.parseDouble(s.substring(start, i));
    }

    private void expect(String lit) {
        if (!s.startsWith(lit, i)) throw err("Expected '" + lit + "'");
        i += lit.length();
    }

    private char peek() {
        if (i >= s.length()) throw err("Unexpected end of input");
        return s.charAt(i);
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
            else break;
        }
    }

    private RuntimeException err(String msg) {
        return new IllegalArgumentException("JSON parse error at index " + i + ": " + msg);
    }

    // --- small typed accessors for callers ---------------------------------

    public static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v == null) throw new IllegalArgumentException("Missing string field '" + k + "'");
        return v.toString();
    }

    public static double num(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (!(v instanceof Number)) throw new IllegalArgumentException("Missing numeric field '" + k + "'");
        return ((Number) v).doubleValue();
    }

    public static double numOr(Map<String, Object> m, String k, double dflt) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : dflt;
    }

    public static boolean boolOr(Map<String, Object> m, String k, boolean dflt) {
        Object v = m.get(k);
        return v instanceof Boolean ? (Boolean) v : dflt;
    }
}
