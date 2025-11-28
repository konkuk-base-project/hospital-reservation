package model;

public class Doctor {
    private final String doctorId;
    private final String username;
    private final String name;
    private final String deptCode;
    private final String phoneNumber;
    private final String registrationDate;

    public Doctor(String doctorId, String username, String name, String deptCode, String phoneNumber) {
        this(doctorId, username, name, deptCode, phoneNumber, null);
    }

    public Doctor(String doctorId, String username, String name, String deptCode, String phoneNumber, String registrationDate) {
        this.doctorId = doctorId;
        this.username = username;
        this.name = name;
        this.deptCode = deptCode;
        this.phoneNumber = phoneNumber;
        this.registrationDate = registrationDate;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public String toDoctorListString() {
        return String.join(" ", doctorId, name, deptCode, phoneNumber, registrationDate);
    }

    public String toDetailFileHeaderString() {
        return String.join(" ", doctorId, name, deptCode, phoneNumber, registrationDate);
    }
}