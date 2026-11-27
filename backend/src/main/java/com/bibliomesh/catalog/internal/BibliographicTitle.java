package com.bibliomesh.catalog.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "bibliographic_titles")
public class BibliographicTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String isbn;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 300)
    private String subtitle;

    @Column(name = "preferred_title", length = 300)
    private String preferredTitle;

    @Column(length = 250)
    private String author;

    @Column(length = 200)
    private String publisher;

    @Column(name = "publication_place", length = 150)
    private String publicationPlace;

    @Column(name = "publication_year", length = 32)
    private String publicationYear;

    @Column(length = 200)
    private String series;

    @Column(length = 64)
    private String volume;

    @Column(name = "dewey_code", length = 64)
    private String deweyCode;

    @Column(name = "cutter_code", length = 64)
    private String cutterCode;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "descriptors", columnDefinition = "text[]", nullable = false)
    private List<String> descriptors = new ArrayList<>();

    private String synopsis;

    @Column(name = "general_note")
    private String generalNote;

    @Column(name = "cover_url")
    private String coverUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "marc_extras", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> marcExtras = new HashMap<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Long getId() { return id; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getPreferredTitle() { return preferredTitle; }
    public void setPreferredTitle(String preferredTitle) { this.preferredTitle = preferredTitle; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getPublicationPlace() { return publicationPlace; }
    public void setPublicationPlace(String publicationPlace) { this.publicationPlace = publicationPlace; }
    public String getPublicationYear() { return publicationYear; }
    public void setPublicationYear(String publicationYear) { this.publicationYear = publicationYear; }
    public String getSeries() { return series; }
    public void setSeries(String series) { this.series = series; }
    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }
    public String getDeweyCode() { return deweyCode; }
    public void setDeweyCode(String deweyCode) { this.deweyCode = deweyCode; }
    public String getCutterCode() { return cutterCode; }
    public void setCutterCode(String cutterCode) { this.cutterCode = cutterCode; }
    public List<String> getDescriptors() { return descriptors; }
    public void setDescriptors(List<String> descriptors) {
        this.descriptors = descriptors != null ? new ArrayList<>(descriptors) : new ArrayList<>();
    }
    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }
    public String getGeneralNote() { return generalNote; }
    public void setGeneralNote(String generalNote) { this.generalNote = generalNote; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public Map<String, String> getMarcExtras() { return marcExtras; }
    public void setMarcExtras(Map<String, String> marcExtras) {
        this.marcExtras = marcExtras != null ? new HashMap<>(marcExtras) : new HashMap<>();
    }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}