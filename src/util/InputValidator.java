package util;

public final class InputValidator {
    private InputValidator() { }
    public static void requireText(String value) { if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Name cannot be empty."); }
    public static void requirePositive(double value, String label) { if (value <= 0) throw new IllegalArgumentException(label + " must be greater than 0."); }
    public static void requireRange(double value, double min, double max, String label) { if (value < min || value > max) throw new IllegalArgumentException(label + " must be between " + min + " and " + max + "."); }
    public static void requireRange(int value, int min, int max, String label) { if (value < min || value > max) throw new IllegalArgumentException(label + " must be between " + min + " and " + max + "."); }
}
