package br.comsouza.twitterclone.feed.configuration.authorization;

import br.comsouza.twitterclone.feed.properties.SecurityProperties;
import br.comsouza.twitterclone.feed.service.redis.RedisService;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenProvider {

    private static final int UNAUTHORIZED = 401;
    private static final int EXPIRED = 440;
    private final SecurityProperties securityProperties;
    private final RedisService redisService;

    public TokenProvider(SecurityProperties securityProperties,
                         RedisService redisService) {
        this.securityProperties = securityProperties;
        this.redisService = redisService;
    }

    public boolean isValid(String jwt, ServletResponse response) throws IOException {
        try {
            jwt = extractToken(jwt);
            String secret = securityProperties.getJwtKey();

            SignedJWT signedJWT = SignedJWT.parse(jwt);
            JWSVerifier verifier = new MACVerifier(securityProperties.getJwtKey().getBytes(StandardCharsets.UTF_8));
            if (!signedJWT.verify(verifier)) {
                log.error("Invalid JWT token!");
                ((HttpServletResponse) response).sendError(UNAUTHORIZED);
                return false;
            }
            Claims claims = Jwts.parser().setSigningKey(secret.getBytes(StandardCharsets.UTF_8)).parseClaimsJws(jwt).getBody();
            String sessionIdentifier = claims.getSubject();
            String identifier = (String) redisService.getValue(sessionIdentifier);
            return (identifier != null);
        } catch (ExpiredJwtException e) {
            log.error("Token expirado: {}", e);
            ((HttpServletResponse) response).sendError(EXPIRED);
            return false;
        }catch (Exception e) {
            log.error("Token inválido: {}", e);
            ((HttpServletResponse) response).sendError(UNAUTHORIZED);
            return false;
        }
    }

    public String getIdentifierFromToken(String jwt) {
        jwt = extractToken(jwt);
        String secret = securityProperties.getJwtKey();
        Claims claims = Jwts.parser().setSigningKey(secret.getBytes(StandardCharsets.UTF_8)).parseClaimsJws(jwt).getBody();
        String sessionIdentifier = claims.getSubject();
        return (String) redisService.getValue(sessionIdentifier);
    }

    private String extractToken(String authToken) {
        if (authToken.toLowerCase().startsWith("bearer")) {
            return authToken.substring("bearer ".length());
        }
        return authToken;
    }
}

