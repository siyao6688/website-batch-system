package com.website.repository;

import com.website.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByDomain(String domain);

    List<Company> findByIsPublished(Boolean isPublished);

    List<Company> findByIsActive(Boolean isActive);

    Optional<Company> findByIdAndIsActive(Long id, Boolean isActive);

    // 软删除相关查询
    Optional<Company> findByIdAndIsDeletedFalse(Long id);
    List<Company> findAllByIsDeletedFalse();
    List<Company> findByIsActiveAndIsDeletedFalse(Boolean isActive);
    List<Company> findByIsPublishedAndIsDeletedFalse(Boolean isPublished);
    Optional<Company> findByDomainAndIsDeletedFalse(String domain);
    Optional<Company> findByIdAndIsActiveAndIsDeletedFalse(Long id, Boolean isActive);
}
