package com.ddak.yongha.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FcmTestChildController {

    @GetMapping("/child")
    public String showChildPage() {
        return "FcmTestchild";  // templates/FcmTestchild.html
    }
}
