package lv.pawsitter.service;

import lv.pawsitter.dto.OwnerProfileUpdateDTO;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.OwnerProfileRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OwnerProfileService
{
    private final OwnerProfileRepository ownerProfileRepository;

    public OwnerProfile getProfileByUserEmail(String email)
    {
        return ownerProfileRepository.findByUserEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UserNotFoundException("Owner profile not found"));
    }
}

public interface OwnerProfileService {
    OwnerProfile getProfileByUserEmail(String email);
    OwnerProfile updateProfile(String email, OwnerProfileUpdateDTO dto);
}
