package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.SitterAvailabilityRequest;
import lv.pawsitter.dto.SitterProfileUpdateDTO;
import lv.pawsitter.dto.SitterPublishDTO;
import lv.pawsitter.entity.SitterAvailability;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.exception.AvailabilityNotFoundException;
import lv.pawsitter.exception.InvalidSitterOperationException;
import lv.pawsitter.repository.SitterAvailabilityRepository;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.SitterProfileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;

@Service
@RequiredArgsConstructor
public class SitterProfileService
{
    private final SitterProfileRepository sitterProfileRepository;
    private final ImageStorageService imageStorageService;
    private final SitterAvailabilityRepository sitterAvailabilityRepository;
    private final Validator validator;

    public List<SitterProfile> getAllSitters()
    {
        return sitterProfileRepository.findAll();
    }

    public SitterProfile getSitterById(Long id)
    {
        return sitterProfileRepository.findById(id).orElseThrow(() -> new UserNotFoundException("Sitter profile not found"));
    }

    //Get profile By Email
    public SitterProfile getProfileByUserEmail(String email)
    {
        return sitterProfileRepository.findByUserEmail(email).orElseThrow(() -> new UserNotFoundException("Sitter profile not found"));
    }

    //return only Published Sitters
    public List<SitterProfile> getPublishedSitters()
    {
        return sitterProfileRepository.findByPublishedTrue();
    }

    //Update Profile
    public void updateProfile(String email, SitterProfileUpdateDTO dto)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        sitterProfile.setLocation(dto.location());
        sitterProfile.setDescription(dto.description());
        sitterProfile.setPricePerDay(dto.pricePerDay());

        sitterProfile.getUser().setPhoneNumber(dto.phoneNumber());

        if (dto.image() != null && !dto.image().isEmpty())
        {
            String imageUrl = imageStorageService.saveSitterImage(dto.image());
            String oldImageUrl = sitterProfile.getImageUrl();
            imageStorageService.deleteSitterImage(oldImageUrl);
            sitterProfile.setImageUrl(imageUrl);
        }

        sitterProfileRepository.save(sitterProfile);
    }

    //ad available dates to DB
    public void addAvailability(String email, SitterAvailabilityRequest request)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);
        SitterAvailability availability = new SitterAvailability();

        availability.setSitterProfile(sitterProfile);
        availability.setStartDate(request.startDate());
        availability.setEndDate(request.endDate());

        sitterAvailabilityRepository.save(availability);
    }

    //Get Available Dates From Db
    public List<SitterAvailability> getAvailability(String email)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        return sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId());
    }

    //Remove Available date
    public void deleteAvailability(String email, Long availabilityId)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        SitterAvailability availability = sitterAvailabilityRepository.findById(availabilityId).orElseThrow(() -> new AvailabilityNotFoundException("Availability not found"));

        if (!availability.getSitterProfile().getId().equals(sitterProfile.getId()))
        {
            throw new AccessDeniedException("You cannot remove another sitter's availability");
        }

        sitterAvailabilityRepository.delete(availability);

        boolean hasAvailability = !sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId()).isEmpty();

        //if it was last one, set publish status to false
        if (!hasAvailability)
        {
            sitterProfile.setPublished(false);
            sitterProfileRepository.save(sitterProfile);
        }
    }

    //Try to publish and errors check using DTO (SitterPublishDTO)
    public void publishProfile(String email)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        SitterPublishDTO publishDTO = new SitterPublishDTO(
                sitterProfile.getLocation(),
                sitterProfile.getDescription(),
                sitterProfile.getPricePerDay(),
                sitterProfile.getUser().getPhoneNumber()
        );

        //runs the DTO validation:
        Set<ConstraintViolation<SitterPublishDTO>> violations = validator.validate(publishDTO);

        //if errors
        if (!violations.isEmpty())
        {
            throw new InvalidSitterOperationException(violations.iterator().next().getMessage());
        }

        // Check if sitter has availability
        boolean hasAvailability = !sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId()).isEmpty();

        if (!hasAvailability)
        {
            throw new InvalidSitterOperationException("At least one availability range is required");
        }

        sitterProfile.setPublished(true);
        sitterProfileRepository.save(sitterProfile);
    }

    //Unpublish Profile
    public void unpublishProfile(String email)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);
        sitterProfile.setPublished(false);
        sitterProfileRepository.save(sitterProfile);
    }
}