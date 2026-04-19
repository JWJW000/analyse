package com.ethicssra.repository;

import com.ethicssra.domain.Role;
import com.ethicssra.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND (" +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(COALESCE(u.displayName,'')) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<User> searchByRole(@Param("role") Role role, @Param("q") String q);
}
