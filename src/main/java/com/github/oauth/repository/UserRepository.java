package com.github.oauth.repository;


import com.github.oauth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGithubId(String githubId);
    Optional<User> findByLogin(String login);

    @Modifying
    @Query("UPDATE User u SET u.email = :newEmail WHERE u.githubId = :githubId")
    void updateEmailByGithubId(@Param("githubId") String githubId, @Param("newEmail") String newEmail);

}
