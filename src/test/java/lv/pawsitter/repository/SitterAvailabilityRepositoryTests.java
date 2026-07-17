package lv.pawsitter.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import lv.pawsitter.entity.SitterAvailability;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.model.RoleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class SitterAvailabilityRepositoryTests {

    private static final LocalDate START = LocalDate.of(2026, 8, 10);
    private static final LocalDate END = LocalDate.of(2026, 8, 12);

    @Autowired
    private SitterAvailabilityRepository sitterAvailabilityRepository;

    @Autowired
    private TestEntityManager entityManager;

    @ParameterizedTest
    @MethodSource("availabilityCoverageRanges")
    void existsBySitterProfileIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualChecksFullCoverage(
            LocalDate requestedStart,
            LocalDate requestedEnd,
            boolean expectedAvailable
    ) {
        SitterProfile sitter = sitter("sitter@example.com");
        availability(sitter, START, END);
        flushAndClear();

        boolean available = sitterAvailabilityRepository
                .existsBySitterProfileIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        sitter.getId(),
                        requestedStart,
                        requestedEnd
                );

        assertThat(available).isEqualTo(expectedAvailable);
    }

    @Test
    void existsBySitterProfileIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualIgnoresOtherSitters() {
        SitterProfile sitter = sitter("sitter@example.com");
        SitterProfile otherSitter = sitter("other-sitter@example.com");
        availability(otherSitter, START, END);
        flushAndClear();

        boolean available = sitterAvailabilityRepository
                .existsBySitterProfileIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        sitter.getId(),
                        START,
                        END
                );

        assertThat(available).isFalse();
    }

    @Test
    void findBySitterProfileIdAndEndDateGreaterThanEqualOrderByStartDateAscReturnsOnlyCurrentAndFutureRanges() {
        LocalDate today = LocalDate.now();
        SitterProfile sitter = sitter("sitter@example.com");
        availability(sitter, today.minusDays(10), today.minusDays(1));
        SitterAvailability currentRange = availability(sitter, today, today.plusDays(2));
        SitterAvailability futureRange = availability(sitter, today.plusDays(5), today.plusDays(7));
        flushAndClear();

        List<SitterAvailability> ranges = sitterAvailabilityRepository
                .findBySitterProfileIdAndEndDateGreaterThanEqualOrderByStartDateAsc(sitter.getId(), today);

        assertThat(ranges)
                .extracting(SitterAvailability::getId)
                .containsExactly(currentRange.getId(), futureRange.getId());
    }

    @Test
    void existsBySitterProfileIdAndEndDateGreaterThanEqualReturnsFalseForOnlyExpiredRanges() {
        LocalDate today = LocalDate.now();
        SitterProfile sitter = sitter("sitter@example.com");
        availability(sitter, today.minusDays(10), today.minusDays(1));
        flushAndClear();

        boolean hasCurrentOrFutureAvailability = sitterAvailabilityRepository
                .existsBySitterProfileIdAndEndDateGreaterThanEqual(sitter.getId(), today);

        assertThat(hasCurrentOrFutureAvailability).isFalse();
    }

    private static Stream<Arguments> availabilityCoverageRanges() {
        return Stream.of(
                Arguments.of(START, END, true),
                Arguments.of(START.plusDays(1), END.minusDays(1), true),
                Arguments.of(START.minusDays(1), END, false),
                Arguments.of(START, END.plusDays(1), false),
                Arguments.of(START.minusDays(1), END.plusDays(1), false)
        );
    }

    private SitterAvailability availability(SitterProfile sitter, LocalDate startDate, LocalDate endDate) {
        SitterAvailability availability = new SitterAvailability();
        availability.setSitterProfile(sitter);
        availability.setStartDate(startDate);
        availability.setEndDate(endDate);
        return entityManager.persistAndFlush(availability);
    }

    private SitterProfile sitter(String email) {
        User user = user(email);
        SitterProfile sitter = new SitterProfile();
        sitter.setUser(user);
        entityManager.persist(user);
        return entityManager.persistAndFlush(sitter);
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Sitter");
        user.setPhoneNumber("+37120000000");
        user.setEmail(email);
        user.setPassword("password");
        user.setRole(RoleType.SITTER);
        return user;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
