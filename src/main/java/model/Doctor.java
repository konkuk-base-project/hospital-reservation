package model;

public class Doctor {
    private final String doctorId;
    private final String username;
    private final String name;
    private final String deptCode;
    private final String phoneNumber;

    public Doctor(String doctorId, String username, String name, String deptCode, String phoneNumber) {
        this.doctorId = doctorId;
        this.username = username;
        this.name = name;
        this.deptCode = deptCode;
        this.phoneNumber = phoneNumber;
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

    public String toDoctorListString() {
        return String.join(" ", doctorId, name, deptCode);
    }

    public String toDetailFileHeaderString() {
        return String.join(" ", doctorId, name, deptCode);
    }
}