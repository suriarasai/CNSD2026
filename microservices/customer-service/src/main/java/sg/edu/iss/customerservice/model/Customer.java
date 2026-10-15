package sg.edu.iss.customerservice.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Customer {

	private Long id;
	private String name;
	private List<Order> orders = new ArrayList<>();

	public Customer(Long id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

}
