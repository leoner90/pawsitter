package lv.pawsitter.security;//@Configuration

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    //SecurityContext - holds info who is longed in, and It's authority
    @Bean
    public SecurityContextRepository securityContextRepository()
    {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Keep CSRF protection enabled for the application, but do not require a CSRF token for POST /stripe/webhook,
                // because Stripe sends the request directly from its server,  not through a Thymeleaf form.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/stripe/webhook"))

                .authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/",
                                "/login",
                                "/registration",
                                "/sittersSearch",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/stripe/**",
                                "/fragments/**",
                                "/recovery/**",
                                "/sitters/search"

                        ).permitAll()

                        .requestMatchers("/owner/**").hasAuthority("USER")
                        .requestMatchers("/sitter/**").hasAuthority("SITTER")
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/sitters/**").authenticated()
                        .requestMatchers("/dashboard").authenticated()
                        .anyRequest().denyAll()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                // As we were advised, make the automatic logout cleanup explicit and delete the session cookie.
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
