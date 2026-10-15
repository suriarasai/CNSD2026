package sg.edu.iss.orderservice.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import sg.edu.iss.orderservice.model.Order;

@Repository
public class OrderRepository {

	private List<Order> orders = new ArrayList<>();

	public Order add(Order order) {
		orders.add(order);
		return order;
	}

	public Order findById(Long id) {
		return orders.stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
	}

	public List<Order> findAll() {
		return orders;
	}

	public List<Order> findByCustomer(Long customerId) {
		return orders.stream().filter(a -> a.customerId().equals(customerId)).toList();
	}

}
