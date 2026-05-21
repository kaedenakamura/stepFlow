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

import com.example.stepflow.entity.Shop;
import com.example.stepflow.repository.ShopRepository;

/**
 * 管理者向け：店舗一覧（機能一覧：管理者 → 店舗管理 → 店舗一覧表示画面）
 */
@Controller
@RequestMapping("/admin/shops")
public class AdminShopController {

	@Autowired
	private ShopRepository shopRepository;

	@GetMapping
	public String list(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		model.addAttribute("loginUsername", userDetails.getUsername());
		String authorityId = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.findFirst()
				.orElse("");
		model.addAttribute("authorityId", authorityId);

		try {
			List<Shop> shops = shopRepository.findByDeleteFlagOrderByShopIdDesc(0);
			model.addAttribute("shops", shops);
		} catch (DataAccessException ex) {
			model.addAttribute("shops", Collections.emptyList());
			model.addAttribute("shopError",
					"店舗データを取得できません。MySQL に shop テーブルがあるか確認してください。（"
							+ ex.getMostSpecificCause().getMessage() + "）");
		}

		return "admin/shop-list";
	}
}
