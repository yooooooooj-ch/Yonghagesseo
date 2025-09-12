package com.ddak.yongha.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ddak.yongha.vo.BannerVO;

@Mapper
public interface BannerMapper {

    // 활성화된 배너 목록을 조회(기간)
    List<BannerVO> selectActiveBanners();
    
	// 활성화된 배너 목록을 조회(전체 기간)
    List<BannerVO> selectActiveBanners2();

    // 배너 추가
    void insertBanner(BannerVO banner);

    // 배너 업데이트
    void updateBanner(BannerVO banner);
    
    // 배너 1개 조회 메서드
    BannerVO selectBannerByNo(int bannerNo);

    // 배너 삭제
    void deleteBanner(int bannerNo);  // 배너 번호로 삭제
}
