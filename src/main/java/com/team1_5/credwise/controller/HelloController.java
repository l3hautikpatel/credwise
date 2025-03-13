package com.team1_5.credwise.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    public static void main(String[] args) {

    }

//    @RequestMapping(value = '/', method = RequestMethod.GET)
    @GetMapping("/")
    public static String  helloworld(){

        return "hello this is CreadWise hello ";
    }
}



