package com.learn.rest.HelloApp.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping(method = RequestMethod.GET, path="/")
    public String helloWorld(){
        return "Hello World from the App";

    }

    @GetMapping(path="/welcome")
    public String Welcome(){
        return "Welcome from the App";
    }


}
