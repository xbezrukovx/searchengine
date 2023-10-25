package searchengine.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.persistence.Index;

@Entity
@Getter
@Setter
@Table(indexes = {@Index(name = "lemma_site_ndx", unique = true, columnList = "site_model_id, lemma")})
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @JoinColumn
    @ManyToOne(cascade = CascadeType.MERGE)
    private SiteModel siteModel;
    @Column(nullable = false)
    private String lemma;
    @Column(nullable = false)
    private int frequency;
}
