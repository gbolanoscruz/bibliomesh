package com.bibliomesh.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "library_settings")
public class LibrarySettings {

    @Id
    private Short id = 1;

    @Column(name = "library_code", nullable = false, unique = true, length = 32)
    private String libraryCode;

    @Column(name = "library_name", nullable = false, length = 150)
    private String libraryName;

    @Column(name = "inscription_prefix", nullable = false, length = 16)
    private String inscriptionPrefix = "LIB";

    @Column(name = "inscription_sequence", nullable = false)
    private Integer inscriptionSequence = 1;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "digital_library_drive_folder_id", length = 128)
    private String digitalLibraryDriveFolderId;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public String nextInscriptionNumber() {
        int current = this.inscriptionSequence;
        this.inscriptionSequence = current + 1;
        this.updatedAt = OffsetDateTime.now();
        return this.inscriptionPrefix + String.format("%05d", current);
    }

    public Short getId() { return id; }
    public String getLibraryCode() { return libraryCode; }
    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }
    public String getInscriptionPrefix() { return inscriptionPrefix; }
    public void setInscriptionPrefix(String inscriptionPrefix) { this.inscriptionPrefix = inscriptionPrefix; }
    public Integer getInscriptionSequence() { return inscriptionSequence; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getDigitalLibraryDriveFolderId() { return digitalLibraryDriveFolderId; }
    public void setDigitalLibraryDriveFolderId(String id) { this.digitalLibraryDriveFolderId = id; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}