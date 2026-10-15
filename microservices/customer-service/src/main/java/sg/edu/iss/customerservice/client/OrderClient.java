package sg.edu.iss.customerservice.client;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import sg.edu.iss.customerservice.model.Order;

@HttpExchange
public interface OrderClient {
	
	@GetExchange("/order/customer/{customerId}")
    public List<Order> findByCustomer(@PathVariable("customerId") Long customerId);

}
