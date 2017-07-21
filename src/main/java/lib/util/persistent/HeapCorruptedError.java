package lib.util.persistent;

public class HeapCorruptedError extends Error {
    public HeapCorruptedError(String message) {
        super(message);
    }
    public HeapCorruptedError(String message, Throwable cause) {
        super(message, cause);
    }
    public HeapCorruptedError(Throwable cause) {
        super(cause);
    }
}
