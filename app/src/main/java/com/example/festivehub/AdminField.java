package com.example.festivehub;

/** One input field of an admin form (Add / Edit screen). */
public class AdminField {

    public enum Type {
        TEXT, MULTILINE, EMAIL, PHONE, NUMBER, BOOLEAN, CHOICE, CHOICE_FROM_COLLECTION
    }

    public final String key;      // Firestore field name
    public final String label;    // label shown above the input
    public final Type type;
    public final boolean required;
    public final String hint;

    public String[] options;          // for CHOICE
    public String sourceCollection;   // for CHOICE_FROM_COLLECTION (e.g. "colleges")
    public String sourceField;        // for CHOICE_FROM_COLLECTION (e.g. "name")

    public AdminField(String key, String label, Type type, boolean required, String hint) {
        this.key = key;
        this.label = label;
        this.type = type;
        this.required = required;
        this.hint = hint;
    }

    public static AdminField text(String key, String label, boolean required) {
        return new AdminField(key, label, Type.TEXT, required, null);
    }

    public static AdminField text(String key, String label, boolean required, String hint) {
        return new AdminField(key, label, Type.TEXT, required, hint);
    }

    public static AdminField multiline(String key, String label, boolean required) {
        return new AdminField(key, label, Type.MULTILINE, required, null);
    }

    public static AdminField email(String key, String label, boolean required) {
        return new AdminField(key, label, Type.EMAIL, required, null);
    }

    public static AdminField phone(String key, String label, boolean required) {
        return new AdminField(key, label, Type.PHONE, required, "10 digit number");
    }

    public static AdminField number(String key, String label, boolean required) {
        return new AdminField(key, label, Type.NUMBER, required, null);
    }

    public static AdminField bool(String key, String label) {
        return new AdminField(key, label, Type.BOOLEAN, false, null);
    }

    public static AdminField choice(String key, String label, boolean required, String... options) {
        AdminField f = new AdminField(key, label, Type.CHOICE, required, null);
        f.options = options;
        return f;
    }

    public static AdminField choiceFromCollection(String key, String label, boolean required,
                                                  String collection, String field) {
        AdminField f = new AdminField(key, label, Type.CHOICE_FROM_COLLECTION, required, null);
        f.sourceCollection = collection;
        f.sourceField = field;
        return f;
    }
}
