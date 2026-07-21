package lv.pawsitter.security.sessionless.jwttoken;

import lv.pawsitter.security.sessionless.SignInRequest;

public interface AuthenticationService {

    JwtAuthenticationResponse authenticate(SignInRequest request);
}