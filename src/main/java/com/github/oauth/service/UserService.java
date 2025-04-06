package com.github.oauth.service;

import java.util.List;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

import com.github.oauth.model.AppRole;
import com.github.oauth.model.Tech;
import com.github.oauth.model.User;
import com.github.oauth.payload.ProjectDTO;

public interface UserService {
    
    Set<Tech> addTech(Set<String> techStack, User user);


    @Transactional
    Set<Tech> removeTech(String technology, User user);

    List<ProjectDTO> getMyCreatedProjects(User user);

    void updateEmail(Authentication authentication, String newEmail);

    User getCurrentUser(Authentication authentication);

    void addRoleToUser(Authentication authentication, AppRole roleName);
}
