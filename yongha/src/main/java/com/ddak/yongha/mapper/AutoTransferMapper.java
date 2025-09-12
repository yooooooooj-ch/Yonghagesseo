package com.ddak.yongha.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.ddak.yongha.vo.AutoTransferVO;

@Mapper
public interface AutoTransferMapper {

        public void insertAutoTransfer(AutoTransferVO atvo);

        public List<AutoTransferVO> selectDueTransfers();

        public List<AutoTransferVO> selectDueTransfersTest();

        public void updateLastTransferDate(int from_account_no, int to_account_no);

        public AutoTransferVO selectAutoTransfer(AutoTransferVO atvo);

        public void deleteAutoTransfer(AutoTransferVO atvo);

        public void updateAutoTransfer(AutoTransferVO atvo);

}
