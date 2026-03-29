package com.learn.rest.HelloApp.repository;

import com.learn.rest.HelloApp.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Find all active employees
     * @return List of employees where isActive is true
     */
    List<Employee> findByIsActiveTrue();

    List<Employee> findByIsActiveFalse();
}

