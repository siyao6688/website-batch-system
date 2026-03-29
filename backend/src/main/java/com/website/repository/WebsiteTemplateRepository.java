package com.website.repository;

import com.website.entity.WebsiteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebsiteTemplateRepository extends JpaRepository<WebsiteTemplate, Long> {

    Optional<WebsiteTemplate> findByTemplateCode(String templateCode);

    List<WebsiteTemplate> findByIsActive(Boolean isActive);

    List<WebsiteTemplate> findByIsActiveOrderByCreatedAtDesc(Boolean isActive);

    Optional<WebsiteTemplate> findByIsStandardAndIsActive(Boolean isStandard, Boolean isActive);
}
