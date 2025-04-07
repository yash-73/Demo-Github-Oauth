package com.github.oauth.service;


import com.github.oauth.model.User;
import com.github.oauth.repository.UserRepository;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public OAuth2UserService (UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract user information from OAuth2User
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String githubId = attributes.get("id").toString();
        String login = (String) attributes.get("login");
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");

        // Get access token from the user request
        String accessToken = userRequest.getAccessToken().getTokenValue();

        // Save or update user in the database
        Optional<User> userOptional = userRepository.findByGithubId(githubId);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setLogin(login);
            user.setName(name);
            user.setEmail(email);
            user.setAvatarUrl(avatarUrl);
            user.setAccessToken(accessToken);
            user.setTokenExpiryDate(calculateExpiryDate(userRequest));
        } else {
            user = new User();
            user.setGithubId(githubId);
            user.setLogin(login);
            user.setName(name);
            user.setEmail(email);
            user.setAvatarUrl(avatarUrl);
            user.setAccessToken(accessToken);
            user.setTokenExpiryDate(calculateExpiryDate(userRequest));
        }

        userRepository.save(user);

        return oAuth2User;
    }

    private LocalDateTime calculateExpiryDate(OAuth2UserRequest userRequest) {
        // GitHub tokens typically don't expire, but in case they have an expiration:
        if (userRequest.getAccessToken().getExpiresAt() != null) {
            return LocalDateTime.ofInstant(
                    userRequest.getAccessToken().getExpiresAt(),
                    java.time.ZoneId.systemDefault()
            );
        }
        // Default to a long time in the future if no expiration is set
        return LocalDateTime.now().plusYears(1);
    }
}