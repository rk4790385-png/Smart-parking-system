package com.parking.smartparking.service;

import com.parking.smartparking.model.Vehicle;
import com.parking.smartparking.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ParkingService {

    @Autowired
    private VehicleRepository vehicleRepository;

    public final int TOTAL_SLOTS = 50;

    public Map<String, Object> getDashboardStatus() {
        List<Vehicle> parked = vehicleRepository.findByStatus("PARKED");
        Map<String, Object> status = new HashMap<>();
        status.put("totalSlots", TOTAL_SLOTS);
        status.put("occupiedSlots", parked.size());
        status.put("availableSlots", TOTAL_SLOTS - parked.size());
        
        List<Map<String, String>> activeSlots = parked.stream().map(v -> {
            Map<String, String> map = new HashMap<>();
            map.put("slotNumber", v.getSlotNumber());
            map.put("licensePlate", v.getLicensePlate());
            map.put("vehicleType", v.getVehicleType());
            map.put("entryTime", v.getEntryTime().toString());
            return map;
        }).collect(Collectors.toList());
        status.put("activeSlots", activeSlots);
        
        return status;
    }

    public Vehicle parkVehicle(String licensePlate, String vehicleType, String slotNumber) {
        if(slotNumber == null || slotNumber.trim().isEmpty()){
            throw new RuntimeException("Slot Number is required.");
        }
        
        Optional<Vehicle> existingPlate = vehicleRepository.findByLicensePlateAndStatus(licensePlate, "PARKED");
        if (existingPlate.isPresent()) {
            throw new RuntimeException("Vehicle " + licensePlate + " is already parked.");
        }
        
        List<Vehicle> currentlyParked = vehicleRepository.findByStatus("PARKED");
        boolean slotTaken = currentlyParked.stream().anyMatch(v -> v.getSlotNumber().equalsIgnoreCase(slotNumber));
        if (slotTaken) {
            throw new RuntimeException("Slot " + slotNumber + " is already occupied.");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(licensePlate.toUpperCase());
        vehicle.setVehicleType(vehicleType != null ? vehicleType.toUpperCase() : "CAR");
        vehicle.setSlotNumber(slotNumber.toUpperCase());
        vehicle.setEntryTime(LocalDateTime.now());
        vehicle.setStatus("PARKED");
        return vehicleRepository.saveAndFlush(vehicle);
    }

    public Vehicle unparkVehicle(String licensePlate) {
        Vehicle vehicle = vehicleRepository.findByLicensePlateAndStatus(licensePlate, "PARKED")
                .orElseThrow(() -> new RuntimeException("Parked vehicle " + licensePlate + " not found."));
        
        vehicle.setExitTime(LocalDateTime.now());
        vehicle.setStatus("UNPARKED");
        
        // Professional billing engine calculation
        long minutesLogged = Duration.between(vehicle.getEntryTime(), vehicle.getExitTime()).toMinutes();
        if(minutesLogged < 1) minutesLogged = 1; // Minimum 1 minute for rapid testing
        
        // Convert to hours (at least 1 hour minimum charge for simulation)
        double hoursParked = Math.ceil(minutesLogged / 60.0);
        if(hoursParked < 1) hoursParked = 1; 
        
        double ratePerHour = 5.0; // default for CAR
        if("BIKE".equalsIgnoreCase(vehicle.getVehicleType())) {
            ratePerHour = 2.0;
        } else if ("EV".equalsIgnoreCase(vehicle.getVehicleType())) {
            ratePerHour = 6.0; // specific EV premium charging slot rate
        }
        
        double totalFee = hoursParked * ratePerHour;
        vehicle.setFee(totalFee);
        
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getParkedVehicles() {
        return vehicleRepository.findByStatus("PARKED");
    }
}
