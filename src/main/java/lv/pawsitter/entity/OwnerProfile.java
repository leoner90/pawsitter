package lv.pawsitter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "owner_profiles")
@Getter
@Setter
@NoArgsConstructor
public class OwnerProfile
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String location = "Not provided";

    @Column(length = 1000)
    private String description = "";

    private String imageUrl;

    @OneToMany(mappedBy = "ownerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pet> pets = new ArrayList<>();
}