package com.mmall.util;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;

import java.net.URLEncoder;

public class QiniuUtil {

    private static String accesskey = "1mNw9EhpW5quaZdISNlHVfRFESotrHb1Op8DJ36Z";

    private static String secretKey = "F2f7qxlv5nHR6f4siacgwcPUrARFtHs3l_g2c8fG";

    private static String bucket = "nljshoxbb1";

    private static String domain = "http://pvxd9okvu.bkt.clouddn.com";

    //默认不指定key的情况下，以文件内容的hash值作为文件名
    private static String key = null;

    /*
     * 这两个参数就是在定义PutPolicy参数时指定的内容
     */
    //回调地址
    private static String callbackUrl = "http://api.example.com/qiniu/callback";
    //定义回调内容的组织格式，与上传策略中的callbackBodyType要保持一致
    //String callbackBodyType = "application/x-www-form-urlencoded"; //回调鉴权的签名包括请求内容callbackBody
    private static String callbackBodyType = "application/json";//回调鉴权的签名不包括请求内容

    /**
     * 这两个参数根据实际所使用的HTTP框架进行获取
     */
    //通过获取请求的HTTP头部Authorization字段获得
    private static  String callbackAuthHeader = "xxx";
    //通过读取回调POST请求体获得，不要设置为null
    private static byte[] callbackBody = null;



    private static Configuration configuration = new Configuration(Zone.zone2());

    private static Auth auth = Auth.create(accesskey, secretKey);

    private static BucketManager bucketManager = new BucketManager(auth, configuration);

    public static String upload(String filePath) {

        UploadManager uploadManager = new UploadManager(configuration);

        String upToken = auth.uploadToken(bucket);

        try {
            Response response = uploadManager.put(filePath, key, upToken);
            //解析上传成功的结果
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            System.out.println(putRet.key);
            System.out.println(putRet.hash);

            try {
                String fileName = putRet.key;
                String encodedFileName = URLEncoder.encode(fileName, "utf-8").replace("+", "%20");
                String publicUrl = String.format("%s/%s", domain, encodedFileName);
                Auth auth = Auth.create(accesskey, secretKey);
                long expireInSeconds = 3600;//1小时，可以自定义链接过期时间
                String finalUrl = auth.privateDownloadUrl(publicUrl, expireInSeconds);
                System.out.println(finalUrl);
                return finalUrl;
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        } catch (QiniuException ex) {
            Response r = ex.response;
            System.err.println(r.toString());
            try {
                System.err.println(r.bodyString());
            } catch (QiniuException ex2) {
                //ignore
            }
        }
        return null;
    }
}
