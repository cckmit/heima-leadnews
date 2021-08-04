package com.heima.admin.controller.v1;

import com.heima.admin.service.LoginService;
import com.heima.model.admin.dtos.AdUserDto;
import com.heima.model.admin.pojos.AdUser;
import com.heima.model.common.dtos.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Api(value = "登录&注册", tags = "登录&注册", description = "登录&注册")
public class LoginController {

    @Autowired
    private LoginService loginService;
    @PostMapping("/login/in")
    @ApiModelProperty("登录")
    public ResponseResult logIn(@RequestBody AdUserDto userDto){
        return loginService.logIn(userDto);
    }


    @PostMapping("/register")
    @ApiModelProperty("登录")
    public ResponseResult register(@RequestBody AdUser adUser){
        return loginService.register(adUser);
    }
}
