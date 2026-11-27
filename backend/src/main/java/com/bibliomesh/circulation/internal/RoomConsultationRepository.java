package com.bibliomesh.circulation.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomConsultationRepository extends JpaRepository<RoomConsultation, Long> {

    List<RoomConsultation> findTop20ByOrderByConsultedAtDesc();
}