package com.example.stepflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.security.core.GrantedAuthority;


@Controller
@RequestMapping("/admin/system-settings")
public class AdminSystemSettingsController {
    @GetMapping("")
    /**
     * システム設定（機能一覧：管理者メニュー）
     * GET http://localhost:8080/admin/system-settings
     */
    public String index(
        @AuthenticationPrincipal UserDetails userDetails,
        Model model){
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority)
              .findFirst()
              .orElse("");
            model.addAttribute("authorityId", authorityId);
            return "admin/system-settings";


    

}
}