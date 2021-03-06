package com.abase.okhttp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.RequiresApi;

import com.abase.global.AbAppConfig;
import com.abase.okhttp.Interceptor.DownInterceptor;
import com.abase.okhttp.Interceptor.GzipRequestInterceptor;
import com.abase.okhttp.body.FileRequestBody;
import com.abase.okhttp.body.MultipartBodyRbody;
import com.abase.okhttp.cookies.PersistentCookieStore;
import com.abase.okhttp.db.SQLTools;
import com.abase.okhttp.log.HttpLoggingInterceptor;
import com.abase.okhttp.util.DownLoad;
import com.abase.task.AbThreadFactory;
import com.abase.util.AbFileUtil;
import com.abase.util.AbLogUtil;
import com.abase.util.GsonUtil;
import com.abase.util.Tools;
import com.wj.eventbus.WjEventBus;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Credentials;
import okhttp3.FormBody.Builder;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;
import okio.Buffer;

/**
 * okhttp?????????????????????
 *
 * @author wangjun
 * @version 2.0
 * @date 2016???2???1???
 */
public class OhHttpClient {
    /**
     * ????????????
     */
    public static final String OKHTTP_TIMEOUT = "OKHTTP_TIMEOUT";
    /**
     * ????????????
     */
    public static final String OKHTTP_FAILURE = "OKHTTP_FAILURE";
    /**
     * ??????.
     */
    protected static final int SUCCESS_MESSAGE = 0;
    /**
     * ??????.
     */
    protected static final int FAILURE_MESSAGE = 1;
    /**
     * ??????
     */
    protected static final int ERROE_MESSAGE = 2;
    /**
     * ????????????
     */
    protected static final int PROGRESS_MESSAGE = 3;
    /**
     * ????????????
     */
    protected static final int FINSH_MESSAGE = 4;
    /**
     * ????????????
     */
    protected static final int START_MESSAGE = 5;

    /**
     * ?????????????????????
     */
    public static int CONNECTTIMEOUT = 30;
    /**
     * ?????????????????????
     */
    public static int WRITETIMEOUT = 60;
    /**
     * ?????????????????????
     */
    public static int READTIMEOUT = 60;
    /**
     * ???????????????
     */
    public static String CACHEPATH = null;
    /**
     * ???????????????
     */
    public static int cacheSize = 100 * 1024;
    /**
     * ??????????????????????????????
     */
    public static int cacheTimeOut = 5;
    /**
     * ?????????????????????
     */
    public static String DOWNDIR = "/storage/emulated/0/Download";
    /**
     * ??????????????????????????????json?????????
     */
    private boolean isJsonFromMat = true;
    /**
     * ?????????????????????????????????????????????
     */
    public int dowmUpTime = -1;
    /**
     * ???????????????url?????? ????????????10???
     */
    private final ArrayList<String> destroyUrls = new ArrayList<>();

    private static OhHttpClient ohHttpClient;
    private OkHttpClient client;
    private static Headers headers = null;
    private PersistentCookieStore cookieStore;//cookies
    public Handler handler = new Handler(Looper.getMainLooper());
    private HttpLoggingInterceptor logging=new HttpLoggingInterceptor();//????????????
    private final Charset UTF8 = Charset.forName("UTF-8");


    public HttpLoggingInterceptor getLogging() {
        return logging;
    }

    public OhHttpClient setLogging(HttpLoggingInterceptor logging) {
        this.logging = logging;
        return this;
    }

    /**
     * ???????????????urls ?????? ????????????10???
     *
     * @return ???????????????url??????
     */
    public ArrayList<String> getDestroyUrls() {
        return destroyUrls;
    }


