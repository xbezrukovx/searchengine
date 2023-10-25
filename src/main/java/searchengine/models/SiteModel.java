package searchengine.models;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "sites")
public class SiteModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SiteStatusType status;
    @Column(nullable = false)
    private LocalDateTime statusTime;
    @Column(columnDefinition = "TEXT")
    private String lastError; // Text of last error
    @Column(nullable = false)
    private String url;
    @Column(nullable = false)
    private String name; //Name of Site
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "siteModel")
    private List<Page> sitePages = new ArrayList<>();

    public SiteModel(SiteStatusType status, LocalDateTime statusTime, String lastError, String url, String name){
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }
}
