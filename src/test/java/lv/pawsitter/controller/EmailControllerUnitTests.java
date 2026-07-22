package lv.pawsitter.controller;

import lv.pawsitter.client.EmailWebClient;
import lv.pawsitter.security.sessionless.jwttoken.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import reactor.core.publisher.Mono;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(EmailController.class)
@AutoConfigureMockMvc
class EmailControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailWebClient emailWebClient;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;


    @Test
    @WithMockUser
    void sendTestEmail_returnsResponseBody() throws Exception {

        when(emailWebClient.sendEmail("klaatu", "barada", "nikto"))
                .thenReturn(Mono.just("sent"));

        var mvcResult = mockMvc.perform(get("/email/test"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string("sent"));

        verify(emailWebClient)
                .sendEmail("klaatu", "barada", "nikto");
    }
}