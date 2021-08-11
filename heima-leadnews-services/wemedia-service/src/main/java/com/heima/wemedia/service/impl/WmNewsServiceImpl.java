package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.message.NewsAutoScanConstants;
import com.heima.common.constants.wemedia.WemediaConstants;
import com.heima.common.exception.CustException;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Value("${file.oss.web-site}")
    String website;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        //1.判断参数
        if (dto == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "传送参数有误");
        }
        dto.checkParam();
        WmUser wmUser = WmThreadLocalUtils.getUser();
        if (wmUser == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
//        2.查的表：WmNews
//          条件：
//          1.文章状态
//          2.关键字
//          3.频道列表(需要去admin里实现)
//          4.发布日期（起始和结束）
//          5.当前用户的id
        LambdaQueryWrapper<WmNews> wmNewsQueryWrapper = Wrappers.<WmNews>lambdaQuery();
        wmNewsQueryWrapper.eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus());
        wmNewsQueryWrapper.like(StringUtils.isNotBlank(dto.getKeyword()), WmNews::getTitle, dto.getKeyword());
        wmNewsQueryWrapper.ge(dto.getBeginPubDate() != null, WmNews::getPublishTime, dto.getBeginPubDate());
        wmNewsQueryWrapper.le(dto.getEndPubDate() != null, WmNews::getPublishTime, dto.getEndPubDate());
        wmNewsQueryWrapper.eq(WmNews::getUserId, wmUser.getId());
        // 2.2 频道
        wmNewsQueryWrapper.eq(dto.getChannelId() != null, WmNews::getChannelId, dto.getChannelId());
        //按照发布时间降序
        wmNewsQueryWrapper.orderByDesc(WmNews::getPublishTime);
        IPage<WmNews> pageReq = new Page<>(dto.getPage(), dto.getSize());
        IPage<WmNews> pagerResult = page(pageReq, wmNewsQueryWrapper);
        PageResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), pagerResult.getTotal());
        //返回的结果里面需要将图片前缀访问路径设置到host属性中
        responseResult.setData(pagerResult.getRecords());
        responseResult.setHost(website);
        //3.返回结果
        return responseResult;
    }

    @Autowired
    WmNewsMaterialMapper wmNewsMaterialMapper;

    @Autowired
    WmMaterialMapper wmMaterialMapper;

    @Override
    public ResponseResult submitNews(WmNewsDto wmNewsDto) {
        //1.校验参数
        if (wmNewsDto == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "传送参数有误");
        }
        WmUser wmUser = WmThreadLocalUtils.getUser();
        if (wmUser == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //2.业务实现
        //将dto数据补全到wmNews
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(wmNewsDto, wmNews);
        if (wmNews.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            wmNews.setType(null);
        }
        //封面图片images传过来是集合，但是数据库是，分隔的字符串
        // 1.4 如果传入的images集合不为空  转为字符串  用逗号拼接
        if (CollectionUtils.isNotEmpty(wmNewsDto.getImages())) {
            String images = imageListToString(wmNewsDto.getImages());
            wmNews.setImages(images);
        }
        System.out.println("老师加setUserId语句前====" + wmNews.getUserId());
        wmNews.setUserId(wmUser.getId());
        //这里新建的文章还没有当前的userid，需要赋值
        System.out.println("老师加setUserId语句后====" + wmNews.getUserId());
        // 保存前补全信息
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable(WemediaConstants.WM_NEWS_UP); // 上架
        System.out.println(wmNews.getId());
        //如果id为空，保存wmNews
        if (wmNewsDto.getId() == null) {
            save(wmNews);
            System.out.println(wmNews.getId());
        }
        System.out.println(wmNews.getId());
        //如果id不为空，删除素材与文章的关系后进行更新
        wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNewsDto.getId()));
        updateById(wmNews);
        // 草稿
        if (wmNewsDto.getStatus().intValue() == 0) {
            return ResponseResult.okResult();
        }
        // 3.如果是待审核状态 需要保存 内容图片和封面图片 与素材的关联关系
        //抽取content里的图片内容
        List<String> contentImages = parseContentImages(wmNewsDto.getContent());
        //保存content图片与文章的关系
        if (contentImages.size()>0) {
            saveRelativeInfo(contentImages, wmNews.getId(), WemediaConstants.WM_CONTENT_REFERENCE);
        }
        //如果文章和封面都是无图，直接返回

        //保存封面图片与文章的关系，封面图片需要从内容图片里面选，dto用来传选择的封面status
        //wnNews用来设置封面的模式
        if (wmNews.getStatus() == WmNews.Status.SUBMIT.getCode()) {
            if (contentImages.size()>0) {
                saveCoverRelativeInfo(contentImages, wmNews, wmNewsDto);
            }
            Map map = new HashMap<>();
            map.put("newsId", wmNews.getId());
            kafkaTemplate.send(NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_TOPIC, JSON.toJSONString(map));
            log.info(" 文章自动审核消息 已成功发送   文章id: {}", wmNews.getId());
        }
        //3.返回结果
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult findWmNewsById(Integer id) {
        //1.校验参数
//        WmUser wmUser = WmThreadLocalUtils.getUser();
//        if (wmUser == null) {
//            throw new CustomException(AppHttpCodeEnum.NEED_LOGIN, "未登录");
//        }
        if (id == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST, "修改的文章不存在");
        }
        //2.业务实现
        WmNews wmNews = getOne(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getId, id));
        if (wmNews == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST, "修改的文章不存在");
        }
        ResponseResult responseResult = ResponseResult.okResult(wmNews);
        responseResult.setHost(website);
        //3.返回结果
        return responseResult;
    }

    @Override
    public ResponseResult delNews(Integer id) {
        //1.校验参数
        //1.校验参数
        WmUser wmUser = WmThreadLocalUtils.getUser();
        if (wmUser == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "删除的文章不存在");
    }
        if (id == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "删除的文章不存在");
        }
        //2.业务实现
        WmNews wmNews = getOne(Wrappers.<WmNews>lambdaQuery().eq(WmNews::getId, id));
        if (wmNews == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "删除的文章不存在");
        }
        if (wmNews.getEnable().equals(WemediaConstants.WM_NEWS_UP) && wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())) {
//            throw new CustomException(AppHttpCodeEnum.DATA_NOT_ALLOW, "该文章已发布，并且是上架状态，不能删除");
           CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "该文章已发布，并且是上架状态，不能删除");

        }
        //3.返回结果
        removeById(id);
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {
        //1.校验参数
        if (dto == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //查询文章
        WmNews wmNews = getById(dto.getId());
        if (wmNews == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        //判断文章是否发布
        if (!wmNews.getStatus().equals(WemediaConstants.WM_NEWS_PUBLISH_STATUS)) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW, "此文章不是发布状态，无法上下架");
        }
        //2.业务实现
        if (dto.getEnable() == null && dto.getEnable().intValue() != -1 && dto.getEnable().intValue() != 0) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        wmNews.setEnable(dto.getEnable());
        updateById(wmNews);
        //3.返回结果
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    private void saveCoverRelativeInfo(List<String> contentImages, WmNews wmNews, WmNewsDto wmNewsDto) {
        //封面的图片集合
        List<String> images = wmNewsDto.getImages();
        //对状态进行判断
        if (wmNewsDto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            if (CollectionUtils.isEmpty(contentImages)) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "图片内容为空");
                images = null;
            } else if (contentImages.size() <= 2) {
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = contentImages.stream().limit(1).collect(Collectors.toList());
            } else if (contentImages.size() > 2) {
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = contentImages.stream().limit(3).collect(Collectors.toList());
            }
            if (CollectionUtils.isNotEmpty(images)) {
                wmNews.setImages(imageListToString(images));
            }
            updateById(wmNews);
        }
        //保存封面图片与文章的关系
        if (CollectionUtils.isNotEmpty(images)) {
            List<String> imagesList = images.stream().map(url -> url.replaceAll(website, "")).collect(Collectors.toList());
            saveRelativeInfo(imagesList, wmNews.getId(), WemediaConstants.WM_IMAGE_REFERENCE);
        }

    }

    private void saveRelativeInfo(List<String> contentImages, Integer id, Short wmContentReference) {
        //保存内容图片与文章的关系到关系表
        //type=0，newsid，材料id
        //首先要根据内容图片查出来材料的集合，再查出来材料id
        List<WmMaterial> wmMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery()
                .in(WmMaterial::getUrl, contentImages)
                .eq(WmMaterial::getUserId, WmThreadLocalUtils.getUser().getId()));
        if (CollectionUtils.isEmpty(wmMaterials)) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "引用的素材不存在");
        }
        //将素材信息转换为map集合，key url，value id
        Map<String, Integer> urlIdMap = wmMaterials.stream().collect(Collectors.toMap(WmMaterial::getUrl, WmMaterial::getId));
        //遍历素材url集合，将每个url所对应的id 存入到素材id集合中
        List<Integer> materialIds = new ArrayList<>();
        contentImages.forEach(url -> {
            if (!urlIdMap.containsKey(url)) {
                CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "引用的素材不存在");
            }
            materialIds.add(urlIdMap.get(url));
        });
        //调用批量插入方法
        wmNewsMaterialMapper.saveRelations(materialIds, id, wmContentReference);
    }

    private List<String> parseContentImages(String content) {
        List<Map> maps = JSONArray.parseArray(content, Map.class);
        List<String> imageList = maps.stream()
                //过滤type为image的map
                .filter(map -> WemediaConstants.WM_NEWS_TYPE_IMAGE.equals(map.get("type")))
                //获取map中的值
                .map(map -> (String) map.get("value"))
                //替换url中的前缀
                .map(url -> url.replaceAll(website, ""))
                //将路径收集成集合
                .collect(Collectors.toList());
        return imageList;
    }

    private String imageListToString(List<String> images) {
        return images.stream()
                .map(str -> str.replaceAll(website, ""))
                .collect(Collectors.joining(","));
    }
}
