package lv.pawsitter.service;

import lv.pawsitter.dto.OwnerProfileUpdateDTO;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OwnerProfileServiceImplUnitTests {
    @Mock
    private OwnerProfileRepository ownerProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @InjectMocks
    private OwnerProfileServiceImpl ownerProfileService;

    private User user;
    private OwnerProfile ownerProfile;
    private OwnerProfileUpdateDTO updateDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPhoneNumber("+37120000001");

        ownerProfile = new OwnerProfile();
        ownerProfile.setId(10L);
        ownerProfile.setUser(user);
        ownerProfile.setLocation("Riga");
        ownerProfile.setDescription("Loves animals");
        ownerProfile.setImageUrl("/images/ownersImages/old.jpg");

        updateDTO = new OwnerProfileUpdateDTO(
                "Alex",
                "Potter",
                "+37129999999",
                "Jurmala",
                "Updated description",
                null
        );
    }

    @Test
    void getProfileByUserEmail_returnsProfile_whenFound() {
        when(ownerProfileRepository.findByUserEmail("jane@example.com"))
                .thenReturn(Optional.of(ownerProfile));

        OwnerProfile result = ownerProfileService.getProfileByUserEmail("jane@example.com");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUser().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void getProfileByUserEmail_normalizesEmail_beforeLookup() {
        when(ownerProfileRepository.findByUserEmail("jane@example.com"))
                .thenReturn(Optional.of(ownerProfile));

        ownerProfileService.getProfileByUserEmail("  JANE@Example.com  ");

        verify(ownerProfileRepository).findByUserEmail("jane@example.com");
    }

    @Test
    void getProfileByUserEmail_throwsUserNotFoundException_whenNotFound() {
        when(ownerProfileRepository.findByUserEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerProfileService.getProfileByUserEmail("missing@example.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Owner profile not found");
    }

    @Test
    void getProfileByUserEmail_throwsNullPointerException_whenEmailIsNull() {
        assertThatThrownBy(() -> ownerProfileService.getProfileByUserEmail(null))
                .isInstanceOf(NullPointerException.class);
    }


    @Test
    void updateProfile_updatesUserAndProfileFields_whenNoImageProvided() {
        when(ownerProfileRepository.findByUserEmail("jane@example.com"))
                .thenReturn(Optional.of(ownerProfile));
        when(ownerProfileRepository.save(any(OwnerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OwnerProfile result = ownerProfileService.updateProfile("jane@example.com", updateDTO);

        assertThat(result.getUser().getFirstName()).isEqualTo("Alex");
        assertThat(result.getUser().getLastName()).isEqualTo("Potter");
        assertThat(result.getUser().getPhoneNumber()).isEqualTo("+37129999999");
        assertThat(result.getLocation()).isEqualTo("Jurmala");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getImageUrl()).isEqualTo("/images/ownersImages/old.jpg");

        verify(userRepository).save(user);
        verify(ownerProfileRepository).save(ownerProfile);
        verify(imageStorageService, never()).saveOwnerImage(any());
        verify(imageStorageService, never()).deleteOwnerImage(any());
    }

    @Test
    void updateProfile_replacesImage_whenNewImageProvided() {
        MultipartFile newImage = new MockMultipartFile(
                "image", "new.jpg", "image/jpeg", "content".getBytes());

        OwnerProfileUpdateDTO dtoWithImage = new OwnerProfileUpdateDTO(
                "Alex", "Potter", "+37129999999", "Jurmala", "Updated description", newImage);

        when(ownerProfileRepository.findByUserEmail("jane@example.com"))
                .thenReturn(Optional.of(ownerProfile));
        when(imageStorageService.saveOwnerImage(newImage)).thenReturn("/images/ownersImages/new.jpg");
        when(ownerProfileRepository.save(any(OwnerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OwnerProfile result = ownerProfileService.updateProfile("jane@example.com", dtoWithImage);

        assertThat(result.getImageUrl()).isEqualTo("/images/ownersImages/new.jpg");
        verify(imageStorageService).saveOwnerImage(newImage);
        verify(imageStorageService).deleteOwnerImage("/images/ownersImages/old.jpg");
    }

    @Test
    void updateProfile_doesNotTouchImage_whenImageIsEmpty() {
        MultipartFile emptyImage = new MockMultipartFile("image", new byte[0]);

        OwnerProfileUpdateDTO dtoWithEmptyImage = new OwnerProfileUpdateDTO(
                "Alex", "Potter", "+37129999999", "Jurmala", "Updated description", emptyImage);

        when(ownerProfileRepository.findByUserEmail("jane@example.com"))
                .thenReturn(Optional.of(ownerProfile));
        when(ownerProfileRepository.save(any(OwnerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OwnerProfile result = ownerProfileService.updateProfile("jane@example.com", dtoWithEmptyImage);

        assertThat(result.getImageUrl()).isEqualTo("/images/ownersImages/old.jpg");
        verify(imageStorageService, never()).saveOwnerImage(any());
        verify(imageStorageService, never()).deleteOwnerImage(any());
    }

    @Test
    void updateProfile_throwsUserNotFoundException_whenProfileDoesNotExist() {
        when(ownerProfileRepository.findByUserEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownerProfileService.updateProfile("missing@example.com", updateDTO))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
        verify(ownerProfileRepository, never()).save(any());
    }

    @Test
    void updateProfile_throwsNullPointerException_whenDtoIsNull() {
        when(ownerProfileRepository.findByUserEmail("jane@example.com"))
                .thenReturn(Optional.of(ownerProfile));

        assertThatThrownBy(() -> ownerProfileService.updateProfile("jane@example.com", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateProfile_throwsNullPointerException_whenEmailIsNull() {
        assertThatThrownBy(() -> ownerProfileService.updateProfile(null, updateDTO))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void updateProfile_propagatesIllegalArgumentException_whenImageIsInvalid() {
        MultipartFile badImage = new MockMultipartFile(
                "image", "bad.gif", "image/gif", "content".getBytes());

        OwnerProfileUpdateDTO dtoWithBadImage = new OwnerProfileUpdateDTO(
                "Alex", "Potter", "+37129999999", "Jurmala", "Updated description", badImage);

        when(ownerProfileRepository.findByUserEmail("jane@example.com"))
                .thenReturn(Optional.of(ownerProfile));
        when(imageStorageService.saveOwnerImage(badImage))
                .thenThrow(new IllegalArgumentException("Only JPEG and PNG images are allowed"));

        assertThatThrownBy(() -> ownerProfileService.updateProfile("jane@example.com", dtoWithBadImage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPEG and PNG");

        verify(ownerProfileRepository, never()).save(any());
    }
}
