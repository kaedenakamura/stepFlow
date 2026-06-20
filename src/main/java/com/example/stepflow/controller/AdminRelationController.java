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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.stepflow.entity.Relation;
import com.example.stepflow.repository.RelationRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.WarehouseRepository;

@Controller
@RequestMapping("/admin/relations")
public class AdminRelationController {

	@Autowired
	private RelationRepository relationRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@GetMapping
	public String list(
			@RequestParam(name = "shopId", required = false) Integer shopId,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		addSidebarAttributes(userDetails, model);

		model.addAttribute("shops", shopRepository.findByDeleteFlagOrderByShopIdDesc(0));
		model.addAttribute("selectedShopId", shopId);

		List<Relation> relations;
		if (shopId != null) {
			relations = relationRepository.findByDeleteFlagAndShopIdWithShopAndWarehouse(0, shopId);
		} else {
			relations = relationRepository.findByDeleteFlagWithShopAndWarehouse(0);
		}
		model.addAttribute("relations", relations);

		return "admin/relation-list";
	}

	@GetMapping("/new")
	public String showNewForm(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		addSidebarAttributes(userDetails, model);
		addMasterOptions(model);
		model.addAttribute("relation", new Relation());
		return "admin/relation-form";
	}

	@GetMapping("/edit/{id}")
	public String showEditForm(
			@PathVariable("id") Integer id,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		addSidebarAttributes(userDetails, model);
		addMasterOptions(model);

		Relation relation = relationRepository.findById(id).orElse(null);
		if (relation == null || !Integer.valueOf(0).equals(relation.getDeleteFlag())) {
			return "redirect:/admin/relations";
		}

		model.addAttribute("relation", relation);
		return "admin/relation-form";
	}

	@PostMapping
	public String save(
			@Validated @ModelAttribute("relation") Relation relation,
			BindingResult result,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model,
			RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			addSidebarAttributes(userDetails, model);
			addMasterOptions(model);
			return "admin/relation-form";
		}

		boolean isNew = (relation.getRelationId() == null);
		relation.setDeleteFlag(0);
		relationRepository.save(relation);

		redirectAttributes.addFlashAttribute("successMessage",
				isNew ? "連携を登録しました" : "連携を更新しました");
		return "redirect:/admin/relations";
	}

	@PostMapping("/delete/{id}")
	public String delete(
			@PathVariable("id") Integer id,
			RedirectAttributes redirectAttributes) {

		Relation relation = relationRepository.findById(id).orElse(null);
		if (relation != null && Integer.valueOf(0).equals(relation.getDeleteFlag())) {
			relation.setDeleteFlag(1);
			relationRepository.save(relation);
			redirectAttributes.addFlashAttribute("successMessage", "連携を削除しました");
		}
		return "redirect:/admin/relations";
	}

	/** サイドバー表示用（loginUsername / authorityId） */
	private void addSidebarAttributes(UserDetails userDetails, Model model) {
		model.addAttribute("loginUsername", userDetails.getUsername());
		String authorityId = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.findFirst()
				.orElse("");
		model.addAttribute("authorityId", authorityId);
	}

	/** 新規・編集フォームの店舗・倉庫プルダウン用 */
	private void addMasterOptions(Model model) {
		model.addAttribute("shops", shopRepository.findByDeleteFlagOrderByShopIdDesc(0));
		model.addAttribute("warehouses", warehouseRepository.findByDeleteFlagOrderByWarehouseIdDesc(0));
	}
}
