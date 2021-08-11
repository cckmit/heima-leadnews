package com.heima.admin.service;

public interface WemediaNewsAutoScanService {
    /**
     * 自媒体文章审核，自动审核所以不需要有controller来触发
     * @param wmNewsId
     */
    public void autoScanByMediaNewsId(Integer wmNewsId);
}
