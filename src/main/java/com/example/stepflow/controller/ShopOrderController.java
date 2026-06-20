package com.example.stepflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import java.util.List;
import com.example.stepflow.entity.ShopOrder;
import com.example.stepflow.service.ShopOrderService;
import com.example.stepflow.service.UserService;
import com.example.stepflow.entity.User;
import java.util.Collections;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.stepflow.form.ShopOrderForm;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.GrantedAuthority;
import com.example.stepflow.dto.ShopOrderGoodsRow;
import com.example.stepflow.entity.Category;
import com.example.stepflow.service.ShopOrderSelectionService;
import com.example.stepflow.service.ShopOrderExcelService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import java.nio.charset.StandardCharsets;
import org.springframework.web.multipart.MultipartFile;
import com.example.stepflow.dto.ShopOrderImportResult;




@Controller
@RequestMapping("/shop/order")
public class ShopOrderController {
    @Autowired
    private ShopOrderService shopOrderService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShopOrderSelectionService shopOrderSelectionService;

    @Autowired
    private ShopOrderExcelService shopOrderExcelService;

    /** GET /shop/order … 発注商品選択（カテゴリ絞り込み可） */
    
    // ═══════════════════════════════════════════════════════
    // 発注商品選択画面  GET /shop/order
    // カテゴリ絞り込み  GET /shop/order?categoryId=3
    // ═══════════════════════════════════════════════════════
    @GetMapping //メソッドにパスがない→クラスの/shop/order　そのもの
    public String showOrderSelection(
        @AuthenticationPrincipal UserDetails userDetails,//ログインユーザーの情報を取得
        @RequestParam(value = "categoryId", required = false) Integer categoryId, //カテゴリIDを取得
        Model model){
            //サイドバー用の共通データを追加
            addSidebarModel(userDetails, model);

            //DBのuserテーブルから、ログイン名で１件取得(shop_id を知るため)
            User loginUser = userService.findUserByName(userDetails.getUsername());

            //店舗情報が取得できない場合はエラーメッセージを表示して一覧画面にリダイレクト
            if(loginUser == null || loginUser.getShopId() == null){
                model.addAttribute("goodsRows", Collections.emptyList());
                model.addAttribute("categories", Collections.emptyList());
                model.addAttribute("errorMessage", "店舗情報が取得できません。管理者ユーザー設定を確認してください。");
                return "shop/order-select";//発注商品選択画面にリダイレクト
            }
            try{
                //プルダウン用カテゴリー集
                List<Category> categories = shopOrderSelectionService.findActiveCategories();

                //カテゴリ絞り込み用の商品一覧
                List<ShopOrderGoodsRow> rows = shopOrderSelectionService.findGoodsRowsForShop(loginUser.getShopId(),categoryId);

                /** カテゴリIDが指定されていれば、そのカテゴリの商品を取得。されていなければ全商品を取得 *
                 * */


                model.addAttribute("categories", categories);//カテゴリ一覧をモデルに追加
                model.addAttribute("goodsRows", rows);//商品一覧をモデルに追加
                model.addAttribute("selectedCategoryId", categoryId);//選択されたカテゴリIDをモデルに追加

            }catch(IllegalArgumentException ex){
                model.addAttribute("goodsRows", Collections.emptyList());
                model.addAttribute("categories", Collections.emptyList());
                model.addAttribute("selectionError", ex.getMessage());
            }

            return "shop/order-select";//発注商品選択画面にリダイレクト

        }
   



