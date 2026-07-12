package lv.pawsitter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "sitter_profiles")
@Getter
@Setter
@NoArgsConstructor
public class SitterProfile
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String location = "Not provided";

    @Column(length = 1000)
    private String description = "";

    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay = BigDecimal.ZERO;

    private String imageUrl;

    @Column(nullable = false)
    private boolean published = false;
}