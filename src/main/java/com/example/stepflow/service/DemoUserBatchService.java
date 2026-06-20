package com.example.stepflow.service;

// ============================================================
// 【このファイルの役割】
// 「バッチ処理の中身」= ユーザーを1件 DB に追加する。
//
// バッチ処理とは:
//   人が画面で1件ずつ登録する代わりに、
//   プログラムが決まった処理を自動で繰り返すこと。
//   ここでは「1回呼ばれたら user テーブルに1行 INSERT」。
//
// 画面のユーザー登録（UserController → UserService.saveUser）と
// 同じ saveUser を使うので、パスワード暗号化も同じ。
// ============================================================

import java.time.LocalDateTime;           // 今の日時
import java.time.format.DateTimeFormatter; // 日時 → 文字列

import org.slf4j.Logger;                  // ログ（コンソールに出す文字）
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// ↑ DB の INSERT を「1まとまり」として扱う（途中失敗時は巻き戻し）

import com.example.stepflow.UserRepository.UserRepository;
import com.example.stepflow.entity.User;

@Service
public class DemoUserBatchService {

    // このクラス用のログ。コンソールに [demo-batch] と出る
   private final Logger log = LoggerFactory.getLogger(DemoUserBatchService.class);
   //これで、DemoUserBatchServiceクラス用のログを作成することができます。

    // ユーザー名の末尾: batch_20260529_173045 のような形式
    private static final DateTimeFormatter NAME_SUFFIX =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final UserService userService;       // 保存処理（暗号化込み）
    private final UserRepository userRepository; // 重複チェック用

    /**
     * コンストラクタ注入:
     * Spring が UserService と UserRepository を自動で渡してくれる。
     * （new DemoUserBatchService(...) は自分で書かない）
     */
    public DemoUserBatchService(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * デモユーザーを1件登録する（バッチ1回分の仕事）。
     *
     * @return true = 登録した / false = 同名が既にありスキップ
     */
    @Transactional
    // ↑ このメソッド内の DB 変更を1トランザクションに。
    //   成功 → commit（確定） / 例外 → rollback（取り消し）

    public boolean addOneDemoUser() {

        // --- 1) 一意なユーザー名を作る ---
        // "batch_" + 今の日時 → ほぼ毎回違う名前になる
        String userName = "batch_" + LocalDateTime.now().format(NAME_SUFFIX);

        // --- 2) 同じ名前が既にいないか確認 ---
        if (userRepository.findUserByUserName(userName) != null) {
            log.warn("[demo-batch] skip duplicate userName={}", userName);
            return false; // 登録せず終了
        }

        // --- 3) User オブジェクトに値をセット ---
        User user = new User();
        user.setUserName(userName);           // ログインID
        user.setUserPassword("BatchPass1");   // 平文（saveUser 内で BCrypt 化される）
        user.setAuthorityId(1);               // 1=管理者（shop_id / warehouse_id 不要）
        user.setUserGender(0);                // 0=その他
        user.setDeleteFlag(0);                // 0=有効（論理削除されていない）

        // --- 4) 既存の登録処理で DB に保存 ---
        userService.saveUser(user);
        // saveUser の中身: パスワード暗号化 → userRepository.save(user) = INSERT

        log.info("[demo-batch] user created: {}", userName);
        return true;
    }
}
