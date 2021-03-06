package com.wj.ktolin.baselibrary

import android.Manifest
import android.graphics.PixelFormat
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.abase.okhttp.OhFileCallBakListener
import com.abase.okhttp.OhHttpClient
import com.abase.okhttp.OhObjectListener
import com.abase.okhttp.util.DownLoad
import com.abase.util.AbAppUtil
import com.abase.util.AbDoubleTool
import com.abase.util.ToastUtil
import com.wj.ktutils.WjSP
import com.abase.view.parent.BaseActivity
import com.abase.view.weight.RecyclerSpace
import com.wj.ktolin.baselibrary.weight.TestAdapter
import com.wj.permission.PermissionUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*

class MainActivityBase : BaseActivity(), View.OnClickListener {

    override fun setContentView(): Int {
        return R.layout.activity_main
    }

    private var cpc: Button? = null
    private var downLoad: DownLoad? = null
    private var textView: TextView? = null

    override fun init() {
        window.setFormat(PixelFormat.TRANSLUCENT)
//        AndroidKeyboardHeight.assistActivity(this)
        val start = findViewById<Button>(R.id.start)
        val pause = findViewById<Button>(R.id.pause)
        val stop = findViewById<Button>(R.id.stop)
        textView = findViewById(R.id.progress)

        start.setOnClickListener(this)
        pause.setOnClickListener(this)
        stop.setOnClickListener(this)

        recycler_list.addItemDecoration(RecyclerSpace(2, resources.getColor(R.color.colorPrimary)))
        recycler_list.adapter = TestAdapter()
        recycler_list.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 6)

//        QqWebHelper.X5Init(this)
//        web.loadUrl("https://baidu.com")
//        web.webMethodsListener=object : WebMethodsListener(){
//            override fun onX5GeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissionsCallback?): Boolean {
//                callback!!.invoke(origin, true, false)
//                return true
//            }
//        }
//        val intent = Intent("adasa")
//        intent.putExtra("data","asdas")
//        NotificationUtils(activity).setContentIntent(PendingIntent.getBroadcast(activity, (System.currentTimeMillis() / 1000).toInt(), intent, PendingIntent.FLAG_CANCEL_CURRENT))
//                .sendNotification((System.currentTimeMillis()/1000).toInt(),"","11111", R.mipmap.ic_launcher,  R.mipmap.ic_launcher)
//        Glide.with(this).load("https://n.sinaimg.cn/fashion/crawl/162/w550h412/20190814/594a-icapxpi5137199.jpg").preload()
//        WjSP.init(activity).setValues("123","1231")
        val result= WjSP.init(activity).getValues("123","")
        println(result+" ---------------------- ")
        OhHttpClient.getInit().setLogcat()
        OhHttpClient.getInit().get("https://www.baidu.com",object :OhObjectListener<String>(){
            override fun onFailure(code: Int, content: String?, error: Throwable?) {
            }

            override fun onSuccess(content: String?) {
            }

            override fun onFinish() {
            }

        })
    }

    //    override fun before() {
//        sonicFast(this, 1, "http://baidu.com")
//    }
    override fun onClick(view: View) {
        when (view.id) {
            R.id.start -> downLoad = OhHttpClient.getInit().downFile(this@MainActivityBase, "http://imtt.dd.qq.com/16891/apk/5BF428A44C92DDE2AEB3EE34E1785EB6.apk?fsname=com.wj.makebai_v1.5.1_38.apk&hsr=4d5s", object : OhFileCallBakListener() {
                override fun onSuccess(content: String) {
                    AbAppUtil.installApk(this@MainActivityBase, File(content))
                }

                override fun onFailure(code: String, content: String) {

                }

                override fun onError(e: Exception) {

                }

                override fun onFinish() {

                }

                override fun onRequestProgress(bytesWritten: Long, contentLength: Long, done: Boolean) {
                    textView!!.text = AbDoubleTool.div(bytesWritten.toDouble(), contentLength.toDouble()).toString() + "%"
                }
            })
            R.id.pause -> {
                ToastUtil.showTip(this, "asdasdasds")
                ToastUtil.showTip(this, "??????")
                if (downLoad != null) {
                    downLoad!!.cancle()
                }
            }
            R.id.stop -> {
                if (downLoad != null) {
                    downLoad!!.stop()
                }
//                OhHttpClient.getInit().isJsonFromMat=false
//                OhHttpClient.getInit().post("https://app-api-ali.zysc.dchost.cn/v1/content/recommend/palyer/recommend/more.api?authCode=5AFC725FAA7B4220AA3B6A4DD5A142C0&access_key=bcc00105941a4a128b5ffbb5ceed9794&areaCode=0001%2F0002%2F", OhHttpParams().put("name","sdf dasd12#"),object : OhObjectListener<String>(){
//                    override fun onFailure(code: Int, content: String?, error: Throwable?) {
//                    }
//
//                    override fun onSuccess(content: String?) {
//                    }
//
//                    override fun onFinish() {
//                    }
//
//                })
                PermissionUtils.permission(
                        activity!!,
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                )
                        .rationale(object : PermissionUtils.OnRationaleListener {
                            override fun rationale(shouldRequest: PermissionUtils.OnRationaleListener.ShouldRequest) {
                                shouldRequest.again(true)
                            }
                        })
                        .callback(object : PermissionUtils.FullCallback {
                            override fun onGranted(permissionsGranted: ArrayList<String>) {
                            }

                            override fun onDenied(permissionsDeniedForever: ArrayList<String>?, permissionsDenied: ArrayList<String>) {
                            }

                        }).request()

            }
        }
    }
}
