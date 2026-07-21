package lv.pawsitter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Recovery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @Column(nullable = false, unique = true)
    private String recoveryToken;

    @Column(nullable = false)
    private LocalDateTime endOfLifeCycle;

    @PrePersist
    public void setEndOfLifeCycle() {
        if (this.endOfLifeCycle == null) {
            this.endOfLifeCycle = LocalDateTime.now().plusMinutes(15);
        }
    }

}
