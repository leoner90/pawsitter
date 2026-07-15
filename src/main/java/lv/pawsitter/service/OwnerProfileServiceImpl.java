package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.pawsitter.dto.OwnerProfileUpdateDTO;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerProfileServiceImpl implements OwnerProfileService
{
    private final OwnerProfileRepository ownerProfileRepository;
    private final UserRepository userRepository;

    @Override
    public OwnerProfile getProfileByUserEmail(String email)
    {
        return ownerProfileRepository.findByUserEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("Owner profile not found"));
    }

    @Override
    @Transactional
    public OwnerProfile updateProfile(String email, OwnerProfileUpdateDTO dto){
        OwnerProfile ownerProfile = getProfileByUserEmail(email);
        User user = ownerProfile.getUser();

        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhoneNumber(dto.phoneNumber());
        userRepository.save(user);

        ownerProfile.setLocation(dto.location());
        ownerProfile.setDescription(dto.description());
        ownerProfile.setImageUrl(dto.imageUrl());

        log.info("Updated owner profile with the email: {}", email);
        return ownerProfileRepository.save(ownerProfile);
    }
}