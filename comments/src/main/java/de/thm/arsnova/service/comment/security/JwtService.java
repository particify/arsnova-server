package de.thm.arsnova.service.comment.security;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private Algorithm algorithm;
    private JWTVerifier verifier;
    @Value("${jwt.secret:secret}") private String jwtSecret;

    public JwtService() {
        jwtSecret = "secret";
        algorithm = Algorithm.HMAC256(jwtSecret);
        verifier = JWT.require(algorithm)
                .build();
    }

    public String verifyToken(final String token) {
        final DecodedJWT decodedJwt = verifier.verify(token);

        return token;
    }
}
