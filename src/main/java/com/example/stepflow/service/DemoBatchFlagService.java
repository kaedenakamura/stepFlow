package com.example.stepflow.service;

// ============================================================
// 【このファイルの役割】
// 「バッチを動していいか？」を、ファイルの有無で判定する。
//
// イメージ:
//   start-demo-user-batch.bat → demo-batch.ON というファイルを作る
//   Java は「そのファイルがある？」だけ見る → あれば ON
//   stop-demo-user-batch.bat  → ファイルを消す → OFF
//
// アプリを再起動しなくても、.bat だけで開始/停止できるのがポイント。
// ============================================================

import java.nio.file.Files;  // ファイルが存在するか調べる
import java.nio.file.Path;   // ファイルの場所（パス）を表す型
import java.nio.file.Paths;  // 文字列 "scripts/demo-batch.ON" → Path に変換

import org.springframework.beans.factory.annotation.Value;
// ↑ application.properties の値をコンストラクタに渡す

import org.springframework.stereotype.Service;
// ↑ Service として Spring に登録（他クラスから @Autowired で使える）

@Service
public class DemoBatchFlagService {

    // フラグファイルの場所。final = 一度決めたら変更しない
    private final Path flagFile;

    /**
     * コンストラクタ: Spring がこのクラスを作るときに1回だけ呼ばれる。
     *
     * @Value("${stepflow.demo-batch.flag-file:scripts/demo-batch.ON}")
     *   - application.properties の stepflow.demo-batch.flag-file を読む
     *   - 無ければデフォルト scripts/demo-batch.ON
     */
    public DemoBatchFlagService (
        @Value("${stepflow.demo-batch.flag-file:scripts/demo-batch.ON}") String flagFilePath){
        // Paths.get … 相対パスは「アプリを起動したフォルダ（通常 stepflow 直下）」基準で相対パスを指定する
   
       this.flagFile = Paths.get(flagFilePath);
       // this.flagFile … このクラスの flagFile フィールドに、Paths.get で作った Path を代入する
       // flagFilePath … 上の @Value で読んだ値（scripts/demo-batch.ON など）
       // Paths.get … 相対パスは「アプリを起動したフォルダ（通常 stepflow 直下）」基準で相対パスを指定する
       // 例えば、アプリを stepflow 直下で起動した場合：
       //   Paths.get("scripts/demo-batch.ON") → stepflow/scripts/demo-batch.ON
       // アプリを stepflow/src で起動した場合：
       //   Paths.get("../../scripts/demo-batch.ON") → stepflow/scripts/demo-batch.ON
       // アプリを stepflow/target で起動した場合：
       //   Paths.get("../../../scripts/demo-batch.ON") → stepflow/scripts/demo-batch.ON
       // アプリを stepflow/target/classes で起動した場合：
       //   Paths.get("../../../../scripts/demo-batch.ON") → stepflow/scripts/demo-batch.ON
    }
    /**
     * バッチが動いてよいか？
     * @return true = demo-batch.ON がある（start.bat 済み）
     *         false = ファイルが無い（stop.bat 済み or 未開始）
     */
    public boolean isEnabled() {
        return Files.exists(flagFile);
        // Files.exists … OS に「このファイルある？」と聞く。中身は読まない
    }

    /** どのパスを見ているか確認したいとき用（ログやデバッグ） */
    public Path getFlagFile() {
        return flagFile;
    }
}
