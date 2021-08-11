package com.heima.admin.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.heima.admin.mapper.AdSensitiveMapper;
import com.heima.admin.mapper.ChannelMapper;
import com.heima.admin.service.WemediaNewsAutoScanService;
import com.heima.aliyun.GreenImageScan;
import com.heima.aliyun.GreenTextScan;
import com.heima.common.constants.wemedia.WemediaConstants;
import com.heima.common.exception.CustException;
import com.heima.feigns.ArticleFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.utils.common.SensitiveWordUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 自动审核实现类
 */
@Service
@Slf4j
public class WemediaNewsAutoScanServiceImpl implements WemediaNewsAutoScanService {

    @Autowired
    WemediaFeign wemediaFeign;

    @Autowired
    AdSensitiveMapper adSensitiveMapper;

    @Value("${file.oss.web-site}")
    String webSite;


    @Autowired
    ChannelMapper channelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoScanByMediaNewsId(Integer wmNewsId) {
        //TODO 1.msg：wmNewsId从Kafka发出（wemedia端发送）

        // 2.远程调用feign查询文章数据：wmNews wmUser
        //wmNews查出来
        if (wmNewsId == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "Kafka发送的文章id为空");
        }
        ResponseResult<WmNews> wmNewsResult = wemediaFeign.findById(wmNewsId);
        System.out.println("wnNews数据是======" + wmNewsResult.getData());
        System.out.println("结果码是======" + wmNewsResult.getCode().intValue());
        if (wmNewsResult.getCode().intValue() != 0) {
            log.error("自动审核文章失败    远程查询文章信息失败, 原因:{}", wmNewsResult.getErrorMessage());
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        WmNews wmNews = wmNewsResult.getData();
        if (wmNews == null) {
            log.error("自动审核文章失败    未查询自媒体文章信息  wmNewsId:{}", wmNewsId);
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 3.如果wmNews。status为4（人工审核通过）/8（自动审核通过），并且发布时间小于等于当前时间
        // 发布文章 还是一样要更新关系表信息
        if ((wmNews.getStatus().shortValue() == WmNews.Status.ADMIN_SUCCESS.getCode() || wmNews.getStatus().shortValue() == WmNews.Status.SUCCESS.getCode()) && (wmNews.getPublishTime().getTime() <= System.currentTimeMillis())) {
            publishArticle(wmNews);
        }
        // 4.如果wmNews。status为1 进行审核
        if (wmNews.getStatus().shortValue() == WmNews.Status.SUBMIT.getCode()) {
// 首先抽取文章内容中的文本和图片url集合 Map<String,Object> content 内容 images List<String>
            Map<String, Object> contentAndImageResult = handleTextAndImage(wmNews);
//     4.1 DFA敏感词检测 不通过设置status为2 不确定status为3
            boolean isSensitive = handleSensitive((String) contentAndImageResult.get("content"), wmNews);
            if (!isSensitive) {
                return;
            }
//     4.2 阿里云图片和文本检测 不通过设置status为2 不确定status为3
            boolean isTextScan = handleTextScan((String) contentAndImageResult.get("content"), wmNews);
            if (!isTextScan) {
                return;
            }
            Object images = contentAndImageResult.get("images");

            boolean isImageScan = handleImageScan((List<String>) images, wmNews);
            if (!isImageScan) {
                return;
            }
//    4.3 都通过保存app端文章信息 保存更新wmNews apArticle apAuthor apArticleConfig apArticeContent
            //
            if (wmNews.getPublishTime().after(new Date())) {
                updateWmNews(wmNews, WmNews.Status.SUCCESS.getCode(), "审核成功！");
                return;
            }
            publishArticle(wmNews);
        }


        //TODO 5.ES索引库添加文章信息
    }

    /**
     * @param wmNews 更新的文章对象
     * @param status 枚举来设置更新的文章status
     */
    private void updateWmNews(WmNews wmNews, short status, String reason) {
        // 保存更新 wmNews
        wmNews.setStatus(status);
        wmNews.setReason(reason);
        ResponseResult responseResult = wemediaFeign.updateWmNews(wmNews);
        if (responseResult.getCode().intValue()!=0) {
            log.error(" 远程 调用修改自媒体文章接口 出错，  原因:{}     参数:{}",responseResult.getErrorMessage(),wmNews);
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
    }

    @Autowired
    GreenImageScan greenImageScan;

    private boolean handleImageScan(List<String> images, WmNews wmNews) {
        //    4.2 阿里云图片检测 不通过设置status为2 不确定status为3
        boolean flag = true;
        try {
            Map map = greenImageScan.imageUrlScan(images);
            if (map.size()==0) {
                return flag=true;
            }
            String suggestion = (String) map.get("suggestion");
            switch (suggestion) {
                case "block":
                    updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "文章中包含敏感图片，审核失败");
                    flag = false;
                    break;
                case "review":
                    updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "文章疑似包含禁止信息，待人工审核");
                    flag = false;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("阿里云文本审核出现异常，原因：{}", e.getMessage());
            updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "AI审核调用失败，转为人工审核");
        }
        return flag;
    }

