package model;

public class Patient {
    private final String patientId;
    private final String username;
    private final String name;
    private final String birthDate;
    private final String phoneNumber;

    public Patient(String patientId, String username, String name, String birthDate, String phoneNumber) {
        this.patientId = patientId;
        this.username = username;
        this.name = name;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
    }

    public String getPatientId() { return patientId; }

    public String toPatientListString() {
        return String.join(" ", patientId, username, name, birthDate, phoneNumber);
    }

    public String toDetailFileHeaderString() {
        return String.join(" ", patientId, name, birthDate, phoneNumber);
    }
}