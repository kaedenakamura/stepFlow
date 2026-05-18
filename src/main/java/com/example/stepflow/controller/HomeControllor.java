package com.example.stepflow.controller;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeControllor {
		
	@GetMapping("/home")
	public String showHomePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		//ログイン中のユーザー名を取得して画面に渡す
		model.addAttribute("loginUsername", userDetails.getUsername());
		
		//ユーザーの権限をチェックして、管理者かどうかを判定
		//authorityIdは、ユーザーの権限を表すIDで1が管理者、2が店舗スタッフ、3が倉庫スタッフなどの区別をするために使用されます。
		//ユーザーの権限を取得するために、UserDetailsからAuthoritiesを取得し、その中から最初の権限を取り出す。
		String authorityId = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				//UserDetailsのgetAuthorities()メソッドは、ユーザーの権限を表すGrantedAuthorityオブジェクトのコレクションを返します。
				//これをストリームに変換し、map()メソッドを使って各GrantedAuthorityから権限名を取得します。
				.findFirst()//最初の権限を取得します。ユーザーが複数の権限を持っている場合は、最初のものが使用されます。
				.orElse(""); //権限がない場合は空文字を返す
		
		//管理者かどうかの情報を画面に渡す
		//画面側でisAdminの値を使用して、管理者向けのコンテンツを表示するかどうかを制御できるようにする。
		//例えば、Thymeleafの条件式でisAdminを使用して、管理者向けのセクションを表示するかどうかを制御することができます。
		//例: <div th:if="${isAdmin}">管理者向けのコンテンツ</div>
		//このようにすることで、管理者と一般ユーザーで表示内容を分けることができます。
		model.addAttribute("authorityId", authorityId);
		//【デバッグ】画面に送る直前の authorityIdをコンソールに出力する
		System.out.println("【デバッグ】画面に送る直前の authorityId: " + authorityId);
		
		
		//ホーム画面のテンプレートを返す//
		return "home";
	}
}
