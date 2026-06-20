package com.example.stepflow.service;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.stepflow.entity.Inquiry;
import com.example.stepflow.entity.User;
import com.example.stepflow.repository.InquiryRepository;

@Service
public class InquiryService {

    @Autowired
    private InquiryRepository inquiryRepository;

    /**
     * ログインユーザーから見える問い合わせ一覧（宛先が自組織のみ）
     */
    @Transactional(readOnly = true)
    public List<Inquiry> findVisibleInquiries(User loginUser, String authorityId, String statusFilter) {
        if (loginUser == null) {
            return List.of();
        }

        boolean filterStatus = statusFilter != null && !statusFilter.isBlank();
        String status = filterStatus ? statusFilter.strip() : null;

        if ("ROLE_ADMIN".equals(authorityId)) {
            if (filterStatus) {
                return inquiryRepository.findByDeleteFlagAndAuthorityIdAndInquiryStatusOrderByInquiryIdDesc(
                        0, 1, status);
            }
            return inquiryRepository.findByDeleteFlagAndAuthorityIdOrderByInquiryIdDesc(0, 1);
        }
        if ("ROLE_SHOP".equals(authorityId) && loginUser.getShopId() != null) {
            if (filterStatus) {
                return inquiryRepository.findByDeleteFlagAndAuthorityIdAndInquiryStatusAndShopIdOrderByInquiryIdDesc(
                        0, 2, status, loginUser.getShopId());
            }
            return inquiryRepository.findByDeleteFlagAndAuthorityIdAndShopIdOrderByInquiryIdDesc(
                    0, 2, loginUser.getShopId());
        }
        if ("ROLE_WAREHOUSE".equals(authorityId) && loginUser.getWarehouseId() != null) {
            if (filterStatus) {
                return inquiryRepository.findByDeleteFlagAndAuthorityIdAndInquiryStatusAndWarehouseIdOrderByInquiryIdDesc(
                        0, 3, status, loginUser.getWarehouseId());
            }
            return inquiryRepository.findByDeleteFlagAndAuthorityIdAndWarehouseIdOrderByInquiryIdDesc(
                    0, 3, loginUser.getWarehouseId());
        }
        return List.of();
    }

    /** 一覧に出る行のみ詳細表示可（有効かつ宛先が自組織） */
    @Transactional(readOnly = true)
    public Inquiry findVisibleInquiry(Integer inquiryId, User loginUser, String authorityId) {
        if (loginUser == null || inquiryId == null) {
            return null;
        }
        return inquiryRepository.findById(inquiryId)
                .filter(i -> Integer.valueOf(0).equals(i.getDeleteFlag()))
                .filter(i -> isRecipient(i, loginUser, authorityId))
                .orElse(null);
    }

    /** 問い合わせの宛先がログインユーザーの組織か */
    public boolean isRecipient(Inquiry inquiry, User loginUser, String authorityId) {
        if (inquiry == null || loginUser == null) {
            return false;
        }
        if ("ROLE_ADMIN".equals(authorityId)) {
            return Integer.valueOf(1).equals(inquiry.getAuthorityId())
                    && inquiry.getShopId() == null
                    && inquiry.getWarehouseId() == null;
        }
        if ("ROLE_SHOP".equals(authorityId)) {
            return Integer.valueOf(2).equals(inquiry.getAuthorityId())
                    && Objects.equals(loginUser.getShopId(), inquiry.getShopId());
        }
        if ("ROLE_WAREHOUSE".equals(authorityId)) {
            return Integer.valueOf(3).equals(inquiry.getAuthorityId())
                    && Objects.equals(loginUser.getWarehouseId(), inquiry.getWarehouseId());
        }
        return false;
    }
}
