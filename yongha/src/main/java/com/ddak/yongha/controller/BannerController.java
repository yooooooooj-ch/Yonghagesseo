package com.ddak.yongha.controller;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.ddak.yongha.service.BannerService;
import com.ddak.yongha.vo.BannerVO;

@Controller
public class BannerController {

	@Autowired
	BannerService bannerService;

	// 배너 출력 페이지
	// @GetMapping("/TestMainK")
	// public String showMainPage(Model model) {
	// List<BannerVO> allBanners = bannerService.getAllBanners();
	// LocalDateTime now = LocalDateTime.now();
	//
	// // 유효한 배너만 필터링
	// List<BannerVO> validBanners = allBanners.stream()
	// .filter(b -> {
	// LocalDateTime start =
	// b.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	// LocalDateTime end =
	// b.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	// return !now.isBefore(start) && !now.isAfter(end);
	// })
	// .collect(Collectors.toList());
	//
	// model.addAttribute("bannerList", validBanners);
	// return "TestMainK";
	// }

	// @GetMapping("/")
	// public String showMainPage(Model model) {
	// List<BannerVO> allBanners = bannerService.getAllBanners();
	// LocalDateTime now = LocalDateTime.now();
	//
	// // 유효한 배너만 필터링
	// List<BannerVO> validBanners = allBanners.stream()
	// .filter(b -> {
	// LocalDateTime start =
	// b.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	// LocalDateTime end =
	// b.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	// return !now.isBefore(start) && !now.isAfter(end);
	// })
	// .collect(Collectors.toList());
	//
	// model.addAttribute("bannerList", validBanners);
	// return "mainpage";
	// }

	// 배너 업로드 폼 페이지
	@GetMapping("/BanUpload")
	public String showBannerUploadPage(Model model) {
		List<BannerVO> banners = bannerService.getAllBanners2();
		Date now = new Date();

		Map<Integer, String> bannerStatusMap = new HashMap<>();

		for (BannerVO b : banners) {
			if (now.before(b.getStartDate())) {
				bannerStatusMap.put(b.getBannerNo(), "게시 전");
			} else if (now.after(b.getEndDate())) {
				bannerStatusMap.put(b.getBannerNo(), "만료");
			} else {
				bannerStatusMap.put(b.getBannerNo(), "게시 중");
			}
		}

		model.addAttribute("banners", banners);
		model.addAttribute("bannerStatusMap", bannerStatusMap);

		return "BanUpload";
	}

	// 배너 저장
	@PostMapping("/saveBanner")
	// @ResponseBody
	public String saveBanner(@RequestParam("image") MultipartFile file, @RequestParam("startDate") String startDateStr,
			@RequestParam("endDate") String endDateStr, @RequestParam("bannerIndex") int bannerIndex) {
		try {
			// 날짜 파싱
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
			LocalDateTime startLdt = LocalDateTime.parse(startDateStr, formatter);
			LocalDateTime endLdt = LocalDateTime.parse(endDateStr, formatter);

			Date startDate = Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant());
			Date endDate = Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant());

			// 저장 경로
			String uploadDir = "C:/upload/event_img/";
			File dir = new File(uploadDir);
			if (!dir.exists())
				dir.mkdirs();

			// 고유한 파일명 생성
			String originalFilename = file.getOriginalFilename();
			String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

			// 실제 저장
			File dest = new File(uploadDir + uniqueFilename);
			file.transferTo(dest);

			// DB 저장용 VO 생성
			BannerVO banner = new BannerVO();
			banner.setImgPath("/upload/event_img/" + uniqueFilename); // 상대경로 저장
			banner.setStartDate(startDate);
			banner.setEndDate(endDate);
			banner.setBannerIndex(bannerIndex); // 기본값, 추후 수정 가능

			bannerService.saveBanner(banner);

			// return "redirect:/admin/banners";
		} catch (Exception e) {
			e.printStackTrace();
			// return "업로드 실패: " + e.getMessage();
		}

		return "redirect:/admin/banners";
	}

	// 배너 삭제
	@PostMapping("/deleteBanner")
	@ResponseBody
	public Map<String, Object> deleteBanner(@RequestParam("bannerNo") int bannerNo) {
	    Map<String, Object> result = new HashMap<>();
	    try {
	        BannerVO banner = bannerService.getBannerByNo(bannerNo);
	        if (banner != null) {
	            String imgPath = banner.getImgPath();
	            String baseDir = "C:/upload/event_img/";
	            String filename = imgPath.replace("/upload/event_img/", "");
	            File imgFile = new File(baseDir + filename);

	            if (imgFile.exists() && !imgFile.delete()) {
	                System.out.println("이미지 파일 삭제 실패: " + imgFile.getAbsolutePath());
	            }
	        }
	        bannerService.deleteBannerByBannerNo(bannerNo);

	        result.put("success", true);
	    } catch (Exception e) {
	        e.printStackTrace();
	        result.put("success", false);
	        result.put("message", e.getMessage());
	    }
	    return result;
	}

}