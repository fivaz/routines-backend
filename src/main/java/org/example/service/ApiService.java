package org.example.service;


import org.example.model.ApiData;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ApiService {
    private final RestTemplate restTemplate;

    public ApiService() {
        this.restTemplate = new RestTemplate();
    }

    public ApiData callFirstApi(String param) {
        String url = "https://api1.example.com/endpoint?param=" + param;
        return restTemplate.getForObject(url, ApiData.class);
    }

    public ApiData callSecondApi(String param) {
        String url = "https://api2.example.com/endpoint?param=" + param;
        return restTemplate.getForObject(url, ApiData.class);
    }
}
