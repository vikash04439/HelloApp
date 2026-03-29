package com.learn.rest.HelloApp.service;

import com.learn.rest.HelloApp.entity.Employee;
import com.learn.rest.HelloApp.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Get all active employees from database
     *
     * @return List of active employees
     */
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findByIsActiveTrue();
    }

    /**
     * Get all not active employees from database
     *
     * @return List of active employees
     */
    public List<Employee> getAllNotActiveEmployees() {
        return employeeRepository.findByIsActiveFalse();
    }

    /**
     * Create a new employee in the database
     *
     * @param employee Employee object with name, email, department, isActive, and optionally status
     * @return The created employee
     */
    public Employee createEmployee(Employee employee) {
        // Default status to 'Y' if not provided or empty
        if (employee.getStatus() == null || employee.getStatus().isBlank()) {
            employee.setStatus("Y");
        }
        return employeeRepository.save(employee);
    }

    /**
     * Get a single employee by ID
     *
     * @param id Employee ID
     * @return Optional containing the employee if found
     */
    public Optional<Employee> getEmployeeById(Long id) {
        return employeeRepository.findById(id);
    }

    /**
     * Update an existing employee in the database.
     * Only updates fields that are provided (non-null).
     *
     * @param id Employee ID to update
     * @param updatedEmployee Employee object with updated fields
     * @return Optional containing the updated employee if found, empty otherwise
     */
    public Optional<Employee> updateEmployee(Long id, Employee updatedEmployee) {
        return employeeRepository.findById(id).map(existing -> {
            if (updatedEmployee.getName() != null) {
                existing.setName(updatedEmployee.getName());
            }
            if (updatedEmployee.getEmail() != null) {
                existing.setEmail(updatedEmployee.getEmail());
            }
            if (updatedEmployee.getDepartment() != null) {
                existing.setDepartment(updatedEmployee.getDepartment());
            }
            if (updatedEmployee.getIsActive() != null) {
                existing.setIsActive(updatedEmployee.getIsActive());
            }
            if (updatedEmployee.getStatus() != null && !updatedEmployee.getStatus().isBlank()) {
                existing.setStatus(updatedEmployee.getStatus());
            }
            return employeeRepository.save(existing);
        });
    }

    /**
     * Delete an employee by ID
     *
     * @param id Employee ID to delete
     * @return true if employee existed and was deleted, false if not found
     */
    public boolean deleteEmployee(Long id) {
        if (employeeRepository.existsById(id)) {
            employeeRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
