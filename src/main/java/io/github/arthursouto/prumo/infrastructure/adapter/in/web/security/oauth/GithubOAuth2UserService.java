package io.github.arthursouto.prumo.infrastructure.adapter.in.web.security.oauth;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GithubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String ID_ATTRIBUTE = "id";
    private static final String EMAIL_ATTRIBUTE = "email";

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private final RestClient restClient;

    public GithubOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate, RestClient restClient) {
        this.delegate = delegate;
        this.restClient = restClient;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(request);

        Map<String, Object> attributes = new LinkedHashMap<>(user.getAttributes());
        attributes.remove(EMAIL_ATTRIBUTE);

        String email = verifiedEmail(request.getAccessToken().getTokenValue());
        if (email != null) {
            attributes.put(EMAIL_ATTRIBUTE, email);
        }

        return new DefaultOAuth2User(user.getAuthorities(), attributes, ID_ATTRIBUTE);
    }

    private String verifiedEmail(String accessToken) {
        List<Map<String, Object>> emails;

        try {
            emails = restClient.get()
                    .uri("/user/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientException exception) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_fetch_failed"), exception);
        }

        if (emails == null) {
            return null;
        }

        return emails.stream()
                .filter(entry -> Boolean.TRUE.equals(entry.get("verified")))
                .sorted((first, second) -> Boolean.compare(
                        Boolean.TRUE.equals(second.get("primary")),
                        Boolean.TRUE.equals(first.get("primary"))
                ))
                .map(entry -> (String) entry.get(EMAIL_ATTRIBUTE))
                .findFirst()
                .orElse(null);
    }
}
