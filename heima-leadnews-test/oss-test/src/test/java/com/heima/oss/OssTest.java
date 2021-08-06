package com.heima.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;

import java.io.FileInputStream;

public class OssTest {
    public static void main(String[] args) throws Exception {
        // Endpoint以杭州为例，其它Region请按实际情况填写。
        String endpoint = "oss-cn-shanghai.aliyuncs.com";
        // 阿里云主账号AccessKey拥有所有API的访问权限，风险很高。
        String accessKeyId = "LTAI5tNWWBdYDfeddvaJF3aZ";
        String accessKeySecret = "vulnJ1yDT4eBlCIEDveIXZnXLep3VV";

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 上传Byte数组。
        FileInputStream inputStream = new FileInputStream("D:\\1.png");
        PutObjectResult result = ossClient.putObject("hmtt130lmf", "material/a.jpg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
    }
}
