package com.heima.admin;

import com.heima.admin.service.WemediaNewsAutoScanService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class AutoTest {
    @Autowired
    WemediaNewsAutoScanService wemediaNewsAutoScanService;

    @Test
    public  void  test(){
        wemediaNewsAutoScanService.autoScanByMediaNewsId(6291);
    }
}
