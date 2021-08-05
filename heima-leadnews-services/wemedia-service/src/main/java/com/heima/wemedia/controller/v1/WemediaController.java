package com.heima.wemedia.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.service.WemediaServie;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Api(value = "自媒体用户API", tags = "自媒体用户API")
@RestController
@RequestMapping("/api/v1/user")
public class WemediaController {

    @Autowired
    private WemediaServie wemediaServie;

    @ApiOperation("根据用户名查询自媒体用户")
    @GetMapping("/findByName/{name}")
    public ResponseResult<WmUser> findByName(@PathVariable("name") String name) {
       return wemediaServie.findByName(name);

    }

    @ApiOperation("保存自媒体用户")
    @PostMapping("/save")
    public ResponseResult<WmUser> save(@RequestBody WmUser wmUser) {
        wemediaServie.save(wmUser);
        return ResponseResult.okResult(wmUser);
    }
}
