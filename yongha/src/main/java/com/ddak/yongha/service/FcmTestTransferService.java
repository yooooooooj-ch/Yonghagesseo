package com.ddak.yongha.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ddak.yongha.mapper.FcmTestAccountsMapper2;
import com.ddak.yongha.mapper.FcmTestUserMapper;
import com.ddak.yongha.vo.FcmRequestVo;
import com.ddak.yongha.vo.FcmTestTransferVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTestTransferService {

    private final FcmTestUserMapper userMapper;
    private final FcmTestAccountsMapper2 accountsMapper2;
    private final FcmService fcmService;

    /**
     * 💸 용돈 충전 처리 + 잔액 업데이트(부모 차감/자녀 증가)
     * ✅ 푸시 알림은 커밋 이후에 발송 (롤백 시 발송 방지)
     */
    @Transactional
    public boolean chargeAllowance(FcmTestTransferVO vo) {
        // 0) 기본 검증
        Objects.requireNonNull(vo, "요청 VO가 null");
        if (vo.getAmount() == null || vo.getAmount() <= 0) {
            throw new IllegalArgumentException("이체 금액이 0 이하");
        }
        if (isBlank(vo.getFromAccountId()) || isBlank(vo.getToAccountId())) {
            throw new IllegalArgumentException("계좌 ID 누락");
        }

        // 1) 부모 잔액 검증 (부족하면 예외)
        Long parentBalance = accountsMapper2.selectBalanceByAccountId(vo.getFromAccountId());
        if (parentBalance == null) parentBalance = 0L;
        if (parentBalance < vo.getAmount()) {
            throw new IllegalStateException("부모 잔액 부족");
        }

        // 2) ✅ FK 충족을 위한 계좌번호 조회 (ID → NO)
        Long fromNo = accountsMapper2.selectAccountNoByAccountId(vo.getFromAccountId());
        Long toNo   = accountsMapper2.selectAccountNoByAccountId(vo.getToAccountId());
        if (fromNo == null || toNo == null) {
            throw new IllegalStateException("계좌번호 없음");
        }
        vo.setFromAccountNo(fromNo);
        vo.setToAccountNo(toNo);

        // 3) 이체 내역 INSERT (NO 컬럼으로 저장)
        int inserted = accountsMapper2.insertTransfer(vo);
        if (inserted <= 0) {
            throw new IllegalStateException("이체 내역 저장 실패");
        }

        // 4) 부모 차감 / 자녀 증가 (ID 기준 기존 쿼리 재사용)
        int dec = accountsMapper2.updateParentBalance(vo); // balance = balance - amount
        int inc = accountsMapper2.updateChildBalance(vo);  // balance = balance + amount
        if (dec != 1 || inc != 1) {
            throw new IllegalStateException("잔액 갱신 실패(dec=" + dec + ", inc=" + inc + ")");
        }

        // 5) 토큰 조회 (자녀/부모)
        String childAccountId = vo.getToAccountId();
        String parentToken = userMapper.getParentFcmTokenByChildAccountId(childAccountId);
        String childToken  = userMapper.getChildFcmTokenByChildAccountId(childAccountId);

        // 6) 커밋 후 푸시 발송 예약
        List<FcmRequestVo> toSend = new ArrayList<>();

        if (!isBlank(parentToken)) {
            FcmRequestVo req = new FcmRequestVo();
            req.setToken(parentToken);
            req.setNotification(new FcmRequestVo.Notification(
                    "💰 용돈 충전 완료!",
                    "자녀에게 " + vo.getAmount() + "원이 충전되었습니다.",
                    null
            ));
            toSend.add(req);
        }

        if (!isBlank(childToken)) {
            FcmRequestVo req = new FcmRequestVo();
            req.setToken(childToken);
            req.setNotification(new FcmRequestVo.Notification(
                    "🐷 용돈이 도착했어요!",
                    "부모님이 " + vo.getAmount() + "원을 보내주셨어요 🎉",
                    null
            ));
            toSend.add(req);
        }

        if (!toSend.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (FcmRequestVo req : toSend) {
                        try {
                            fcmService.sendPushNotification(req);
                        } catch (Exception e) {
                            log.error("FCM 전송 실패 token={}", req.getToken(), e);
                        }
                    }
                }
            });
        }

        return true;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
