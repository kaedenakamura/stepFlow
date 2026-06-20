package com.example.stepflow.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.stepflow.entity.Shop;
import com.example.stepflow.repository.ShopRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.stepflow.service.AdminShopService;

/**
 * 管理者向け：店舗一覧（機能一覧：管理者 → 店舗管理 → 店舗一覧表示画面）
 */
@Controller
@RequestMapping("/admin/shops")
public class AdminShopController {

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private AdminShopService adminShopService;


	@GetMapping
	public String list(
			@RequestParam(name = "address", required = false) String address,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		model.addAttribute("loginUsername", userDetails.getUsername());
		String authorityId = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.findFirst()
				.orElse("");
		model.addAttribute("authorityId", authorityId);
		model.addAttribute("addressFilter", address != null ? address : "");

		try {
			List<Shop> shops;
			if (address != null && !address.isBlank()) {
				shops = shopRepository.findByDeleteFlagAndShopAddressContainingOrderByShopIdDesc(
						0, address.strip());
			} else {
				shops = shopRepository.findByDeleteFlagOrderByShopIdDesc(0);
			}
			model.addAttribute("shops", shops);
		} catch (DataAccessException ex) {
			model.addAttribute("shops", Collections.emptyList());
			model.addAttribute("shopError",
					"店舗データを取得できません。MySQL に shop テーブルがあるか確認してください。（"
							+ ex.getMostSpecificCause().getMessage() + "）");
		}

		return "admin/shop-list";
	}
  // ========== ② 新規登録画面 ==========
    /**
     * GET http://localhost:8080/admin/shops/new
     * 空の Shop をフォームに渡す（倉庫の new Warehouse() と同じ）
     */
	@GetMapping("/new")
	public String showNewForm(
		@AuthenticationPrincipal UserDetails userDetails,
		Model model){
		//サイドバー共通処理(AdmininquiryControllerと同じ)IDを取得して画面に渡す
		
		model.addAttribute("loginUsername", userDetails.getUsername());
		String authorityId = userDetails.getAuthorities().stream()
		    .map(GrantedAuthority::getAuthority)
			.findFirst()
			.orElse("");
			model.addAttribute("authorityId",authorityId);
		
		model.addAttribute("shop",new Shop());
		return "admin/shop-form";
		}

		   // ========== ③ 編集画面 ==========
    /**
     * GET http://localhost:8080/admin/shops/edit/2
     * 倉庫の edit と同じ。削除済みは開かせない（※条件に注意）
     */

	@GetMapping("/edit/{id}")
	public String showEditForm(
		@PathVariable("id") Integer id,
		@AuthenticationPrincipal UserDetails userDetails,
		Model model){
        //サイドバー共通処理(AdmininquiryControllerと同じ)IDを取得して画面に渡す
		model.addAttribute("loginUsername", userDetails.getUsername());
		String authorityId = userDetails.getAuthorities().stream()
		  .map(GrantedAuthority::getAuthority)
		  .findFirst()
		  .orElse("");
		  model.addAttribute("authorityId",authorityId);

		  //DBから店舗を取得(delete_flag=0)
		  Shop shop =shopRepository.findById(id).orElse(null);

		  //店舗が存在しないか、削除されている場合は店舗一覧画面にリダイレクト
		  if(shop == null || !Integer.valueOf(0).equals(shop.getDeleteFlag())){
			return "redirect:/admin/shops";
		  }
		  model.addAttribute("shop",shop);
		  return "admin/shop-form"; //新規と同じHTMLを使いまわす
		}
		  // ========== ④ 保存（新規 INSERT / 編集 UPDATE） ==========
    /**
     * POST http://localhost:8080/admin/shops
     * shopId が null → INSERT、値あり → UPDATE（JPA が自動判定）
     */
	@PostMapping
	public String save(
		@Validated @ModelAttribute("shop") Shop shop, 
		BindingResult result,
		@AuthenticationPrincipal UserDetails userDetails,
		Model model,
		RedirectAttributes redirectAttributes){
		//サイドバー共通処理(AdmininquiryControllerと同じ)IDを取得して画面に渡す
		//BindingResultはバリデーションの結果を受け取るため
		//RedirectAttributesはリダイレクト時にメッセージを渡すため
		//@Validatedはバリデーションの結果を受け取るため
		//@ModelAttributeはフォームから送信されたデータを受け取るため
		//@AuthenticationPrincipalはログインしているユーザーの情報を受け取るため
		//Modelは画面に渡すデータを受け取るため
		//@PathVariableはURLのパラメータを受け取るため
		if(result.hasErrors()){
			//バリデーションエラーがある場合は店舗一覧画面にリダイレクト
		  model.addAttribute("loginUsername", userDetails.getUsername());
		  String authorityId = userDetails.getAuthorities().stream()
		      .map(GrantedAuthority::getAuthority)
			  .findFirst()
			  .orElse("");
			model.addAttribute("authorityId",authorityId);
			return "admin/shop-form";
		}

		boolean isNew = (shop.getShopId() == null);//save前に新規登録か編集かを判定
        if(isNew){
			adminShopService.registerShopWithInitialStock(shop);
		}else{
			adminShopService.updateShop(shop);
	
		}
		String successMessage = isNew ? "店舗に登録しました":"店舗を更新しました";
		redirectAttributes.addFlashAttribute("successMessage",successMessage);
		return "redirect:/admin/shops";
	}

    // ========== ⑤ 論理削除 ==========
    /**
     * POST http://localhost:8080/admin/shops/delete/3
     * 物理削除ではなく delete_flag = 1
     */
	@PostMapping("/delete/{id}")
	public String delete(
		@PathVariable("id") Integer id,
		RedirectAttributes redirectAttributes){
		//DBから店舗を取得(delete_flag=0)
		Shop shop =shopRepository.findById(id).orElse(null);
		if(shop != null && Integer.valueOf(0).equals(shop.getDeleteFlag())){
			adminShopService.deleteShopWithStocks(id);
			redirectAttributes.addFlashAttribute("successMessage","店舗を削除しました");
		}
		return "redirect:/admin/shops";
		}

}


