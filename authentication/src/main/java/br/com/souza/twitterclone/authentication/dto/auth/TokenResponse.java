package br.com.souza.twitterclone.authentication.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenResponse {

    private String username;
    private String token;
    private Long expiresIn;
    private Boolean firstAccess;
    private String firstName;
    private Boolean isVerified;
    private String profilePhotoUrl;

}
