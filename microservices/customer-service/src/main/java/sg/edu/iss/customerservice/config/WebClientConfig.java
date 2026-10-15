package sg.edu.iss.customerservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import sg.edu.iss.customerservice.client.OrderClient;

@Configuration
public class WebClientConfig {

	@Autowired
	private LoadBalancedExchangeFilterFunction filterFunction;

	@Bean
	public WebClient orderWebClient() {
		return WebClient.builder().baseUrl("http://order-service").filter(filterFunction).build();
	}

	@Bean
	public OrderClient orderClient() {
		HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
				.builder(WebClientAdapter.forClient(orderWebClient())).build();
		return httpServiceProxyFactory.createClient(OrderClient.class);
	}

}
