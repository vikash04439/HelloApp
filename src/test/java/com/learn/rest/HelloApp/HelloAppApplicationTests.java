package com.learn.rest.HelloApp;

import com.learn.rest.HelloApp.entity.Employee;
import com.learn.rest.HelloApp.repository.EmployeeRepository;
import com.learn.rest.HelloApp.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class HelloAppApplicationTests {

	@Autowired
	private EmployeeService employeeService;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Test
	void contextLoads() {
	}

	/**
	 * Test GET /allemployee returns only active employees
	 */
	@Test
	void testGetAllActiveEmployees() {
		List<Employee> activeEmployees = employeeService.getAllActiveEmployees();
		assertNotNull(activeEmployees);
		assertTrue(activeEmployees.size() > 0, "Should have at least one active employee from seed data");
		for (Employee emp : activeEmployees) {
			assertTrue(emp.getIsActive(), "All returned employees should be active");
		}
	}

	/**
	 * Test GET /allemployee-notactive returns only inactive employees
	 */
	@Test
	void testGetAllNotActiveEmployees() {
		List<Employee> notActiveEmployees = employeeService.getAllNotActiveEmployees();
		assertNotNull(notActiveEmployees);
		for (Employee emp : notActiveEmployees) {
			assertFalse(emp.getIsActive(), "All returned employees should be inactive");
		}
	}

	/**
	 * Test POST /addemployee with explicit status value
	 */
	@Test
	void testAddEmployeeWithExplicitStatus() {
		Employee newEmployee = new Employee("Test User", "test@example.com", "Engineering", true, "S");
		Employee savedEmployee = employeeService.createEmployee(newEmployee);

		assertNotNull(savedEmployee.getId(), "Saved employee should have an ID");
		assertEquals("Test User", savedEmployee.getName());
		assertEquals("test@example.com", savedEmployee.getEmail());
		assertEquals("Engineering", savedEmployee.getDepartment());
		assertTrue(savedEmployee.getIsActive());
		assertEquals("S", savedEmployee.getStatus(), "Status should match provided value");
	}

	/**
	 * Test POST /addemployee with null status defaults to 'Y'
	 */
	@Test
	void testAddEmployeeWithNullStatusDefaultsToY() {
		Employee newEmployee = new Employee("Test User 2", "test2@example.com", "Finance", true, null);
		Employee savedEmployee = employeeService.createEmployee(newEmployee);

		assertNotNull(savedEmployee.getId(), "Saved employee should have an ID");
		assertEquals("Test User 2", savedEmployee.getName());
		assertEquals("test2@example.com", savedEmployee.getEmail());
		assertEquals("Finance", savedEmployee.getDepartment());
		assertTrue(savedEmployee.getIsActive());
		assertEquals("Y", savedEmployee.getStatus(), "Status should default to 'Y' when null");
	}

	/**
	 * Test POST /addemployee with empty status defaults to 'Y'
	 */
	@Test
	void testAddEmployeeWithEmptyStatusDefaultsToY() {
		Employee newEmployee = new Employee("Test User 3", "test3@example.com", "HR", false, "");
		Employee savedEmployee = employeeService.createEmployee(newEmployee);

		assertNotNull(savedEmployee.getId(), "Saved employee should have an ID");
		assertEquals("Test User 3", savedEmployee.getName());
		assertEquals("test3@example.com", savedEmployee.getEmail());
		assertEquals("HR", savedEmployee.getDepartment());
		assertFalse(savedEmployee.getIsActive());
		assertEquals("Y", savedEmployee.getStatus(), "Status should default to 'Y' when empty");
	}

	/**
	 * Test POST /addemployee accepts client-supplied isActive value
	 */
	@Test
	void testAddEmployeeAcceptsClientIsActiveValue() {
		Employee activeEmp = new Employee("Active Employee", "active@example.com", "Sales", true, "Y");
		Employee savedActive = employeeService.createEmployee(activeEmp);
		assertTrue(savedActive.getIsActive(), "Should save with isActive=true");

		Employee inactiveEmp = new Employee("Inactive Employee", "inactive@example.com", "Support", false, "D");
		Employee savedInactive = employeeService.createEmployee(inactiveEmp);
		assertFalse(savedInactive.getIsActive(), "Should save with isActive=false");
	}

}


