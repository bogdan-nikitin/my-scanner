public class WhitespaceCharGroup implements CharGroup {
    public boolean contains(char c) {
        return Character.isWhitespace(c);
    }
}
