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

import com.example.stepflow.entity.User;
import com.example.stepflow.entity.WarehouseStock;
import com.example.stepflow.service.UserService;
import com.example.stepflow.service.WarehouseInventoryService;

@Controller
@RequestMapping("/warehouse/inventory")
public class WarehouseInventoryController {

    @Autowired
    private UserService userService;

    @Autowired
    private WarehouseInventoryService warehouseInventoryService;

    /** GET /warehouse/inventory … 自倉庫の在庫一覧 */
    @GetMapping
    public String list(
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        addSidebarAttributes(userDetails, model);
        model.addAttribute("categories", warehouseInventoryService.findActiveCategories());
        model.addAttribute("selectedCategoryId", categoryId);

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getWarehouseId() == null) {
            model.addAttribute("stocks", Collections.emptyList());
            model.addAttribute("inventoryError",
                    "所属倉庫が設定されていません。管理者にユーザー設定を確認してください。");
            return "warehouse/inventory-list";
        }

        try {
            List<WarehouseStock> stocks = warehouseInventoryService.findActiveByWarehouseIdFiltered(
                    loginUser.getWarehouseId(), categoryId);
            model.addAttribute("stocks", stocks);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("stocks", Collections.emptyList());
            model.addAttribute("inventoryError", ex.getMessage());
        }
        return "warehouse/inventory-list";
    }

    /** GET /warehouse/inventory/edit/{warehouseStockId} … 在庫編集画面 */
    @GetMapping("/edit/{warehouseStockId}")
    public String showEditForm(
            @PathVariable("warehouseStockId") Integer warehouseStockId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        addSidebarAttributes(userDetails, model);

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getWarehouseId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "所属倉庫が設定されていません。");
            return "redirect:/warehouse/inventory";
        }

        WarehouseStock stock = warehouseInventoryService.findEditableStock(
                loginUser.getWarehouseId(), warehouseStockId);
        if (stock == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "在庫が見つかりませんでした。");
            return "redirect:/warehouse/inventory";
        }

        model.addAttribute("stock", stock);
        return "warehouse/inventory-edit";
    }

    /** POST /warehouse/inventory/edit/{warehouseStockId} … 在庫数量更新 */
    @PostMapping("/edit/{warehouseStockId}")
    public String updateQuantity(
            @PathVariable("warehouseStockId") Integer warehouseStockId,
            @RequestParam(name = "quantity", required = false) String quantityText,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getWarehouseId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "所属倉庫が設定されていません。");
            return "redirect:/warehouse/inventory";
        }

        Integer quantity = parseQuantity(quantityText, model);
        if (quantity == null) {
            return renderEditFormOnError(loginUser.getWarehouseId(), warehouseStockId, quantityText, userDetails, model,
                    redirectAttributes);
        }

        boolean ok = warehouseInventoryService.updateStockQuantity(
                loginUser.getWarehouseId(), warehouseStockId, quantity);
        if (ok) {
            redirectAttributes.addFlashAttribute("successMessage", "在庫数量が更新されました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "在庫数量の更新に失敗しました。");
        }
        return "redirect:/warehouse/inventory";
    }

    private Integer parseQuantity(String quantityText, Model model) {
        if (quantityText == null || quantityText.isBlank()) {
            model.addAttribute("quantityError", "在庫数を入力してください");
            return null;
        }
        try {
            int value = Integer.parseInt(quantityText.strip());
            if (value < 0) {
                model.addAttribute("quantityError", "在庫数は0以上で入力してください");
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            model.addAttribute("quantityError", "在庫数は数値で入力してください");
            return null;
        }
    }

    private String renderEditFormOnError(
            Integer warehouseId,
            Integer warehouseStockId,
            String quantityText,
            UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        WarehouseStock stock = warehouseInventoryService.findEditableStock(warehouseId, warehouseStockId);
        if (stock == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "在庫が見つかりませんでした。");
            return "redirect:/warehouse/inventory";
        }
        addSidebarAttributes(userDetails, model);
        model.addAttribute("stock", stock);
        model.addAttribute("inputQuantity", quantityText);
        return "warehouse/inventory-edit";
    }

    private void addSidebarAttributes(UserDetails userDetails, Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);
    }
}
