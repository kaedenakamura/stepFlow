package com.example.stepflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.stepflow.entity.User;

@Service
// ここで、UserDetailsServiceインターフェースを実装するクラスを作成します。これが、ユーザーの認証情報を提供する役割を果たします。
public class CustomUserDetailService implements UserDetailsService{
	
	@Autowired // これで、UserServiceのインスタンスが自動的に注入されます。
	private UserService userService;
	
	@Override// UserDetailsServiceインターフェースのメソッドをオーバーライドして、ユーザー名をもとにユーザー情報を取得する処理を実装します。
	// ここで、ユーザー名をもとにユーザー情報を取得し、CustomUserDetailsオブジェクトを返す処理を実装します。
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
		//ここで、ユーザー名をもとにユーザー情報を取得し、CustomUserDetailsオブジェクトを返す処理を実装します。
		// 例えば、ユーザー名をもとにユーザー情報を取得する
		User user = userService.findUserByName(userName);
		
		// ユーザーが見つからない場合は、UsernameNotFoundExceptionをスローします
		if(user == null || Integer.valueOf(1).equals(user.getDeleteFlag())){
			throw new UsernameNotFoundException("ユーザーが見つかりませんでした: " + userName);
		}
		// ユーザーが見つかった場合は、ユーザーの情報をもとにCustomUserDetailsオブジェクトを作成して返します。
		// ここで、ユーザーの情報をもとにCustomUserDetailsオブジェクトを作成して返す処理を実装します。
		String roleName = null;
		if(user.getAuthorityId() == 1) {
			roleName = "ADMIN"; // 管理者の権限
		} else if(user.getAuthorityId() == 2) {
			roleName = "SHOP"; // 店舗スタッフの権限
		}else if(user.getAuthorityId() == 3) {
			roleName ="WAREHOUSE";// 倉庫スタッフの権限
			}
		
		// ここで、ユーザーの情報をもとにCustomUserDetailsオブジェクトを作成して返す処理を実装します。
		return org.springframework.security.core.userdetails.User.withUsername(user.getUserName())
				.password(user.getUserPassword())
				.roles(roleName) // ADMIN,SHOPなどの権限
				.build();
	
}
}