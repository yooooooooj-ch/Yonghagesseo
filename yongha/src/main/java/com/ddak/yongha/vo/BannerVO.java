package com.ddak.yongha.vo;

import java.util.Date;

public class BannerVO {
    private int bannerNo;             // 배너 번호 (banner_no)
    private String imgPath;           // 이미지 경로 (img_path)
    private int bannerIndex;          // 배너 인덱스 (banner_index)
    private Date startDate;      // 시작 날짜 (start_date)
    private Date endDate;        // 종료 날짜 (end_date)

    // Getters and Setters
    public int getBannerNo() {
        return bannerNo;
    }
    public void setBannerNo(int bannerNo) {
        this.bannerNo = bannerNo;
    }
    public String getImgPath() {
        return imgPath;
    }
    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }
    public int getBannerIndex() {
        return bannerIndex;
    }
    public void setBannerIndex(int bannerIndex) {
        this.bannerIndex = bannerIndex;
    }
    public Date getStartDate() {
        return startDate;
    }
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
    public Date getEndDate() {
        return endDate;
    }
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
