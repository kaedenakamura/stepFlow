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

import com.example.stepflow.entity.ShopOrder;
import com.example.stepflow.entity.User;
import com.example.stepflow.service.UserService;
import com.example.stepflow.service.WarehouseOrderService;

import org.springframework.http.ContentDisposition;   // ダウンロードファイル名用
import org.springframework.http.HttpHeaders;          // HTTPレスポンスヘッダー
import org.springframework.http.HttpStatus;           // 400/500 など
import org.springframework.http.MediaType;            // CSV/PDF の Content-Type
import org.springframework.http.ResponseEntity;       // バイナリ返却用
import java.nio.charset.StandardCharsets;             // UTF-8 エンコード
import com.example.stepflow.service.ShopSalesPdfService; // PDF生成（流用）
import java.io.IOException;                           // PDF生成失敗時
import org.slf4j.Logger;                              // エラーログ
import org.slf4j.LoggerFactory;


import java.time.LocalDate;//日付型


@Controller
@RequestMapping("/warehouse/stock")
public class WarehouseOrderController {

    @Autowired
    private UserService userService;

    @Autowired
    private WarehouseOrderService warehouseOrderService;

    private static final Logger log = LoggerFactory.getLogger(WarehouseOrderController.class);

    @Autowired
    private ShopSalesPdfService shopSalesPdfService;
    //PDF生成用

    /** GET /warehouse/stock … 自倉庫への受注一覧 */
    @GetMapping
    public String list(
            @RequestParam(name = "status", required = false) String status,
            //@RequestParam→画面から送信されたパラメータを受け取るためのアノテーション。
            // name属性でパラメータ名を指定。required = false→パラメータが送信されなくてもエラーにならない。
            @RequestParam(name = "from" , required = false) LocalDate from,
            @RequestParam(name = "to" , required = false) LocalDate to,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        addSidebarAttributes(userDetails, model);

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getWarehouseId() == null) {
            model.addAttribute("orders", Collections.emptyList());
            model.addAttribute("orderError",
                    "所属倉庫が設定されていません。管理者にユーザー設定を確認してください。");
            //絞り込みフォームの前に値を戻す用
            model.addAttribute("statusFilter", status != null ? status : "");
            model.addAttribute("fromDate", from != null ? from : "");
            model.addAttribute("toDate", to != null ? to : "");
            return "warehouse/stock-list";
        }

