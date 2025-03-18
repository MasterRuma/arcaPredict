package com.application.arcagambling.repository;

import com.application.arcagambling.domain.Thermo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ThermoRepository extends JpaRepository<Thermo, Long> {

}
