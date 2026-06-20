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

import com.example.stepflow.entity.Goods;
import com.example.stepflow.repository.CategoryRepository;
import com.example.stepflow.repository.GoodsRepository;
import com.example.stepflow.service.AdminGoodsService;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/goods")
public class AdminGoodsController {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AdminGoodsService adminGoodsService;
    
    
    /**
     * 商品一覧表示
     * GET http://localhost:8080/admin/goods
     */
    @GetMapping("")
    public String list(
        @RequestParam(name ="categoryId", required = false) Integer categoryId,//カテゴリーIDを取得
        @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);

        //カテゴリー一覧を取得
        model.addAttribute("categories", categoryRepository.findByDeleteFlagOrderByCategoryIdDesc(0));
        model.addAttribute("selectedCategoryId", categoryId);//選択されたカテゴリーIDを取得

        //商品一覧を取得
        List<Goods> goods;
        if(categoryId != null){
            goods = goodsRepository.findByDeleteFlagAndCategoryIdWithCategory(0, categoryId);   
        }else{
            goods = goodsRepository.findByDeleteFlagWithCategory(0);   
        }
        model.addAttribute("goods", goods);
        return "admin/goods-list";
    }

    /**
     * 商品新規登録画面
     * GET http://localhost:8080/admin/goods/new
     */
    @GetMapping("/new")
    public String showCreateGoodsForm(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);

        model.addAttribute("goods", new Goods());
        model.addAttribute("categories", categoryRepository.findByDeleteFlagOrderByCategoryIdDesc(0));
        return "admin/goods-form";
    }

    /**
     * 商品編集画面
     * GET http://localhost:8080/admin/goods/edit/1
     */
    @GetMapping("/edit/{id}")
    public String showEditGoodsForm(
            @PathVariable("id") Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);

        Goods goods = goodsRepository.findById(id).orElse(null);
        if (goods == null || !Integer.valueOf(0).equals(goods.getDeleteFlag())) {
            return "redirect:/admin/goods";
        }
        model.addAttribute("goods", goods);
        model.addAttribute("categories", categoryRepository.findByDeleteFlagOrderByCategoryIdDesc(0));
        return "admin/goods-form";
    }

    /**
     * 商品保存（新規 INSERT / goodsId ありで UPDATE）
     * POST http://localhost:8080/admin/goods
     */
    @PostMapping
    public String save(
            @Validated @ModelAttribute("goods") Goods goods,
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

        boolean isNew = (goods.getGoodsId() == null);

        if (isNew) {
            // 新規：商品名が重複しているかをチェック
            if (goodsRepository.existsByGoodsNameAndDeleteFlag(goods.getGoodsName(), 0)) {
                //商品名が重複している場合は、エラーを追加
                result.rejectValue("goodsName", "error.goodsName.duplicate", "この商品名は既に登録されています");
            }
        } else {
            // 編集：商品名が重複しているかをチェック
            if (goodsRepository.existsByGoodsNameAndDeleteFlagAndGoodsIdNot(
                goods.getGoodsName(), 0, goods.getGoodsId())) {
                // 商品名が重複している場合は、エラーを追加
                result.rejectValue("goodsName", "error.goodsName.duplicate", "この商品名は既に登録されています");
            }
        }
        // エラーがある場合は、商品登録画面を表示
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findByDeleteFlagOrderByCategoryIdDesc(0));
            return "admin/goods-form";
        }

        if (isNew) {
            // 新規：商品保存 + 全店舗・全倉庫に在庫0を登録
            adminGoodsService.registerGoodsWithInitialStock(goods);
        } else {
            // 編集：商品だけ更新（在庫は触らない）
            adminGoodsService.updateGoods(goods);
        }
        String successMessage = isNew ? "商品が新規登録されました。" : "商品が更新されました。";
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return "redirect:/admin/goods";
    }

    /**
     * 商品論理削除（delete_flag = 1）
     * POST http://localhost:8080/admin/goods/delete/3
     */
    @PostMapping("/delete/{id}")
    public String delete(
            @PathVariable("id") Integer id,
            RedirectAttributes redirectAttributes) {
        adminGoodsService.deleteGoodsWithStocks(id);
        redirectAttributes.addFlashAttribute("successMessage", "商品が削除されました。");
        return "redirect:/admin/goods";
    }
}
