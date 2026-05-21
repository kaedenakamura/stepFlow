package com.example.stepflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.security.core.GrantedAuthority;
import java.util.List;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.repository.WarehouseRepository;

@Controller
@RequestMapping("/admin/warehouses") //管理者用/倉庫のURL共通部分
public class AdminWarehouseController {
    @Autowired
    private WarehouseRepository warehouseRepository;

      /**
     * 倉庫一覧表示（機能一覧：管理者＞倉庫管理＞倉庫一覧表示画面）
     * GET http://localhost:8080/admin/warehouses
     */
    @GetMapping
    public String list(
        @AuthenticationPrincipal UserDetails userDetails,
        Model model
    ){
        //サイドバー共通処理(AdmininquiryControllerと同じ)IDを取得して画面に渡す
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .findFirst()
        .orElse("");
        model.addAttribute("authorityId",authorityId);

        //DBから有効な倉庫だけ取得(delete_flag=0)
        List<Warehouse> warehouses = warehouseRepository.findByDeleteFlagOrderByWarehouseIdDesc(0);
        model.addAttribute("warehouses",warehouses);

        //warehouse-list.htmlを表示
        return "admin/warehouse-list";
     }


}
