package com.example.appointivahealth.models;

import java.io.Serializable;

public class User implements Serializable {
    private String id;
    private String name;
    private String email;
    private String role; // "Patient", "Doctor", "Admin"
    private String specialization; // Only for Doctor
    private String phone;
    private String profileImageUrl;
    private String experience;
    private String hospitalName;
    private String licenseNumber;
    private boolean isVerified;
    private String fee;
    private String availableTime;
    private String lastUpdated;
    private String age;
    private String gender;
    private String registrationDate;
    
    public User() {
        // Required for Firebase
    }

    public User(String id, String name, String email, String role, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.phone = phone;
        this.profileImageUrl = ""; // Default to empty
        this.experience = "";
        this.hospitalName = "";
        this.licenseNumber = "";
        this.isVerified = false; // By default, all new doctors need verification. Patients can skip it.
        this.fee = "";
        this.availableTime = "";
        this.lastUpdated = "";
        this.age = "";
        this.gender = "";
        this.registrationDate = "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getFee() { return fee; }
    public void setFee(String fee) { this.fee = fee; }

    public String getAvailableTime() { return availableTime; }
    public void setAvailableTime(String availableTime) { this.availableTime = availableTime; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(String registrationDate) { this.registrationDate = registrationDate; }

    @com.google.firebase.database.PropertyName("isVerified")
    public boolean isVerified() { return isVerified; }
    
    @com.google.firebase.database.PropertyName("isVerified")
    public void setVerified(boolean verified) { isVerified = verified; }
}
