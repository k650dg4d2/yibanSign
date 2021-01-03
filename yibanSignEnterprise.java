package com.yiban;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.sun.deploy.util.ArrayUtil;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.core.util.ArrayUtils;

public class yibanSignEnterprise {


    private static String access_token;
    private static String user_id;
    private static String token;
    private static String verity_request;
    private static String seession;
    private static String request_token;
    private static String jsessionid;

    public static void main(String[] args) throws Exception{


        //test();


        //创建学生信息集合
        String[] info = new String[2];
        Map<String,String[]> students = new HashMap();

        //一号 大帅比
        info[0] = "加密后的密码";
        info[1] = "signJson={\"studentId\":\"2019232070101\",\"collegeName\":\"XXXXXX学院\",\"className\":\"网络工程XXX\",\"realName\":\"大帅比\",\"answer\":\"{\\\"q1\\\":\\\"是\\\",\\\"q2\\\":\\\"是\\\",\\\"q3\\\":\\\"是\\\",\\\"q4\\\":\\\"是\\\",\\\"q4_1\\\":\\\"\\\",\\\"q5\\\":\\\"是\\\",\\\"position\\\":\\\"XX省XX市XX区\\\"}\"}";
        students.put("158XXXXXXX9",info);

      


        //遍历集合
        Iterator<Map.Entry<String, String[]>> entries = students.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, String[]> entry = entries.next();
            String key = entry.getKey();
            String[] value = entry.getValue();
            String passwd = value[0];
            //System.out.println("passwd = " + passwd);
            String signjson = value[1];



            //循环所有学生的信息,并提交
            //第一步 取得权限控制token  用户ID  和  token
            loginAndGetCookie(passwd,key);


            //第二步 获取verity_request,最终提交需要此请求返回的JSEEIONID
            getVerity_request();

            //第三步 获取cookie中的JSESSIONID,用于换取token
            getJSESSIONID();

            //第四步 获取token,此token为请求签到时携带的参数
            getToken();

            //第五步 获取提交签到所需的JSEESIONID
            getLastJSESSIONID();

            //第六步 提交请求
            requestSign(signjson);


            //System.out.println(key + ":" + value[0] + value[1]);
        }




