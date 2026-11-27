package com.bibliomesh.circulation.internal;

import com.bibliomesh.circulation.LoanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_code", nullable = false, unique = true, length = 64)
    private String loanCode;

    @Column(name = "patron_id", nullable = false)
    private Long patronId;

    @Column(name = "copy_id")
    private Long copyId;

    @Column(name = "tech_resource_id")
    private Long techResourceId;

    @Column(name = "loaned_at", nullable = false)
    private OffsetDateTime loanedAt = OffsetDateTime.now();

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LoanStatus status = LoanStatus.ACTIVE;

    private String notes;

    public Long getId() { return id; }
    public String getLoanCode() { return loanCode; }
    public void setLoanCode(String loanCode) { this.loanCode = loanCode; }
    public Long getPatronId() { return patronId; }
    public void setPatronId(Long patronId) { this.patronId = patronId; }
    public Long getCopyId() { return copyId; }
    public void setCopyId(Long copyId) { this.copyId = copyId; }
    public Long getTechResourceId() { return techResourceId; }
    public void setTechResourceId(Long techResourceId) { this.techResourceId = techResourceId; }
    public OffsetDateTime getLoanedAt() { return loanedAt; }
    public void setLoanedAt(OffsetDateTime loanedAt) { this.loanedAt = loanedAt; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public OffsetDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(OffsetDateTime returnedAt) { this.returnedAt = returnedAt; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}