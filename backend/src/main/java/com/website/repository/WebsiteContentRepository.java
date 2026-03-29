package com.website.repository;

import com.website.entity.WebsiteContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebsiteContentRepository extends JpaRepository<WebsiteContent, Long> {

    Optional<WebsiteContent> findByIdAndCompanyId(Long id, Long companyId);

    List<WebsiteContent> findByCompanyIdOrderBySortOrderAsc(Long companyId);

    List<WebsiteContent> findByCompanyIdAndIsActive(Long companyId, Boolean isActive);

    List<WebsiteContent> findByCompanyIdAndContentTypeOrderBySortOrderAsc(Long companyId, String contentType);
}
