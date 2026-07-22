package lv.pawsitter.controller;

import lv.pawsitter.entity.OwnerProfile;
import lv.pawsitter.entity.Pet;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.service.BookingService;
import lv.pawsitter.service.OwnerProfileService;
import lv.pawsitter.service.SitterProfileService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc
public class DashboardControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SitterProfileService sitterProfileService;

    @MockitoBean
    private OwnerProfileService ownerProfileService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "sitter@example.com", authorities = "SITTER")
    void dashboardRedirect_redirectsToSitterDashboard_whenUserIsSitter() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sitter/dashboard"));
    }

    @Test
    @WithMockUser(username = "owner@example.com", authorities = "USER")
    void dashboardRedirect_redirectsToOwnerDashboard_whenUserIsNotSitter() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/dashboard"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void ownerDashboard_returnsOwnerDashboardView_withModelAttributes() throws Exception {
        User user = new User();
        user.setFirstName("Jane");

        Pet activePet = new Pet();
        activePet.setActive(true);
        Pet inactivePet = new Pet();
        inactivePet.setActive(false);

        OwnerProfile profile = new OwnerProfile();
        profile.setUser(user);
        profile.setLocation("Riga");
        profile.setPets(List.of(activePet, inactivePet));

        when(ownerProfileService.getProfileByUserEmail("owner@example.com")).thenReturn(profile);
        when(bookingService.getOwnerBookings(eq("owner@example.com"), eq(null))).thenReturn(List.of());

        mockMvc.perform(get("/owner/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("owner/ownerDashboard"))
                .andExpect(model().attribute("firstName", "Jane"))
                .andExpect(model().attribute("petCount", 1L))
                .andExpect(model().attribute("bookingCount", 0))
                .andExpect(model().attribute("location", "Riga"));
    }

    @Test
    @WithMockUser(username = "sitter@example.com")
    void sitterDashboard_returnsSitterDashboardView_withModelAttributes() throws Exception {
        User user = new User();
        user.setFirstName("John");

        SitterProfile profile = new SitterProfile();
        profile.setUser(user);
        profile.setPublished(true);
        profile.setPricePerDay(BigDecimal.TEN);

        when(sitterProfileService.getProfileByUserEmail("sitter@example.com")).thenReturn(profile);
        when(sitterProfileService.getAvailability("sitter@example.com")).thenReturn(List.of());

        mockMvc.perform(get("/sitter/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("sitter/sitterDashboard"))
                .andExpect(model().attribute("firstName", "John"))
                .andExpect(model().attribute("published", true))
                .andExpect(model().attribute("pricePerDay", BigDecimal.TEN))
                .andExpect(model().attribute("availabilityCount", 0));
    }

}
