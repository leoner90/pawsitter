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

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @ToString.Exclude
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleType role;

    @OneToOne(mappedBy = "user")
    @JsonManagedReference
    private OwnerProfile ownerProfile;

    @OneToOne(mappedBy = "user")
    @JsonManagedReference
    private SitterProfile sitterProfile;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
