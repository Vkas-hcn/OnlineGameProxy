package com.vkas.onlinegameproxy.base

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivityNew  : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())
        initView()
        initData()
        setupListeners()
    }

    abstract fun getLayoutId(): Int

    open fun initView() {
        // 初始化视图，可在子类中重写
    }

    open fun initData() {
        // 初始化数据，可在子类中重写
    }

    open fun setupListeners() {
        // 设置监听器，可在子类中重写
    }

    protected fun <T : View> bindView(viewId: Int): Lazy<T> {
        @Suppress("UNCHECKED_CAST")
        return lazy { findViewById<T>(viewId) }
    }
}