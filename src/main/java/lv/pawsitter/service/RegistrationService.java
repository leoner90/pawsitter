package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.RegistrationRequest;
import lv.pawsitter.entity.User;
import lv.pawsitter.entity.UserRole;
import lv.pawsitter.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lv.pawsitter.repository.OwnerProfileRepository;
import lv.pawsitter.repository.SitterProfileRepository;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.SitterProfile;

@Service
@RequiredArgsConstructor
public class RegistrationService
{
    private final UserRepository userRepository;
    private final SitterProfileRepository sitterProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegistrationRequest request)
    {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email))
        {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        if (!request.getPassword().equals(request.getConfirmPassword()))
        {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (request.getRole() == UserRole.ADMIN)
        {
            throw new IllegalArgumentException("Administrator registration is not allowed");
        }

        User user = new User();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == UserRole.SITTER)
        {
            SitterProfile sitterProfile = new SitterProfile();
            sitterProfile.setUser(savedUser);
            sitterProfile.setPublished(false);

            sitterProfileRepository.save(sitterProfile);
        }

        if (savedUser.getRole() == UserRole.OWNER)
        {
            OwnerProfile ownerProfile = new OwnerProfile();
            ownerProfile.setUser(savedUser);

            ownerProfileRepository.save(ownerProfile);
        }
    }
}