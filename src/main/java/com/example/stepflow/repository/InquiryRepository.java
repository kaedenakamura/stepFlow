package com.example.stepflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stepflow.entity.Inquiry;

@Repository // これで、Springがこのクラスを「リポジトリ」として認識します。

public interface InquiryRepository extends JpaRepository<Inquiry, Integer> {

	/** delete_flag=0 を inquiry_id 降順で取得（管理者一覧） */
	List<Inquiry> findByDeleteFlagOrderByInquiryIdDesc(Integer deleteFlag);

	/** ステータス絞り込み付き一覧 */
	List<Inquiry> findByDeleteFlagAndInquiryStatusOrderByInquiryIdDesc(Integer deleteFlag, String inquiryStatus);
}
