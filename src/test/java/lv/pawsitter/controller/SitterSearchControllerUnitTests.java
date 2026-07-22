package lv.pawsitter.controller;

import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.service.SitterProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SitterSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SitterSearchControllerUnitTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SitterProfileService sitterProfileService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private SitterProfile buildSitterProfile(Long id) {
        User user = new User();
        user.setFirstName("Sam");
        user.setLastName("Sitter" + id);

        SitterProfile profile = new SitterProfile();
        profile.setId(id);
        profile.setUser(user);
        profile.setLocation("Riga");
        profile.setDescription("Experienced pet sitter");
        profile.setPricePerDay(BigDecimal.TEN);
        return profile;
    }

    @Test
    void sitterSearchPage_returnsSittersSearchView_withPublishedSitters() throws Exception {
        when(sitterProfileService.getPublishedSitters()).thenReturn(List.of(buildSitterProfile(1L)));

        mockMvc.perform(get("/sittersSearch"))
                .andExpect(status().isOk())
                .andExpect(view().name("sittersSearch"))
                .andExpect(model().attributeExists("sitters"));
    }

    @Test
    void sitterDetailsPage_returnsSitterDetailsView_withSitter() throws Exception {
        when(sitterProfileService.getSitterById(1L)).thenReturn(buildSitterProfile(1L));

        mockMvc.perform(get("/sitters/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("sitter/sitterDetails"))
                .andExpect(model().attributeExists("sitter"));
    }

    @Test
    void searchSitters_returnsSittersSearchView_withResults_whenValid() throws Exception {
        when(sitterProfileService.searchSitters(eq("Riga"), any(LocalDate.class), any(LocalDate.class),
                any(BigDecimal.class), eq(false))).thenReturn(List.of(buildSitterProfile(1L)));

        mockMvc.perform(get("/sitters/search")
                        .param("city", "Riga")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-05")
                        .param("maxPrice", "50")
                        .param("includePartial", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("sittersSearch"))
                .andExpect(model().attribute("selectedCity", "Riga"))
                .andExpect(model().attributeExists("sitters"));
    }

    @Test
    void searchSitters_returnsEmptyResultsWithError_whenServiceThrowsIllegalArgument() throws Exception {
        when(sitterProfileService.searchSitters(any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new IllegalArgumentException("End date must be after start date"));

        mockMvc.perform(get("/sitters/search")
                        .param("startDate", "2026-08-05")
                        .param("endDate", "2026-08-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("sittersSearch"))
                .andExpect(model().attribute("sitters", List.of()))
                .andExpect(model().attributeExists("searchError"));
    }

    @Test
    void searchSitters_worksWithNoOptionalParams() throws Exception {
        when(sitterProfileService.searchSitters(isNull(), isNull(), isNull(), isNull(), eq(false)))
                .thenReturn(List.of());

        mockMvc.perform(get("/sitters/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("sittersSearch"));
    }
}