// メインアプリと同じパッケージ（@SpringBootApplication を自動で見つけられる）
package com.example.stepflow;

// JUnit：テストメソッドであることを示す
import org.junit.jupiter.api.Test;
// Spring Test：アプリ全体を起動する統合テスト用
import org.springframework.boot.test.context.SpringBootTest;
// Spring Test：テスト用プロファイル（application-test.properties）を有効化
import org.springframework.test.context.ActiveProfiles;

// アプリ全体がエラーなく起動できるか確認するスモークテスト
@SpringBootTest // StepflowApplication を含む Spring コンテキストを丸ごと起動
@ActiveProfiles("test") // 本番 DB ではなく stepflow_test_db を使う
class StepflowApplicationTests { // 起動確認専用のテストクラス

	@Test // このメソッドをテストとして実行
	void contextLoads() { // 中身が空でも OK（起動できればテスト成功）
		// 何も書かなくてよい
		// Spring が Bean を全部作れればこのテストはパスする
	}
}
