//
// Copyright (c) 2019, Salesforce.com, inc.
// All rights reserved.
// SPDX-License-Identifier: BSD-3-Clause
// For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
//

package com.salesforce.nimbus.bridge.tests.webview

import android.util.Log
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.test.rule.ActivityTestRule
import com.salesforce.nimbus.BoundMethod
import com.salesforce.nimbus.JSONEncodable
import com.salesforce.nimbus.Plugin
import com.salesforce.nimbus.PluginOptions
import com.salesforce.nimbus.bridge.tests.CallbackTestPlugin
import com.salesforce.nimbus.bridge.tests.WebViewActivity
import com.salesforce.nimbus.bridge.tests.webViewBinder
import com.salesforce.nimbus.bridge.webview.bridge
import com.salesforce.nimbus.bridge.webview.broadcastMessage
import kotlinx.serialization.Serializable
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WebViewMochaTests {

    data class MochaMessage(val stringField: String = "This is a string", val intField: Int = 42) : JSONEncodable {
        override fun encode(): String {
            val jsonObject = JSONObject()
            jsonObject.put("stringField", stringField)
            jsonObject.put("intField", intField)
            return jsonObject.toString()
        }
    }

    @Serializable
    data class SerializableMochaMessage(val stringField: String = "This is a string", val intField: Int = 42)

    @PluginOptions(name = "mochaTestBridge")
    class MochaTestBridge(private val webView: WebView) : Plugin {

        val readyLatch = CountDownLatch(1)
        val completionLatch = CountDownLatch(1)

        // Set to -1 initially to indicate we never got a completion callback
        var failures = -1

        @BoundMethod
        fun ready() {
            readyLatch.countDown()
        }

        @BoundMethod
        fun testsCompleted(failures: Int) {
            this.failures = failures
            completionLatch.countDown()
        }
        @BoundMethod
        fun onTestFail(testTitle: String, errMessage: String) {
            Log.e("MOCHA", "[$testTitle]: $errMessage")
        }

        @BoundMethod
        fun sendMessage(name: String, includeParam: Boolean) {
            webView.post {
                var arg: JSONEncodable? = null
                if (includeParam) {
                    arg = MochaMessage()
                }
                webView.broadcastMessage(name, arg)
            }
        }
    }

    @Rule
    @JvmField
    val activityRule: ActivityTestRule<WebViewActivity> = ActivityTestRule<WebViewActivity>(
        WebViewActivity::class.java, false, true)

    @Test
    fun runMochaTests() {

        val webView = activityRule.activity.webView
        val testBridge =
            MochaTestBridge(
                webView
            )
        val jsAPITest = JSAPITestPlugin()

        runOnUiThread {
            webView.bridge {
                bind { CallbackTestPlugin().webViewBinder() }
                bind { testBridge.webViewBinder() }
                bind { jsAPITest.webViewBinder() }
            }
            webView.loadUrl("file:///android_asset/test-www/index.html")
        }

        assertTrue(testBridge.readyLatch.await(30, TimeUnit.SECONDS))

        runOnUiThread {
            webView.evaluateJavascript("""
            const titleFor = x => x.parent ? (titleFor(x.parent) + " " + x.title) : x.title
            mocha.run(failures => { __nimbus.plugins.mochaTestBridge.testsCompleted(failures); })
                 .on('fail', (test, err) => __nimbus.plugins.mochaTestBridge.onTestFail(titleFor(test), err.message));
            true;
            """.trimIndent()) {}
        }

        assertTrue(testBridge.completionLatch.await(30, TimeUnit.SECONDS))

        assertEquals(0, testBridge.failures)
    }
}

@Serializable
data class JSAPITestStruct(var stringField: String = "JSAPITEST", var intField: Int = 42)

@PluginOptions(name = "jsapiTestPlugin")
class JSAPITestPlugin : Plugin {

    @BoundMethod
    fun nullaryResolvingToInt(): Int {
        return 5
    }

    @BoundMethod
    fun nullaryResolvingToIntArray(): Array<Int> {
        return arrayOf(1, 2, 3)
    }

    @BoundMethod
    fun nullaryResolvingToObject(): JSAPITestStruct {
        return JSAPITestStruct()
    }

    @BoundMethod
    fun unaryResolvingToVoid(param: Int) {
        assertEquals(param, 5)
    }

    @BoundMethod
    fun unaryObjectResolvingToVoid(param: JSAPITestStruct) {
        assertEquals(param, JSAPITestStruct())
    }

    @BoundMethod
    fun binaryResolvingToIntCallback(param0: Int, param1: (result: Int) -> Unit) {
        assertEquals(param0, 5)
        param1(5)
    }

    @BoundMethod
    fun binaryResolvingToObjectCallback(param0: Int, param1: (result: JSAPITestStruct) -> Unit) {
        assertEquals(param0, 5)
        param1(JSAPITestStruct())
    }

    @BoundMethod
    fun binaryResolvingToObjectCallbackToInt(param0: Int, param1: (result: JSAPITestStruct) -> Unit): Int {
        assertEquals(param0, 5)
        param1(JSAPITestStruct())
        return 1
    }
}
