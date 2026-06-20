package com.example.stepflow.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.stepflow.entity.ShopOrder;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * 店舗発注履歴のPDF生成サービス。
 * CSVと同じ列（商品名/倉庫名/発注数/ステータス/発注日時/更新日時）を表形式で出力する。
 */
@Service
public class ShopSalesPdfService {

    /**
     * 発注履歴一覧をPDF(byte[])に変換して返す。
     */
    public byte[] buildSalesHistoryPdf(List<ShopOrder> orders) throws DocumentException, IOException {

        // A4横置き（列が6つあるので横の方が見やすい）
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);

        // PDFの中身をメモリに溜める
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // 日本語フォント（resources/fonts/ipaexg.ttf）
        BaseFont baseFont = BaseFont.createFont(
                "fonts/ipaexg.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED);

        // フォントサイズと太さを設定
        Font titleFont = new Font(baseFont, 14, Font.BOLD);
        Font headerFont = new Font(baseFont, 10, Font.BOLD);
        Font bodyFont = new Font(baseFont, 9, Font.NORMAL);

        // PDFWriterを設定
        PdfWriter.getInstance(document, out);

        // PDFを開く
        document.open();

        // タイトルを設定
        Paragraph title = new Paragraph("発注履歴一覧", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10f);
        document.add(title);

        // 表を作成
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100f);
        table.setWidths(new float[] { 3f, 2.5f, 1f, 1.2f, 2.2f, 2.2f });

        // 表のヘッダーを作成
        addHeaderCell(table, "商品名", headerFont);
        addHeaderCell(table, "倉庫名", headerFont);
        addHeaderCell(table, "発注数", headerFont);
        addHeaderCell(table, "ステータス", headerFont);
        addHeaderCell(table, "発注日時", headerFont);
        addHeaderCell(table, "更新日時", headerFont);

        // 表のボディーを作成
        if (orders.isEmpty()) {//データがない場合
            PdfPCell emptyCell = new PdfPCell(new Phrase("表示できるデータがありません。", bodyFont));
            emptyCell.setColspan(6);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(emptyCell);
        } else {//データがある場合
            // データを1件ずつ取り出して表に追加
            for (ShopOrder order : orders) {
                table.addCell(createBodyCell(resolveGoodsName(order), bodyFont));
                table.addCell(createBodyCell(resolveWarehouseName(order), bodyFont));
                table.addCell(createBodyCell(String.valueOf(order.getOrderQuantity()), bodyFont));
                table.addCell(createBodyCell(safeText(order.getOrderStatus()), bodyFont));
                table.addCell(createBodyCell(formatDateTime(order.getOrderDate()), bodyFont));
                table.addCell(createBodyCell(formatDateTime(order.getUpdateDate()), bodyFont));
            }
        }

        // 表をPDFに追加
        document.add(table);
        // PDFを閉じる
        document.close();

