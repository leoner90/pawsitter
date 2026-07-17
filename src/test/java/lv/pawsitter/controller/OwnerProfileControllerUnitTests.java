package lv.pawsitter.controller;

import lv.pawsitter.dto.OwnerProfileUpdateDTO;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.PetResponseDto;
import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.PetNotFoundException;
import lv.pawsitter.security.JwtService;
import lv.pawsitter.service.OwnerProfileService;
import lv.pawsitter.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OwnerProfileController.class)
@AutoConfigureMockMvc
public class OwnerProfileControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerProfileService ownerProfileService;

    @MockitoBean
    private PetService petService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private OwnerProfile buildOwnerProfile() {
        User user = new User();
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");
        user.setPhoneNumber("+37120000001");

        OwnerProfile profile = new OwnerProfile();
        profile.setId(10L);
        profile.setUser(user);
        profile.setLocation("Riga");
        profile.setDescription("Loves animals");
        return profile;
    }

    private PetResponseDto buildPetResponseDto(Long id) {
        PetResponseDto dto = new PetResponseDto();
        dto.setId(id);
        dto.setOwnerId(10L);
        dto.setFirstName("Buddy");
        dto.setLastName("Doe");
        dto.setNickName("Bud");
        dto.setBreed("Mixed");
        dto.setAge(3);
        dto.setDescription("Friendly dog");
        dto.setSpecialNeeds("None");
        return dto;
    }

    @Test
    void ownerProfilePage_returnsProfileView_withOwnerModelAttribute() throws Exception {
        when(ownerProfileService.getProfileByUserEmail("jane@example.com")).thenReturn(buildOwnerProfile());

        mockMvc.perform(get("/owner/profile").with(user("jane@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/ownerProfile"))
                .andExpect(model().attributeExists("owner"));
    }

    @Test
    void addPetPage_returnsAddPetView_withEmptyPetRequest() throws Exception {
        mockMvc.perform(get("/owner/pets/add").with(user("jane@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/addPet"))
                .andExpect(model().attributeExists("petRequest"));
    }

    @Test
    void editOwnerProfilePage_returnsEditView_withPrefilledProfileRequest() throws Exception {
        when(ownerProfileService.getProfileByUserEmail("jane@example.com")).thenReturn(buildOwnerProfile());

        mockMvc.perform(get("/owner/profile/edit").with(user("jane@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/editOwnerProfile"))
                .andExpect(model().attributeExists("profileRequest"));
    }

    @Test
    void addPet_redirectsToProfile_whenValidationPasses() throws Exception {
        when(ownerProfileService.getProfileByUserEmail("jane@example.com")).thenReturn(buildOwnerProfile());
        when(petService.createPet(anyLong(), any(PetRequestDto.class))).thenReturn(buildPetResponseDto(1L));

        mockMvc.perform(post("/owner/pets/add")
                        .with(user("jane@example.com"))
                        .with(csrf())
                        .param("firstName", "Buddy")
                        .param("lastName", "Doe")
                        .param("animalType", "DOG")
                        .param("age", "3")
                        .param("description", "Friendly dog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/profile"));

        verify(petService).createPet(eq(10L), any(PetRequestDto.class));
    }

    @Test
    void addPet_returnsFormView_whenValidationFails() throws Exception {
        mockMvc.perform(post("/owner/pets/add")
                        .with(user("jane@example.com"))
                        .with(csrf())
                        .param("firstName", "")
                        .param("lastName", "")
                        .param("age", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/addPet"))
                .andExpect(model().attributeExists("petRequest"));

        verify(petService, never()).createPet(anyLong(), any());
    }

    @Test
    void addPet_rejectsImageField_whenServiceThrowsIllegalArgumentException() throws Exception {
        when(ownerProfileService.getProfileByUserEmail("jane@example.com")).thenReturn(buildOwnerProfile());
        when(petService.createPet(anyLong(), any(PetRequestDto.class)))
                .thenThrow(new IllegalArgumentException("Only JPEG and PNG images are allowed"));

        mockMvc.perform(post("/owner/pets/add")
                        .with(user("jane@example.com"))
                        .with(csrf())
                        .param("firstName", "Buddy")
                        .param("lastName", "Doe")
                        .param("animalType", "DOG")
                        .param("age", "3")
                        .param("description", "Friendly dog"))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/addPet"))
                .andExpect(model().attributeExists("petRequest"));
    }

    @Test
    void updateOwnerProfile_redirectsToProfile_whenValid() throws Exception {
        mockMvc.perform(post("/owner/profile/edit")
                        .with(user("jane@example.com"))
                        .with(csrf())
                        .param("firstName", "Jane")
                        .param("lastName", "Doe")
                        .param("phoneNumber", "+37120000001")
                        .param("location", "Jurmala")
                        .param("description", "Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/profile"));

        verify(ownerProfileService).updateProfile(eq("jane@example.com"), any(OwnerProfileUpdateDTO.class));
    }

    @Test
    void updateOwnerProfile_returnsFormView_whenValidationFails() throws Exception {
        mockMvc.perform(post("/owner/profile/edit")
                        .with(user("jane@example.com"))
                        .with(csrf())
                        .param("firstName", "")
                        .param("lastName", "")
                        .param("phoneNumber", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/editOwnerProfile"))
                .andExpect(model().attributeExists("profileRequest"));

        verify(ownerProfileService, never()).updateProfile(anyString(), any());
    }

    @Test
    void deletePet_redirectsToProfile() throws Exception {
        mockMvc.perform(post("/owner/pets/1/delete")
                        .with(user("jane@example.com"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/profile"));

        verify(petService).deleteOwnerPet("jane@example.com", 1L);
    }

    @Test
    void editPetPage_returnsEditPetView_withPrefilledPetRequest() throws Exception {
        when(petService.getOwnerPet("jane@example.com", 1L)).thenReturn(buildPetResponseDto(1L));

        mockMvc.perform(get("/owner/pets/1/edit").with(user("jane@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/editPet"))
                .andExpect(model().attributeExists("petRequest"))
                .andExpect(model().attribute("petId", 1L));
    }

    @Test
    void editPetPage_propagatesNotFound_whenPetDoesNotExist() throws Exception {
        when(petService.getOwnerPet("jane@example.com", 999L))
                .thenThrow(new PetNotFoundException("Pet not found with id 999"));

        mockMvc.perform(get("/owner/pets/999/edit").with(user("jane@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePet_redirectsToProfile_whenValid() throws Exception {
        when(petService.updateOwnerPet(eq("jane@example.com"), eq(1L), any(PetRequestDto.class)))
                .thenReturn(buildPetResponseDto(1L));

        mockMvc.perform(post("/owner/pets/1/edit")
                        .with(user("jane@example.com"))
                        .with(csrf())
                        .param("firstName", "Buddy")
                        .param("lastName", "Doe")
                        .param("animalType", "DOG")
                        .param("age", "3")
                        .param("description", "Friendly dog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/profile"));
    }

    @Test
    void updatePet_returnsFormView_whenValidationFails() throws Exception {
        mockMvc.perform(post("/owner/pets/1/edit")
                        .with(user("jane@example.com"))
                        .with(csrf())
                        .param("firstName", "")
                        .param("age", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/editPet"))
                .andExpect(model().attribute("petId", 1L));
    }
}