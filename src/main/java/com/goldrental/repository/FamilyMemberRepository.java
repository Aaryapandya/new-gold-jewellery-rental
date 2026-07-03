package com.goldrental.repository;

import com.goldrental.domain.entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
