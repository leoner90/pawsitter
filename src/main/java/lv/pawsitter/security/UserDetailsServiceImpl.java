package lv.pawsitter.security;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.entity.User;
import lv.pawsitter.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User userFromDB = repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User with login " + username + " is not found"));
        return new org.springframework.security.core.userdetails.User(userFromDB.getEmail(),
                userFromDB.getPassword(),
                List.of(new SimpleGrantedAuthority(userFromDB.getRole().name())));
    }
}