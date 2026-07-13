package lv.pawsitter.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table
@Getter
@Setter
@NoArgsConstructor
public class AvailabilityCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "sitter_id", referencedColumnName = "id")
    @JsonBackReference
    SitterProfile sitterProfile;

    LocalDateTime availableFrom;

    LocalDateTime availableTo;
}
