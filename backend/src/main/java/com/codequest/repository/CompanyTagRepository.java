package com.codequest.repository;

import com.codequest.entity.CompanyTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyTagRepository extends JpaRepository<CompanyTag, Long> {
    List<CompanyTag> findAllByOrderBySortOrder();
    boolean existsByCode(String code);
}
