package lv.pawsitter.service;

import lv.pawsitter.dto.OwnerProfileUpdateDTO;
import lv.pawsitter.entity.OwnerProfile;

public interface OwnerProfileService {
    OwnerProfile getProfileByUserEmail(String email);
    OwnerProfile updateProfile(String email, OwnerProfileUpdateDTO dto);
}
