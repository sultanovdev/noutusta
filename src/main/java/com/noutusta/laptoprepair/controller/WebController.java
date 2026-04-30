package com.noutusta.laptoprepair.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/services")
    public String services() {
        return "services";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/categories/accessories-tech")
    public String accessoriesTech() {
        return "category-accessories-tech";
    }

    @GetMapping("/categories/laptop-pc")
    public String laptopPc() {
        return "category-laptop-pc";
    }

    @GetMapping("/categories/soft")
    public String soft() {
        return "category-soft";
    }

    @GetMapping("/categories/batteries-chargers")
    public String batteriesChargers() {
        return "category-batteries-chargers";
    }
}
