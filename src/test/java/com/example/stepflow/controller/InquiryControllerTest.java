// お問い合わせ画面（/inquiry）をテストするクラス
package com.example.stepflow.controller;

// static import：GET 送信用
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// static import：POST 送信用
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// static import：Model 検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
// static import：リダイレクト先 URL 検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
// static import：リダイレクト先 URL のパターン一致検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
// static import：HTTP ステータス検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// static import：画面名検証用
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
// Spring Security Test：POST 時の CSRF トークン
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.UserRepository.UserRepository;
import com.example.stepflow.entity.Inquiry;
import com.example.stepflow.entity.Shop;
import com.example.stepflow.entity.User;
import com.example.stepflow.entity.Warehouse;
import com.example.stepflow.repository.InquiryRepository;
import com.example.stepflow.repository.ShopRepository;
import com.example.stepflow.repository.WarehouseRepository;

// ─────────────────────────────────────────
// ゲスト送信（user_id=99）・ログイン一覧・新規フォーム・詳細の権限を確認
// LoginSecurityTest で GET /inquiry の permitAll は既にカバー済み
// ─────────────────────────────────────────
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InquiryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private WarehouseRepository warehouseRepository;

	@Autowired
	private InquiryRepository inquiryRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/** InquiryController.GUEST_USER_ID と同じ値（FK 用ゲストユーザー） */
	private static final int GUEST_USER_ID = 99;

	private Integer testShopId;
	private Integer testWarehouseId;
	private Integer adminInquiryId;

	@BeforeEach
	void setUp() {
		// 子 → 親の順で削除（inquiry.user_id → user）
		inquiryRepository.deleteAll();
		userRepository.deleteAll();

		Shop shop = new Shop();
		shop.setShopName("問い合わせテスト店舗");
		shop.setShopAddress("東京都渋谷区1-1");
		shop.setDeleteFlag(0);
		testShopId = shopRepository.save(shop).getShopId();

		Warehouse warehouse = new Warehouse();
		warehouse.setWarehouseName("問い合わせテスト倉庫");
		warehouse.setWarehouseAddress("東京都品川区1-1");
		warehouse.setDeleteFlag(0);
		testWarehouseId = warehouseRepository.save(warehouse).getWarehouseId();

		// ゲスト送信は user_id=99 固定（JPA の IDENTITY では指定できないため JDBC で投入）
		jdbcTemplate.update(
				"INSERT INTO user (user_id, user_name, user_password, authority_id, delete_flag) VALUES (?, ?, ?, ?, ?)",
				GUEST_USER_ID,
				"guest_inquiry",
				passwordEncoder.encode("guestpass1"),
				1,
				0);

		User admin = new User();
		admin.setUserName("admin_inquiry_test");
		admin.setUserPassword(passwordEncoder.encode("admin"));
		admin.setAuthorityId(1);
		admin.setDeleteFlag(0);
		userRepository.save(admin);

		User shopUser = new User();
		shopUser.setUserName("shop_inquiry_test");
		shopUser.setUserPassword(passwordEncoder.encode("shop"));
		shopUser.setAuthorityId(2);
		shopUser.setShopId(testShopId);
		shopUser.setDeleteFlag(0);
		userRepository.save(shopUser);

		// 管理者宛の問い合わせ（詳細画面テスト用）
		Inquiry adminInquiry = new Inquiry();
		adminInquiry.setUserId(GUEST_USER_ID);
		adminInquiry.setInquiryCategoryId(1);
		adminInquiry.setInquiryDetail("管理者宛テスト問い合わせ");
		adminInquiry.setInquiryStatus("未対応");
		adminInquiry.setAuthorityId(1);
		adminInquiry.setDeleteFlag(0);
		adminInquiryId = inquiryRepository.save(adminInquiry).getInquiryId();
	}

	// ─────────────────────────────────────────
	// TC：未ログインのトップ（ゲストフォーム）
	// ─────────────────────────────────────────
	@Test
	@DisplayName("未ログインではゲスト用問い合わせ画面を表示できる")
	void guestCanViewInquiryGuestPage() throws Exception {

		mockMvc.perform(get("/inquiry"))
				.andExpect(status().isOk())
				.andExpect(view().name("inquiry-guest"))
				.andExpect(model().attribute("guestMode", true));
	}

	// ─────────────────────────────────────────
	// TC：ゲスト送信
	// ─────────────────────────────────────────
	@Test
	@DisplayName("未ログインから問い合わせを送信するとログイン画面へリダイレクトする")
	void guestCanSendInquiry() throws Exception {

		mockMvc.perform(post("/inquiry/send")
						.with(csrf())
						.param("receiverRole", "ROLE_ADMIN")
						.param("content", "ゲストからのテスト問い合わせ"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}

	// ─────────────────────────────────────────
	// TC：ログイン済み一覧（管理者）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "admin_inquiry_test", roles = "ADMIN")
	@DisplayName("管理者は問い合わせ一覧画面を表示できる")
	void adminCanViewInquiryList() throws Exception {

		mockMvc.perform(get("/inquiry"))
				.andExpect(status().isOk())
				.andExpect(view().name("inquiry-list"))
				.andExpect(model().attributeExists("rows"));
	}

	// ─────────────────────────────────────────
	// TC：ログイン済み一覧（店舗）
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "shop_inquiry_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは問い合わせ一覧画面を表示できる")
	void shopUserCanViewInquiryList() throws Exception {

		mockMvc.perform(get("/inquiry"))
				.andExpect(status().isOk())
				.andExpect(view().name("inquiry-list"))
				.andExpect(model().attributeExists("rows"));
	}

	// ─────────────────────────────────────────
	// TC：新規フォーム（ログイン必須）
	// ─────────────────────────────────────────
	@Test
	@DisplayName("未ログインでは新規問い合わせ画面に入れずログインへ")
	void unauthenticatedCannotAccessNewForm() throws Exception {

		mockMvc.perform(get("/inquiry/new"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	@WithMockUser(username = "admin_inquiry_test", roles = "ADMIN")
	@DisplayName("管理者は新規問い合わせフォームを表示できる")
	void adminCanAccessNewForm() throws Exception {

		mockMvc.perform(get("/inquiry/new"))
				.andExpect(status().isOk())
				.andExpect(view().name("inquiry-form"))
				.andExpect(model().attribute("guestMode", false));
	}

	// ─────────────────────────────────────────
	// TC：詳細画面の権限
	// ─────────────────────────────────────────
	@Test
	@WithMockUser(username = "admin_inquiry_test", roles = "ADMIN")
	@DisplayName("管理者は管理者宛の問い合わせ詳細を表示できる")
	void adminCanViewInquiryDetail() throws Exception {

		mockMvc.perform(get("/inquiry/" + adminInquiryId))
				.andExpect(status().isOk())
				.andExpect(view().name("inquiry-detail"))
				.andExpect(model().attributeExists("inquiry"));
	}

	@Test
	@WithMockUser(username = "shop_inquiry_test", roles = "SHOP")
	@DisplayName("店舗ユーザーは管理者宛の問い合わせ詳細を閲覧できない")
	void shopUserCannotViewAdminInquiryDetail() throws Exception {

		mockMvc.perform(get("/inquiry/" + adminInquiryId))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/inquiry"));
	}

	@Test
	@WithMockUser(username = "admin_inquiry_test", roles = "ADMIN")
	@DisplayName("No.11: 管理者はステータスで問い合わせ一覧を絞り込める")
	void adminCanFilterInquiryListByStatus() throws Exception {

		mockMvc.perform(get("/inquiry").param("status", "未対応"))
				.andExpect(status().isOk())
				.andExpect(view().name("inquiry-list"))
				.andExpect(model().attribute("statusFilter", "未対応"));
	}

	@Test
	@WithMockUser(username = "admin_inquiry_test", roles = "ADMIN")
	@DisplayName("No.12: 管理者は共通問い合わせ詳細でステータスを更新できる")
	void adminCanUpdateInquiryStatus() throws Exception {

		mockMvc.perform(post("/inquiry/" + adminInquiryId + "/status")
						.with(csrf())
						.param("inquiryStatus", "対応中"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/inquiry/" + adminInquiryId));
	}
}
