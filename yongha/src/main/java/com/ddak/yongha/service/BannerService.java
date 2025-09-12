package com.ddak.yongha.service;

import com.ddak.yongha.mapper.BannerMapper;
import com.ddak.yongha.vo.BannerVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service("bannerService")
public class BannerService {

    @Autowired
    BannerMapper bannerMapper;

    // 배너 목록 조회 (기한을 반영한 조회)
    public List<BannerVO> getAllBanners() {
        return bannerMapper.selectActiveBanners();
    }
    
    // 배너 목록 조회 (전체)
    public List<BannerVO> getAllBanners2() {
        return bannerMapper.selectActiveBanners2();
    }
    
    // 현재 시간 기준 유효한 배너만 필터링
	public List<BannerVO> getActiveBanners() {
		List<BannerVO> allBanners = bannerMapper.selectActiveBanners();
		LocalDateTime now = LocalDateTime.now();

		return allBanners.stream()
				.filter(b -> {
				LocalDateTime start = b.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				LocalDateTime end = b.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				return !now.isBefore(start) && !now.isAfter(end);
			})
			.toList();
    }

    // 배너 저장
    public void saveBanner(BannerVO banner) {
        bannerMapper.insertBanner(banner);
    }
    
    // 배너 1개 조회
	public BannerVO getBannerByNo(int bannerNo) {
    	return bannerMapper.selectBannerByNo(bannerNo);
	}

    // 배너 삭제 (DB)
    public void deleteBannerByBannerNo(int bannerNo) {
        bannerMapper.deleteBanner(bannerNo);
    }
}
