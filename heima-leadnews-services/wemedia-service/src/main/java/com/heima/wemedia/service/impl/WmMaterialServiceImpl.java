package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.common.exception.CustomException;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.threadlocal.WmThreadLocalUtils;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service("wmMaterialService")
@Slf4j
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Value("${file.oss.prefix}")
    String prefix;


    @Value("${file.oss.web-site}")
    String webSite;

    @Autowired
    FileStorageService fileStorageService;

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        // 1. 检查参数 (multipartFile   校验图片格式==> jpg jpeg png gif    用户是否登录   )
        if (multipartFile == null || multipartFile.getSize() == 0) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "上传的文件不存在");
        }
        WmUser user = WmThreadLocalUtils.getUser();
        if (user == null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN,"登录超时");
        }
        //校验图片格式对不对
        // 获取原始文件名称
        String originalFilename = multipartFile.getOriginalFilename();
        // 2. 上传文件到阿里云OSS    sa.da.sd.jpg
        //       文件名称:  获取原始的后缀  meinv.jpg    .jpg
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (suffix == "jpg" || suffix == "jpeg" || suffix == "png" || suffix == "gif") {
            CustException.cust(AppHttpCodeEnum.PARAM_IMAGE_FORMAT_ERROR);
        }
        //2.创造条件
        //调用uuid创建上传图片的id，但是需要进行-符合的去除
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String fileName = uuid + suffix;
        //3.返回结果
        try {
            String store = fileStorageService.store(prefix, fileName, multipartFile.getInputStream());
            //上传完还需要对数据库进行保存
            WmMaterial wmMaterial = new WmMaterial();
            wmMaterial.setUserId(user.getId());
            wmMaterial.setUrl(store);//Oss路径
            wmMaterial.setType((short) 0);
            wmMaterial.setIsCollection((short) 0);
            wmMaterial.setCreatedTime(new Date());
            //保存到数据库
            save(wmMaterial);
            //返回结果，需要在原来的表里的url字段加上website来让前端可以显示的显示页面
            wmMaterial.setUrl(webSite + wmMaterial.getUrl());
            return ResponseResult.okResult(wmMaterial);
        } catch (IOException e) {
            log.info("文件上传异常 == > {}", e.getMessage());
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR, "服务端出现异常，上传素材失败");
        }

    }

    @Override
    public ResponseResult findList(WmMaterialDto dto) {
        //1.校验参数
        if (dto == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "WmMaterial参数为null");
        }
        dto.checkParam();
        //2.创造条件
        IPage<WmMaterial> page = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmMaterial> wmWrapper = Wrappers.<WmMaterial>lambdaQuery().eq(StringUtils.isNotBlank(dto.getIsCollection().toString()), WmMaterial::getIsCollection, dto.getIsCollection());
        IPage<WmMaterial> pageResult = page(page, wmWrapper);
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal());
        List<WmMaterial> records = pageResult.getRecords();
        for (WmMaterial record : records) {
            record.setUrl(webSite + record.getUrl());
        }
        responseResult.setData(records);
        return responseResult;
    }

    @Autowired
    WmNewsMaterialMapper wmNewsMaterialMapper;

    @Override
    public ResponseResult delPicture(Integer id) {
        //1.校验参数
        if (id==null) {
            CustException.cust(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //如果图片被引用，无法删除
        Integer count = wmNewsMaterialMapper.selectCount(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getMaterialId, id));
        if (count>0) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW,"图片被引用无法删除");
        }
        WmMaterial wmMaterial = getById(id);
        if (wmMaterial.getUrl()==null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_ALLOW);
        }
        fileStorageService.delete(wmMaterial.getUrl());
        //2.创造条件
        removeById(id);
        //3.返回结果
        return ResponseResult.okResult();
    }

    @Override
    public ResponseResult updateStatus(Integer id, Short type) {
        //1.校验参数
        WmUser wmUser = WmThreadLocalUtils.getUser();
        if (wmUser==null) {
            CustException.cust(AppHttpCodeEnum.NEED_LOGIN);
        }
        //2.创造条件
        update(Wrappers.<WmMaterial>lambdaUpdate().set(WmMaterial::getIsCollection,type).eq(WmMaterial::getId,id).eq(WmMaterial::getUserId,wmUser.getId()));
        //3.返回结果
        return ResponseResult.okResult();
    }
}
