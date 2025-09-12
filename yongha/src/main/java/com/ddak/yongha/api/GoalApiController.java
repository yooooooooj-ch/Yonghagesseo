package com.ddak.yongha.api;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ddak.yongha.mapper.GoalHistoryMapper;
import com.ddak.yongha.service.FcmService;
import com.ddak.yongha.service.UsersService;
import com.ddak.yongha.vo.GoalHistoryEntityVO;
import com.ddak.yongha.vo.UsersVO;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/api/goals")
@RestController
@RequiredArgsConstructor
public class GoalApiController {

	private final GoalHistoryMapper goalHistoryMapper;
	private final UsersService usersService;
	private final FcmService fcmService;

	/*
	 * 새 목표 등록
	 */
	@PostMapping
    public ResponseEntity<String> createGoal(@RequestBody GoalHistoryEntityVO goal, HttpSession session) {
        Integer childNo = (Integer) session.getAttribute("user_no");
        if (childNo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        // 진행 중 목표 존재 시 등록 불가 (2차 방지)
        int activeCnt = goalHistoryMapper.countActiveByChildNo(childNo);
        if (activeCnt > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("이미 진행 중인 목표가 있습니다.");
        }

        // 1) 저장
        goal.setChild_no(childNo);
        LocalDate startDate = LocalDate.now();
		goal.setStart_date(startDate);
		goal.setEnd_date(startDate.plusDays(30));
        // useGeneratedKeys 설정 시 goal.goal_no 세팅됨
        int inserted = goalHistoryMapper.insertGoalHistory(goal);
        if (inserted != 1) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("목표 저장 실패");
        }

        // 2) (비핵심) 알림은 서비스에 위임
        List<UsersVO> parents = usersService.getMyParentsInfo(childNo);
        if (parents != null && !parents.isEmpty()) {
        	try {
                UsersVO child = usersService.findByUserNo(childNo);
                String title = "자녀가 새로운 목표를 등록했어요 🚀"; 
                String body  = String.format("[%s] %s (목표금액 %,d원)",
                        child != null ? child.getUser_name() : "자녀",
                        goal.getGoal_name(),
                        goal.getTarget_amount());
                
				// 부모별로 호출 
				for (UsersVO p : parents) {
//					log.info("FCM (goal_created) -> parentNo={}", p.getUser_no());
					try {
						fcmService.sendNotification(p.getFcm_token(), title, body, null);
					} catch (Exception e) {
//						log.warn("부모 FCM 전송 실패 parentNo={}, childNo={}", p.getUser_no(), childNo, e);
					}
				}
                
            } catch (Exception e) {
                // 저장은 성공했으니 알림 실패만 로깅
//                log.warn("새 목표 알림 전송 실패 childNo={}, goalNo={}", childNo, goal.getGoal_no(), e);
            }
        }

        return ResponseEntity.ok("OK");
    }

	/*
	 * 내 목표 히스토리 조회 - child
	 */
	//
	@GetMapping
	public ResponseEntity<?> listMyGoals(HttpSession session) {
		Integer childNo = (Integer) session.getAttribute("user_no");
		if (childNo == null)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

		return ResponseEntity.ok(goalHistoryMapper.findByChildNo(childNo));
	}

	// 진행 중(achieved = 0) 목표 1건 조회 - child
	@GetMapping("/active")
	public ResponseEntity<?> getMyActiveGoal(HttpSession session) {
		Integer childNo = (Integer) session.getAttribute("user_no");
		if (childNo == null)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

		GoalHistoryEntityVO active = goalHistoryMapper.findActiveByChildNo(childNo);
		if (active == null)
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("진행 중 목표가 없습니다.");
		return ResponseEntity.ok(active);
	}
	
	/*
	 * 내 자녀들의 목표 히스토리 조회 - parent
	 */
	@GetMapping("/parent/goals")
	public ResponseEntity<?> getMyChildrenGoals(HttpSession session) {
	    Integer parentNo = (Integer) session.getAttribute("user_no");
	    if (parentNo == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

	    List<UsersVO> children = usersService.getMyChildsInfo(parentNo);
	    List<ChildGoalsDTO> result = new ArrayList<>();

	    if (children != null) {
	        for (UsersVO child : children) {
	            Integer childNo = child.getUser_no();
	            if (childNo == null || childNo <= 0) continue;
	            List<GoalHistoryEntityVO> goals = goalHistoryMapper.findByChildNo(childNo);
	            result.add(ChildGoalsDTO.of(child, goals));
	        }
	    }
	    return ResponseEntity.ok(result);
	}
	

	/*
	 * 목표 수정
	 */
	@PatchMapping("/{goalNo}")
	public ResponseEntity<?> updateMyGoal(@PathVariable int goalNo, @RequestBody GoalHistoryEntityVO patch,
			HttpSession session) {
		Integer childNo = (Integer) session.getAttribute("user_no");
		if (childNo == null)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

		int updated = goalHistoryMapper.updateGoal(goalNo, patch);
		if (updated == 0)
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("수정할 목표가 없습니다.");
		return ResponseEntity.ok("수정 완료");
	}

	/*
	 * "완료" 버튼으로 목표 수동 달성 처리
	 */
	@PostMapping("/{goalNo}/complete")
	public ResponseEntity<?> completeMyGoal(@PathVariable int goalNo, HttpSession session) {
		Integer childNo = (Integer) session.getAttribute("user_no");
		if (childNo == null)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
		
		int done = goalHistoryMapper.completeGoalByChild(goalNo, childNo);
		
		if (done == 0)
			return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 달성이거나 권한이 없습니다.");
		
		return ResponseEntity.ok("달성 처리 완료");
	}
	

	/*
	 * 목표 삭제
	 */
	@DeleteMapping("/{goalNo}")
	public ResponseEntity<?> deleteMyGoal(@PathVariable int goalNo, HttpSession session) {
		Integer childNo = (Integer) session.getAttribute("user_no");
		if (childNo == null)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

		int deleted = goalHistoryMapper.deleteGoal(goalNo);
		if (deleted == 0)
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("삭제할 목표가 없습니다.");
		return ResponseEntity.ok("삭제 완료");
	}
	
	/*
	 * ChildGoalsDTO - 자녀 목표 모니터링 페이지('/goals/parent')에서 `자녀이름(child_name)-목표데이터(List<GoalHistoryEntityVO>)` 묶어서 출력하기 위한 DTO
	*/
	@Data @NoArgsConstructor @AllArgsConstructor
	static class ChildGoalsDTO {
	    private Integer child_no;
	    private String  child_id;
	    private String  child_name;
	    private List<GoalHistoryEntityVO> goals;

	    static ChildGoalsDTO of(UsersVO c, List<GoalHistoryEntityVO> rows) {
	        return new ChildGoalsDTO(
	            c.getUser_no(),
	            c.getUser_id(),
	            c.getUser_name(),
	            rows != null ? rows : List.of()
	        );
	    }
	}

}
