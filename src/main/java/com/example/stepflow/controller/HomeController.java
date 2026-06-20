package com.example.stepflow.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.stepflow.dto.OrderRankingRow;
import com.example.stepflow.entity.User;
import com.example.stepflow.service.DailyOrderSummaryService;
import com.example.stepflow.service.UserService;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @Autowired
    private DailyOrderSummaryService dailyOrderSummaryService;

    @GetMapping("/home")
    public String showHomePage(
            @RequestParam(name = "scope", defaultValue = "my") String scope,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());

        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser != null) {
            // ホーム表示用：権限に応じて所属IDを出し分ける
            // （データ上は両方入っていても、店舗は shopId のみ・倉庫は warehouseId のみ表示）
            Integer displayShopId = null;
            Integer displayWarehouseId = null;
            if ("ROLE_SHOP".equals(authorityId)) {
                displayShopId = loginUser.getShopId();
            } else if ("ROLE_WAREHOUSE".equals(authorityId)) {
                displayWarehouseId = loginUser.getWarehouseId();
            }
            model.addAttribute("displayShopId", displayShopId);
            model.addAttribute("displayWarehouseId", displayWarehouseId);
        } else {
            model.addAttribute("displayShopId", null);
            model.addAttribute("displayWarehouseId", null);
        }

        if ("ROLE_SHOP".equals(authorityId) && loginUser != null && loginUser.getShopId() != null) {
            addShopDashboard(model, scope, loginUser.getShopId());
        }

        return "home";
    }

    private void addShopDashboard(Model model, String scope, Integer myShopId) {
        boolean allShops = "all".equalsIgnoreCase(scope);//全店舗を表示するかどうか
        Integer filterShopId = allShops ? null : myShopId;//自店舗を表示するかどうか

        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusMonths(1);

        List<OrderRankingRow> rankingRows = dailyOrderSummaryService.findRanking(
                fromDate, toDate, filterShopId);

        model.addAttribute("showShopDashboard", true);
        model.addAttribute("rankingScope", allShops ? "all" : "my");
        model.addAttribute("rankingRows", rankingRows);
        model.addAttribute("rankingFrom", fromDate);
        model.addAttribute("rankingTo", toDate);
        model.addAttribute("rankingEmpty", rankingRows.isEmpty());
    }
}
