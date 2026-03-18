package com.parking.smartparking.repository;

import com.parking.smartparking.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByLicensePlateAndStatus(String licensePlate, String status);
    List<Vehicle> findByStatus(String status);
}
