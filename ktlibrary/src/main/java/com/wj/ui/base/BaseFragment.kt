package com.wj.ui.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * @author wangjun
 * @version 1.0
 * @date 2017/12/30
 */

abstract class BaseFragment : androidx.fragment.app.Fragment() {
    var content_view: View?=null
    var isInit = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        content_view = inflater.inflate(setContentView(), container, false)
        return content_view
    }
    override fun onStart() {
        super.onStart()
        if (!isInit) {
            isInit = true
            init()
        }
    }

    /**
     * 设置参数
     */
    abstract fun setContentView(): Int

    /**
     * 初始化
     */
    abstract fun init()
}