    /** GET /shop/order/list … 自店舗の発注一覧 */
    @GetMapping("/list")
    public String showShopOrderList(@AuthenticationPrincipal UserDetails
        userDetails, Model model){
        /** サイドバー等で使う共通データ */
        addSidebarModel(userDetails, model);

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if(loginUser == null || loginUser.getShopId() == null){
            model.addAttribute("orders", Collections.emptyList());
            model.addAttribute("errorMessage", "店舗情報が取得できません。管理者ユーザー設定を確認してください。");
            return "shop/order-list";
        }


        try{
            List<ShopOrder> orders =
                    shopOrderService.findActiveByShopId(loginUser.getShopId());
            model.addAttribute("orders", orders);
            
        }catch(IllegalArgumentException ex){
            model.addAttribute("orders", Collections.emptyList());
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "shop/order-list";
    }

       /** GET /shop/order/new … 発注申請画面（goodsId で商品を事前選択可） */
       @GetMapping("/new")
       public String showNewForm(@AuthenticationPrincipal UserDetails userDetails,
        @RequestParam(value = "goodsId", required = false) Integer goodsId,
        Model model,RedirectAttributes redirectAttributes){
           /** サイドバー等で使う共通データ */
            addSidebarModel(userDetails, model);
          
            User loginUser = userService.findUserByName(userDetails.getUsername());
            /** 店舗情報が取得できない場合はエラーメッセージを表示して一覧画面にリダイレクト */
            if(loginUser == null || loginUser.getShopId() == null){
            redirectAttributes.addFlashAttribute("errorMessage", "店舗情報が取得できません。管理者ユーザー設定を確認してください。");
            return "redirect:/shop/order";
        }
        /** フォームデータを初期化（商品選択画面から来たときは goodsId をセット） */
        if(!model.containsAttribute("shopOrderForm")){
            ShopOrderForm form = new ShopOrderForm();
            if (goodsId != null) {
                form.setGoodsId(goodsId);
            }
            model.addAttribute("shopOrderForm", form);
        }
        model.addAttribute("preselectedGoodsId", goodsId);
       /** 連携倉庫一覧 */
        model.addAttribute("relations",
         shopOrderService.findRelationsForShop(loginUser.getShopId()));

        model.addAttribute("goodsList", shopOrderService.findActiveGoods());


        return "shop/order-form";
        
        }

         /** POST /shop/order … 発注登録 */
         @PostMapping
         public String create(@Validated @ModelAttribute("shopOrderForm") ShopOrderForm shopOrderForm,
          BindingResult result,
          @AuthenticationPrincipal UserDetails userDetails,
          Model model,
          RedirectAttributes redirectAttributes){
            /** サイドバー等で使う共通データ */
            addSidebarModel(userDetails, model);

            User loginUser = userService.findUserByName(userDetails.getUsername());
            if(loginUser == null || loginUser.getShopId() == null){
                redirectAttributes.addFlashAttribute("errorMessage", "店舗情報が取得できません。管理者ユーザー設定を確認してください。");
                return "redirect:/shop/order";
            }
            /** バリデーションエラーの場合はフォーム画面に戻す */
            if(result.hasErrors()){
                model.addAttribute("relations",
                 shopOrderService.findRelationsForShop(loginUser.getShopId()));
                 model.addAttribute("goodsList", shopOrderService.findActiveGoods());
                 return "shop/order-form";
            }

            boolean ok = shopOrderService.registerShopOrder(
                loginUser.getShopId(),
                shopOrderForm
            );
            //registerShopOrderメソッドの戻り値がtrueの場合は成功メッセージを表示し、falseの場合はエラーメッセージを表示する
            if(ok){
                redirectAttributes.addFlashAttribute("successMessage", "発注申請を送信しました。");
            }else{
                redirectAttributes.addFlashAttribute("errorMessage", "発注申請に失敗しました。再度お試しください。");
            }
            return "redirect:/shop/order";
            }

    /**
     * GET /shop/order/template/download
     * 発注一括用 Excel テンプレートをダウンロード
     */
    @GetMapping("/template/download")
    public ResponseEntity<byte[]> downloadTemplate(
            @AuthenticationPrincipal UserDetails userDetails) {
        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getShopId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            byte[] excelBytes = shopOrderExcelService.buildOrderTemplate(loginUser.getShopId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("shop_order_template.xlsx", StandardCharsets.UTF_8)
                    .build());

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================
    // A4: Excel 一括発注
    // GET  … アップロード画面
    // POST … ファイル受け取り → Service で読み込み → 結果を Flash で表示
    // ============================================================

    /**
     * GET /shop/order/import
     * Excel を選んで送る画面（order-import.html）
     */
    @GetMapping("/import")
    public String showImportForm(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        addSidebarModel(userDetails, model);
        return "shop/order-import";
    }

    /**
     * POST /shop/order/import
     * multipart/form-data で送られた .xlsx を取り込む
     */
    @PostMapping("/import")
    public String uploadImport(
            @RequestParam("file") MultipartFile file,
            // ↑ HTML の <input name="file"> と名前を一致させる
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getShopId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "店舗情報が取得できません。管理者ユーザー設定を確認してください。");
            return "redirect:/shop/order";
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "ファイルを選択してください。");
            return "redirect:/shop/order/import";
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            redirectAttributes.addFlashAttribute("errorMessage", ".xlsx 形式のファイルを選択してください。");
            return "redirect:/shop/order/import";
        }

        try {
            ShopOrderImportResult result = shopOrderExcelService.importOrdersFromExcel(
                    loginUser.getShopId(),
                    file.getInputStream());

            // 次の画面表示用に Flash（1回だけ表示されるメッセージ）に載せる
            redirectAttributes.addFlashAttribute("importResult", result);
            redirectAttributes.addFlashAttribute("successMessage",
                    "一括発注完了。成功 " + result.getSuccessCount()
                            + " 件 / 失敗 " + result.getFailureCount() + " 件");

            if (result.getFailureCount() > 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "一部の行は登録できませんでした。下のエラー一覧を確認してください。");
            }
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Excelの読み込みに失敗しました。ファイル形式を確認してください。");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/shop/order/import";
    }


            // ── 以下は別画面（参考）──
            // GET  /shop/order/list  → 発注一覧 order-list.html
            // GET  /shop/order/new   → 発注申請 order-form.html（?goodsId= で商品固定可）
            // POST /shop/order       → 発注登録後 redirect:/shop/order（successMessage を Flash で表示）


            private void addSidebarModel(UserDetails userDetails, Model model){
                model.addAttribute("loginUsername", userDetails.getUsername());
                model.addAttribute("authorityId", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)//GrantedAuthorityはSpring Securityのインターフェースで、ユーザーの権限を表す。getAuthority()メソッドで権限を取得する。
                .findFirst()
                .orElse(""));//ユーザーの権限が取得できない場合は空文字列を返す。
            }

        }//ShopOrderControllerクラスの終わり

