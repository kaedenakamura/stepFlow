package com.example.stepflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // 「このクラスは設定用のクラスですよ」という印
@EnableWebSecurity // Spring Securityを有効にするためのアノテーション
public class SecurityConfig {

    @Bean // 「このメソッドが作る道具（PasswordEncoder）をSpringで共有してね」という印
    public PasswordEncoder passwordEncoder() {
        // 根拠：実務で最も信頼されている暗号化方式（BCrypt）を返します
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		   http.authorizeHttpRequests(auth -> auth

			// 「/login」へのアクセスを誰でも許可する設定
		       .requestMatchers("/login","/login/").permitAll()
			   // ゲスト問い合わせ（未ログイン可）
			   .requestMatchers(HttpMethod.GET, "/inquiry", "/inquiry/", "/inquiry/form").permitAll()
			   .requestMatchers(HttpMethod.POST,"/inquiry/send").permitAll()

			// 管理者専用（CustomUserDetailService .roles("ADMIN") → ROLE_ADMIN）
			   .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")

			// ユーザー管理（一覧・新規・編集・保存・削除）は管理者のみ
			   .requestMatchers("/users", "/users/**").hasAuthority("ROLE_ADMIN")
			   
			   .requestMatchers("/shop/**").hasAuthority("ROLE_SHOP")
			   // 「/shop/**」へのアクセスを「店舗スタッフ（ROLE_SHOP）」のみ許可する設定
			   .requestMatchers("/warehouse/**").hasAuthority("ROLE_WAREHOUSE")
			   // 「/warehouse/**」へのアクセスを「倉庫スタッフ（ROLE_WAREHOUSE）」のみ許可する設定
			   //静的リソース（CCS/JS）
			   .requestMatchers("/css/**","/js/**").permitAll()// 「/css/**」と「/js/**」へのアクセスを誰でも許可する設定
			   
			   //それ以外は全部ログイン必須
			   .anyRequest().authenticated() //「それ以外」は、ログインしてる人だけ！
			   
			   )

			   //アクセス拒否時のハンドリング
			   .exceptionHandling(exception ->
				 exception.accessDeniedPage("/access-denied"))
				 //アクセス拒否時のハンドリング

	   
	   .formLogin(form -> form
			   .loginPage("/login")// ログインページのURLを指定する設定
			   .defaultSuccessUrl("/home", true)// ログイン成功後のリダイレクト先を指定する設定
			   .permitAll()// ログインページへのアクセスを許可する設定
	   );
	   return http.build();// SecurityFilterChainオブジェクトを返す
   }
}