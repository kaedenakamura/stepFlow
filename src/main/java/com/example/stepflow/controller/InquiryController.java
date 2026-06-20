package com.example.stepflow.controller;

import java.util.ArrayList;
import java.util.Collections;
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

import com.example.stepflow.dto.AdminInquiryListRow;
import com.example.stepflow.entity.Inquiry;
import com.example.stepflow.entity.User;
import com.example.stepflow.form.InquiryForm;
import com.example.stepflow.form.PartnerOption;
import com.example.stepflow.repository.InquiryRepository;
import com.example.stepflow.service.AdminInquiryService;
import com.example.stepflow.service.InquiryService;
import com.example.stepflow.service.UserService;

@Controller
@RequestMapping("/inquiry")
public class InquiryController {

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private AdminInquiryService adminInquiryService;

    /** 未ログイン問い合わせの送信者として DB に登録した user.user_id */
    private static final int GUEST_USER_ID = 99;

    /**
     * GET /inquiry … ログイン済みは一覧、未ログインはゲスト用送信フォーム
     */
    @GetMapping({ "", "/" })
    public String entry(
            @RequestParam(name = "status", required = false) String statusFilter,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        if (userDetails == null) {
            return showGuestForm(model);
        }
        return showList(userDetails, model, statusFilter);
    }

    /** GET /inquiry/new … お問い合わせ新規作成（ログイン必須） */
    @GetMapping("/new")
    public String showNewForm(
            @RequestParam(name = "warehouseId", required = false) Integer warehouseId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        addCommonModelAttributes(userDetails, model);
        model.addAttribute("guestMode", false);
        InquiryForm inquiryForm = model.containsAttribute("inquiryForm")
                ? (InquiryForm) model.getAttribute("inquiryForm")
                : new InquiryForm();
        if (warehouseId != null) {
            inquiryForm.setReceiverRole("ROLE_WAREHOUSE");
            inquiryForm.setPartnerId(warehouseId);
        }
        model.addAttribute("inquiryForm", inquiryForm);
        addPartnerListsToModel(model);
        return "inquiry-form";
    }

