import DeviceActivity
import Foundation
import OSLog

/// Converts DeviceActivity callbacks into UsageEventRecords that the host app ingests later.
final class DeviceActivityMonitorExtension: DeviceActivityMonitor {
    private let logger = Logger(subsystem: "com.dangerfield.merizo.monitor", category: "ActivityMonitor")
    private lazy var configurationStore = try? DeviceActivityConfigurationStore()
    private lazy var logWriter = try? UsageLogWriter()
    private let intervalTracker = IntervalTracker()

    override func intervalDidStart(for activity: DeviceActivityName) {
        super.intervalDidStart(for: activity)
        intervalTracker.trackStart(for: activity, date: Date())
        logger.debug("Interval started for \(activity.rawValue, privacy: .public)")
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
        let context = intervalTracker.consume(for: activity, now: Date())
        logger.info("Interval ended for \(activity.rawValue, privacy: .public); start=\(String(describing: context.startDate), privacy: .public) duration=\(String(describing: context.duration), privacy: .public)")
        recordUsage(
            for: activity,
            event: .intervalEnded(startDate: context.startDate, duration: context.duration)
        )
    }

    override func eventDidReachThreshold(_ event: DeviceActivityEvent.Name, activity: DeviceActivityName) {
        super.eventDidReachThreshold(event, activity: activity)
        logger.info("Threshold reached for \(activity.rawValue, privacy: .public); event=\(event.rawValue, privacy: .public)")
        recordUsage(
            for: activity,
            event: .thresholdReached(event: event)
        )
    }

    private func recordUsage(for activity: DeviceActivityName, event: MonitorEvent) {
        guard let configuration = configurationStore?.configuration(for: activity) else {
            logger.error("Missing configuration for activity \(activity.rawValue, privacy: .public); dropping event")
            return
        }

        guard !configuration.selections.isEmpty else {
            logger.warning("Configuration for \(activity.rawValue, privacy: .public) has zero selections; skipping log write")
            return
        }

        guard let writer = logWriter else {
            logger.error("UsageLogWriter unavailable; cannot persist event for \(activity.rawValue, privacy: .public)")
            return
        }

        let now = Date()
        let openedAt = event.openedAt(now: now, configuration: configuration)
        let closedAt = event.closedAt(now: now)
        let duration = event.duration(using: configuration)
        let eventMetadata = event.metadata(activityName: activity.rawValue)

        let records: [UsageEventRecord] = configuration.selections.map { selection in
            var metadata = configuration.metadata ?? [:]
            metadata["ruleId"] = configuration.ruleId
            metadata["activityName"] = configuration.activityName
            metadata["selectionKind"] = selection.kind.rawValue
            metadata.merge(eventMetadata) { _, new in new }
            return UsageEventRecord(
                selectionId: selection.id,
                bundleIdentifier: selection.bundleIdentifier ?? selection.id,
                displayName: selection.displayName ?? selection.bundleIdentifier ?? selection.id,
                openedAt: openedAt,
                closedAt: closedAt,
                duration: duration,
                openCountDelta: event.openCountDelta,
                metadata: metadata
            )
        }

        writer.append(records: records)
        let ruleId = configuration.ruleId ?? "unknown"
        logger.debug("Appended \(records.count, privacy: .public) usage records for rule=\(ruleId, privacy: .public) activity=\(activity.rawValue, privacy: .public)")
    }
}

private enum MonitorEvent {
    case intervalEnded(startDate: Date?, duration: TimeInterval?)
    case thresholdReached(event: DeviceActivityEvent.Name)

    var openCountDelta: Int { 0 }

    func openedAt(now: Date, configuration: DeviceActivityConfigurationRecord) -> Date {
        switch self {
        case let .intervalEnded(startDate, _):
            return startDate ?? now
        case .thresholdReached:
            guard let duration = configuration.durationThresholdSeconds, duration > 0 else {
                return now
            }
            return now.addingTimeInterval(-duration)
        }
    }

    func closedAt(now: Date) -> Date? {
        now
    }

    func duration(using configuration: DeviceActivityConfigurationRecord) -> TimeInterval? {
        switch self {
        case let .intervalEnded(_, duration):
            return duration
        case .thresholdReached:
            return configuration.durationThresholdSeconds
        }
    }

    func metadata(activityName: String) -> [String: String] {
        switch self {
        case .intervalEnded:
            return ["monitorEvent": "interval-ended", "deviceActivityName": activityName]
        case let .thresholdReached(event):
            return [
                "monitorEvent": "threshold-reached",
                "deviceActivityName": activityName,
                "deviceActivityEvent": event.rawValue
            ]
        }
    }
}

private final class IntervalTracker {
    private let defaults = UserDefaults(suiteName: AppGroup.identifier)
    private let keyPrefix = "monitor.interval.start."

    func trackStart(for activity: DeviceActivityName, date: Date) {
        defaults?.set(date.timeIntervalSince1970, forKey: key(for: activity))
    }

    func consume(for activity: DeviceActivityName, now: Date) -> (startDate: Date?, duration: TimeInterval?) {
        guard let defaults else { return (nil, nil) }
        let key = key(for: activity)
        guard let storedValue = defaults.object(forKey: key) as? Double else {
            return (nil, nil)
        }
        let timestamp = storedValue
        defaults.removeObject(forKey: key)
        guard timestamp > 0 else { return (nil, nil) }
        let startDate = Date(timeIntervalSince1970: timestamp)
        let duration = max(0, now.timeIntervalSince(startDate))
        return (startDate, duration)
    }

    private func key(for activity: DeviceActivityName) -> String {
        keyPrefix + activity.rawValue
    }
}