        try {
            //絞り込み条件を渡して受注一覧を取得
            List<ShopOrder> orders = warehouseOrderService.findActiveByWarehouseIdFiltered(
                loginUser.getWarehouseId(),//倉庫ID
                status,//ステータス
                from,//開始日
                to//終了日
            );
            model.addAttribute("orders", orders);//受注一覧を表示
            model.addAttribute("statusFilter", status != null ? status : "");//絞り込みフォームの前に値を戻す用
            model.addAttribute("fromDate", from != null ? from : "");//絞り込みフォームの前に値を戻す用
            model.addAttribute("toDate", to != null ? to : "");//絞り込みフォームの前に値を戻す用
        } catch (IllegalArgumentException ex) {
            model.addAttribute("orders", Collections.emptyList());//受注一覧を空にする
            model.addAttribute("orderError", ex.getMessage());//エラーメッセージを表示
            model.addAttribute("statusFilter", status != null ? status : "");//絞り込みフォームの前に値を戻す用
            model.addAttribute("fromDate", from != null ? from : "");//絞り込みフォームの前に値を戻す用
            model.addAttribute("toDate", to != null ? to : "");//絞り込みフォームの前に値を戻す用
        }
        return "warehouse/stock-list";//倉庫受注一覧画面を表示
    }
        /**
     * GET /warehouse/stock/csv
     * /warehouse/stock と同じ絞り込み条件で CSV をダウンロード
     */
    @GetMapping("/csv")
    public ResponseEntity<byte[]> downloadStockCsv(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "from", required = false) LocalDate from,
        @RequestParam(name = "to" , required = false) LocalDate to,
        @AuthenticationPrincipal UserDetails userDetails){
            //ログインユーザー情報を取得
            User loginUser = userService.findUserByName(userDetails.getUsername());
            if(loginUser == null || loginUser.getWarehouseId() == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();//エラーを返す400エラー
            }
        //一覧画面と同じ条件でデータ取得（Service再利用）
        List<ShopOrder> orders = warehouseOrderService.findActiveByWarehouseIdFiltered(
            loginUser.getWarehouseId(),
            status,
            from,
            to
        );
        // CSVヘッダー（倉庫画面の列に合わせる：店舗名→商品名→…）
        StringBuilder csv = new StringBuilder();
        csv.append("店舗名,商品名,発注数,ステータス,発注日時,更新日時\n");
        //データを1件ずつ取り出してCSVに追加
        for(ShopOrder order : orders){
        //先に、名前を取得しておく（Entityの関連が取れない時はID表示）LAZYの読み込み
        //店舗名
            String shopName = order.getShop() != null ? order.getShop().getShopName() : "店舗ID" + order.getShopId();
        //商品名
            String goodsName = order.getGoods() != null ? order.getGoods().getGoodsName() :"商品ID" + order.getGoodsId();
        //発注日時
        String orderDate = order.getOrderDate() != null ? order.getOrderDate().toString() : "";
        //更新日時
        String updateDate = order.getUpdateDate() != null ? order.getUpdateDate().toString() : "";
        //CSV本体に追加していく
        csv.append(toCsvCell(shopName)).append(',')
           .append(toCsvCell(goodsName)).append(',')
           .append(toCsvCell(String.valueOf(order.getOrderQuantity()))).append(',')
           .append(toCsvCell(order.getOrderStatus())).append(',')
           .append(toCsvCell(orderDate)).append(',')
           .append(toCsvCell(updateDate)).append('\n');
        }
        //UTF-8 + BOM（Excel文字化け対策）→\uFEFFはBOMを付与するためのUnicodeエスケープシーケンス
        byte[] csvBytes = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);

        //レスポンスヘッダー
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("warehouse-order-history.csv",StandardCharsets.UTF_8)
            .build());
            return new ResponseEntity<>(csvBytes,headers,HttpStatus.OK);
        }
        /**
     * GET /warehouse/stock/pdf
     * 受注一覧を PDF でダウンロード
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadStockPdf(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "from" , required = false) LocalDate from,
        @RequestParam(name = "to" , required = false) LocalDate to,
        @AuthenticationPrincipal UserDetails userDetails){
            User loginUser = userService.findUserByName(userDetails.getUsername());
            if(loginUser == null || loginUser.getWarehouseId() == null){
                //ここのstatusはステータスコードではなく、HTTPステータスコード(400)を返すためのもの
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();//エラーを返す400エラー
            }
        //一覧画面と同じ条件でデータ取得（Service再利用）
            try{
                List<ShopOrder> orders = warehouseOrderService.findActiveByWarehouseIdFiltered(
                    loginUser.getWarehouseId(),
                    status,
                    from,
                    to
                );
                //倉庫用PDF生成（PDFサービスを再利用）
                byte[] pdfBytes = shopSalesPdfService.buildWarehouseOrderHistoryPdf(orders);//PDFをbyte[]に変換
                //レスポンスヘッダー
                HttpHeaders headers = new HttpHeaders();
                //PDFのMIMEタイプを設定
                headers.setContentType(MediaType.APPLICATION_PDF);
                //ファイル名を設定　配置フォルダはsrc/main/resources/static/pdf/
                //倉庫受注一覧.pdfというファイル名でダウンロードされる
                headers.setContentDisposition(ContentDisposition.attachment()
                .filename("warehouse-order-history.pdf",StandardCharsets.UTF_8)//UTF-8でエンコード
                .build());
                //200 OK + PDF本体を返す
                return new ResponseEntity<>(pdfBytes,headers,HttpStatus.OK);
            } catch (IOException ex) {
                log.error("倉庫受注一覧PDFの生成に失敗しました", ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();//エラーを返す500エラー
            }


            }


    /** POST /warehouse/stock/{shopOrderId}/status … ステータス更新 */
    @PostMapping("/{shopOrderId}/status")
    public String updateStatus(
            @PathVariable("shopOrderId") Integer shopOrderId,
            @RequestParam("orderStatus") String orderStatus,
            @RequestParam("filterStatus") String filterStatus,
            @RequestParam("filterFrom") LocalDate filterFrom,
            @RequestParam("filterTo") LocalDate filterTo,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getWarehouseId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "所属倉庫が設定されていません。管理者にユーザー設定を確認してください。");
            return "redirect:/warehouse/stock";
        }

        boolean ok = warehouseOrderService.updateOrderStatus(
                loginUser.getWarehouseId(),
                shopOrderId,
                orderStatus);

        if (ok) {
            redirectAttributes.addFlashAttribute("successMessage", "ステータスを更新しました。");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "ステータスの更新に失敗しました。");
        }

        //絞り込み条件を渡して受注一覧を取得
        StringBuilder redirect = new StringBuilder("redirect:/warehouse/stock");
        //url→redirect:/warehouse/stock→この先をappendで追加　
        // 例：redirect:/warehouse/stock?status=準備中&from=2026-01-01&to=2026-01-31
        //絞り込み条件を追加
        boolean first = true;
        if(filterStatus != null && !filterStatus.isEmpty()){
            //絞り込み条件がある場合はクエリパラメータを追加
            redirect.append(first ? "?" : "&").append("status=").append(filterStatus);
            first = false;
            //firstがtrueの場合は?を追加、falseの場合は&を追加 最初のみ?を追加以降は&を追加
        }
        if(filterFrom != null){
            redirect.append(first ? "?" : "&").append("from=").append(filterFrom);
            first = false;
        }
        if(filterTo != null){
            redirect.append(first ? "?" : "&").append("to=").append(filterTo);
            first = false;
        }

        return redirect.toString();//toString()はStringBuilderをStringに変換するメソッド
        //例：redirect:/warehouse/stock?status=準備中&from=2026-01-01&to=2026-01-31
    }
    /** サイドバーの属性を追加 */
    private void addSidebarAttributes(UserDetails userDetails, Model model) {
        model.addAttribute("loginUsername", userDetails.getUsername());
        String authorityId = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");
        model.addAttribute("authorityId", authorityId);
    }
            /** CSVのセル値として安全に出力する（カンマ/改行/ダブルクォート対策） */
            private String toCsvCell(String value){
                if(value == null){
                    return "\"\"";//空文字をダブルクォートで囲む
                }
                String escaped = value.replace("\"", "\"\"");//ダブルクォートをエスケープ
                return "\"" + escaped + "\"";//ダブルクォートで囲む
            }

    }//クラスの終わり

