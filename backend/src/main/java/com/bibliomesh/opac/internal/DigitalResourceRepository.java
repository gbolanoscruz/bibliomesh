package com.bibliomesh.opac.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitalResourceRepository extends JpaRepository<DigitalResource, Long> {

    List<DigitalResource> findAllByOrderByTitleAsc();
}