package com.learn.rest.HelloApp.Controller;

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
    public List<Employee> getAllActiveEmployees() {
        return employeeService.getAllActiveEmployees();
    }

    /**
     * REST API endpoint to get all not active employees
     * @return List of active employees
     */
    @GetMapping("/allemployee-notactive")
    public List<Employee> getAllNotActiveEmployees() {
        return employeeService.getAllNotActiveEmployees();
    }

    /**
     * REST API endpoint to add a new employee
     * @param employee Employee object with name, email, department, isActive, and optionally status
     * @return HTTP status code only
     */
    @PostMapping("/addemployee")
    public ResponseEntity<Void> addEmployees(@RequestBody Employee employee) {
        try {
            employeeService.createEmployee(employee);
            logger.info("Employee created successfully: {}", employee);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST API endpoint to get a single employee by ID
     * @param id Employee ID
     * @return Employee if found, 404 otherwise
     */
    @GetMapping("/employee/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id)
                .map(employee -> {
                    logger.info("Employee found: {}", employee);
                    return new ResponseEntity<>(employee, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.warn("Employee not found with id: {}", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        return employeeService.updateEmployee(id, employee)
                .map(updated -> {
                    logger.info("Employee updated successfully: {}", updated);
                    return new ResponseEntity<>(updated, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.warn("Employee not found for update with id: {}", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    /**
     * REST API endpoint to delete an employee by ID
     * @param id Employee ID to delete
     * @return 204 No Content if deleted, 404 if not found
     */
//    @DeleteMapping("/employee/{id}")
//    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
//        if (employeeService.deleteEmployee(id)) {
//            logger.info("Employee deleted successfully with id: {}", id);
//            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
//        } else {
//            logger.warn("Employee not found for deletion with id: {}", id);
//            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//        }
//    }
}