    @Autowired
    GreenTextScan greenTextScan;

    private boolean handleTextScan(String content, WmNews wmNews) {
        //    4.2 阿里云文本检测 不通过设置status为2 不确定status为3
        boolean flag = true;
        try {
            Map map = greenTextScan.greenTextScan(content);
            String suggestion = (String) map.get("suggestion");
            switch (suggestion) {
                case "block":
                    updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "文章中包含敏感词，审核失败");
                    flag = false;
                    break;
                case "review":
                    updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "文章疑似包含禁止信息，待人工审核");
                    flag = false;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.info("阿里云文本审核出现异常，原因：{}", e.getMessage());
            updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "AI审核调用失败，转为人工审核");
            flag = false;
        }
        return flag;
    }

    private boolean handleSensitive(String content, WmNews wmNews) {
        //   4.1 DFA敏感词检测 不通过设置status为2 不确定status为3
        boolean flag = true;
        //先将敏感词都查出来
        List<String> allSensitive = adSensitiveMapper.findAllSensitive();
        SensitiveWordUtil.initMap(allSensitive);
        Map<String, Integer> resultMap = SensitiveWordUtil.matchWords(content);
        if (resultMap != null && resultMap.size() > 0) {
            log.info("文章中拥有敏感词 ==》 {}", resultMap);
            updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "文章中存在敏感词汇：" + resultMap);
            flag = false;
        }
        return flag;
    }

    private Map<String, Object> handleTextAndImage(WmNews wmNews) {
        // 抽取wnNews中的文本及图片成map集合，方便审核检测
        String contentJson = wmNews.getContent();
        if (contentJson == null) {
            log.info("自动审核失败，文章内容为空");
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //Json转map
        List<Map> contentMaps = JSONArray.parseArray(contentJson, Map.class);
        //  抽取文章中的文本信息
        String content = contentMaps.stream()
                .filter(map -> map.get("type").equals("text"))
                .map(map -> (String) map.get("value"))
                .collect(Collectors.joining("_hmtt_"));
        content = content + "_hmtt_" + wmNews.getTitle();
        //抽取文章中的所有图片
        List<String> imageList = contentMaps.stream()
                .filter(map -> map.get("type").equals("image"))
                .map(map -> (String) map.get("value"))
                .collect(Collectors.toList());
        //抽取封面中的所有图片
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            //按照逗号把images的集合切割，stream成数组，在前面加上website前缀，加入imageList供审核
            List<String> urls = Arrays.stream(wmNews.getImages().split(","))
                    .map(url -> webSite + url)
                    .collect(Collectors.toList());
            imageList.addAll(urls);
        }
        //图片去重
        imageList = imageList.stream().distinct().collect(Collectors.toList());
        HashMap<String, Object> map = new HashMap<>();
        map.put("content", content);
        map.put("images", imageList);
        return map;
    }

    @Autowired
    ArticleFeign articleFeign;

    private void publishArticle(WmNews wmNews) {
        // 保存更新 wmNews apArticle  apArticleConfig apArticleContent的数据
        // 把文章保存到app端 把wmNews的信息拷贝到apArticle里
        // apArticle中apAuthor的id&name apAuthor可以通过wmUser查到 wmUser可以通过wmNews查到
        ArticleDto articleDto = new ArticleDto();
        BeanUtils.copyProperties(wmNews, articleDto);
        articleDto.setId(wmNews.getArticleId());
        articleDto.setFlag((byte) 0); // 普通文章
        articleDto.setLayout(wmNews.getType());// 布局
        articleDto.setWmUserId(wmNews.getUserId());// 用于获取作者信息
        // apArticle中的channel的name通过channelmapper查到 channelId通过wmNews获取
        AdChannel adChannel = channelMapper.selectById(wmNews.getChannelId());
        articleDto.setChannelName(adChannel.getName());
        //远程调用保存文章并获取返回的文章id
        ResponseResult<Long> saveArticleResult = articleFeign.saveArticle(articleDto);
        System.out.println("文章id是======"+saveArticleResult);
        if (saveArticleResult.getCode().intValue() != 0) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        // 设置wmNews的status为9并更新
        Long articleId = saveArticleResult.getData();
        wmNews.setArticleId(articleId);
        updateWmNews(wmNews, WmNews.Status.PUBLISHED.getCode(), "发布成功");
        //TODO 通知ES添加文章

    }
}
