package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FooController {

    @GetMapping("/foo")
    public String foo() {
        return "Hello!";
        //return authentication.getTokenAttributes().get("sub") + " is the subject";
    }
}
