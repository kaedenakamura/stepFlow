package com.example.stepflow.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * A4: Excel一括発注の「結果」を画面に渡すための箱（DTO）。
 *
 * 例:
 *   成功 3件 / 失敗 1件
 *   エラー: 「5行目: 連携していない倉庫IDです」
 */
@Data // Lombok: getter/setter を自動生成
public class ShopOrderImportResult {

    /** 登録に成功した行の数 */
    private int successCount;

    /** 登録に失敗した行の数 */
    private int failureCount;

    /**
     * 失敗した行の理由一覧。
     * 例: "5行目: 発注数量は1以上で入力してください"
     */
    private List<String> messages = new ArrayList<>();

    /**
     * 失敗1件分を記録する。
     *
     * @param excelRowNumber Excel上の行番号（人間が見る 2,3,4... 行目）
     * @param message        失敗理由
     */
    public void addError(int excelRowNumber, String message) {
        failureCount++; // 失敗カウンタ +1
        messages.add(excelRowNumber + "行目: " + message);
    }

    /** 成功1件分を記録する */
    public void addSuccess() {
        successCount++; // 成功カウンタ +1
    }
}
