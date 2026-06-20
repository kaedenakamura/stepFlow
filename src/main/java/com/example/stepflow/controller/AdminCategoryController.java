package com.example.stepflow.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stepflow.entity.Category;
import com.example.stepflow.repository.CategoryRepository;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * カテゴリ一覧表示
     * GET http://localhost:8080/admin/categories
     */
    @GetMapping("")
    public String list(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);

        List<Category> categories = categoryRepository.findByDeleteFlagOrderByCategoryIdDesc(0);
        model.addAttribute("categories", categories);
        return "admin/category-list";
    }

    /**
     * カテゴリ新規登録画面
     * GET http://localhost:8080/admin/categories/new
     */
    @GetMapping("/new")
    public String showCreateCategoryForm(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);

        model.addAttribute("category", new Category());
        return "admin/category-form";
    }

    /**
     * カテゴリ編集画面
     * GET http://localhost:8080/admin/categories/edit/1
     */
    @GetMapping("/edit/{id}")
    public String showEditCategoryForm(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);

        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null || !Integer.valueOf(0).equals(category.getDeleteFlag())) {
            return "redirect:/admin/categories";
        }
        model.addAttribute("category", category);
        return "admin/category-form";
    }

    /**
     * カテゴリ保存（新規 INSERT / categoryId ありで UPDATE）
     * POST http://localhost:8080/admin/categories
     */
    @PostMapping
    public String save(
            @Validated @ModelAttribute("category") Category category,
            BindingResult result,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);

        if (result.hasErrors()) {
            return "admin/category-form";
        }

        boolean isNew = (category.getCategoryId() == null);
        category.setDeleteFlag(0);
        categoryRepository.save(category);

        String successMessage = isNew ? "カテゴリーが新規登録されました。" : "カテゴリーが更新されました。";
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return "redirect:/admin/categories";
    }

    /**
     * カテゴリ論理削除（delete_flag = 1）
     * POST http://localhost:8080/admin/categories/delete/3
     */
    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category != null && Integer.valueOf(0).equals(category.getDeleteFlag())) {
            category.setDeleteFlag(1);
            categoryRepository.save(category);
            redirectAttributes.addFlashAttribute("successMessage", "カテゴリーが論理削除されました。");
        }
        return "redirect:/admin/categories";
    }
}
