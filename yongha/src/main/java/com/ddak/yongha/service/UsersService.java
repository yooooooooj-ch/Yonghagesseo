package com.ddak.yongha.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ddak.yongha.mapper.GoalHistoryMapper;
import com.ddak.yongha.mapper.UsersMapper;
import com.ddak.yongha.security.JwtUtil;
import com.ddak.yongha.vo.ChildInfoVO;
import com.ddak.yongha.vo.GoalHistoryVO;
import com.ddak.yongha.vo.UsersVO;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsersService {

	private final UsersMapper usersMapper;

	private final GoalHistoryMapper goalMapper;

	private final PasswordEncoder passwordEncoder;
	
	private final JwtUtil jwtUtil;

	public UsersVO getUserInfoByNo(int user_no) {
		return usersMapper.getUserInfoByNo(user_no);
	}

	public boolean isChild(int user_no) {
		return usersMapper.getUserType(user_no) == 1;
	}

	public int getUserType(int user_no) {
		return usersMapper.getUserType(user_no);
	}

	public void updateProfile(int user_no, int profile) {
		System.out.println("Mapper 호출 값: user_no=" + user_no + ", profile=" + profile);
		usersMapper.updateProfile(user_no, profile);

	}

	public List<UsersVO> getMyChildsInfo(Integer user_no) {
		return usersMapper.getMyChildsInfo(user_no);
	}

	public List<UsersVO> getMyParentsInfo(Integer user_no) {
		return usersMapper.getMyParentsInfo(user_no);
	}
	
	public List<UsersVO> getMySiblingsInfo(Integer user_no) {
		return usersMapper.getMySiblingsInfo(user_no);
	}

	public ChildInfoVO getChildInfo(Integer child_no) {
		return usersMapper.getChildInfo(child_no);
	}

	public UsersVO authenticate(String user_id, String rawPassword) {
		UsersVO user = usersMapper.findByUserId(user_id);
		if (user == null)
			return null;

		String stored = user.getPassword();

		// BCrypt 암호화된 비밀번호를 사용된 경우 ("$2"로 시작하는 문자열)
		if (stored != null && stored.startsWith("$2")) {
			return passwordEncoder.matches(rawPassword, stored) ? user : null;
		}

		// 혹시 과거 평문이 DB에 남아있을 수 있는 경우(마이그레이션 단계 지원)
		// TODO : 나중에 삭제
		if (stored != null && stored.equals(rawPassword)) {
			return user;
		}

		return null;
	}

	// 토큰 정보를 UserVO로 변환 저장
	public UsersVO getUserFromToken(String token) {
		Claims claims = jwtUtil.getClaims(token);
		int userNo = Integer.valueOf(claims.getSubject());
		return usersMapper.findByUserNo(userNo);
	}

	public UsersVO findByUserEmail(String email) {
		return usersMapper.findByUserEmail(email);
	}

	public UsersVO findByUserNo(int userNo) {
		return usersMapper.findByUserNo(userNo);
	}

	// 가족 테이블에 관계추가
	public void insertFamily(int parent_no, int child_no) {
		usersMapper.insertFamily(parent_no, child_no);

	}

	public boolean isFamilyAlreadyRegistered(int parent_no, int child_no) {
		return usersMapper.isFamilyExist(parent_no, child_no) > 0;
	}

	public void deleteFamily(int parent_no, int child_no) {
		usersMapper.deleteFamily(parent_no, child_no);

	}

	// 목표 달성 랭크 출력
	public List<GoalHistoryVO> selectTopRankedUsers() {
		return goalMapper.selectTopRankedUsers();
	}
	
	public UsersVO findByAccountNo(int accountNo) {
		return usersMapper.findByAccountNo(accountNo);
	}

	@Transactional
	public void deleteUsers(List<Integer> userNo) {
		usersMapper.deleteUsersByNos(userNo);
	}

}
