package com.example.stepflow.scheduler;

// ============================================================
// 【このファイルの役割】
// 「タイマー役」= 決まった時間ごとに「仕事をやるか？」と起きる。
//
// 流れ（5分ごと）:
//   1. Spring が runIfEnabled() を自動で呼ぶ
//   2. フラグファイルがある？ → 無ければ return（何もしない）
//   3. あれば DemoUserBatchService.addOneDemoUser() を1回実行
//
// .bat は「ON/OFF スイッチ」、このクラスは「5分おきのアラーム時計」。
// ============================================================

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
// ↑ 「このメソッドを定期的に実行して」と Spring に指示

import org.springframework.stereotype.Component;
// ↑ Spring が管理する Bean（@Service と似ているが「部品」という意味）

import com.example.stepflow.service.DemoBatchFlagService;
import com.example.stepflow.service.DemoUserBatchService;

@Component
public class DemoUserBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(DemoUserBatchScheduler.class);

    private final DemoBatchFlagService flagService;           // ON/OFF 判定
    private final DemoUserBatchService demoUserBatchService;  // 実際の1件登録

    public DemoUserBatchScheduler(
            DemoBatchFlagService flagService,
            DemoUserBatchService demoUserBatchService) {
        this.flagService = flagService;
        this.demoUserBatchService = demoUserBatchService;
    }

    /**
     * 【バッチの入口】Spring が時間になったら自動で呼ぶメソッド。
     *
     * initialDelayString … アプリ起動後、最初の1回まで何 ms 待つか
     *                        10000 = 10秒（すぐ試せるように短め）
     *
     * fixedRateString      … 2回目以降、何 ms ごとに繰り返すか
     *                        300000 = 5分（60秒×5）
     *
     * 値は application.properties で変更可能。
     */
    @Scheduled(
            initialDelayString = "${stepflow.demo-batch.initial-delay-ms:10000}",
            fixedRateString = "${stepflow.demo-batch.interval-ms:300000}")
    public void runIfEnabled() {

        // ----- ここが「停止」の本体 -----
        // stop.bat が demo-batch.ON を消すと isEnabled() が false → 即 return
        if (!flagService.isEnabled()) {
            return;
            // return だけでメソッド終了。DB には一切触らない。
            // タイマー自体は止まらないが、中身は空振りになる。
        }

        // デバッグ用（ログレベル debug のときだけ表示）
        log.debug("[demo-batch] tick (flag ON) -> add user");

        // 1件登録（成功/失敗は addOneDemoUser 内で処理）
        demoUserBatchService.addOneDemoUser();
    }
}
