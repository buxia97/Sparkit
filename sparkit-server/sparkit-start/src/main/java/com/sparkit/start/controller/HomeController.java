package com.sparkit.start.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页控制器 - 跳转到 Sparkit 欢迎页
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
