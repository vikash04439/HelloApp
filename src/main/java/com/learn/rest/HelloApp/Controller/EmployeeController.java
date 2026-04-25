package com.learn.rest.HelloApp.Controller;

import com.learn.rest.HelloApp.dto.ApiResponse;
import com.learn.rest.HelloApp.entity.Employee;
import com.learn.rest.HelloApp.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * REST API endpoint to get all active employees
     * @return List of active employees
     */
    @GetMapping("/allemployee")
    public ResponseEntity<ApiResponse<List<Employee>>> getAllActiveEmployees() {
        List<Employee> employees = employeeService.getAllActiveEmployees();
        if (employees.isEmpty()) {
            return new ResponseEntity<>(
                    new ApiResponse<>("No active (is_active=true) employees found", employees),
                    HttpStatus.OK);
        }
        return new ResponseEntity<>(
                new ApiResponse<>("Active (is_active=true) employees fetched successfully", employees),
                HttpStatus.OK);
    }

    /**
     * REST API endpoint to get all not active employees
     * @return List of active employees
     */
    @GetMapping("/allemployee-notactive")
    public ResponseEntity<ApiResponse<List<Employee>>> getAllNotActiveEmployees() {
        List<Employee> employees = employeeService.getAllNotActiveEmployees();
        if (employees.isEmpty()) {
            return new ResponseEntity<>(
                    new ApiResponse<>("No inactive (is_active=false) employees found", employees),
                    HttpStatus.OK);
        }
        return new ResponseEntity<>(
                new ApiResponse<>("Inactive (is_active=false) employees fetched successfully", employees),
                HttpStatus.OK);
    }

    /**
     * REST API endpoint to add a new employee
     * @param employee Employee object with name, email, department, isActive, and optionally status
     * @return HTTP status code only
     */
    @PostMapping("/addemployee")
    public ResponseEntity<ApiResponse<Employee>> addEmployees(@RequestBody Employee employee) {
        try {
            Employee created = employeeService.createEmployee(employee);
            logger.info("Employee created successfully: {}", created);
            return new ResponseEntity<>(
                    new ApiResponse<>("Employee has been successfully created", created),
                    HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Failed to create employee: {}", e.getMessage());
            return new ResponseEntity<>(
                    new ApiResponse<>("Employee could not be created: " + e.getMessage(), null),
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST API endpoint to get a single employee by ID
     * @param id Employee ID
     * @return Employee if found, 404 otherwise
     */
    @GetMapping("/employee/{id}")
    public ResponseEntity<ApiResponse<Employee>> getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id)
                .map(employee -> {
                    logger.info("Employee found: {}", employee);
                    return new ResponseEntity<>(
                            new ApiResponse<>("Employee found successfully", employee),
                            HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.warn("Employee not found with id: {}", id);
                    return new ResponseEntity<>(
                            new ApiResponse<>("Employee not found with id: " + id, null),
                            HttpStatus.NOT_FOUND);
                });
    }

    /**
     * REST API endpoint to update an existing employee
     * Only provided (non-null) fields will be updated
     * @param id Employee ID to update
     * @param employee Employee object with fields to update
     * @return Updated employee if found, 404 otherwise
     */
    @PutMapping("/employee/{id}")
    public ResponseEntity<ApiResponse<Employee>> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        return employeeService.updateEmployee(id, employee)
                .map(updated -> {
                    logger.info("Employee updated successfully: {}", updated);
                    return new ResponseEntity<>(
                            new ApiResponse<>("Employee updated successfully", updated),
                            HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.warn("Employee not found for update with id: {}", id);
                    return new ResponseEntity<>(
                            new ApiResponse<>("Employee not found with id: " + id, null),
                            HttpStatus.NOT_FOUND);
                });
    }

    /**
     * REST API endpoint to delete an employee by ID
     * @param id Employee ID to delete
     * @return 204 No Content if deleted, 404 if not found
     */
    @DeleteMapping("/employee/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        if (employeeService.deleteEmployee(id)) {
            logger.info("Employee deleted successfully with id: {}", id);
            return new ResponseEntity<>(
                    new ApiResponse<>("Employee deleted successfully", null),
                    HttpStatus.OK);
        } else {
            logger.warn("Employee not found for deletion with id: {}", id);
            return new ResponseEntity<>(
                    new ApiResponse<>("Employee not found with id: " + id, null),
                    HttpStatus.NOT_FOUND);
        }
    }
}
