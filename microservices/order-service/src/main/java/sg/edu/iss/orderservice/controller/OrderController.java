package sg.edu.iss.orderservice.controller;

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

import sg.edu.iss.orderservice.model.Order;
import sg.edu.iss.orderservice.repository.OrderRepository;

@RestController
@RequestMapping("/order")
public class OrderController {
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

	@Autowired
	OrderRepository repository;

	@PostMapping
	public Order add(@RequestBody Order order) {
		LOGGER.info("Order add: {}", order);
		return repository.add(order);
	}

	@GetMapping
	public List<Order> findAll() {
		LOGGER.info("Order find");
		return repository.findAll();
	}

	@GetMapping("/{id}")
	public Order findById(@PathVariable("id") Long id) {
		LOGGER.info("Order find: id={}", id);
		return repository.findById(id);
	}

	@GetMapping("/customer/{customerId}")
	public List<Order> findByCustomer(@PathVariable("customerId") Long customerId) {
		LOGGER.info("Customer find: customerId={}", customerId);
		return repository.findByCustomer(customerId);
	}
}
