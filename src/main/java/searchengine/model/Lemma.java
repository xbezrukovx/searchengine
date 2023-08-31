package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @JoinColumn
    @ManyToOne(cascade = CascadeType.ALL)
    private SiteModel siteModel;
    @Column(nullable = false)
    private String lemma;
    @Column(nullable = false)
    private int frequency;
}
