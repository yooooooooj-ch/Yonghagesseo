package com.ddak.yongha.controller;

import java.util.List;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ddak.yongha.service.ParentMailService;
import com.ddak.yongha.vo.UsersVO;

@Controller
public class ParentMailController {

    @Autowired
    private ParentMailService parentMailService;

    // 페이지 로딩 (자녀 목록 + 현재 메일 주기)
    @GetMapping("/parent/mail-settings")
    public String parentMailPage(HttpSession session, Model model) {
        Integer parentNo = (Integer) session.getAttribute("user_no");
        if (parentNo == null) return "redirect:/login";

        List<UsersVO> children = parentMailService.getChildrenByParent(parentNo);
        Integer mailCycle = parentMailService.getMailCycle(parentNo);

        model.addAttribute("children", children); // 그래프용
        model.addAttribute("mailCycle", mailCycle);
        return "parent/mail-settings";
    }

    // 설정 저장
    @PostMapping("/parent/mail-settings")
    public String saveMailSetting(HttpSession session,
                                  @RequestParam("mailCycle") int mailCycle,
                                  Model model) {

        Integer parentNo = (Integer) session.getAttribute("user_no");
        if (parentNo == null) return "redirect:/login";

        parentMailService.saveMailCycle(parentNo, mailCycle);

        model.addAttribute("msg", "설정이 저장되었습니다.");
        return "redirect:/parent/mail-settings";
    }
}
