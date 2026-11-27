package com.bibliomesh.patrons.internal;

import com.bibliomesh.patrons.PatronType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "patrons")
public class Patron {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identification_number", nullable = false, unique = true, length = 64)
    private String identificationNumber;

    @Column(name = "first_name", nullable = false, length = 120)
    private String firstName;

    @Column(name = "first_surname", length = 120)
    private String firstSurname;

    @Column(name = "second_surname", length = 120)
    private String secondSurname;

    @Enumerated(EnumType.STRING)
    @Column(name = "patron_type", nullable = false, length = 32)
    private PatronType patronType = PatronType.STUDENT;

    @Column(name = "grade_section", length = 64)
    private String gradeSection;

    @Column(length = 40)
    private String phone;

    @Column(length = 180)
    private String email;

    private String observations;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public String getFullName() {
        return Stream.of(firstName, firstSurname, secondSurname)
                .filter(Objects::nonNull)
                .map(part -> part.trim())
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(" "));
    }

    public Long getId() { return id; }
    public String getIdentificationNumber() { return identificationNumber; }
    public void setIdentificationNumber(String identificationNumber) { this.identificationNumber = identificationNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getFirstSurname() { return firstSurname; }
    public void setFirstSurname(String firstSurname) { this.firstSurname = firstSurname; }
    public String getSecondSurname() { return secondSurname; }
    public void setSecondSurname(String secondSurname) { this.secondSurname = secondSurname; }
    public PatronType getPatronType() { return patronType; }
    public void setPatronType(PatronType patronType) { this.patronType = patronType; }
    public String getGradeSection() { return gradeSection; }
    public void setGradeSection(String gradeSection) { this.gradeSection = gradeSection; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}