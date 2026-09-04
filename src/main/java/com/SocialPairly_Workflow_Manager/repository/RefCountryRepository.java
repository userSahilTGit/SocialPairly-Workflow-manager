package com.SocialPairly_Workflow_Manager.repository;

import com.SocialPairly_Workflow_Manager.entity.RefCountry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefCountryRepository extends JpaRepository<RefCountry, String> {

    List<RefCountry> findByActiveTrueOrderByNameAsc();
}
