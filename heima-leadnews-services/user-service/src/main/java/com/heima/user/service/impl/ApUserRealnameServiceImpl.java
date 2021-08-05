package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.exception.CustException;
import com.heima.feigns.ArticleFeign;
import com.heima.feigns.WemediaFeign;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.AuthDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserRealname;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.mapper.ApUserRealnameMapper;
import com.heima.user.service.ApUserRealnameService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service("apUserRealnameService")
@Slf4j
public class ApUserRealnameServiceImpl extends ServiceImpl<ApUserRealnameMapper, ApUserRealname> implements ApUserRealnameService {
    /**
     * 查询用户认证信息列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadListByStatus(AuthDto dto) {
        //1.校验参数
        if (dto == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID);
        }
        dto.checkParam();
        //2.创造查询条件
        Page<ApUserRealname> page = new Page<>(dto.getPage(), dto.getSize());
        //页面中有对状态进行分组，所有需要添加查询条件
        LambdaQueryWrapper<ApUserRealname> listWrapper = Wrappers.<ApUserRealname>lambdaQuery().eq(dto.getStatus() != null, ApUserRealname::getStatus, dto.getStatus());
        IPage<ApUserRealname> pageResult = page(page, listWrapper);
        //3.返回结果
        return new PageResponseResult(dto.getPage(), dto.getSize(), pageResult.getTotal(), pageResult.getRecords());
    }

    @Autowired
    ApUserMapper apUserMapper;


    @Override
    public ResponseResult updateStatusById(AuthDto dto, Short status) {

        //1.校验参数
        if (dto == null || dto.getId() == null) {
            CustException.cust(AppHttpCodeEnum.PARAM_INVALID, "参数不能为空");
        }
        //根据传入id确认对应apuser是否存在
        ApUser apUser = apUserMapper.selectOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getId, dto.getId()));
        if (apUser == null) {
            CustException.cust(AppHttpCodeEnum.DATA_NOT_EXIST, "此认证用户无app账户，请注册后再进行认证");
        }
        //根据传入id确认对应认证账户状态是否为1
        ApUserRealname apUserRealname = getOne(Wrappers.<ApUserRealname>lambdaQuery().eq(ApUserRealname::getId, dto.getId()));
        if (apUserRealname.getStatus() != 1) {
            CustException.cust(AppHttpCodeEnum.LOGIN_STATUS_ERROR, "用户状态有误");
        }
        //2.将stats更新至apUserRealname
        apUserRealname.setUpdatedTime(new Date());
        apUserRealname.setStatus(status);
        updateById(apUserRealname);
        //将驳回原因更新至新的realname账户中
        if (StringUtils.isNotBlank(dto.getMsg())) {
            apUserRealname.setReason(dto.getMsg());
        }
        //3.返回结果
        if (status == 2) {
            return ResponseResult.okResult("认证状态修改成功");
        }
        // 6.1  开通自媒体账户
        WmUser wmUser = createWmUser(apUser);
        // 6.2  创建作者信息
        createAuthor(apUser, wmUser);
        // 报异常了
//        System.out.println(1/0);
        return ResponseResult.okResult("认证状态修改成功");
    }

    @Autowired
    WemediaFeign wemediaFeign;

    private WmUser createWmUser(ApUser apUser) {
        //1.校验参数
        //通过调用查询出来的apuser对象来确认是否有对应的wemediauser
        ResponseResult<WmUser> wmUserResult = wemediaFeign.findByName(apUser.getName());
        //先确认远程调用是否成功
        if (wmUserResult.getCode().intValue() != 0) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        //远程调用成功了再确认是否有对应的wemediauser
        if (wmUserResult.getData() != null) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST, "此账户已拥有自媒体账户");
        }
        //2.创造条件新建wmuser
        // 2. 如果不存在  创建自媒体账户
        WmUser wmUser = new WmUser();
        wmUser.setApUserId(apUser.getId()); // apuserid
        wmUser.setName(apUser.getName());
        wmUser.setPassword(apUser.getPassword());
        wmUser.setSalt(apUser.getSalt());
        wmUser.setImage(apUser.getImage());
        wmUser.setPhone(apUser.getPhone());
        wmUser.setStatus(9);
        wmUser.setType(0);
        wmUser.setCreatedTime(new Date());
        ResponseResult<WmUser> saveResult = wemediaFeign.save(wmUser);
        if (saveResult.getCode().intValue() != 0) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR, "远程调用失败，自媒体账户无法创建");
        }
        return saveResult.getData(); // 回去带有wmUserId的自媒体账户信息
    }
    @Autowired
    ArticleFeign articleFeign;

    private void createAuthor(ApUser apUser, WmUser wmUser) {
        //1.校验参数
        //确认远程调用是否失败
        ResponseResult<ApAuthor> apAuthorResult = articleFeign.findByUserId(apUser.getId());
        if (apAuthorResult.getCode().intValue()!=0) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR);
        }
        //确认apAuthor是否已经存在
        if (apAuthorResult.getData()!=null) {
            CustException.cust(AppHttpCodeEnum.DATA_EXIST,"作者账户已经存在");
        }
        //2.创造条件新建作者
        ApAuthor apAuthor = new ApAuthor();
        //3.返回结果
        apAuthor = new ApAuthor();
        apAuthor.setName(apUser.getName());
        apAuthor.setType(2);
        apAuthor.setUserId(apUser.getId());
        apAuthor.setCreatedTime(new Date());
        apAuthor.setWmUserId(wmUser.getId());
        ResponseResult saveResult = articleFeign.save(apAuthor);
        if (saveResult.getCode().intValue() != 0) {
            CustException.cust(AppHttpCodeEnum.REMOTE_SERVER_ERROR,"远程调用失败，作者账户无法创建");
        }
    }


}
