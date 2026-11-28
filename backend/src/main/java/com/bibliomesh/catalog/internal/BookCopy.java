package com.bibliomesh.catalog.internal;

import com.bibliomesh.catalog.CopyStatus;
import com.bibliomesh.catalog.PhysicalCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "book_copies")
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private BibliographicTitle title;

    @Column(name = "school_id")
    private Long schoolId;

    @Column(name = "inscription_number", nullable = false, unique = true, length = 64)
    private String inscriptionNumber;

    @Column(unique = true, length = 64)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Column(name = "unavailable_reason", length = 64)
    private String unavailableReason;

    @Column(name = "unavailable_notes")
    private String unavailableNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "physical_condition", nullable = false, length = 20)
    private PhysicalCondition physicalCondition = PhysicalCondition.FAIR;

    @Column(name = "acquisition_source", length = 150)
    private String acquisitionSource;

    @Column(name = "in_library", nullable = false)
    private boolean inLibrary = true;

    @Column(name = "last_inventory_check_at")
    private OffsetDateTime lastInventoryCheckAt;

    @Column(name = "observations")
    private String observations;

    @Column(name = "shelf_location", length = 100)
    private String shelfLocation;

    @Column(name = "queued_for_label", nullable = false)
    private boolean queuedForLabel = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public BibliographicTitle getTitle() { return title; }
    public void setTitle(BibliographicTitle title) { this.title = title; }
    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }
    public String getInscriptionNumber() { return inscriptionNumber; }
    public void setInscriptionNumber(String inscriptionNumber) { this.inscriptionNumber = inscriptionNumber; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public CopyStatus getStatus() { return status; }
    public void setStatus(CopyStatus status) { this.status = status; }
    public String getUnavailableReason() { return unavailableReason; }
    public void setUnavailableReason(String unavailableReason) { this.unavailableReason = unavailableReason; }
    public String getUnavailableNotes() { return unavailableNotes; }
    public void setUnavailableNotes(String unavailableNotes) { this.unavailableNotes = unavailableNotes; }
    public PhysicalCondition getPhysicalCondition() { return physicalCondition; }
    public void setPhysicalCondition(PhysicalCondition physicalCondition) { this.physicalCondition = physicalCondition; }
    public String getAcquisitionSource() { return acquisitionSource; }
    public void setAcquisitionSource(String acquisitionSource) { this.acquisitionSource = acquisitionSource; }
    public boolean isInLibrary() { return inLibrary; }
    public void setInLibrary(boolean inLibrary) { this.inLibrary = inLibrary; }
    public OffsetDateTime getLastInventoryCheckAt() { return lastInventoryCheckAt; }
    public void setLastInventoryCheckAt(OffsetDateTime lastInventoryCheckAt) { this.lastInventoryCheckAt = lastInventoryCheckAt; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }
    public boolean isQueuedForLabel() { return queuedForLabel; }
    public void setQueuedForLabel(boolean queuedForLabel) { this.queuedForLabel = queuedForLabel; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}