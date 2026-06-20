package com.example.stepflow.config;

// ============================================================
// 【このファイルの役割】
// Spring Boot に「定期的にメソッドを実行する機能」を ON にする設定。
// ここが無いと @Scheduled が付いたクラスは動かない。
// ============================================================

import org.springframework.context.annotation.Configuration;
// ↑ 「これは設定用クラスです」と Spring に伝える import

import org.springframework.scheduling.annotation.EnableScheduling;
// ↑ 「@Scheduled（定期実行）を使えるようにして」と伝える import

@Configuration
// ↑ 起動時に Spring が読み込む「設定 Bean」になる
//   （Controller や Service とは別枠の「設定専用クラス」）

@EnableScheduling
// ↑ これ1行で、アプリ全体の「タイマー機能」が有効になる
//   DemoUserBatchScheduler の runIfEnabled() が 5分ごとに呼ばれる前提

public class SchedulingConfig {
    // 中にメソッドは書かない。
    // 「定期実行スイッチ ON」の印だけを付ける空の箱で OK。
}
