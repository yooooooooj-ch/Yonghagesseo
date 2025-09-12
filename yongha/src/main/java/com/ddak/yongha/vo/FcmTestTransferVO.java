package com.ddak.yongha.vo;

import lombok.Data;

/**
 * FCM 테스트 전용 VO (테이블: Transfer)
 * 컬럼: from_account_id, to_account_id, amount, trans_desc
 */
@Data
public class FcmTestTransferVO {

    private String fromAccountId; // 보내는 계좌 ID
    private String toAccountId;   // 받는 계좌 ID
    private Integer amount;           // 이체 금액
    private String transDesc;     // 이체 설명
    private String accountId;
    private Long fromAccountNo;
    private Long toAccountNo;
//
//    // Getter / Setter
//    public String getFromAccountId() {
//        return fromAccountId;
//    }
//
//    public void setFromAccountId(String fromAccountId) {
//        this.fromAccountId = fromAccountId;
//    }
//
//    public String getToAccountId() {
//        return toAccountId;
//    }
//
//    public void setToAccountId(String toAccountId) {
//        this.toAccountId = toAccountId;
//    }
//
//    public int getAmount() {
//        return amount;
//    }
//
//    public void setAmount(int amount) {
//        this.amount = amount;
//    }
//
//    public String getTransDesc() {
//        return transDesc;
//    }
//
//    public void setTransDesc(String transDesc) {
//        this.transDesc = transDesc;
//    }

    @Override
    public String toString() {
        return "FcmTestTransferVO{" +
                "fromAccountId='" + fromAccountId + '\'' +
                ", toAccountId='" + toAccountId + '\'' +
                ", amount=" + amount +
                ", transDesc='" + transDesc + '\'' +
                '}';
    }
}
