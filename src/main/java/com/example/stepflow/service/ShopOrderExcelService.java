package com.example.stepflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import java.util.List;
import com.example.stepflow.entity.Relation;
import com.example.stepflow.entity.Goods;
import com.example.stepflow.dto.ShopOrderImportResult;
import com.example.stepflow.form.ShopOrderForm;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 店舗発注まわりの Excel 生成。
 * A3: テンプレートDL / 次の A4: 同じ列形式で読み込み
 */
@Service
public class ShopOrderExcelService {
   
    @Autowired
    private ShopOrderService shopOrderService;


        /**
     * 自店舗向けの発注テンプレート（.xlsx）を byte[] で返す。
     */

    @Transactional(readOnly = true)
    public byte[] buildOrderTemplate(Integer shopId) throws IOException{

        // 店舗IDがない場合、どの店舗向けテンプレートか決められないため即エラー
        if(shopId == null ){
            throw new IllegalArgumentException("店舗が指定されていません");
        }
            // IllegalArgumentException → 「不正な引数」というエラーを投げる
          // ===== 1) テンプレートに載せる元データをDBから取得 =====
        // 連携倉庫: ログイン店舗に紐づく倉庫だけ（relationテーブル経由）
        List<Relation> relations = shopOrderService.findRelationsForShop(shopId);

          // 商品一覧: 有効な商品のみ（delete_flag=0）
          List<Goods> goodsList = shopOrderService.findActiveGoods();


               // try-with-resources:
        // 例外が起きても workbook / out を自動クローズしてリソースリークを防ぐ
        try(Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()){
                  
            // ===============          // ============================================================
            // シート1: 発注入力（ユーザーが実際に入力するメインシート）
            // ========================================================================================
            Sheet inputSheet = workbook.createSheet("発注入力") ;// シートを作成

            // ヘッダー行を作成 1行目: ヘッダー（A4一括発注で読む列定義と揃える）
            Row headerRow = inputSheet.createRow(0);
            headerRow.createCell(0).setCellValue("商品ID");
            headerRow.createCell(1).setCellValue("倉庫ID");
            headerRow.createCell(2).setCellValue("発注数量");

            
            // 2〜6行目: 入力用の空行（見た目/運用のために先に作っておく）
            // ※ 行数は運用に合わせて増減OK
            for(int i = 1; i <= 5 ; i++){
                inputSheet.createRow(i);
            }

              // 列幅調整（単位は 1/256 文字幅。4000は見やすさ優先の適当値）
              inputSheet.setColumnWidth(0,4000);//商品ID
              inputSheet.setColumnWidth(1,4000);//倉庫ID
              inputSheet.setColumnWidth(2,4000);//発注数量

                // ============================================================
            // シート2: 参考_連携倉庫（ID入力ミスを減らすための参照シート）
            // ============================================================
            Sheet warehouseSheet = workbook.createSheet("参考＿連携倉庫");
            // ヘッダー行を作成 1行目: ヘッダー（A4一括発注で読む列定義と揃える）
            Row whHeader = warehouseSheet.createRow(0);
            whHeader.createCell(0).setCellValue("倉庫ID");
            whHeader.createCell(1).setCellValue("倉庫名");

            // 2〜6行目: 入力用の空行（見た目/運用のために先に作っておく）
            int whRowIndex = 1;
            for (Relation relation : relations){
                Row whRow = warehouseSheet.createRow(whRowIndex++);
            
             // 倉庫ID（発注入力シートで指定する値）
             whRow.createCell(0).setCellValue(relation.getWarehouseId());

             // 倉庫名（発注入力シートで参照する値）
            // relation.warehouse は LAZY でnullの可能性があるのでnull安全に書く
            String warehouseName = (relation.getWarehouse() != null) ? relation.getWarehouse().getWarehouseName() : "";

            whRow.createCell(1).setCellValue(warehouseName);

            }

            // 列幅調整（単位は 1/256 文字幅。4000は見やすさ優先の適当値）
            warehouseSheet.setColumnWidth(0, 4000);//倉庫ID
            warehouseSheet.setColumnWidth(1, 7000);//倉庫名
            
             // ============================================================
            // シート3: 参考_商品（商品ID入力ミスを減らすための参照シート）
            // ============================================================
            Sheet goodsSheet = workbook.createSheet("参考_商品");

            // ヘッダー行を作成 1行目: ヘッダー（A4一括発注で読む列定義と揃える）
            Row goodsHeader = goodsSheet.createRow(0);
            goodsHeader.createCell(0).setCellValue("商品ID");
            goodsHeader.createCell(1).setCellValue("商品名");

            // 2〜6行目: 入力用の空行（見た目/運用のために先に作っておく）
            int goodsRowIndex = 1 ;// 商品行のインデックス
            for (Goods goods : goodsList){
                Row goodsRow = goodsSheet.createRow(goodsRowIndex++);
                goodsRow.createCell(0).setCellValue(goods.getGoodsId());
                goodsRow.createCell(1).setCellValue(goods.getGoodsName());
            }
               goodsSheet.setColumnWidth(0,4000);//商品ID
               goodsSheet.setColumnWidth(0,4000);//商品名

               
            // ============================================================
            // 最後に workbook をバイト列へ書き出し
            // Controller側で ResponseEntity<byte[]> として返す想定
            // ============================================================

            workbook.write(out);
            return out.toByteArray();

        }
        catch(IOException e){
            throw new RuntimeException("Excelファイルの作成に失敗しました", e);
        }
    }

