package com.codequest.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/** One topic in the Java Core Island curriculum (sections A..J). */
@Entity
@Table(name = "java_topics")
public class JavaTopic extends BaseEntity {
    @Column(nullable = false, length = 1) private String section;
    @Column(name = "section_name", nullable = false) private String sectionName;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false, unique = true) private String slug;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String summary;
    @Column(nullable = false) private boolean rebuild;
    @Column(name = "infosys_relevance", nullable = false) private String infosysRelevance;

    /**
     * @ElementCollection = a simple list of values stored in a side table (java_topic_companies).
     * FetchType.LAZY: the tags are NOT loaded until someone calls getCompanyCodes().
     * Lazy loading only works while the DB session (transaction) is still open, which is why
     * the service that reads them is @Transactional(readOnly = true). Touching a lazy field after the
     * transaction closes throws the famous LazyInitializationException.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "java_topic_companies", joinColumns = @JoinColumn(name = "topic_id"))
    @Column(name = "company_code")
    private Set<String> companyCodes = new HashSet<>();

    public String getSection() { return section; }
    public String getSectionName() { return sectionName; }
    public int getSortOrder() { return sortOrder; }
    public String getSlug() { return slug; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public boolean isRebuild() { return rebuild; }
    public String getInfosysRelevance() { return infosysRelevance; }
    public Set<String> getCompanyCodes() { return companyCodes; }

    /** A topic tagged ALL matches every company. */
    public boolean relevantTo(String companyCode) {
        return companyCodes.contains("ALL") || companyCodes.contains(companyCode);
    }
}
