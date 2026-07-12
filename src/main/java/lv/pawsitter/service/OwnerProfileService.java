package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.entity.OwnerProfile;
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
                .orElseThrow(() -> new IllegalArgumentException("Owner profile not found"));
    }
}