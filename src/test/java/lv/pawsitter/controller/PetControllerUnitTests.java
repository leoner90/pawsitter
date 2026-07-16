package lv.pawsitter.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.With;
import lv.pawsitter.dto.PetRequestDto;
import lv.pawsitter.dto.PetResponseDto;
import lv.pawsitter.entity.AnimalTypes;
import lv.pawsitter.exception.PetNotFoundException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.security.JwtService;
import lv.pawsitter.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PetController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PetControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PetService petService;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private PetResponseDto buildResponseDto(Long id, Long ownerId) {
        PetResponseDto dto = new PetResponseDto();
        dto.setId(id);
        dto.setOwnerId(ownerId);
        dto.setFirstName("Moe");
        dto.setLastName("Johnson");
        dto.setNickName("Mo");
        dto.setAnimalType(AnimalTypes.CAT);
        dto.setBreed("Black");
        dto.setAge(4);
        dto.setDescription("Friendly and sleepy cat");
        dto.setSpecialNeeds("None");
        dto.setImageUrl("http://example.com/buddy.jpg");
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    private PetRequestDto buildRequestDto() {
        PetRequestDto dto = new PetRequestDto();
        dto.setFirstName("Moe");
        dto.setLastName("Johnson");
        dto.setNickName("Mo");
        dto.setAnimalType(AnimalTypes.CAT);
        dto.setBreed("Black");
        dto.setAge(4);
        dto.setDescription("Friendly and sleepy cat");
        dto.setSpecialNeeds("None");
        //dto.setImageUrl("http://example.com/buddy.jpg");
        return dto;
    }

    @Test
    @WithMockUser
    void getAllPets_returnsOkWithList() throws Exception{
        when(petService.getAllPets()).thenReturn(List.of(buildResponseDto(1L, 10L)));
        mockMvc.perform(get("/api/pet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Moe"));
    }

    @Test
    @WithMockUser
    void getPetById_returnsOk_forExistingPet() throws Exception{
        when(petService.getById(1L)).thenReturn(buildResponseDto(1L, 10L));
        mockMvc.perform(get("/api/pet/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerId").value(10));
    }

    @Test
    @WithMockUser
    void getPetById_returnsNotFound_forNonExistingPet() throws Exception {
        when(petService.getById(999L)).thenThrow(new PetNotFoundException("Pet not found with id 999"));
        mockMvc.perform(get("/api/pet/999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pet not found with id 999"));
    }

    @Test
    @WithMockUser
    void getPetsByOwner_returnsOkWithList_forExistingPets() throws Exception{
        when(petService.getPetsByOwnerId(10L)).thenReturn(List.of(buildResponseDto(1L, 10L)));
        mockMvc.perform(get("/api/pet/owner/10")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerId").value(10));
    }


    @Test
    @WithMockUser
    void createPet_returnsCreated_whenOwnerExists() throws Exception {
        PetRequestDto petRequestDto = buildRequestDto();
        when(petService.createPet(anyLong(), any(PetRequestDto.class)))
                .thenReturn(buildResponseDto(1L, 10L));

        mockMvc.perform(post("/api/pet/owner/10")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(petRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerId").value(10));
    }

    @Test
    @WithMockUser
    void createPet_returnsNotFound_whenOwnerDoesNotExist() throws Exception {
        PetRequestDto requestDto = buildRequestDto();
        when(petService.createPet(anyLong(), any(PetRequestDto.class)))
                .thenThrow(new UserNotFoundException("Owner not found with the id: 9999"));

        mockMvc.perform(post("/api/pet/owner/9999")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Owner not found with the id: 9999"));
    }

    @Test
    @WithMockUser
    void createPet_returnsBadRequest_forFailedValidation() throws Exception{
        PetRequestDto invalidDto = new PetRequestDto();
        mockMvc.perform(post("/api/pet/owner/10").contentType("application/json").content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
        verify(petService, never()).createPet(anyLong(), any());
    }

    @Test
    @WithMockUser
    void updatePet_returnsOk_whenPetExists() throws Exception {
        PetRequestDto requestDto = buildRequestDto();
        when(petService.updatePet(anyLong(), any(PetRequestDto.class)))
                .thenReturn(buildResponseDto(1L, 10L));

        mockMvc.perform(put("/api/pet/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void updatePet_returnsNotFound_whenPetDoesNotExist() throws Exception {
        PetRequestDto requestDto = buildRequestDto();
        when(petService.updatePet(anyLong(), any(PetRequestDto.class)))
                .thenThrow(new PetNotFoundException("Pet not found with the id: 99"));

        mockMvc.perform(put("/api/pet/99")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deletePet_returnsNoContent_ForExistingPet() throws Exception{
        mockMvc.perform(delete("/api/pet/1"))
                .andExpect(status().isNoContent());
        verify(petService).deletePet(1L);
    }

    @Test
    @WithMockUser
    void deletePet_returnsNotFound_whenPetDoesNotExist() throws Exception {
        doThrow(new PetNotFoundException("Pet not found with the id: 99"))
                .when(petService).deletePet(99L);

        mockMvc.perform(delete("/api/pet/99"))
                .andExpect(status().isNotFound());
    }
}