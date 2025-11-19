package com.example.culturalcompass.model;

import java.util.Date;

public class User {

    private String email;      // PRIMARY KEY
    private String name;
    private String surname;
    private Date birthdate;    // EXACTLY like professor
    //private String passwordHash;

    // Required empty constructor for Firestore
    public User() {}

    public User(String email, String name, String surname,
                //String passwordHash,
                Date birthdate) {

        this.email = email;
        this.name = name;
        this.surname = surname;
        this.birthdate = birthdate;
        //this.passwordHash = passwordHash;
    }

    // GETTERS + SETTERS

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public Date getBirthdate() { return birthdate; }
    public void setBirthdate(Date birthdate) { this.birthdate = birthdate; }

    //public String getPasswordHash() { return passwordHash; }
    //public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
