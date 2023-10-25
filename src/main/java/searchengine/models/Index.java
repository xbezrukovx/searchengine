package searchengine.models;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "search_index", indexes = {@javax.persistence.Index(name = "index_page_lemma_ndx", columnList = "page_id, lemma_id", unique = true)})
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private Page page;
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = CascadeType.MERGE)
    private Lemma lemma;
    @Column(nullable = false, name = "lemma_rank")
    private float rank;
}
