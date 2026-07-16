package lv.pawsitter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;


//JPA entity, and Hibernate to map Java fields to a SQL
@Entity
@Table(name = "pets")
@Getter
@Setter
@NoArgsConstructor
public class Pet
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_profile_id", nullable = false)
    private OwnerProfile ownerProfile;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column
    private String nickName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnimalTypes animalType;

    @Column
    private String breed;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private String description;

    @Column
    private String specialNeeds;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt()
    {
        this.createdAt = LocalDateTime.now();
    }
}
