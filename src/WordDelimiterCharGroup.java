public class WordDelimiterCharGroup implements CharGroup {
    @Override
    public boolean contains(char c) {
        return c != '\'' && Character.getType(c) != Character.DASH_PUNCTUATION && !Character.isLetter(c);
    }
}
