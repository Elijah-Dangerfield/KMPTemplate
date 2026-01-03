import Foundation

/// Mirrors Kotlin's UsageSource enum so Swift-only code can serialize sources without linking the Compose runtime.
enum UsageEventSource: String, Codable {
    case deviceActivity = "DEVICE_ACTIVITY"
    case usageStats = "USAGE_STATS"
    case manualEntry = "MANUAL_ENTRY"
    case unknown = "UNKNOWN"

    static func from(raw: String?) -> UsageEventSource {
        guard let raw else { return .unknown }
        return UsageEventSource(rawValue: raw.uppercased()) ?? .unknown
    }
}
