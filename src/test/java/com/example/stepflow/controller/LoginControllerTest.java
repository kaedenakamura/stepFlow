// このテストクラスが属するパッケージ（メインの Controller と同じ階層に置く）
package com.example.stepflow.controller;

// JUnit：テスト名を日本語で表示するためのアノテーション
import org.junit.jupiter.api.DisplayName;
// JUnit：「このメソッドはテストです」と印を付けるアノテーション
import org.junit.jupiter.api.Test;
// Spring：フィールドへ自動で部品（Bean）を注入するアノテーション
import org.springframework.beans.factory.annotation.Autowired;
// Spring Test：MockMvc（擬似ブラウザ）を有効にするアノテーション
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// Spring Test：Controller だけを切り出して Web テストするアノテーション
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// Spring Test：HTTP リクエストを送るための道具（ブラウザの代わり）
import org.springframework.test.web.servlet.MockMvc;

// static import：get("/login") のように短く書くため（MockMvcRequestBuilders.get の省略）
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// static import：status().isOk() のように HTTP ステータスを検証するため
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// static import：view().name("login") のように返却画面名を検証するため
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// LoginController だけを読み込む軽量な Web テスト（DB や他 Controller は起動しない）
@WebMvcTest(LoginController.class)
// Security フィルタをオフにする（このテストは「画面が返るか」だけ見るため）
@AutoConfigureMockMvc(addFilters = false)
// テストクラス本体
class LoginControllerTest {

	// Spring が用意した MockMvc をこのフィールドに自動注入する
	@Autowired
	// 擬似 HTTP クライアント（GET / POST をコードから送れる）
	private MockMvc mockMvc;

	// ─────────────────────────────────────────
	// テストケース：ログイン画面の表示
	// ─────────────────────────────────────────
	@Test // JUnit に「このメソッドをテストとして実行して」と伝える
	@DisplayName("ログイン画面が表示される") // レポートや IDE に出る日本語のテスト名（仕様書の項目名をここに書く）
	void loginPageIsAccessible() throws Exception { // テストメソッド（throws Exception は MockMvc の決まり）

		// ① GET /login を1回送る（ブラウザでログイン URL を開くのと同じ）
		mockMvc.perform(get("/login"))
				// ② HTTP ステータスが 200 OK であることを確認
				.andExpect(status().isOk())
				// ③ Controller が返した Thymeleaf テンプレート名が "login" であることを確認
				.andExpect(view().name("login"));
	}
}
