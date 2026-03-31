package com.website.repository;

import com.website.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // 批量操作相关查询
    List<Company> findByIdIn(List<Long> ids);
    List<Company> findByTemplateIdAndIsPublishedAndIsDeletedFalse(Long templateId, Boolean isPublished);

    // 统计模板使用次数
    @Query("SELECT COUNT(c) FROM Company c WHERE c.templateId = :templateId AND c.isPublished = :isPublished AND c.isDeleted = false")
    int countByTemplateIdAndIsPublished(@Param("templateId") Long templateId, @Param("isPublished") Boolean isPublished);
}
