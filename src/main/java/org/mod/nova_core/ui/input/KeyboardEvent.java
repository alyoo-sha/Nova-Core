package org.mod.nova_core.ui.input;

// События клавиатуры: нажатие, отпускание, ввод символа / Keyboard events: key down, key up, typed character
public final class KeyboardEvent extends UiEvent {
    private final int key;
    private final int scanCode;
    private final int modifiers;
    private final char character;

    private KeyboardEvent(EventType<KeyboardEvent> type, int key, int scanCode, int modifiers, char character) {
        super(type, true);
        this.key = key;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
        this.character = character;
    }

    public static KeyboardEvent down(int key, int scanCode, int modifiers) {
        return new KeyboardEvent(EventType.KEY_DOWN, key, scanCode, modifiers, '\0');
    }

    public static KeyboardEvent up(int key, int scanCode, int modifiers) {
        return new KeyboardEvent(EventType.KEY_UP, key, scanCode, modifiers, '\0');
    }

    public static KeyboardEvent typed(char character, int modifiers) {
        return new KeyboardEvent(EventType.CHAR, 0, 0, modifiers, character);
    }

    public int getKey() { return key; }
    public int getScanCode() { return scanCode; }
    public int getModifiers() { return modifiers; }

    // Символ для события CHAR, у остальных '\0' / The character for CHAR events, '\0' otherwise
    public char getChar() { return character; }

    public boolean hasModifier(int mask) { return (modifiers & mask) != 0; }
}