package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "search_index")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = CascadeType.ALL)
    private Page page;
    @Column(nullable = false)
    private int lemmaId;
    @Column(nullable = false, name = "lemma_rank")
    private float rank;
}
