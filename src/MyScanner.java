import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;


public class MyScanner {
    private static final int NO_TOKEN = -1;
    private static final int TEXT_TOKEN = 0;
    private static final int INT_TOKEN = 1;
    private static final int NO_VALUE = -1;
    public static final CharGroup STANDARD_DELIMITER_GROUP = new WhitespaceCharGroup();
    private static final CharGroup NEWLINE_GROUP = new NewlineCharGroup();

    private static final int MIN_BUFFER_SIZE = 4;
    private static final int BUFFER_EXTENSION_MULTIPLIER = 2;

    private Reader reader;
    private char[] buffer;
    private int bufferPosition = 0;
    private int bufferLimit = 0;
    private int scanOffset = 0;
    private int lineNumber = 0;
    private boolean crFlag = false;
    private int savedLineNumber = 0;
    private boolean savedCrFlag = false;
    private int lastTokenStart = NO_VALUE;
    private int lastTokenLength = NO_VALUE;
    private int lastTokenType = NO_TOKEN;
    private boolean isOpened = false;
    private IOException ioException = null;
    private CharGroup charGroup = STANDARD_DELIMITER_GROUP;

    private MyScanner() {
        buffer = new char[MIN_BUFFER_SIZE];
    }

    public MyScanner(Reader reader) {
        this();
        this.reader = reader;
        isOpened = true;
    }

    public MyScanner(InputStream in) {
        this(new InputStreamReader(in));
    }

    public MyScanner(String s) {
        this(new StringReader(s));
    }

    public IOException ioException() {
        return ioException;
    }

    public boolean hasNext() {
        if (lastTokenType != NO_TOKEN) {
            return true;
        }
        saveLineState();
        scanTillEdge(charGroup, false, true);
        if (bufferPosition + scanOffset != bufferLimit) {
            lastTokenStart = scanOffset;
            lastTokenType = TEXT_TOKEN;
            scanOffset = 0;
            return true;
        }
        return false;
    }

    public String next() {
        return nextToken(TEXT_TOKEN, false);
    }

    public boolean hasNextLine() {
        readIfEnd();
        return bufferPosition < bufferLimit;
    }

    private static boolean isInt(String token) {
        try {
            Integer.parseInt(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean hasNextInt() {
        String token = nextToken(INT_TOKEN, true);
        if (token != null && isInt(token)) {
            lastTokenType = INT_TOKEN;
            return true;
        }
        return false;
    }

    public int nextInt() {
        String token = nextToken(INT_TOKEN, false);
        return Integer.parseInt(token);
    }

    private void handleCrLf() {
        if (buffer[bufferPosition - 1] == '\r') {
            readIfEnd();
            crFlag = true;
            if (bufferPosition < bufferLimit && buffer[bufferPosition] == '\n') {
                bufferPosition++;
                crFlag = false;
            }
        }
    }

    public String nextLine() {
        scanTillEdge(NEWLINE_GROUP, true, true);
        if (bufferPosition >= bufferLimit) {
            return null;
        }
        String line = new String(buffer, bufferPosition, scanOffset);
        bufferPosition += scanOffset + 1;
        scanOffset = 0;
        handleCrLf();
        lineNumber++;
        saveLineState();
        return line;
    }

    public void close() {
        if (isOpened) {
            try {
                this.reader.close();
            } catch (IOException e) {
                ioException = e;
            }
            isOpened = false;
        }
    }

    public void useDelimiter(CharGroup charGroup) {
        this.charGroup = charGroup;
        lastTokenType = NO_TOKEN;
        restoreLineState();
    }

    public int getLineNumber() {
        return savedLineNumber;
    }

    private void readInput() {
        if (buffer.length - bufferPosition - scanOffset < MIN_BUFFER_SIZE) {
            extendBuffer();
        }
        try {
            int read = reader.read(buffer, bufferPosition + scanOffset,
                    buffer.length - bufferPosition - scanOffset);
            if (read > 0) {
                bufferLimit += read;
            }
        } catch (IOException e) {
            ioException = e;
        }
    }

    private void extendBuffer() {
        char[] newBuffer;
        if (buffer.length - bufferPosition - scanOffset < MIN_BUFFER_SIZE) {
            newBuffer = new char[buffer.length * BUFFER_EXTENSION_MULTIPLIER];
        } else if (buffer.length - bufferPosition >= MIN_BUFFER_SIZE) {
            newBuffer = new char[buffer.length - bufferPosition];
        } else {
            newBuffer = buffer;
        }
        System.arraycopy(buffer, bufferPosition, newBuffer, 0, buffer.length - bufferPosition);
        buffer = newBuffer;
        bufferLimit -= bufferPosition;
        bufferPosition = 0;
    }

    private static boolean isDelimiterChar(final char c, CharGroup delimiters) {
        return delimiters.contains(c);
    }

    private void readIfEnd() {
        if (bufferPosition + scanOffset == bufferLimit) {
            readInput();
        }
    }

    private void scanTillEdge(CharGroup charGroup, boolean inToken, boolean savePosition) {
        while (true) {
            readIfEnd();
            final int pos = bufferPosition + scanOffset;
            if (pos >= bufferLimit || (!(inToken ^ isDelimiterChar(buffer[pos], charGroup)))) {
                break;
            }

            if (NEWLINE_GROUP.contains(buffer[pos])) {
                if (!crFlag || buffer[pos] != '\n') {
                    lineNumber++;
                }
            }
            crFlag = buffer[pos] == '\r';
            if (savePosition) {
                scanOffset++;
            } else {
                bufferPosition++;
            }
        }
    }

    private String nextToken(int tokenType, boolean savePosition) {
        if (tokenType == lastTokenType) {
            if (savePosition) {
                scanOffset = lastTokenStart;
            } else {
                bufferPosition += lastTokenStart;
                lastTokenType = NO_TOKEN;
            }
        } else {
            scanTillEdge(charGroup, false, savePosition);
        }
        int tokenStart;
        int tokenLength;
        if (savePosition) {
            tokenStart = scanOffset;
            lastTokenStart = scanOffset;

            if (lastTokenLength == NO_VALUE) {
                scanTillEdge(charGroup, true, true);
                tokenLength = scanOffset - tokenStart;
            } else {
                tokenLength = lastTokenLength;
            }
            tokenStart += bufferPosition;
        } else {
            if (lastTokenLength == NO_VALUE) {
                scanTillEdge(charGroup, true, true);
                tokenLength = scanOffset;
            } else {
                tokenLength = lastTokenLength;
                lastTokenLength = NO_VALUE;
            }
            tokenStart = bufferPosition;
            bufferPosition += tokenLength;
            saveLineState();
        }
        scanOffset = 0;
        if (tokenLength == 0) {
            return null;
        }
        return new String(buffer, tokenStart, tokenLength);
    }

    private void saveLineState() {
        savedCrFlag = crFlag;
        savedLineNumber = lineNumber;
    }

    private void restoreLineState() {
        crFlag = savedCrFlag;
        lineNumber = savedLineNumber;
    }
}
