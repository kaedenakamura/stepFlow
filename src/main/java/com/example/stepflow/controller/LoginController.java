package com.example.stepflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class LoginController {
		@GetMapping("/login")
	    public String showLoginForm() {
	        return "login"; // ここで「ログイン画面を表示する」という処理をします。
	    }
}
