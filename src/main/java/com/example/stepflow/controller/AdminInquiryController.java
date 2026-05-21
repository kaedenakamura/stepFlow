package com.example.stepflow.controller;

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

import com.example.stepflow.dto.AdminInquiryListRow;
import com.example.stepflow.entity.Inquiry;
import com.example.stepflow.entity.User;
import com.example.stepflow.repository.InquiryRepository;
import com.example.stepflow.service.AdminInquiryService;
import com.example.stepflow.service.UserService;

/**
 * 管理者向け：問い合わせ一覧・詳細・ステータス更新・論理削除。
 */
@Controller
@RequestMapping("/admin/inquiries")
public class AdminInquiryController {

	@Autowired
	private InquiryRepository inquiryRepository;

	@Autowired
	private AdminInquiryService adminInquiryService;

	@Autowired
	private UserService userService;

	@GetMapping
	public String list(
			@RequestParam(name = "status", required = false) String statusFilter,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {
		addSidebarModel(userDetails, model);

		List<Inquiry> inquiries;
		if (statusFilter != null && !statusFilter.isBlank()) {
			inquiries = inquiryRepository.findByDeleteFlagAndInquiryStatusOrderByInquiryIdDesc(0,
					statusFilter.strip());
		} else {
			inquiries = inquiryRepository.findByDeleteFlagOrderByInquiryIdDesc(0);
		}

		List<AdminInquiryListRow> rows = adminInquiryService.toListRows(inquiries);
		model.addAttribute("rows", rows);
		model.addAttribute("statusFilter", statusFilter != null ? statusFilter : "");

		return "admin/inquiry-list";
	}

	@GetMapping("/{id}")
	public String detail(
			@PathVariable Integer id,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model,
			RedirectAttributes redirectAttributes) {
		addSidebarModel(userDetails, model);

		Inquiry inquiry = adminInquiryService.findActiveForAdmin(id);
		if (inquiry == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "問い合わせが存在しないか、削除済みです。");
			return "redirect:/admin/inquiries";
		}

		User sender = userService.getUserById(inquiry.getUserId());
		model.addAttribute("inquiry", inquiry);
		model.addAttribute("sender", sender);
		model.addAttribute("affiliationDisplay", adminInquiryService.formatAffiliation(sender));

		return "admin/inquiry-detail";
	}

	@PostMapping("/{id}/status")
	public String updateStatus(
			@PathVariable Integer id,
			@RequestParam("inquiryStatus") String inquiryStatus,
			RedirectAttributes redirectAttributes) {
		try {
			boolean ok = adminInquiryService.updateStatus(id, inquiryStatus.strip());
			if (!ok) {
				redirectAttributes.addFlashAttribute("errorMessage", "更新できませんでした。");
				return "redirect:/admin/inquiries";
			}
			redirectAttributes.addFlashAttribute("successMessage", "ステータスを更新しました。");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/admin/inquiries/" + id;
	}

	@PostMapping("/{id}/delete")
	public String delete(
			@PathVariable Integer id,
			RedirectAttributes redirectAttributes) {
		boolean ok = adminInquiryService.logicalDelete(id);
		if (!ok) {
			redirectAttributes.addFlashAttribute("errorMessage", "削除できませんでした。");
			return "redirect:/admin/inquiries";
		}
		redirectAttributes.addFlashAttribute("successMessage", "問い合わせを削除しました（一覧から非表示）。");
		return "redirect:/admin/inquiries";
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