    /** 旧URL互換: /inquiry/form → 新規作成へ */
    @GetMapping("/form")
    public String legacyForm(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/inquiry";
        }
        return "redirect:/inquiry/new";
    }

    /** POST /inquiry/send … 送信 */
    @PostMapping("/send")
    public String sendInquiry(
            @Validated @ModelAttribute("inquiryForm") InquiryForm inquiryForm,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        boolean guestMode = (userDetails == null);
        model.addAttribute("guestMode", guestMode);

        if (!guestMode) {
            validatePartnerId(inquiryForm, bindingResult);
        }

        if (bindingResult.hasErrors()) {
            if (guestMode) {
                return showGuestForm(model);
            }
            addCommonModelAttributes(userDetails, model);
            addPartnerListsToModel(model);
            return "inquiry-form";
        }

        Inquiry inquiry = new Inquiry();
        inquiry.setInquiryCategoryId(1);
        inquiry.setInquiryDetail(inquiryForm.getContent());
        inquiry.setInquiryStatus("未対応");

        if (guestMode) {
            inquiry.setUserId(GUEST_USER_ID);
            inquiry.setAuthorityId(1);
            inquiry.setShopId(null);
            inquiry.setWarehouseId(null);
        } else {
            User loginUser = userService.findUserByName(userDetails.getUsername());
            if (loginUser == null) {
                bindingResult.reject("error.login", "ログイン情報が取得できません。再ログインしてください。");
                addCommonModelAttributes(userDetails, model);
                addPartnerListsToModel(model);
                return "inquiry-form";
            }
            inquiry.setUserId(loginUser.getUserId());
            applyReceiverRouting(inquiry, inquiryForm);
        }

        inquiryRepository.save(inquiry);

        if (guestMode) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "ゲストでのお問い合わせを送信しました。内容は管理者が確認後、ご連絡いたします。");
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("successMessage", "お問い合わせを送信しました。");
        return "redirect:/inquiry";
    }

    /** GET /inquiry/{id} … 詳細（宛先が自組織の行のみ） */
    @GetMapping("/{id}")
    public String detail(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        addCommonModelAttributes(userDetails, model);
        String authorityId = (String) model.getAttribute("authorityId");

        User loginUser = userService.findUserByName(userDetails.getUsername());
        Inquiry inquiry = inquiryService.findVisibleInquiry(id, loginUser, authorityId);
        if (inquiry == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "問い合わせが存在しないか、閲覧できません。");
            return "redirect:/inquiry";
        }

        User sender = userService.getUserById(inquiry.getUserId());
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("sender", sender);
        model.addAttribute("affiliationDisplay", adminInquiryService.formatAffiliation(sender));
        model.addAttribute("authorityLabel", formatSenderAuthority(sender));
        return "inquiry-detail";
    }

    /** POST /inquiry/{id}/status … ステータス更新 */
    @PostMapping("/{id}/status")
    public String updateStatus(
            @PathVariable Integer id,
            @RequestParam("inquiryStatus") String inquiryStatus,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (inquiryService.findVisibleInquiry(id, loginUser, authorityId) == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "更新できませんでした。");
            return "redirect:/inquiry";
        }
        try {
            boolean ok = adminInquiryService.updateStatus(id, inquiryStatus.strip());
            if (!ok) {
                redirectAttributes.addFlashAttribute("errorMessage", "更新できませんでした。");
                return "redirect:/inquiry";
            }
            redirectAttributes.addFlashAttribute("successMessage", "ステータスを更新しました。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/inquiry/" + id;
    }

    /** POST /inquiry/{id}/delete … 論理削除 */
    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (inquiryService.findVisibleInquiry(id, loginUser, authorityId) == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除できませんでした。");
            return "redirect:/inquiry";
        }
        boolean ok = adminInquiryService.logicalDelete(id);
        if (!ok) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除できませんでした。");
            return "redirect:/inquiry";
        }
        redirectAttributes.addFlashAttribute("successMessage", "問い合わせを削除しました（一覧から非表示）。");
        return "redirect:/inquiry";
    }

    private String showGuestForm(Model model) {
        model.addAttribute("guestMode", true);
        if (!model.containsAttribute("inquiryForm")) {
            InquiryForm inquiryForm = new InquiryForm();
            inquiryForm.setReceiverRole("ROLE_ADMIN");
            model.addAttribute("inquiryForm", inquiryForm);
        }
        return "inquiry-guest";
    }

    private String showList(UserDetails userDetails, Model model, String statusFilter) {
        addCommonModelAttributes(userDetails, model);
        String authorityId = (String) model.getAttribute("authorityId");

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null) {
            model.addAttribute("rows", Collections.emptyList());
            model.addAttribute("listError", "ログイン情報が取得できません。");
            model.addAttribute("statusFilter", "");
            return "inquiry-list";
        }

        List<Inquiry> inquiries = inquiryService.findVisibleInquiries(
                loginUser, authorityId, statusFilter);
        List<AdminInquiryListRow> rows = adminInquiryService.toListRows(inquiries);
        model.addAttribute("rows", rows);
        model.addAttribute("statusFilter", statusFilter != null ? statusFilter : "");
        return "inquiry-list";
    }

    private static String formatSenderAuthority(User sender) {
        if (sender == null || sender.getAuthorityId() == null) {
            return "-";
        }
        return switch (sender.getAuthorityId()) {
            case 1 -> "管理者";
            case 2 -> "店舗";
            case 3 -> "倉庫";
            default -> "-";
        };
    }

    private void addCommonModelAttributes(UserDetails userDetails, Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);
    }

    private void validatePartnerId(InquiryForm form, BindingResult bindingResult) {
        if ("ROLE_SHOP".equals(form.getReceiverRole()) || "ROLE_WAREHOUSE".equals(form.getReceiverRole())) {
            if (form.getPartnerId() == null) {
                bindingResult.rejectValue("partnerId", "error.partnerId", "連絡先を選択してください");
            }
        }
    }
    //送信先のルーティング
    private void applyReceiverRouting(Inquiry inquiry, InquiryForm form) {
        String receiverRole = form.getReceiverRole();
        if ("ROLE_ADMIN".equals(receiverRole)) {
            inquiry.setAuthorityId(1);
            inquiry.setShopId(null);
            inquiry.setWarehouseId(null);
        } else if ("ROLE_SHOP".equals(receiverRole)) {
            inquiry.setAuthorityId(2);
            inquiry.setShopId(form.getPartnerId());
            inquiry.setWarehouseId(null);
        } else if ("ROLE_WAREHOUSE".equals(receiverRole)) {
            inquiry.setAuthorityId(3);
            inquiry.setShopId(null);
            inquiry.setWarehouseId(form.getPartnerId());
        } else {
            throw new IllegalArgumentException("不正な送信先:" + receiverRole);
        }
    }
    private void addPartnerListsToModel(Model model) {
       
        //店舗の連絡先リストを作成→return partners　をmodelに追加
        model.addAttribute("shopPartnerList", buildShopPartnerList());
        //倉庫の連絡先リストを作成→return partners　をmodelに追加する
        model.addAttribute("warehousePartnerList", buildWarehousePartnerList());
    }

    /** 送信先が店舗のときの連絡先（店舗スタッフのみ） */
    private List<PartnerOption> buildShopPartnerList() {
        List<PartnerOption> partners = new ArrayList<>();
        for (User shopUser : userService.findUsersByAuthority(2)) {
            if (shopUser.getShopId() != null) {
                partners.add(new PartnerOption(shopUser.getShopId(), shopUser.getUserName()));
            }
        }
        return partners;
    }

    /** 送信先が倉庫のときの連絡先（倉庫スタッフのみ） */
    private List<PartnerOption> buildWarehousePartnerList() {
        List<PartnerOption> partners = new ArrayList<>();
        for (User warehouseUser : userService.findUsersByAuthority(3)) {
            if (warehouseUser.getWarehouseId() != null) {
                partners.add(new PartnerOption(warehouseUser.getWarehouseId(), warehouseUser.getUserName()));
            }
        }
        return partners;
    }
}
