package com.ddak.yongha.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ddak.yongha.vo.ChildInfoVO;
import com.ddak.yongha.vo.UsersVO;

@Mapper
public interface UsersMapper {

	public UsersVO getUserInfoByNo(int user_no);

	public int insertUser(UsersVO user);

	UsersVO findByUserId(String user_id);

	UsersVO findByUserNo(int user_no);

	@Select("SELECT * FROM users WHERE email = #{email}")
	UsersVO findByUserEmail(String email);

	@Select("SELECT user_type FROM users WHERE user_no = #{user_no}")
	public int getUserType(int user_no);

	public List<UsersVO> getMyChildsInfo(int user_no);

	public List<UsersVO> getMyParentsInfo(Integer user_no);
	
	public List<UsersVO> getMySiblingsInfo(Integer user_no);

	public ChildInfoVO getChildInfo(int child_no);

	@Update("UPDATE users SET profile = #{profile} WHERE user_no = #{user_no}")
	void updateProfile(@Param("user_no") int user_no, @Param("profile") int profile);

	@Insert("INSERT INTO Family (parent_no, child_no) VALUES (#{parent_no}, #{child_no})")
	public void insertFamily(@Param("parent_no") int parent_no, @Param("child_no") int child_no);

	int isFamilyExist(@Param("parent_no") int parent_no, @Param("child_no") int child_no);

	@Delete("DELETE FROM Family WHERE parent_no = #{parent_no} AND child_no = #{child_no}")
	public void deleteFamily(@Param("parent_no") int parent_no, @Param("child_no") int child_no);

	@Update("UPDATE users SET password = #{new_pw} WHERE user_no = #{user_no}")
	int changePassword(@Param("user_no") int userNo, @Param("new_pw") String newPw);

	@Select("SELECT * FROM users WHERE user_id = #{user_id} AND LOWER(email) = #{email}")
	int existsByIdAndEmail(@Param("user_id") String userId, @Param("email") String lowerCase);

	int updateUser(UsersVO user);

	@Select("SELECT * FROM users WHERE user_no = (SELECT DISTINCT user_no FROM Accounts WHERE account_no = #{accountNo})")
	UsersVO findByAccountNo(int accountNo);

	// 관리자 페이지 조회용

	public void deleteUsersByNos(@Param("user_nos") List<Integer> userNo);

	List<UsersVO> getUserPage(Map<String, Object> params);

	public int countUsers(Map<String, Object> params);

}