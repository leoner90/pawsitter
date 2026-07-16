package lv.pawsitter.controller;

import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.security.JwtService;
import lv.pawsitter.service.SitterProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PageController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PageControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SitterProfileService sitterProfileService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private SitterProfile buildSitterProfile(Long id) {
        User user = new User();
        user.setFirstName("Sitter");
        user.setLastName("Test" + id);

        SitterProfile profile = new SitterProfile();
        profile.setId(id);
        profile.setUser(user);
        profile.setLocation("Riga");
        profile.setDescription("Experienced pet sitter");
        return profile;
    }


    @Test
    @WithMockUser
    void homePage_returnsIndexView_withPublishedSitters() throws Exception {
        when(sitterProfileService.getPublishedSitters())
                .thenReturn(List.of(buildSitterProfile(1L), buildSitterProfile(2L)));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("sitters",
                        org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @WithMockUser
    void homePage_returnsIndexView_withEmptySitterList() throws Exception {
        when(sitterProfileService.getPublishedSitters()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("sitters", org.hamcrest.Matchers.hasSize(0)));
    }



    @Test
    @WithMockUser
    void sitterDetailsPage_returnsDetailsView_whenSitterExists() throws Exception {
        when(sitterProfileService.getSitterById(1L)).thenReturn(buildSitterProfile(1L));

        mockMvc.perform(get("/sitters/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("sitter/sitterDetails"))
                .andExpect(model().attributeExists("sitter"));
    }

    @Test
    @WithMockUser
    void sitterDetailsPage_returnsNotFound_whenSitterDoesNotExist() throws Exception {
        when(sitterProfileService.getSitterById(999L))
                .thenThrow(new UserNotFoundException("Sitter profile not found"));

        mockMvc.perform(get("/sitters/999"))
                .andExpect(status().isNotFound());
    }
}
