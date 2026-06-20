package com.example.stepflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.example.stepflow.service.ShopRelationService;
import com.example.stepflow.service.UserService;
import com.example.stepflow.entity.Relation;
import java.util.Collections;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import com.example.stepflow.entity.User;
import org.springframework.security.core.GrantedAuthority;

@Controller
@RequestMapping("/shop/relations")
public class ShopRelationController {

    @Autowired
    private ShopRelationService shopRelationService;

    @Autowired
    private UserService userService;


    @GetMapping
    public String list (
        @AuthenticationPrincipal UserDetails userDetails,
        Model model){
            //サイドバー用（common/sidebar.html）に追加したコードをここにも追加する
            addCommonModelAttributes(userDetails, model);
            //ログインユーザーを取得
            User loginUser = userService.findUserByName(userDetails.getUsername());
            if(loginUser == null || loginUser.getShopId() == null){
                model.addAttribute("relations", Collections.emptyList());
                model.addAttribute("relationError",
                        "所属店舗が設定されていません。管理者にユーザー設定を確認してください。");
                return "shop/relation-list";
            }
            try{
                List<Relation> relations = shopRelationService.findActiveByShopId(loginUser.getShopId());
                model.addAttribute("relations", relations);
            }catch(IllegalArgumentException ex ){
                model.addAttribute("relations", Collections.emptyList());
                model.addAttribute("relationError", ex.getMessage());
            }
            return "shop/relation-list";
        }
    private void addCommonModelAttributes(UserDetails userDetails, Model model){
        model.addAttribute("loginUsername", userDetails.getUsername());
        model.addAttribute("authorityId", userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .findFirst()
        .orElse(""));
    }
}
