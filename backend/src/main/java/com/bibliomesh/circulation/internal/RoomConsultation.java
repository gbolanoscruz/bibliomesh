package com.bibliomesh.circulation.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "room_consultations")
public class RoomConsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movement_code", nullable = false, unique = true, length = 64)
    private String movementCode;

    @Column(name = "copy_id", nullable = false)
    private Long copyId;

    @Column(name = "patron_id")
    private Long patronId;

    @Column(name = "consulted_at", nullable = false)
    private OffsetDateTime consultedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public String getMovementCode() { return movementCode; }
    public void setMovementCode(String movementCode) { this.movementCode = movementCode; }
    public Long getCopyId() { return copyId; }
    public void setCopyId(Long copyId) { this.copyId = copyId; }
    public Long getPatronId() { return patronId; }
    public void setPatronId(Long patronId) { this.patronId = patronId; }
    public OffsetDateTime getConsultedAt() { return consultedAt; }
    public void setConsultedAt(OffsetDateTime consultedAt) { this.consultedAt = consultedAt; }
}