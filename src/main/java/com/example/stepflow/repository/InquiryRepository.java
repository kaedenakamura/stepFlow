package com.example.stepflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.stepflow.entity.Inquiry;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Integer> {

    /** delete_flag=0 を inquiry_id 降順で取得 */
    List<Inquiry> findByDeleteFlagOrderByInquiryIdDesc(Integer deleteFlag);

    /** ステータス絞り込み付き一覧 */
    List<Inquiry> findByDeleteFlagAndInquiryStatusOrderByInquiryIdDesc(
            Integer deleteFlag, String inquiryStatus);

    /** 自分が送信した問い合わせ */
    List<Inquiry> findByUserIdAndDeleteFlagOrderByInquiryIdDesc(
            Integer userId, Integer deleteFlag);

    /** 自店舗宛の問い合わせ */
    List<Inquiry> findByDeleteFlagAndShopIdOrderByInquiryIdDesc(
            Integer deleteFlag, Integer shopId);
    /** 店舗宛 ＋ ステータス で取得 */
    List<Inquiry> findByDeleteFlagAndAuthorityIdAndInquiryStatusAndShopIdOrderByInquiryIdDesc(
        Integer deleteFlag, Integer authorityId, String inquiryStatus, Integer shopId);

    /** 自倉庫宛の問い合わせ */
    List<Inquiry> findByDeleteFlagAndWarehouseIdOrderByInquiryIdDesc(
            Integer deleteFlag, Integer warehouseId);
    /** 倉庫宛 ＋ ステータス で取得 */
    List<Inquiry> findByDeleteFlagAndAuthorityIdAndInquiryStatusAndWarehouseIdOrderByInquiryIdDesc(
            Integer deleteFlag, Integer authorityId, String inquiryStatus, Integer warehouseId);

    /** 管理者宛（authority_id=1, shop/warehouse なし） */
    List<Inquiry> findByDeleteFlagAndAuthorityIdOrderByInquiryIdDesc(
            Integer deleteFlag, Integer authorityId);

    List<Inquiry> findByDeleteFlagAndAuthorityIdAndInquiryStatusOrderByInquiryIdDesc(
            Integer deleteFlag, Integer authorityId, String inquiryStatus);

    /** 店舗宛 */
    List<Inquiry> findByDeleteFlagAndAuthorityIdAndShopIdOrderByInquiryIdDesc(
            Integer deleteFlag, Integer authorityId, Integer shopId);

    /** 倉庫宛 */
    List<Inquiry> findByDeleteFlagAndAuthorityIdAndWarehouseIdOrderByInquiryIdDesc(
            Integer deleteFlag, Integer authorityId, Integer warehouseId);
}
