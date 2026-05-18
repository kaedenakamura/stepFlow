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
			   //登録画面と、保存処理のURLを「許可」(permitAll)する
			   .requestMatchers("/users/new","/users/new/" ).permitAll()// 「user/new」へのアクセスを誰でも許可する設定
			   //登録画面と、保存処理のURLを「許可」(permitAll)する
			   .requestMatchers(HttpMethod.POST,"/users").permitAll()// 「/users」へのPOSTアクセスを誰でも許可する設定
			   
			// 🌟【画面遷移図の仕様】ユーザー管理機能（/users から始まるすべてのURL）は、
			   // ログインしていて、かつ「管理者（ADMIN）」の権限を持っている人だけしか入れないように強固にガード！
			   .requestMatchers("/users/**").hasAuthority("1")//
			   
			   //静的リソース（CCS/JS）
			   .requestMatchers("/css/**","/js/**").permitAll()// 「/css/**」と「/js/**」へのアクセスを誰でも許可する設定
			   
			   //それ以外は全部ログイン必須
			   .anyRequest().authenticated() //「それ以外」は、ログインしてる人だけ！
			   
			   )
	   
	   .formLogin(form -> form
			   .loginPage("/login")// ログインページのURLを指定する設定
			   .defaultSuccessUrl("/home", true)// ログイン成功後のリダイレクト先を指定する設定
			   .permitAll()// ログインページへのアクセスを許可する設定
	   );
	   return http.build();// SecurityFilterChainオブジェクトを返す
   }
}