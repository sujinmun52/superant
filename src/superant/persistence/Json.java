package superant.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 기록 파일에 필요한 만큼만 구현한 작은 JSON 읽기/쓰기 도우미.
 * 객체는 Map, 배열은 List, 숫자와 문자열은 String, 참/거짓은 Boolean 으로 다룬다.
 */
final class Json {
    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    /**
     * JSON 문자열을 읽는다.
     *
     * @param text JSON 문자열
     * @return Map, List, String, Boolean 또는 null
     * @throws IllegalArgumentException 형식이 잘못되었을 때
     */
    static Object parse(String text) {
        Json p = new Json(text);
        p.skip();
        Object v = p.value();
        p.skip();
        if (p.pos != text.length()) {
            throw p.error("끝에 불필요한 문자");
        }
        return v;
    }

    private Object value() {
        if (pos >= text.length()) {
            throw error("값이 없음");
        }
        char c = text.charAt(pos);
        if (c == '{') {
            return object();
        }
        if (c == '[') {
            return array();
        }
        if (c == '"') {
            return string();
        }
        if (text.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        if (text.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        if (start == pos) {
            throw error("알 수 없는 문자 '" + c + "'");
        }
        return text.substring(start, pos);
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skip();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skip();
            String key = string();
            skip();
            expect(':');
            skip();
            map.put(key, value());
            skip();
            if (peek() == ',') {
                pos++;
                continue;
            }
            expect('}');
            return map;
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        pos++;
        skip();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skip();
            list.add(value());
            skip();
            if (peek() == ',') {
                pos++;
                continue;
            }
            expect(']');
            return list;
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < text.length()) {
            char c = text.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                char e = text.charAt(pos++);
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                        pos += 4;
                        break;
                    default: sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
        throw error("문자열이 닫히지 않음");
    }

    private char peek() {
        if (pos >= text.length()) {
            throw error("예상보다 일찍 끝남");
        }
        return text.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw error("'" + c + "' 가 필요함");
        }
        pos++;
    }

    private void skip() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("JSON 형식 오류(" + pos + "): " + msg);
    }

    /**
     * 문자열을 JSON 문자열 리터럴로 바꾼다.
     *
     * @param s 원본 문자열
     * @return 따옴표로 감싼 문자열
     */
    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
