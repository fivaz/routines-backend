package org.example.controller;

import org.example.model.ApiData;
import org.example.service.ApiService;
import org.example.service.FirebaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {
    @Autowired
    private ApiService apiService;

    @Autowired
    private FirebaseService firebaseService;

    @GetMapping("/process")
    public String processData(@RequestParam String param) {

        // Call first API
        ApiData firstApiResult = apiService.callFirstApi(param);
//
//        // Call second API using result from first
//        ApiData secondApiResult = apiService.callSecondApi(firstApiResult.getData());
//
//        // Save to Firebase
//        firebaseService.saveToFirestore("results", secondApiResult.getId(), secondApiResult);
//
//        return "Data processed successfully";
    }
}