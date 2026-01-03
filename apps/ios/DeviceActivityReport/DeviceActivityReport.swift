//
//  DeviceActivityReport.swift
//  DeviceActivityReport
//
//  Created by Elijah Dangerfield on 12/16/25.
//

import DeviceActivity
import ExtensionKit
import SwiftUI

@main
struct DeviceActivityReport: DeviceActivityReportExtension {
    var body: some DeviceActivityReportScene {
        // Create a report for each DeviceActivityReport.Context that your app supports.
        TotalActivityReport { totalActivity in
            TotalActivityView(totalActivity: totalActivity)
        }
        // Add more reports here...
    }
}
