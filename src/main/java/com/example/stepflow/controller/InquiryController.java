package com.example.stepflow.controller; // Webリクエストを受け取る層

import java.util.ArrayList; // 連絡先リストを組み立てる可変リスト
import java.util.List; // リスト型

import org.springframework.beans.factory.annotation.Autowired; // DI（自動注入）
import org.springframework.security.core.GrantedAuthority; // 権限情報
import org.springframework.security.core.annotation.AuthenticationPrincipal; // ログイン情報を引数で受け取る
import org.springframework.security.core.userdetails.UserDetails; // Spring Securityのログインユーザー
import org.springframework.stereotype.Controller; // 画面を返すコントローラ
import org.springframework.ui.Model; // Thymeleafへ渡すデータ
import org.springframework.validation.BindingResult; // バリデーション結果
import org.springframework.validation.annotation.Validated; // @NotBlank 等を有効化
import org.springframework.web.bind.annotation.GetMapping; // GET用
import org.springframework.web.bind.annotation.ModelAttribute; // フォームDTO受け取り
import org.springframework.web.bind.annotation.PostMapping; // POST用
import org.springframework.web.bind.annotation.RequestMapping; // URLプレフィックス
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // リダイレクト時のメッセージ

import com.example.stepflow.entity.Inquiry; // DB保存用エンティティ
import com.example.stepflow.entity.User; // ログインユーザー取得用
import com.example.stepflow.form.InquiryForm; // 画面入力DTO
import com.example.stepflow.form.PartnerOption; // 連絡先プルダウン1件
import com.example.stepflow.repository.InquiryRepository; // inquiryテーブルへのsave
import com.example.stepflow.service.UserService; // ユーザー検索

@Controller
@RequestMapping("/inquiry")
public class InquiryController {
    
    @Autowired
    private InquiryRepository inquiryRepository;
    
    @Autowired
    private UserService userService;

    /**
     * お問い合わせ画面表示
     * GET /inquiry ・ GET /inquiry/ ・ GET /inquiry/form のいずれでも表示（サイドバーは /inquiry を使用）
     */
    @GetMapping({ "", "/", "/form" })
    public String showInquiryForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // 💡 修正ポイント②：引数の「,（カンマ）」のタイポを修正
        addCommonModelAttributes(userDetails, model);
        
        if (!model.containsAttribute("inquiryForm")) {
            model.addAttribute("inquiryForm", new InquiryForm());
        }
        model.addAttribute("partnerList", buildPartnerList());
        return "inquiry"; // src/main/resources/templates/inquiry.html を呼び出す
    }

    /**
     * お問い合わせ送信（POST /inquiry/send）
     */
    @PostMapping("/send")
    public String sendInquiry(
            @Validated @ModelAttribute("inquiryForm") InquiryForm inquiryForm,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validatePartnerId(inquiryForm, bindingResult); // 店舗・倉庫宛は連絡先必須
        
        if (bindingResult.hasErrors()) {
            addCommonModelAttributes(userDetails, model);
            model.addAttribute("partnerList", buildPartnerList());
            return "inquiry";
        }
        
        
        User loginUser = userService.findUserByName(userDetails.getUsername()); // DBから送信者を取得
        if (loginUser == null) {
            bindingResult.reject("error.login", "ログイン情報が取得できません。再ログインしてください。");
            addCommonModelAttributes(userDetails, model);
            model.addAttribute("partnerList", buildPartnerList());
            return "inquiry";
        }
        
        Inquiry inquiry = new Inquiry();
        inquiry.setUserId(loginUser.getUserId());
        inquiry.setInquiryCategoryId(1);
        inquiry.setInquiryDetail(inquiryForm.getContent());
        inquiry.setInquiryStatus("未対応");
        
        applyReceiverRouting(inquiry, inquiryForm);
        
        
        inquiryRepository.save(inquiry);
        
        redirectAttributes.addFlashAttribute("successMessage", "お問い合わせを送信しました。");
        return "redirect:/home"; // ダッシュボードへ
    }

    /** サイドバー等で使う共通データ */
    private void addCommonModelAttributes(UserDetails userDetails, Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername()); // ログイン名を表示
        
        // 💡 修正ポイント③：メソッド内で迷子になっていた authorityId を正しく取得・定義
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        
        model.addAttribute("authorityId", authorityId);
    }

    /** 店舗・倉庫宛のとき partnerId 必須 */
    private void validatePartnerId(InquiryForm form, BindingResult bindingResult) {
        if ("ROLE_SHOP".equals(form.getReceiverRole()) || "ROLE_WAREHOUSE".equals(form.getReceiverRole())) {
            if (form.getPartnerId() == null) {
                bindingResult.rejectValue("partnerId", "error.partnerId", "連絡先を選択してください");
            }
        }
    }

    /** 送信先ロール→DBの振り分け */
    // 💡 修正ポイント④：引数に inquiry を受け取れるように修正
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
    
    /** 連絡先プルダウン用（登録済みユーザーから暫定生成） */
    private List<PartnerOption> buildPartnerList() {
        // 💡 修正ポイント⑥：メソッド内で使う partners リストのインスタンス化を定義、タイポ修正、最後の return 位置の修正
        List<PartnerOption> partners = new ArrayList<>();
        
        // 💡 呼び出し先サービスメソッド名をリポジトリ内の実装（findActiveUsersByAuthority）に統一、タイポ修正
        for (User shopUser : userService.findUsersByAuthority(2)) {
            if (shopUser.getShopId() != null && !shopUser.getShopId().isBlank()) {
                try {
                    Integer shopId = Integer.valueOf(shopUser.getShopId().trim());
                    partners.add(new PartnerOption(shopId, "店舗: " + shopUser.getUserName()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        for (User warehouseUser : userService.findUsersByAuthority(3)) {
            if (warehouseUser.getWarehouseId() != null && !warehouseUser.getWarehouseId().isBlank()) {
                try {
                    Integer warehouseId = Integer.valueOf(warehouseUser.getWarehouseId().trim());
                    partners.add(new PartnerOption(warehouseId, "倉庫: " + warehouseUser.getUserName()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return partners;
    }
}