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

// If the frontend will be separate (React, Vue, Angular),
// then you should NOT remove the JWT filter or stateless mode.
// In that case, use the JWT version of SecurityConfig instead of this Thymeleaf version.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // ❌ REMOVE FOR THYMELEAF (JWT is not used in a stateful MVC application)
    // private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    //SecurityContext - holds info who is loged in, and It's authority
    @Bean
    public SecurityContextRepository securityContextRepository()
    {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF must be disabled if you use POST forms without CSRF tokens and session is stateless
                // .csrf(AbstractHttpConfigurer::disable)

                // ❌ REMOVE FOR THYMELEAF (stateless mode breaks formLogin and sessions)
                // .sessionManagement(session -> session
                //         .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Keep CSRF protection enabled for the application,
                // but do not require a CSRF token for POST /stripe/webhook,
                // because Stripe sends the request directly from its server,
                // not through a Thymeleaf form.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/stripe/webhook"))

                .authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/",
                                "/sittersSearch",
                                "/registration",
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/sitters/search",
                                "/stripe/**"
                        ).permitAll()

                        // ❌ REMOVE — these are only needed for JWT API endpoints
                        // .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        // .requestMatchers(HttpMethod.POST, "/users/login").permitAll()
                        // .requestMatchers(HttpMethod.GET, "/users/**").authenticated()

                        .requestMatchers("/owner/**").hasAuthority("USER")
                        .requestMatchers("/sitter/**").hasAuthority("SITTER")
                        .requestMatchers("/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/sitters/**").authenticated()
                        .requestMatchers("/dashboard").authenticated()
                        .anyRequest().denyAll()
                )

                // ❌ REMOVE FOR THYMELEAF (JWT filter is not needed)
                // .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ✔ REQUIRED FOR THYMELEAF (classic form-based login)
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                // ✔ REQUIRED FOR THYMELEAF (session-based logout)
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
