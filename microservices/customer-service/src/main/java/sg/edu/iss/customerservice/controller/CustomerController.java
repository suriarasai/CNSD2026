package sg.edu.iss.customerservice.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sg.edu.iss.customerservice.client.OrderClient;
import sg.edu.iss.customerservice.model.Customer;
import sg.edu.iss.customerservice.repository.CustomerRepository;

@RestController
@RequestMapping("/customer")
public class CustomerController {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerController.class);

	@Autowired
	private CustomerRepository repository;

	@Autowired
	private OrderClient orderClient;

	@PostMapping
	public Customer add(@RequestBody Customer customer) {
		LOGGER.info("Customer add: {}", customer);
		return repository.addCustomer(customer);
	}

	@GetMapping
	public List<Customer> findAll() {
		LOGGER.info("Customer find");
		return repository.findAll();
	}

	@GetMapping("/{id}")
	public Customer findById(@PathVariable Long id) {
		LOGGER.info("Customer find: id={}", id);
		return repository.findById(id);
	}

	@GetMapping("/with-orders")
	public List<Customer> findAllWithOrders() {
		LOGGER.info("Customer find");
		List<Customer> customers = repository.findAll();
		customers.forEach(customer -> customer.setOrders(orderClient.findByCustomer(customer.getId())));
		return customers;
	}

}
