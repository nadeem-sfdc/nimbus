//
// Copyright (c) 2019, Salesforce.com, inc.
// All rights reserved.
// SPDX-License-Identifier: BSD-3-Clause
// For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
//

import UIKit
import WebKit

import Nimbus

class ViewController: UIViewController, WKNavigationDelegate {
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        title = "Nimbus"

        bridge.addExtension(device)
    }

    override func loadView() {
        view = webView

        let url = Bundle.main.url(forResource: "ip", withExtension: "txt")
            .flatMap { try? String(contentsOf: $0) }
            .flatMap { $0.trimmingCharacters(in: CharacterSet.newlines) }
            .flatMap { URL(string: "http://\($0):3000") }
            ?? URL(string: "http://localhost:3000")!

        bridge.attach(to: webView)
        webView.navigationDelegate = self
        webView.load(URLRequest(url: url))
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        device.getWebInfo { err, result in
            if let err = err {
                NSLog("Error: \(err)")
            } else if let result = result {
                NSLog("userAgent: \(result["userAgent"]!)")
                NSLog("href: \(result["href"]!)")
            }
        }
    }

    lazy var webView = WKWebView(frame: .zero)
    let bridge = NimbusBridge()
    let device = DeviceExtension()
}