        return;

    }

    public static void requestSign(String signjson) throws IOException {
        //System.out.println("=============第六步============");


        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://211.68.191.30/epidemic/student/sign");
        httpPost.addHeader("Host","211.68.191.30");
        httpPost.setHeader("Connection","keep-alive");
        httpPost.setHeader("Accept","*/*");
        httpPost.setHeader("X-Requested-With","XMLHttpRequest");
        httpPost.setHeader("User-Agent","Mozilla/5.0");
        httpPost.setHeader("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
        httpPost.setHeader("Origin","http://211.68.191.30");
        httpPost.setHeader("Referer","http://211.68.191.30/epidemic/student/sign");
        httpPost.setHeader("Accept-Encoding","gzip, deflate");
        httpPost.setHeader("Accept-Language","zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        httpPost.setHeader("Cookie","client=android;JSESSIONID="+jsessionid);
        httpPost.setEntity(new StringEntity(signjson.toString(), StandardCharsets.UTF_8));

        CloseableHttpResponse response = httpclient.execute(httpPost);

        int code = response.getStatusLine().getStatusCode();
        //System.out.println(code);

        String s = EntityUtils.toString(response.getEntity());
        //System.out.println(s);
        String data = JSON.parseObject(s).getString("data");
        String msg = JSON.parseObject(s).getString("msg");
        String realName = JSON.parseObject(data).getString("realName");
        System.out.println(realName+msg);


        response.close();
    }

    public static void getLastJSESSIONID() throws IOException {
        //System.out.println("=============第五步============");

        //设置不允许重定向
        RequestConfig config = RequestConfig.custom().setRedirectsEnabled(false).build();
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(config).build();

        List<NameValuePair> params = Lists.newArrayList();
        params.add(new BasicNameValuePair("token", request_token));
        params.add(new BasicNameValuePair("verify_request", verity_request));
        String str = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));
        HttpGet httpGet = new HttpGet("http://211.68.191.30/epidemic/index"+"?"+str);
        httpGet.addHeader("Host","211.68.191.30");
        httpGet.addHeader("Connection","keep-alive");
        httpGet.addHeader("Upgrade-Insecure-Requests","1");
        httpGet.addHeader("User-Agent","Mozilla/5.0");
        httpGet.addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        httpGet.addHeader("X-Requested-With","com.yiban.app");
        httpGet.addHeader("Referer",verity_request+"&yb_uid="+user_id);
        httpGet.addHeader("Accept-Encoding","gzip, deflate");
        httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");

        HttpResponse response = httpclient.execute(httpGet);

        int code = response.getStatusLine().getStatusCode();
        //System.out.println(code);

        if (code == 302) {
            Header header = response.getFirstHeader("Set-Cookie"); // 跳转的目标地址是在response的 HTTP-HEAD 中的，location的值
           // System.out.println(header);

            //处理字符串,拿到JESSIONID的值
            String[] tmp = header.toString().split("=", 2);
            String[] s = tmp[1].toString().split(";", 2);
            jsessionid = s[0].toString();
            //System.out.println("JSESSIONID = " + jsessionid);

            /*String jump = header.getValue(); // 这就是跳转后的地址，再向这个地址发出新申请，以便得到跳转后的信息是啥。
            System.out.println(jump);*/
        }
    }

    public static void getToken() throws IOException {
        //System.out.println("=============第四步============");
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://211.68.191.30/youni/getToken");
        httpGet.addHeader("Host","211.68.191.30");
        httpGet.addHeader("Connection","keep-alive");
        httpGet.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
        httpGet.addHeader("User-Agent","Mozilla/5.0 (Linux; Android 10; Redmi K20 Pro; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/87.0.4280.101 Mobile Safari/537.36 yiban_android\n");
        httpGet.addHeader("X-Requested-With","XMLHttpRequest");
        httpGet.addHeader("Referer",verity_request+"&yb_uid="+user_id);
        httpGet.addHeader("Accept-Encoding","gzip, deflate");
        httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        httpGet.addHeader("Cookie","JSESSIONID="+seession+";client=android");
        CloseableHttpResponse response = httpclient.execute(httpGet);

        String s = EntityUtils.toString(response.getEntity());
        //System.out.println(s);
        JSONObject jsonObject = JSON.parseObject(s);
        request_token = jsonObject.getString("token");
        //System.out.println(request_token);
        response.close();
    }

    public static void getJSESSIONID() throws IOException {
        //System.out.println("=============第三步============");
        //处理verify
        String[] ver = verity_request.split("=",3);
        verity_request = ver[1].substring(0,(ver[1].length()-7));


        /*
        构建带参的get请求,可以封装为一个方法
         */
        List<NameValuePair> params = Lists.newArrayList();
        params.add(new BasicNameValuePair("verify_request", verity_request));
        params.add(new BasicNameValuePair("yb_uid", user_id));
        String str = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://211.68.191.30/youni/index"+"?"+str);
        //System.out.println(httpGet);
        httpGet.addHeader("Host","211.68.191.30");
        httpGet.addHeader("Connection","keep-alive");
        httpGet.addHeader("Upgrade-Insecure-Requests","1");
        httpGet.addHeader("User-Agent","Mozilla/5.0");
        httpGet.addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        httpGet.addHeader("X-Requested-With","com.yiban.app");
        httpGet.addHeader("Accept-Encoding","gzip, deflate");
        httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        Header[] cookie_jseesionid = response.getHeaders("Set-Cookie");
        //System.out.println(cookie_jseesionid[0]);
        String jessionid = cookie_jseesionid[0].toString();
        //分割字符串
        String[] split = jessionid.split(";");
        String[] jsessionid = split[0].toString().split("=");
        seession = jsessionid[1].toString();
        //System.out.println("seession: "+ seession);
        response.close();
    }

    public static void getVerity_request() throws IOException {
        //System.out.println("=============第二步============");
        String login_token=null;
        //设置请求参数并拼接到uri
        List<NameValuePair> params = Lists.newArrayList();
        params.add(new BasicNameValuePair("act", "iapp256743"));
        params.add(new BasicNameValuePair("v", access_token));
        String str = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));

        //设置不允许重定向
        RequestConfig config = RequestConfig.custom().setRedirectsEnabled(false).build();
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultRequestConfig(config).build();


        HttpGet httpGet = new HttpGet("http://f.yiban.cn/iapp/index"+"?"+str);
        httpGet.addHeader("Host","f.yiban.cn");
        httpGet.addHeader("Connection","1");
        httpGet.addHeader("Upgrade-Insecure-Requests","f.yiban.cn");
        httpGet.addHeader("User-Agent","Mozilla/5.0");
        httpGet.addHeader("Accept"," text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        httpGet.addHeader("authorization"," Bearer "+access_token);
        httpGet.addHeader("appversion"," 4.9.2");
        httpGet.addHeader("logintoken",access_token);
        httpGet.addHeader("signature"," 1Ib6Ljqrmbn8nniimH5k/y1oO119MfezeQ1OfgIaNyoUcissuMn2eWgi0JDNpCJKUKenW/vfnQwO6pL17jTqZQ");
        httpGet.addHeader("X-Requested-With"," com.yiban.app");
        httpGet.addHeader("Accept-Encoding"," gzip, deflate");
        httpGet.addHeader("Accept-Language"," zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
        httpGet.addHeader("Cookie","loginToken="+access_token);

        HttpResponse response = httpclient.execute(httpGet);

        int code = response.getStatusLine().getStatusCode();
        //System.out.println(code);
        verity_request = "";
        if (code == 302) {
            Header header = response.getFirstHeader("location"); // 跳转的目标地址是在response的 HTTP-HEAD 中的，location的值
            verity_request = header.getValue(); // 这就是跳转后的地址，再向这个地址发出新申请，以便得到跳转后的信息是啥。
            //System.out.println(verity_request);
        }


    }

    public static void loginAndGetCookie(String passwd,String account) throws IOException {
        //System.out.println("=============第一步============");
        List<NameValuePair> params = Lists.newArrayList();
        params.add(new BasicNameValuePair("account", account));
        params.add(new BasicNameValuePair("passwd", passwd));
        params.add(new BasicNameValuePair("ct", "2"));
        params.add(new BasicNameValuePair("app", "1"));
        params.add(new BasicNameValuePair("v", "4.9.3"));
        params.add(new BasicNameValuePair("apn", "wifi"));
        params.add(new BasicNameValuePair("identify", "35bc22783154f57e"));
        params.add(new BasicNameValuePair("sig", "aa563e15204040ae"));
        params.add(new BasicNameValuePair("token", ""));
        params.add(new BasicNameValuePair("device", "Xiaomi%3ARedmi+K20+Pro"));
        params.add(new BasicNameValuePair("sversion", "29"));
        //params.add(new BasicNameValuePair("Content-Type", "application/json"));
        String str = EntityUtils.toString(new UrlEncodedFormEntity(params, Consts.UTF_8));

        CloseableHttpClient httpclient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet("https://mobile.yiban.cn/api/v2/passport/login"+"?"+str);

        /*
            执行get请求,返回结果
            输出状态码和返回内容
         */
        CloseableHttpResponse response1 = httpclient.execute(httpGet);
        //System.out.println(response1.getStatusLine().getStatusCode());
        HttpEntity entity = response1.getEntity();
        //System.out.println();
        String s = EntityUtils.toString(response1.getEntity());
        JSONObject jsonObject = JSON.parseObject(s);
        //System.out.println(jsonObject);

        //拿到返回数据的access_token
        String data_1 = jsonObject.getString("data");
        JSONObject data = JSON.parseObject(data_1);
        access_token = data.getString("access_token");
        //System.out.println("权限控制token为: "+ access_token);

        //拿到返回数据的user_id
        user_id = data.getJSONObject("user").getString("user_id");
        //System.out.println("用户ID为: "+ user_id);

        //  找到token
        token = data.getString("token");
        //System.out.println("token为: "+ token);

        //关闭连接
        response1.close();

    }
}

