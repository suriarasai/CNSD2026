package sg.edu.iss.customerservice.model;

public record Order(Long id, Long customerId, String itemName, int quantity) {

}
