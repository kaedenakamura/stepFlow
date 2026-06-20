package com.example.stepflow.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.stepflow.UserRepository.UserRepository;
import com.example.stepflow.entity.User;


@Service
public class UserService {
		 // ここに「ユーザーに関するビジネスロジック」を書いていきます。
	 // 例えば、「ユーザーを新規登録する」「ユーザー情報を更新する」などの処理です。
	@Autowired // これで、UserRepositoryのインスタンスが自動的に注入されます。
	private UserRepository userRepository;
	
	
	
	
	
	//名前でログイン機能を利用するための処理userRepositoryに定義したfindByNameを呼び出す
	public User findUserByName(String userName) {
		// ここで「ユーザー名でユーザーを検索する」という処理をします。
		return userRepository.findUserByUserName(userName);
	}
	
	//パスワード暗号化の為の道具を追加
		@Autowired
		private PasswordEncoder passwordEncoder;//これで、PasswordEncoderのインスタンスが自動的に注入されます。
		
	
	public void saveUser(User user) {
		if (user.getUserId() == null) {
			user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
		} else {
			User existing = userRepository.findById(user.getUserId()).orElse(null);
			if (existing == null) {
				throw new IllegalArgumentException("更新対象のユーザーが見つかりません");
			}
			String rawPassword = user.getUserPassword();
			if (rawPassword == null || rawPassword.isBlank()) {
				user.setUserPassword(existing.getUserPassword());
			} else {
				user.setUserPassword(passwordEncoder.encode(rawPassword));
			}
		}
		userRepository.save(user);
	}
	/**
     * 全ユーザーを取得する
     * ※将来的にはここで「is_deletedがfalseのものだけ」という絞り込みを入れます
     */
	public List<User> getAllUsers() {
		// ここで「ユーザー全員の情報をデータベースから取ってきて、画面に渡す」という処理をします。
		return userRepository.findByDeleteFlag(0);
	}
	
	public User getUserById(Integer userId) {
		User user = userRepository.findById(userId).orElse(null);
		// ユーザーが存在する　かつ　削除されていない　場合のみ返す
		// ここで「ユーザーの情報をデータベースから取ってきて、ユーザー編集の画面に渡す」という処理をします。
		if (user != null && Integer.valueOf(0).equals(user.getDeleteFlag())) {
			return user;
		}
		return null;//ユーザーが存在しない、または削除されている場合は null を返す
	}
	//論理削除
	public void deleteUser(Integer userId) {
		//対象ユーザー取得
		User user = userRepository.findById(userId).orElse(null);
		
		//isDeletedは boolean型で、trueなら削除されたとみなす
		if(user != null) {
			//論理削除のフラグを立てる
			user.setDeleteFlag(1);// 1は削除されたとみなす
			//状態を更新して保存
			userRepository.save(user);
		}
	}
	
	public List<User> findUsersByAuthority(Integer authorityId){
		// ここで「権限IDでユーザーを検索する」という処理をします。
		return userRepository.findByAuthorityIdAndDeleteFlag(authorityId, 0);// 0は削除されていないユーザーを意味します
	}
	
}