    public boolean isJsonFromMat() {
        return isJsonFromMat;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    //json????????????????????? ?????????
    public static String JSONTYE = "MediaTypeJson";

    /**
     * ??????????????????????????????json
     */
    public OhHttpClient setJsonFromMat(boolean jsonFromMat) {
        isJsonFromMat = jsonFromMat;
        return ohHttpClient;
    }

    /**
     * ?????????
     */
    public OhHttpClient() {
        if (client == null) {
            client = new OkHttpClient();
            setOkHttpClient();
        }
    }


    /**
     * ????????????
     */
    public static synchronized OhHttpClient getInit() {
        if (ohHttpClient == null) {
            ohHttpClient = new OhHttpClient();
        }
        return ohHttpClient;
    }

    /**
     * ??????
     */
    public static synchronized OhHttpClient destroy() {
        if (ohHttpClient != null && ohHttpClient.client != null) {
            ohHttpClient.client.dispatcher().cancelAll();
            ohHttpClient.client = null;
            ohHttpClient = null;
        }
        return ohHttpClient;
    }

    /**
     * ?????????????????????
     */
    public void closeLog() {

    }

    /**
     * ?????????
     */
    public OhHttpClient setHeaders(Headers headers) {
        OhHttpClient.headers = headers;
        return ohHttpClient;
    }

    /**
     * ?????????
     */
    public Headers getHeaders() {
        return headers;
    }

    /**
     * ??????Cookies
     */
    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    public OhHttpClient setCookies(Context context) {
        if (cookieStore == null) {
            cookieStore = new PersistentCookieStore(context);
        }
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        client.newBuilder().cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                if (cookies != null && cookies.size() > 0) {
                    for (Cookie item : cookies) {
                        cookieStore.add(url, item);
                    }
                }
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url);
                return cookies;
            }
        }).build();
        return ohHttpClient;
    }

    /**
     * ????????????????????????
     */
    public OhHttpClient setCacheHeader(Context context) {
        if (headers != null) {
            headers.newBuilder().add("Cache-Control", "max-stale=10");
        } else {
            headers = new Headers.Builder().add("Cache-Control", "max-stale=10").build();
        }
        File file;
        if (CACHEPATH == null) {
            file = context.getCacheDir();
        } else {
            file = new File(CACHEPATH);
        }

        Cache cache = new Cache(file, cacheSize);
        client.newBuilder().cache(cache).build();
        return ohHttpClient;
    }

    /**
     * ???????????????url
     */
    public void destroyUrl(final String url) {
        if (destroyUrls.size() > 10) {
            destroyUrls.remove(0);
        }
        for (int i = 0; i < destroyUrls.size(); i++) {
            if (destroyUrls.get(i).equals(url)) {
                destroyUrls.remove(i);
                break;
            }
        }
        destroyUrls.add(url);
        if (client != null && url != null) {
//            if(queue==null){
//                queue=new LinkedList<String>();
//            }
//            queue.offer(url);
            AbThreadFactory.getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (client.dispatcher().getClass()) {
                        for (Call call : client.dispatcher().queuedCalls()) {
                            if (url.equals(call.request().tag())) {
                                call.cancel();
                            }
                        }
                        for (Call call : client.dispatcher().runningCalls()) {
                            if (url.equals(call.request().tag())) call.cancel();
                        }
                    }
                }
            });
        }
    }

    /**
     * ???????????????url
     */
    public boolean isHaveUrl(final String url) {
        if (client != null && url != null) {
            synchronized (client.dispatcher().getClass()) {
                for (Call call : client.dispatcher().queuedCalls()) {
                    if (url.equals(call.request().tag())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * ????????????url
     */
    public void destroyAll() {
        if (client != null) {
            client.dispatcher().cancelAll();
        }
        ohHttpClient = null;
    }

    /**
     * ??????http
     */
    private void setOkHttpClient() {
        okhttp3.OkHttpClient.Builder builder = client.newBuilder();
        builder.connectTimeout(CONNECTTIMEOUT, TimeUnit.SECONDS);
        builder.writeTimeout(WRITETIMEOUT, TimeUnit.SECONDS);
        builder.readTimeout(READTIMEOUT, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(true);//????????????
        client = builder.build();
    }

    /**
     * ???????????????????????????????????????????????????????????????
     */
    public void setLogcat(){
        okhttp3.OkHttpClient.Builder builder = client.newBuilder();
        //???????????????????????????
        if (logging == null) {
            logging = new HttpLoggingInterceptor();
        }
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addNetworkInterceptor(logging);
        client = builder.build();
    }

    /**
     * ????????????gzip????????????body
     */
    public void setGzip(boolean isGzip) {
        if (isGzip) {
            client = client.newBuilder().addInterceptor(new GzipRequestInterceptor()).build();// ???????????????
        } else {
            client.interceptors().remove(new GzipRequestInterceptor());
        }
    }

    /**
     * ??????ssl?????? ???????????????????????????????????? ,context.getAssets().open("zhy_server.cer") ????????? ????????????setCertificates ???????????????
     */
    public void setCertificates(InputStream... certificates) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            int index = 0;
            for (InputStream certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));

                try {
                    if (certificate != null)
                        certificate.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            client = client.newBuilder().sslSocketFactory(sslContext.getSocketFactory()).build();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ??????ssl?????? ????????????????????????????????????
     */
    public void setCertificates(String certificates) {
        InputStream certificate = new Buffer().writeUtf8(certificates).inputStream();
        setCertificates(certificate);
    }

    /**
     * ?????????tag
     * get??????
     */
    public void get(String url,String tag, OhObjectListener<?> callbackListener) {
        haveNoBody(url, callbackListener, 0,tag);
    }

    /**
     * ?????????tag
     * post??????
     */
    public void post(String url,String tag, OhObjectListener<?> callbackListener) {
        haveNoBody(url, callbackListener, 2,tag);
    }

    /**
     * ?????????tag
     * delete??????
     */
    public void delete(String url,String tag, OhCallBackListener<Object> callbackListener) {
        haveNoBody(url, callbackListener, 1,tag);
    }

    /**
     * get??????
     */
    public void get(String url, OhObjectListener<?> callbackListener) {
        haveNoBody(url, callbackListener, 0,null);
    }

    /**
     * post??????
     */
    public void post(String url, OhObjectListener<?> callbackListener) {
        haveNoBody(url, callbackListener, 2,null);
    }

    /**
     * delete??????
     */
    public void delete(String url, OhCallBackListener<Object> callbackListener) {
        haveNoBody(url, callbackListener, 1,null);
    }

    /**
     * ????????????????????????string??????????????? 0 ???get 1???delete
     */
    private void haveNoBody(String url, OhCallBackListener<Object> callbackListener, int type,String tag) {
        okhttp3.Request.Builder builder = new Request.Builder().url(url);
        // ??????tag
        builder.tag(url);
        switch (type) {
            case 0:// get
                builder.get();
                break;
            case 1:// delete
                builder.delete();
                break;
            case 2:// post
                builder.post(new Builder().build());
                break;
        }
        if (headers != null) {
            builder.headers(headers);
        }
        Request request = builder.build();
        client.newCall(request).enqueue(new OKHttpCallBack(request, callbackListener));
    }

    /**
     * ?????????tag
     * post??????
     */
    public void post(String url, OhHttpParams requestParams,String tag,
                     OhObjectListener<?> callbackListener) {
        haveBody(url, requestParams, callbackListener, 0,tag);
    }

    /**
     *  ?????????tag
     * put??????
     */
    public void put(String url, OhHttpParams requestParams,String tag,
                    OhObjectListener<?> callbackListener) {
        haveBody(url, requestParams, callbackListener, 1,tag);
    }

    /**
     *  ?????????tag
     * patch??????
     */
    public void patch(String url, OhHttpParams requestParams,String tag,
                      OhObjectListener<?> callbackListener) {
        haveBody(url, requestParams, callbackListener, 2,tag);
    }

    /**
     *  ?????????tag
     * dedelete??????
     */
    public void delete(String url, OhHttpParams requestParams,String tag,
                       OhCallBackListener<Object> callbackListener) {
        haveBody(url, requestParams, callbackListener, 3,tag);
    }

    /**
     * post??????
     */
    public void post(String url, OhHttpParams requestParams,
                     OhObjectListener<?> callbackListener) {
        haveBody(url, requestParams, callbackListener, 0,null);
    }

    /**
     * put??????
     */
    public void put(String url, OhHttpParams requestParams,
                    OhObjectListener<?> callbackListener) {
        haveBody(url, requestParams, callbackListener, 1,null);
    }

    /**
     * patch??????
     */
    public void patch(String url, OhHttpParams requestParams,
                      OhObjectListener<?> callbackListener) {
        haveBody(url, requestParams, callbackListener, 2,null);
    }

    /**
     * dedelete??????
     */
    public void delete(String url, OhHttpParams requestParams,
                       OhCallBackListener<Object> callbackListener) {
        haveBody(url, requestParams, callbackListener, 3,null);
    }

    /**
     * ?????????????????????string??????????????? 0 ???post 1???put 2???patch 3???delete
     */
    private void haveBody(String url, OhHttpParams requestParams,
                          OhCallBackListener<Object> callbackListener, int type,String tag) {
        RequestBody body;
        if (requestParams != null && requestParams.getKeys().contains(JSONTYE)) {
            body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), (String) requestParams.get(JSONTYE));
        } else {
            Builder requestBody = new Builder();
            if (requestParams != null) {
                ArrayList<String> keys = requestParams.getKeys();
                for (int i = 0; i < keys.size(); i++) {
                    requestBody.add(keys.get(i), requestParams.get(keys.get(i)).toString());
                }
            }
            body = requestBody.build();
        }

        okhttp3.Request.Builder builder = new Request.Builder().url(url);
        if(tag==null) builder.tag(tag); else builder.tag(url);// ??????tag
        switch (type) {
            case 0:// post
                builder.post(body);
                break;
            case 1:// put
                builder.put(body);
                break;
            case 2:// patch
                builder.patch(body);
                break;
            case 3:// delete
                builder.delete(body);
                break;
        }
        if (headers != null) {
            builder.headers(headers);
        }
        Request request = builder.build();
            client.newCall(request).enqueue(new OKHttpCallBack(request, callbackListener));
    }

    /**
     * ????????????
     * ?????????????????????????????????????????????,??????????????????????????????
     */
    public void upFile(String url,String param, OhHttpParams requestParams, File file,
                       OhFileCallBakListener callbackListener) {
        // ??????????????? ????????????????????????
        // text/x-markdown; charset=utf-8
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), file);

        // ???????????????????????????body ????????????
        okhttp3.MultipartBody.Builder multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(param, getFileEndCode(file.getName()), requestBody);
        // ??????post??????
        if (requestParams != null) {
            String key;
            Iterator<String> iterator = requestParams.getParams().keySet().iterator();
            while (iterator.hasNext()) {
                key = iterator.next();
                multipartBody.addFormDataPart(key, requestParams.get(key).toString());
            }
        }
        // ????????????
        okhttp3.Request.Builder builder = new Request.Builder().url(url).tag(url);// ??????tag
        if (headers != null) {
            builder.headers(headers);
        }
        builder.post(multipartBody.build());
        Request request = builder.build();
        callbackListener.ohType = 0;// ?????????????????????
        client.newCall(request).enqueue(new OKHttpCallBack(request, callbackListener));
    }

    /**
     * ????????????
     * ?????????????????????????????????????????????,??????????????????????????????
     */
    public void upFiles(String url, String param, OhHttpParams requestParams, List<File> files,
                        OhFileCallBakListener callbackListener) {
        // ???????????????????????????body ????????????
        okhttp3.MultipartBody.Builder multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
        // ??????????????????
        for (int i = 0; i < files.size(); i++) {
            multipartBody.addFormDataPart(param, getFileEndCode(files.get(i).getName()), RequestBody.create(MediaType.parse("application/octet-stream"), files.get(i)));
        }
        // ??????post??????
        if (requestParams != null) {
            String key;
            Iterator<String> iterator = requestParams.getParams().keySet().iterator();
            while (iterator.hasNext()) {
                key = iterator.next();
                multipartBody.addFormDataPart(key, requestParams.get(key).toString());
            }
        }
        // ????????????
        okhttp3.Request.Builder builder = new Request.Builder().url(url).tag(url);// ??????tag
        if (headers != null) {
            builder.headers(headers);
        }
        builder.post(new MultipartBodyRbody(multipartBody.build(), callbackListener));
        Request request = builder.build();
        callbackListener.ohType = 0;// ?????????????????????
        client.newCall(request).enqueue(new OKHttpCallBack(request, callbackListener));
    }

    /**
     * ????????????????????????okHttp3???header?????????????????????
     *
     * @param fileName
     * @return
     */
    private String getFileEndCode(String fileName) {
        try {
            return URLEncoder.encode(fileName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return Tools.setMD5(fileName);
    }


    /**
     * ???????????????
     *
     * @param url              ??????
     * @param requestParams    ???????????????
     * @param file             ??????
     * @param type             ?????????application/octet-stream
     * @param callbackListener ??????
     * @return
     */
    public FileRequestBody upFileStream(String url, OhHttpParams requestParams, File file, String type,
                                        OhFileCallBakListener callbackListener) {
        if (type == null) {
            type = "application/octet-stream";
        }
        // ??????????????? ????????????????????????
        // text/x-markdown; charset=utf-8
        FileRequestBody requestBody = new FileRequestBody(MediaType.parse(type), file, 0, callbackListener);
        // ???????????????????????????body ????????????
        MultipartBody.Builder multipartBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(AbFileUtil.getFileType(file), file.getName(), requestBody);
        // ??????post??????
        if (requestParams != null) {
            String key;
            Iterator<String> iterator = requestParams.getParams().keySet().iterator();
            while (iterator.hasNext()) {
                key = iterator.next();
                multipartBody.addFormDataPart(key, requestParams.get(key).toString());
            }
        }
        // ????????????
        Request.Builder builder = new Request.Builder().url(url).tag(url);// ??????tag
        if (headers != null) {
            builder.headers(headers);
        }
        builder.post(multipartBody.build());
        Request request = builder.build();
        callbackListener.ohType = 0;// ?????????????????????
        client.newCall(request).enqueue(new OKHttpCallBack(request, callbackListener));
        return requestBody;
    }


    /**
     * ????????????
     *
     * @param url
     * @param isBreakpoint     ????????????????????????
     * @param callbackListener
     */
    public DownLoad downFile(Context context, String url, final OhFileCallBakListener callbackListener) {
        boolean isLoading = false;
        synchronized (client.dispatcher().getClass()) { //????????????????????????????????????
            for (Call call : client.dispatcher().queuedCalls()) {
                if (url.equals(call.request().tag())) {
                    isLoading = true;
                    return null;
                }
            }
            for (Call call : client.dispatcher().runningCalls()) {
                if (url.equals(call.request().tag())) call.cancel();
            }
        }
        if (isLoading) {
            return null;
        }
        okhttp3.Request.Builder builder = new Request.Builder().url(url).tag(url).get();// ??????tag
        if (headers != null) {
            builder.headers(headers);
        }
        String id = Tools.setMD5(url);
        File file = new File(DOWNDIR, id + ".temp");
        if (file.exists()) {
            String total = "";
            JSONObject jsonObject = SQLTools.init(context).selectDownLoad(id);
            if (jsonObject != null && jsonObject.has("id")) {
                try {
                    total = jsonObject.getLong("totallength") + "";
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            builder.header("range", "bytes=" + file.length() + "-" + total);//????????????????????????????????????????????????
           AbLogUtil.d(OhHttpClient.class,"bytes=" + file.length() + "-" + total + "  ???????????????????????????" + jsonObject.toString());
        }

        Request request = builder.build();
        List<Interceptor> interceptors=client.networkInterceptors();
        for (int i=0;i<interceptors.size();i++){
            if(interceptors.get(i) instanceof DownInterceptor){
                break;
            }else if(i==interceptors.size()-1){
                client=client.newBuilder().addNetworkInterceptor(new DownInterceptor(callbackListener)).build();
            }
        }
        //???????????????
        destroyUrls.clear();
        DownLoad downLoad = new DownLoad(context);
        callbackListener.ohType = 1;// ?????????????????????
        client.newCall(request).enqueue(new OKHttpCallBack(request, downLoad, callbackListener));
        return downLoad;
    }

    /**
     * ???????????????okhttp????????????????????????
     */
    public class OKHttpCallBack implements Callback {
        private OhCallBackListener<Object> callbackListener;
        private Request request;
        private int failNum = 0;// ????????????
        public long time;//????????????
        private DownLoad downLoad;

        public OKHttpCallBack(Request request, OhCallBackListener<Object> callbackListener) {
            this.request = request;
            init(callbackListener);
        }

        public OKHttpCallBack(Request request, DownLoad downLoad, OhCallBackListener<Object> callbackListener) {
            this.request = request;
            this.downLoad = downLoad;
            init(callbackListener);
        }

        private void init(OhCallBackListener<Object> callbackListener) {
            if (callbackListener == null) {
                callbackListener = new OhObjectListener<String>() {

                    @Override
                    public void onSuccess(String content) {

                    }

                    @Override
                    public void onFinish() {

                    }

                    @Override
                    public void onFailure(int code, String content, Throwable error) {

                    }
                };
            }
            this.callbackListener = callbackListener;
            this.callbackListener.setHandler(new ResponderHandler(callbackListener));
            // ??????hander
            this.callbackListener.sendStartMessage();//??????
        }


        @Override
        public void onFailure(Call call, IOException e) {
            if (SocketTimeoutException.class.equals(e.getCause())) {
                AbLogUtil.e(OhHttpClient.class, request.url().toString() + ", ???????????????,????????????");
                //??????????????????
                AbLogUtil.d(OhHttpClient.class, "????????????:" + request.url().toString() + ",??????" + failNum + "???");
                if (failNum > 2) {//???????????????0
                    callbackListener.sendFailureMessage(0,
                            "????????????:" + request.url().toString() + ",???????????????????????????" + failNum, null);
                    callbackListener.sendFinishMessage();

                    //?????????????????????????????????????????????
                    e.printStackTrace();
                    if (Looper.myLooper() == null) Looper.prepare();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            WjEventBus.getInit().post(OKHTTP_TIMEOUT, 0);
                        }
                    });
                } else {
                    if (Looper.myLooper() == null) Looper.prepare();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            client.newCall(request).enqueue(OKHttpCallBack.this);
                        }
                    });
                    failNum++;
                }

            } else {//code==-1 ??????????????????
                AbLogUtil.e(OhHttpClient.class, request.url().toString() + "," + e.getMessage());
                e.printStackTrace();
                callbackListener.sendFailureMessage(-1, e.getMessage(), e);
                WjEventBus.getInit().post(OKHTTP_FAILURE,e);//????????????????????????????????????
            }
        }

        @Override
        public void onResponse(final Call call, final Response response) throws IOException {
            final String url = request.url().toString();
            int code = response.code();
            // ??????????????????????????????????????? okhttp???????????????????????????
//			if (response.header("Content-Encoding") != null && response.header("Content-Encoding").equals("gzip")) {
//				body = gunzip(body);
//			}
//            WjEventBus.getInit().post(response.headers());
            if (code >=200 && code<300) {// ????????????????????? 206?????????
                if (callbackListener instanceof OhObjectListener) {// ??????sring?????????
                    ResponseBody responseBody = response.body();
                    Charset charset = UTF8;
                    assert responseBody != null;
                    MediaType contentType = responseBody.contentType();
                    if (contentType != null) {
                        charset = contentType.charset(UTF8);
                    }
                    assert charset != null;
                    String body = responseBody.source().readString(charset);
                    if (!String.class.equals(((OhObjectListener<Object>) callbackListener).classname)) {
                        try {
                            callbackListener.sendSuccessMessage(GsonUtil.getGson().fromJson(body, ((OhObjectListener<Object>) callbackListener).classname));
                        } catch (Exception e) {
                            e.printStackTrace();
                            AbLogUtil.e(OhHttpClient.class, ((OhObjectListener<Object>) callbackListener).classname + ";" + url + ",??????json???????????????" + body);
                            if (failNum == 3) {
                                ((OhObjectListener<Object>) callbackListener).onFailure(400, "??????????????????", e);
                            }
                            return;
                        }
                    } else
                        callbackListener.sendSuccessMessage(body);

                } else if (callbackListener instanceof OhFileCallBakListener) {// ?????????????????????
                    if (callbackListener.ohType == 0) {// ??????
                        String bodyError = Objects.requireNonNull(response.body()).string();
                        AbLogUtil.i(OhHttpClient.class, url + "," + bodyError);
                        callbackListener.sendSuccessMessage(bodyError);
                    } else if (callbackListener.ohType == 1) {// ??????
                        final String name = Tools.setMD5(url);
                        downLoad.saveFile(response, callbackListener, DOWNDIR, name + ".temp");
                        downLoad = null;
                        return;
                    }
                }
            } else if (code == 301 || code == 302) {// ??????
                AbLogUtil.d(OhHttpClient.class, url + ",??????" + failNum + "???");
                if (failNum > 2) {
                    callbackListener.sendFailureMessage(response.code(),
                            request.url().toString() + ",???????????????????????????" + failNum, null);
                    callbackListener.sendFinishMessage();
                } else {
                    if (Looper.myLooper() == null) Looper.prepare();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            client.newCall(request).enqueue(OKHttpCallBack.this);
                        }
                    });

                    failNum++;
                }

            } else if (code == 401) {// ????????????
                client.newBuilder().authenticator(new Authenticator() {

                    @Override
                    public Request authenticate(Route route, Response response) throws IOException {
                        String credential = Credentials.basic("user", "password");
                        return response.request().newBuilder().header("Authorization", credential).build();
                    }
                });
                String bodyError = Objects.requireNonNull(response.body()).string();
                callbackListener.sendFailureMessage(code, bodyError, null);
                AbLogUtil.e(OhHttpClient.class, url + "," + bodyError);
            } else if (code == 404) {// ???????????????????????????
                String bodyError = Objects.requireNonNull(response.body()).string();
                callbackListener.sendFailureMessage(code, bodyError, null);
                AbLogUtil.e(OhHttpClient.class, url + "," + bodyError);
            } else {
                String bodyError = Objects.requireNonNull(response.body()).string();
                callbackListener.sendFailureMessage(code, bodyError, null);
                AbLogUtil.e(OhHttpClient.class, url + "," + bodyError);
            }


            try {
                callbackListener.sendFinishMessage();
//                AbFileUtil.writeAppend("jk", "\n??????:" + request.url().toString() + ",??????:" + (System.currentTimeMillis() - time));
            } catch (Exception e) {
                e.printStackTrace();
                AbLogUtil.i(getClass(), url + ",??????");
            }
        }
    }


    /**
     * ?????????????????????
     */
    @SuppressLint("HandlerLeak")
    private class ResponderHandler implements OhCallBackMessageInterface {

        /**
         * ????????????.
         */
        private Object[] response;

        /**
         * ??????????????????.
         */
        private OhCallBackListener<Object> responseListener;

        /**
         * ??????????????????.
         */
        public ResponderHandler(OhCallBackListener<Object> responseListener) {
            this.responseListener = responseListener;
        }

        /**
         * ????????????
         *
         * @param what
         * @param response
         */
        private void callBack(int what) {
            switch (what) {
                case SUCCESS_MESSAGE:// ??????
                    if (responseListener instanceof OhObjectListener) {// ??????????????????
                        ((OhObjectListener<Object>) responseListener).onSuccess(
                                response[0]);
                    } else if (responseListener instanceof OhFileCallBakListener) {// ??????
                        ((OhFileCallBakListener) responseListener).onSuccess(
                                (String) response[0]);
                    }
                    break;
                case FAILURE_MESSAGE:// ??????
                    if (responseListener instanceof OhObjectListener) {// ??????????????????
                        ((OhObjectListener<Object>) responseListener).onFailure((Integer) response[0],
                                (String) response[1], null);
                    } else if (responseListener instanceof OhFileCallBakListener) {// ??????
                        ((OhFileCallBakListener) responseListener).onFailure(response[0] + "",
                                response[1]+"");
                    }
                    break;
                case ERROE_MESSAGE:// ??????
                    if (responseListener instanceof OhObjectListener) {// ??????????????????
                        ((OhObjectListener<Object>) responseListener).onFailure(-1, "??????", (Exception) response[0]);
                    } else if (responseListener instanceof OhFileCallBakListener) {// ??????
                        ((OhFileCallBakListener) responseListener).onError((Exception) response[0]);
                    }
                    break;
                case PROGRESS_MESSAGE:// ????????????
                    if (response != null && response.length >= 2) {
                        ((OhFileCallBakListener) responseListener).onRequestProgress((Long) response[0], (Long) response[1],
                                (Boolean) response[2]);
                    } else {
                        AbLogUtil.e(OhHttpClient.class, "PROGRESS_MESSAGE " + AbAppConfig.MISSING_PARAMETERS);
                    }
                    break;
                case FINSH_MESSAGE:// ??????
                    if (responseListener instanceof OhObjectListener) {// ??????????????????
                        ((OhObjectListener<Object>) responseListener).onFinish();
                    } else if (responseListener instanceof OhFileCallBakListener) {// ??????
                        ((OhFileCallBakListener) responseListener).onFinish();
                    }
                    break;
                case START_MESSAGE://??????
                    if (responseListener instanceof OhObjectListener) {// ??????????????????
                        ((OhObjectListener<Object>) responseListener).onStart();
                    }
                    break;
            }
        }

        @Override
        public void handedMessage(final Message msg) {
            if (Looper.myLooper() == null) Looper.prepare();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    response = (Object[]) msg.obj;
                    callBack(msg.what);
                }
            });

        }
    }

    /**
     * ??????gzip????????????
     */
    public static String gzip(String primStr) {
        if (primStr == null || primStr.length() == 0) {
            return primStr;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        GZIPOutputStream gzip = null;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(primStr.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (gzip != null) {
                try {
                    gzip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new String(out.toByteArray());
    }

    /**
     * gzip???????????????
     */
    public static String gunzip(String compressedStr) {
        if (compressedStr == null) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = null;
        GZIPInputStream ginzip = null;
        byte[] compressed = null;
        String decompressed = null;
        try {
            compressed = compressedStr.getBytes();
            in = new ByteArrayInputStream(compressed);
            ginzip = new GZIPInputStream(in);

            byte[] buffer = new byte[1024];
            int offset = -1;
            while ((offset = ginzip.read(buffer)) != -1) {
                out.write(buffer, 0, offset);
            }
            decompressed = out.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ginzip != null) {
                try {
                    ginzip.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return decompressed;
    }

}
