package com.goldrental.repository;

import com.goldrental.domain.entity.User;
import com.goldrental.domain.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

    Page<User> findAllByRole(UserRole role, Pageable pageable);

    Page<User> findAllByEmailVerifiedFalseOrMobileVerifiedFalse(Pageable pageable);

    boolean existsByRole(UserRole role);

    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (CAST(:city AS string) IS NULL OR LOWER(u.city) LIKE LOWER(CONCAT('%', CAST(:city AS string), '%')))
            """)
    Page<User> findAllWithFilters(
            @Param("role") UserRole role,
            @Param("city") String city,
            Pageable pageable);
}