        // PDFをbyte[]に変換して返す
        return out.toByteArray();
    }

    /** 表のヘッダーを作成 */
    private void addHeaderCell(PdfPTable table, String text, Font font) {
        // セルを作成
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        // 表に追加
        table.addCell(cell);
    }

    /** 表のボディーを作成 */
    private PdfPCell createBodyCell(String text, Font font) {
        // セルを作成
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        // セルを返す
        return cell;
    }

    /** 商品名を文字列化（Entityの関連が取れない時はID表示） */
    private String resolveGoodsName(ShopOrder order) {
        if (order.getGoods() != null) {//商品がある場合
            return safeText(order.getGoods().getGoodsName());//商品名を返す
        }
        return "商品ID:" + order.getGoodsId();//商品IDを返す
    }

    /** 倉庫名を文字列化（Entityの関連が取れない時はID表示） */
    private String resolveWarehouseName(ShopOrder order) {
        if (order.getWarehouse() != null) {//倉庫がある場合
            return safeText(order.getWarehouse().getWarehouseName());//倉庫名を返す
        }
        return "倉庫ID:" + order.getWarehouseId();//倉庫IDを返す
    }

    /** nullを空文字にする */
    private String safeText(String value) {
        // nullの場合は空文字を返す
        return value != null ? value : "";
    }

    /** 日時を文字列化 */
    private String formatDateTime(LocalDateTime value) {
        // nullの場合は空文字を返す
        return value != null ? value.toString() : "";//日時を文字列化して返す
    }

     /**
     * 倉庫受注一覧をPDF(byte[])に変換して返す。
     * 列: 店舗名/商品名/発注数/ステータス/発注日時/更新日時
     */

     public byte[] buildWarehouseOrderHistoryPdf(List<ShopOrder> orders) throws DocumentException, IOException {
     
        //A4横置き（列が6つあるので横の方が見やすい）
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        //PDFの中身をメモリに溜める→
        // document.open()でPDFを開いてから、document.add(table)で表を追加してから、document.close()でPDFを閉じる
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        //日本語フォント（resources/fonts/ipaexg.ttf）
        BaseFont baseFont = BaseFont.createFont(
            //resources/fonts/ipaexg.ttfのパスを指定
            "fonts/ipaexg.ttf",
            //IDENTITY_Hは横書きのフォント
            BaseFont.IDENTITY_H,
            //EMBEDDEDはフォントを埋め込む
            BaseFont.EMBEDDED
        );
        //フォントサイズと太さを設定
        Font titleFont = new Font(baseFont, 14, Font.BOLD);
        Font headerFont = new Font(baseFont, 10, Font.BOLD);
        Font bodyFont = new Font(baseFont, 9, Font.NORMAL);

        //PDFWriterを設定
        PdfWriter.getInstance(document, out);
        document.open();

        //タイトルを設定(倉庫画面に合わせる)
        Paragraph title = new Paragraph("受注一覧", titleFont);
        //中央寄せ
        title.setAlignment(Element.ALIGN_CENTER);
        //下に10ポイント空ける
        title.setSpacingAfter(10f);
        //タイトルをPDFに追加
        document.add(title);


        //表を作成
        //列数は6列（店舗名/商品名/発注数/ステータス/発注日時/更新日時）
        PdfPTable table = new PdfPTable(6);
        //表の幅を100%にする
        table.setWidthPercentage(100f);
        //列の幅を設定（店舗名/商品名/発注数/ステータス/発注日時/更新日時）
        table.setWidths(new float[] { 3f, 2.5f, 1f, 1.2f, 2.2f, 2.2f });
        //3:店舗名/2.5:商品名/1:発注数/1.2:ステータス/2.2:発注日時/2.2:更新日時の画面出力する比率

        //表のヘッダーを作成
        addHeaderCell(table,"店舗名",headerFont);
        addHeaderCell(table,"商品名",headerFont);
        addHeaderCell(table,"発注数",headerFont);
        addHeaderCell(table,"ステータス",headerFont);
        addHeaderCell(table,"発注日時",headerFont);
        addHeaderCell(table,"更新日時",headerFont);

        //表のボディーを作成
        if (orders.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("表示できるデータがありません。", bodyFont));
            emptyCell.setColspan(6);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(emptyCell);
        } else {
            for (ShopOrder order : orders) {
                table.addCell(createBodyCell(resolveShopName(order), bodyFont));
                table.addCell(createBodyCell(resolveGoodsName(order), bodyFont));
                table.addCell(createBodyCell(String.valueOf(order.getOrderQuantity()), bodyFont));
                table.addCell(createBodyCell(safeText(order.getOrderStatus()), bodyFont));
                table.addCell(createBodyCell(formatDateTime(order.getOrderDate()), bodyFont));
                table.addCell(createBodyCell(formatDateTime(order.getUpdateDate()), bodyFont));
            }
        }
        //表をPDFに追加
        document.add(table);
        document.close();//PDFを閉じる
        //PDFをbyte[]に変換して返す→return out.toByteArray();は固定のコードdocumentの中身を入れるコード
        return out.toByteArray();//toByteArray()はメモリに溜めたPDFをbyte[]に変換して返す標準メソッド

    }
    /** 店舗名を文字列化（Entityの関連が取れない時はID表示） */
    private String resolveShopName(ShopOrder order){
        //Entityの関連が取れない時はID表示
        if(order.getShop() != null){//店舗がある場合
            //店舗名を返す
            return safeText(order.getShop().getShopName());//店舗名を返す
        }
        return "店舗ID:" + order.getShopId();//店舗IDを返す
    }

}
