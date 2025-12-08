package model;

public class Patient {
    private final String patientId;
    private final String username;
    private final String name;
    private final String birthDate;
    private final String phoneNumber;
    private int noshowCount = 0;

    public Patient(String patientId, String username, String name, String birthDate, String phoneNumber) {
        this.patientId = patientId;
        this.username = username;
        this.name = name;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
    }

    public void setNoshowCount(int noshowCount) {
        this.noshowCount = noshowCount;
    }

    public int getNoshowCount() {
        return noshowCount;
    }

    public String getPatientId() {
        return patientId;
    }

    public String toPatientListString() {
        return String.join(" ", patientId, username, name, birthDate, phoneNumber, String.valueOf(noshowCount));
    }

    public String toDetailFileHeaderString() {
        return String.join(" ", patientId, name, birthDate, phoneNumber, String.valueOf(noshowCount));
    }
}