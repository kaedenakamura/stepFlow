package com.example.stepflow.UserRepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stepflow.entity.User;

@Repository // 「ここはデータベースとの窓口です」という印
public interface UserRepository extends JpaRepository<User, Integer> {
	// JpaRepositoryは「データベースに対する基本的な操作（保存、検索、削除など）を自動で作ってくれる機能」であり、findById() や findAll() などのメソッドが最初から使えるようになります。
    // 括弧の中の <User, Integer> は、
    // 「Userクラスのデータを扱い、主キー（ID）はInteger型ですよ」という意味
    // これだけで、save() や findAll() など使える
	User findUserByUserName(String userName); // 「ユーザー名でユーザーを検索する」という処理を定義、名前でログイン機能を利用するため必要な処理。IDならfindByIDで省略してもいい
	List<User> findByDeleteFlag(Integer deleteFlag); // 削除フラグ（0=有効）でユーザーを検索
	List<User> findByAuthorityIdAndDeleteFlag(Integer authorityId ,Integer deleteFlag);// 「権限IDでユーザーを検索する」という処理を定義、ユーザーの権限管理のため必要な処理
	}