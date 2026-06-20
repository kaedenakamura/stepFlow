package com.example.stepflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.example.stepflow.service.ShopOrderService;
import com.example.stepflow.service.UserService;
import java.util.Collections;
import com.example.stepflow.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import com.example.stepflow.entity.ShopOrder;
import java.util.List;
import java.time.LocalDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import com.example.stepflow.service.ShopSalesPdfService;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/shop/sales")
public class ShopSalesController {

    private static final Logger log = LoggerFactory.getLogger(ShopSalesController.class);

    @Autowired
    private ShopOrderService shopOrderService;
    //発注一覧画面用

    @Autowired
    private UserService userService;
    //ユーザー情報用

    @Autowired
    private ShopSalesPdfService shopSalesPdfService;
    //PDF生成用

        /**
     * GET /shop/sales
     * 発注履歴（shop_order）を表示する
     */
    @GetMapping//発注履歴画面を表示
    public String showSalesHistory(
        @RequestParam(name = "status", required = false) String status,
        // 画面の <select name="status"> の値。無くてもOK(required=false)
        @RequestParam(name = "from" , required = false) LocalDate from,
        // 画面の <input type="date" name="from"> の値。無くてもOK(required=false)
        @RequestParam(name = "to" , required = false) LocalDate to,
        // 画面の <input type="date" name="to"> の値。無くてもOK(required=false)
        @AuthenticationPrincipal UserDetails userDetails,
        // ログインユーザー情報
        Model model
    ){
        //サイドバー用（common/sidebar.html）に追加したコードをここにも追加する
        addSidebarAttributes(userDetails, model);
        //ログインユーザー情報を取得
        User loginUser = userService.findUserByName(userDetails.getUsername());

        //店舗IDを取得 → 店舗スタッフのみがアクセス可能
        if(loginUser == null || loginUser.getShopId() == null){
            //ログインしていない、または権限がない場合はエラーメッセージを表示
            model.addAttribute("orders", Collections.emptyList());
            model.addAttribute("errorMessage", "店舗情報が取得できません。管理者ユーザー設定を確認してください。");
            
            //絞り込みフォームの前に値を戻す用
            model.addAttribute("statusFilter", status != null ? status : "");
            model.addAttribute("fromDate", from != null ? from : "");
            model.addAttribute("toDate", to);
            return "shop/sales-list";//発注履歴画面を表示
        }

          // 自店舗の発注だけ、条件付きで取得
          List<ShopOrder> orders = shopOrderService.findActiveByShopIdFiltered(
            loginUser.getShopId(),//店舗ID
            status,//ステータス
            from,//開始日
            to//終了日
          );

          model.addAttribute("orders", orders);//発注履歴を表示
          model.addAttribute("statusFilter", status != null ? status : "");
          model.addAttribute("fromDate", from);
          model.addAttribute("toDate", to);
          return "shop/sales-list";//発注履歴画面を表示

    } 

    /**
     * GET /shop/sales/csv
     * /shop/sales と同じ絞り込み条件で CSV をダウンロード
     */
    @GetMapping("/csv")
    public ResponseEntity<byte[]> downloadSalesCsv(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "from", required = false) LocalDate from,
        @RequestParam(name = "to", required = false) LocalDate to,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
         // 1) ログインユーザーから店舗IDを確定
        User loginUser = userService.findUserByName(userDetails.getUsername());
        if (loginUser == null || loginUser.getShopId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

         // 2) 既存の一覧と同じ条件でデータ取得（再利用）
        List<ShopOrder> orders = shopOrderService.findActiveByShopIdFiltered(
                loginUser.getShopId(), status, from, to);

         // 3) CSV本体作成
        StringBuilder csv = new StringBuilder();
        csv.append("商品名,倉庫名,発注数,ステータス,発注日時,更新日時\n");

        for (ShopOrder order : orders) {
            String goodsName = order.getGoods() != null
                    ? order.getGoods().getGoodsName()
                    : "商品ID:" + order.getGoodsId();
            String warehouseName = order.getWarehouse() != null
                    ? order.getWarehouse().getWarehouseName()
                    : "倉庫ID:" + order.getWarehouseId();
            String orderDate = order.getOrderDate() != null ? order.getOrderDate().toString() : "";
            String updateDate = order.getUpdateDate() != null ? order.getUpdateDate().toString() : "";

            // 4) CSV本体に追加
            csv.append(toCsvCell(goodsName)).append(',')
                    .append(toCsvCell(warehouseName)).append(',')
                    .append(order.getOrderQuantity()).append(',')
                    .append(toCsvCell(order.getOrderStatus())).append(',')
                    .append(toCsvCell(orderDate)).append(',')
                    .append(toCsvCell(updateDate)).append('\n');
        }

        // 4) CSV本体をUTF-8でエンコードしてbyte配列に変換
        // // 5) Excelでの文字化け対策として BOM を付与
        byte[] csvBytes = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);//BOMを付与

        // 6) HTTPヘッダーを設定
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("shop-sales-history.csv", StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }



    /** CSVのセル値として安全に出力する（カンマ/改行/ダブルクォート対策） */
    private String toCsvCell(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

        /**
     * GET /shop/sales/pdf
     * 発注履歴をPDFでダウンロード
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadSalesPdf(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "from", required = false) LocalDate from,
        @RequestParam(name = "to", required = false) LocalDate to,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        //ログインユーザー情報を取得
        User loginUser = userService.findUserByName(userDetails.getUsername());
        if(loginUser == null || loginUser.getShopId() == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();//エラーを返す
        }
        try{
           // CSVと同じ条件で一覧取得（ここが超重要）
           //絞り込み条件を適用して一覧を取得

           List<ShopOrder> orders = shopOrderService.findActiveByShopIdFiltered(
            loginUser.getShopId(),//自店舗のみ
            status,//ステータス
            from,//開始日
            to//終了日
           );

           // PDF本体作成
           byte[] pdfBytes = shopSalesPdfService.buildSalesHistoryPdf(orders);//PDFをbyte[]に変換

           // HTTPヘッダーを設定
           HttpHeaders headers = new HttpHeaders();

           //PDFのMIMEタイプを設定
           headers.setContentType(MediaType.APPLICATION_PDF);

           //ファイル名を設定
           headers.setContentDisposition(ContentDisposition.attachment()
            .filename("shop-sales-history.pdf", StandardCharsets.UTF_8)//UTF-8でエンコード
            .build());

            // 200 OK + PDF本体を返す
            return new ResponseEntity<>(pdfBytes,headers,HttpStatus.OK);         
        } catch (IOException ex) {
            log.error("発注履歴PDFの生成に失敗しました", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** サイドバー共通で使う値を Model に入れる（他Controllerと同じ） */
    private void addSidebarAttributes(UserDetails userDetails, Model model){
        model.addAttribute("loginUsername", userDetails.getUsername());
        model.addAttribute("authorityId", userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)//権限IDをカンマ区切りで取得
        .findFirst()//権限IDをカンマ区切りで取得
        .orElse(""));//権限IDをカンマ区切りで取得
    }

}
