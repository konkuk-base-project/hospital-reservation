package model;

public class Major {
    private final String majorCode;
    private final String majorName;

    public Major(String majorCode, String majorName) {
        this.majorCode = majorCode;
        this.majorName = majorName;
    }

    public String getMajorCode() {
        return majorCode;
    }

    public String getMajorName() {
        return majorName;
    }

    public String toFileString() {
        return String.join(" ", majorCode, majorName);
    }
}
