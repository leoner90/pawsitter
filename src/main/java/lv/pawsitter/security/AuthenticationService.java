package lv.pawsitter.security;

public interface AuthenticationService {

    JwtAuthenticationResponse authenticate(SignInRequest request);
}