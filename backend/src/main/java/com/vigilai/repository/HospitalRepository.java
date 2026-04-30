package com.vigilai.repository;

import com.vigilai.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    List<Hospital> findByIsActiveTrue();
    Optional<Hospital> findByCode(String code);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(h.totalIcuBeds) FROM Hospital h")
    Integer sumTotalIcuBeds();

    @org.springframework.data.jpa.repository.Query("SELECT SUM(h.occupiedBeds) FROM Hospital h")
    Integer sumOccupiedBeds();
}
