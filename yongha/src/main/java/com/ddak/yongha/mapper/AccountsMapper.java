package com.ddak.yongha.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ddak.yongha.vo.AccountsVO;

@Mapper
public interface AccountsMapper {

	void insertAccount(AccountsVO avo, int user_no);

	@Select("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM accounts WHERE account_id = #{account_id}")
	boolean hasAccountByAccountId(String account_id);

	@Select("SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM accounts WHERE user_no = #{user_no}")
	boolean hasAccountByUserNo(int user_no);

	@Update("UPDATE accounts SET balance = balance + #{amount} WHERE account_no = #{account_no}")
	void updateBalance(int account_no, int amount);

	void updateAccount(AccountsVO avo, Integer user_no);

	Long findBalanceByUserNo(Integer user_no);
}
