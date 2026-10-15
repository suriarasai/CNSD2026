package sg.edu.iss.customerservice.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import sg.edu.iss.customerservice.model.Customer;

@Repository
public class CustomerRepository {

	private List<Customer> customers = new ArrayList<>();

	public Customer addCustomer(Customer customer) {
		customers.add(customer);
		return customer;
	}

	public Customer findById(Long id) {
		return customers.stream().filter(customer -> customer.getId().equals(id)).findFirst().orElseThrow();
	}

	public List<Customer> findAll() {
		return customers;
	}

}
