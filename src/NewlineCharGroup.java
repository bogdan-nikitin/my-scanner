public class NewlineCharGroup implements CharGroup {
    public boolean contains(char c) {
        return c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029' || c == '\u0085';
    }
}
