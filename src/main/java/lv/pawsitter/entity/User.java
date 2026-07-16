package lv.pawsitter.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lv.pawsitter.model.RoleType;

import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @ToString.Exclude
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleType role;

    // cascade = CascadeType.ALL so a newly created OwnerProfile or SitterProfile
    // is automatically saved together with its User.
    @OneToOne(mappedBy = "user" , cascade = CascadeType.ALL)
    @JsonManagedReference
    private OwnerProfile ownerProfile;

    @OneToOne(mappedBy = "user" , cascade = CascadeType.ALL)
    @JsonManagedReference
    private SitterProfile sitterProfile;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }


    public void setOwnerProfile(OwnerProfile ownerProfile)
    {
        this.ownerProfile = ownerProfile;
        if (ownerProfile != null) {ownerProfile.setUser(this);}
    }

    public void setSitterProfile(SitterProfile sitterProfile)
    {
        this.sitterProfile = sitterProfile;

        if (sitterProfile != null) {sitterProfile.setUser(this);}
    }
}
