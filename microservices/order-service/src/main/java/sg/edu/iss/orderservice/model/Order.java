package sg.edu.iss.orderservice.model;

public record Order(Long id, Long customerId, String itemName, int quantity) {

}
