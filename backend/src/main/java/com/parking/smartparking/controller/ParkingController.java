package com.parking.smartparking.controller;

import com.parking.smartparking.model.Vehicle;
import com.parking.smartparking.service.ParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*") 
public class ParkingController {

    @Autowired
    private ParkingService parkingService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDashboardStatus() {
        return ResponseEntity.ok(parkingService.getDashboardStatus());
    }

    @PostMapping("/park")
    public ResponseEntity<?> parkVehicle(
            @RequestParam("licensePlate") String licensePlate,
            @RequestParam("vehicleType") String vehicleType,
            @RequestParam("slotNumber") String slotNumber) {
        try {
            Vehicle vehicle = parkingService.parkVehicle(licensePlate, vehicleType, slotNumber);
            Map<String, String> success = new HashMap<>();
            success.put("message", "Parked successfully");
            success.put("licensePlate", vehicle.getLicensePlate());
            return ResponseEntity.ok(success);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Unknown parking error");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/unpark")
    public ResponseEntity<?> unparkVehicle(@RequestParam("licensePlate") String licensePlate) {
        try {
            Vehicle vehicle = parkingService.unparkVehicle(licensePlate);
            Map<String, Object> resp = new HashMap<>();
            resp.put("licensePlate", vehicle.getLicensePlate());
            resp.put("vehicleType", vehicle.getVehicleType());
            resp.put("entryTime", vehicle.getEntryTime() != null ? vehicle.getEntryTime().toString() : "");
            resp.put("exitTime", vehicle.getExitTime() != null ? vehicle.getExitTime().toString() : "");
            resp.put("fee", vehicle.getFee() != null ? vehicle.getFee() : 0.0);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage() != null ? e.getMessage() : "Unknown unparking error");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<Vehicle>> getParkedVehicles() {
        return ResponseEntity.ok(parkingService.getParkedVehicles());
    }
}
