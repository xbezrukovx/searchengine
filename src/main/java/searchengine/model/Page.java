package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;

@Entity
@Getter
@Setter
@Table(name = "pages")
@NoArgsConstructor
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private SiteModel siteModel;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String path; // A page path from root of website, starts with '/'.
    @Column(nullable = false)
    private int code; // An answer code, when page gives a response first time.
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content; // An html code of page

    public Page(SiteModel siteModel, String path, int code, String content) {
        this.siteModel = siteModel;
        this.path = path;
        this.code = code;
        this.content = content;
    }
}
