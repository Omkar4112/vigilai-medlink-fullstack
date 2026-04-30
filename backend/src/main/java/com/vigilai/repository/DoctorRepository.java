package com.vigilai.repository;

import com.vigilai.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByHospitalId(Long hospitalId);
    List<Doctor> findByHospitalIdAndIsAvailableTrue(Long hospitalId);
    long countByIsAvailableTrue();
}
