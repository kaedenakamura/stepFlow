package com.example.stepflow.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stepflow.entity.ShopStock;
import com.example.stepflow.entity.User;
import com.example.stepflow.service.ShopInventoryService;
import com.example.stepflow.service.UserService;

@Controller
@RequestMapping("/shop/inventory")
public class ShopInventoryController {

    @Autowired
    private UserService userService;

    @Autowired
    private ShopInventoryService shopInventoryService;

    /** GET /shop/inventory … 自店舗の在庫一覧 */
    @GetMapping
    public String list(
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        addSidebarModel(userDetails, model);
        model.addAttribute("categories", shopInventoryService.findActiveCategories());
        model.addAttribute("selectedCategoryId", categoryId);

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getShopId() == null) {
            model.addAttribute("stocks", Collections.emptyList());
            model.addAttribute("inventoryError",
                    "所属店舗が設定されていません。管理者にユーザー設定を確認してください。");
            return "shop/inventory-list";
        }

        try {
            List<ShopStock> stocks = shopInventoryService.findActiveByShopIdFiltered(
                    loginUser.getShopId(), categoryId);
            model.addAttribute("stocks", stocks);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("stocks", Collections.emptyList());
            model.addAttribute("inventoryError", ex.getMessage());
        }

        return "shop/inventory-list";
    }

    /** GET /shop/inventory/edit/{shopStockId} … 在庫編集画面 */
    @GetMapping("/edit/{shopStockId}")
    public String showEditForm(
            @PathVariable("shopStockId") Integer shopStockId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        addSidebarModel(userDetails, model);

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getShopId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "所属店舗が設定されていません。");
            return "redirect:/shop/inventory";
        }

        ShopStock stock = shopInventoryService.findEditableStock(
                loginUser.getShopId(), shopStockId);
        if (stock == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "在庫が見つかりませんでした。");
            return "redirect:/shop/inventory";
        }

        model.addAttribute("stock", stock);
        return "shop/inventory-edit";
    }

    /** POST /shop/inventory/edit/{shopStockId} … 在庫数量更新 */
    @PostMapping("/edit/{shopStockId}")
    public String updateQuantity(
            @PathVariable("shopStockId") Integer shopStockId,
            @RequestParam("quantity") Integer quantity,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getShopId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "所属店舗が設定されていません。");
            return "redirect:/shop/inventory";
        }

        boolean ok = shopInventoryService.updateStockQuantity(
                loginUser.getShopId(), shopStockId, quantity);
        if (ok) {
            redirectAttributes.addFlashAttribute("successMessage", "在庫を更新しました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "在庫を更新できませんでした。数量を確認してください。");
        }
        return "redirect:/shop/inventory";
    }

    private void addSidebarModel(UserDetails userDetails, Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);
    }
}
