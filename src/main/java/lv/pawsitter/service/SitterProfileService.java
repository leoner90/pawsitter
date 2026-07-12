package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.repository.SitterProfileRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SitterProfileService
{
    private final SitterProfileRepository sitterProfileRepository;

    public List<SitterProfile> getAllSitters()
    {
        return sitterProfileRepository.findAll();
    }

    public SitterProfile getSitterById(Long id)
    {
        return sitterProfileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Sitter profile not found"));
    }

    public SitterProfile getProfileByUserEmail(String email)
    {
        return sitterProfileRepository.findByUserEmail(email).orElseThrow(() -> new IllegalArgumentException("Sitter profile not found"));
    }
}