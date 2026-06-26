package wurdal.structures;

import jakarta.persistence.*;


@Entity
@Table(name = "")
public class Player {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
}