    // ============================================================
    // A4: Excel 一括発注（読み込み）
    // テンプレートの「発注入力」シートと同じ列を読む
    //   A列(0)=商品ID  B列(1)=倉庫ID  C列(2)=発注数量
    // ============================================================

    /** 読み込むシート名（buildOrderTemplate の createSheet と一致させる） */
    private static final String INPUT_SHEET_NAME = "発注入力";

    /**
     * アップロードされた .xlsx を読み、1行ずつ発注登録する。
     *
     * @param shopId       ログイン店舗ID（自店舗の発注だけ登録する）
     * @param inputStream  ブラウザから送られた Excel ファイルの中身
     */
    @Transactional
  // ↑ このメソッド内の DB 変更を1まとまりに（行ごとの register も含む）
    public ShopOrderImportResult importOrdersFromExcel(Integer shopId, InputStream inputStream)
            throws IOException {

        if (shopId == null) {
            throw new IllegalArgumentException("店舗IDが指定されていません");
        }

        // 結果を入れる箱（成功件数・失敗メッセージ）
        ShopOrderImportResult result = new ShopOrderImportResult();

        // Excel のセルを「見た目どおりの文字列」で読む道具（数式セルでも安全）
        DataFormatter formatter = new DataFormatter();

        // try-with-resources: 読み終わったら workbook を自動で閉じる
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet(INPUT_SHEET_NAME);
            if (sheet == null) {
                // シート名が違う Excel を上げたとき
                result.addError(0, "シート「" + INPUT_SHEET_NAME + "」がありません。テンプレートを使ってください");
                return result;
            }

            // rowIndex=0 はヘッダー行 → 1 からデータ行
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

                Row row = sheet.getRow(rowIndex);
                // 行が無い、または3列とも空 → スキップ（テンプレの空行対策）
                if (row == null || isEmptyDataRow(row, formatter)) {
                    continue;
                }

                // Excel の行番号（1行目=ヘッダーなので +1）
                int excelRowNumber = rowIndex + 1;

                // A/B/C 列を Integer に変換
                Integer goodsId = readIntegerCell(row, 0, formatter);
                Integer warehouseId = readIntegerCell(row, 1, formatter);
                Integer orderQuantity = readIntegerCell(row, 2, formatter);

                if (goodsId == null || warehouseId == null || orderQuantity == null) {
                    result.addError(excelRowNumber, "商品ID・倉庫ID・発注数量は数値で入力してください");
                    continue; // 次の行へ
                }
                if (orderQuantity < 1) {
                    result.addError(excelRowNumber, "発注数量は1以上で入力してください");
                    continue;
                }

                // 画面の1件発注と同じ Form に詰める
                ShopOrderForm form = new ShopOrderForm();
                form.setGoodsId(goodsId);
                form.setWarehouseId(warehouseId);
                form.setOrderQuantity(orderQuantity);

                // 既存の登録処理を再利用（連携倉庫チェック・商品有効チェック込み）
                boolean ok = shopOrderService.registerShopOrder(shopId, form);
                if (ok) {
                    result.addSuccess();
                } else {
                    result.addError(excelRowNumber, "登録できません（連携倉庫・商品IDを確認してください）");
                }
            }
        }

        return result;
    }

    /** 3列とも空なら true（空行は読み飛ばす） */
    private boolean isEmptyDataRow(Row row, DataFormatter formatter) {
        return readIntegerCell(row, 0, formatter) == null
                && readIntegerCell(row, 1, formatter) == null
                && readIntegerCell(row, 2, formatter) == null;
    }

    /**
     * 1セルの値を Integer にする。
     * Excel は数値を 1.0 のように返すことがあるので Double 経由で int にする。
     */
    private Integer readIntegerCell(Row row, int columnIndex, DataFormatter formatter) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String text = formatter.formatCellValue(cell).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return (int) Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return null; // 文字だけ入っている → 無効
        }
    }

}
