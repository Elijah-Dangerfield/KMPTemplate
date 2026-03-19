import DeviceActivity
import Foundation
import OSLog
import UniformTypeIdentifiers

// MARK: - App Group Helpers

enum AppGroup {
    static let identifier = "group.com.kmptemplate"
    private static let logger = Logger(subsystem: "com.kmptemplate.monitor", category: "AppGroup")

    static func containerURL(fileManager: FileManager = .default) throws -> URL {
        guard let containerURL = fileManager.containerURL(forSecurityApplicationGroupIdentifier: identifier) else {
            logger.error("Failed to resolve App Group container for \(identifier, privacy: .public)")
            throw UsageLogError.containerMissing
        }
        logger.debug("Resolved App Group container: \(containerURL.path, privacy: .public)")
        return containerURL
    }
}

enum UsageLogError: Error {
    case containerMissing
    case invalidPayload
    case ioFailure(Error)
}

// MARK: - Configuration Store Mirrors

/// Serialized metadata that allows extensions to map DeviceActivity callbacks back to rules/selections.
struct DeviceActivitySelectionRecord: Codable {
    enum Kind: String, Codable {
        case application
        case category
        case webDomain
    }

    let id: String
    let kind: Kind
    let displayName: String?
    let bundleIdentifier: String?
    let encodedToken: String?
}

struct DeviceActivityConfigurationRecord: Codable {
    let activityName: String
    let ruleId: String
    let selections: [DeviceActivitySelectionRecord]
    let durationThresholdSeconds: Double?
    let maxDailyOpens: Int?
    let metadata: [String: String]?
}

// Mirrors the shared UsageEventSource enum so the extension can encode logs without linking other targets.
enum UsageEventSource: String, Codable {
    case deviceActivity = "DEVICE_ACTIVITY"
    case usageStats = "USAGE_STATS"
    case manualEntry = "MANUAL_ENTRY"
    case unknown = "UNKNOWN"
}

/// Lightweight copy of UsageEventRecord for extension-only logging.
struct UsageEventRecord: Codable {
    let selectionId: String
    let bundleIdentifier: String
    let displayName: String
    let openedAt: Date
    let closedAt: Date?
    let duration: TimeInterval?
    let openCountDelta: Int
    let source: UsageEventSource
    let metadata: [String: String]

    private enum CodingKeys: String, CodingKey {
        case selectionId
        case bundleIdentifier
        case displayName
        case openedAt
        case closedAt
        case duration
        case openCountDelta
        case source
        case metadata
    }

    init(
        selectionId: String,
        bundleIdentifier: String,
        displayName: String,
        openedAt: Date,
        closedAt: Date? = nil,
        duration: TimeInterval? = nil,
        openCountDelta: Int,
        source: UsageEventSource = .deviceActivity,
        metadata: [String: String] = [:]
    ) {
        self.selectionId = selectionId
        self.bundleIdentifier = bundleIdentifier
        self.displayName = displayName
        self.openedAt = openedAt
        self.closedAt = closedAt
        self.duration = duration
        self.openCountDelta = openCountDelta
        self.source = source
        self.metadata = metadata
    }
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let selectionId = try container.decode(String.self, forKey: .selectionId)
        let bundleIdentifier = try container.decodeIfPresent(String.self, forKey: .bundleIdentifier) ?? selectionId
        let displayName = try container.decodeIfPresent(String.self, forKey: .displayName) ?? bundleIdentifier
        self.selectionId = selectionId
        self.bundleIdentifier = bundleIdentifier
        self.displayName = displayName
        self.openedAt = try container.decode(Date.self, forKey: .openedAt)
        self.closedAt = try container.decodeIfPresent(Date.self, forKey: .closedAt)
        self.duration = try container.decodeIfPresent(TimeInterval.self, forKey: .duration)
        self.openCountDelta = try container.decode(Int.self, forKey: .openCountDelta)
        self.source = try container.decode(UsageEventSource.self, forKey: .source)
        self.metadata = try container.decodeIfPresent([String: String].self, forKey: .metadata) ?? [:]
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(selectionId, forKey: .selectionId)
        if bundleIdentifier != selectionId {
            try container.encode(bundleIdentifier, forKey: .bundleIdentifier)
        }
        if displayName != bundleIdentifier && displayName != selectionId {
            try container.encode(displayName, forKey: .displayName)
        }
        try container.encode(openedAt, forKey: .openedAt)
        try container.encodeIfPresent(closedAt, forKey: .closedAt)
        try container.encodeIfPresent(duration, forKey: .duration)
        try container.encode(openCountDelta, forKey: .openCountDelta)
        try container.encode(source, forKey: .source)
        if !metadata.isEmpty {
            try container.encode(metadata, forKey: .metadata)
        }
    }
}

final class DeviceActivityConfigurationStore {
    private let fileURL: URL
    private let decoder = JSONDecoder()
    private let logger = Logger(subsystem: "com.kmptemplate.monitor", category: "DeviceActivityConfigurationStore")

    init(fileManager: FileManager = .default) throws {
        let container = try AppGroup.containerURL(fileManager: fileManager)
        let directory = container.appendingPathComponent("Monitoring", isDirectory: true)
        if !fileManager.fileExists(atPath: directory.path) {
            try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
            logger.debug("Created Monitoring directory at \(directory.path, privacy: .public)")
        }
        self.fileURL = directory.appendingPathComponent("device-activity-config.json", conformingTo: .json)
    }

    func configuration(for activityName: DeviceActivityName) -> DeviceActivityConfigurationRecord? {
        guard let data = try? Data(contentsOf: fileURL), !data.isEmpty else {
            logger.error("Configuration file missing or empty when resolving \(activityName.rawValue, privacy: .public)")
            return nil
        }
        let records = (try? decoder.decode([DeviceActivityConfigurationRecord].self, from: data)) ?? []
        let record = records.first { $0.activityName == activityName.rawValue }
        if let record {
            logger.debug("Loaded configuration for activity \(activityName.rawValue, privacy: .public) with \(record.selections.count, privacy: .public) selections")
        } else {
            logger.warning("No configuration record found for activity \(activityName.rawValue, privacy: .public)")
        }
        return record
    }
}

// MARK: - Usage Log Writer Mirror

struct UsageLogEntry: Codable {
    enum EntryType: String, Codable {
        case usageEvent
    }

    let id: Int64
    let createdAt: Date
    let type: EntryType
    let payload: UsageEventRecord
}

final class UsageLogWriter {
    private enum Keys {
        static let nextRecordId = "usageLog.nextRecordId"
    }

    private let queue = DispatchQueue(label: "com.kmptemplate.usage.log.writer", qos: .utility)
    private let fileURL: URL
    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let logger = Logger(subsystem: "com.kmptemplate.monitor", category: "UsageLogWriter")

    init(fileManager: FileManager = .default, defaults: UserDefaults? = UserDefaults(suiteName: AppGroup.identifier)) throws {
        self.defaults = defaults ?? .standard
        let container = try AppGroup.containerURL(fileManager: fileManager)
        let logsFolder = container.appendingPathComponent("Logs", isDirectory: true)
        if !fileManager.fileExists(atPath: logsFolder.path) {
            try fileManager.createDirectory(at: logsFolder, withIntermediateDirectories: true)
            logger.debug("Created Logs directory at \(logsFolder.path, privacy: .public)")
        }
        self.fileURL = logsFolder.appendingPathComponent("usage-events.log", conformingTo: .text)
        encoder.dateEncodingStrategy = .iso8601
        logger.debug("Usage log file initialized at \(self.fileURL.path, privacy: .public)")
    }

    func append(records: [UsageEventRecord]) {
        guard !records.isEmpty else { return }
        let recordCount = records.count
        logger.debug("Scheduling append for \(recordCount, privacy: .public) usage records")
        queue.async { [weak self] in
            guard let self else { return }
            do {
                try self.ensureLogFileExists()
                let handle = try FileHandle(forWritingTo: self.fileURL)
                try handle.seekToEnd()
                var nextId = self.nextRecordId()
                for record in records {
                    let entry = UsageLogEntry(
                        id: nextId,
                        createdAt: Date(),
                        type: .usageEvent,
                        payload: record
                    )
                    let data = try self.encoder.encode(entry)
                    if let jsonLine = String(data: data, encoding: .utf8)?.appending("\n").data(using: .utf8) {
                        try handle.write(contentsOf: jsonLine)
                    }
                    nextId += 1
                }
                try handle.close()
                self.defaults.set(nextId, forKey: Keys.nextRecordId)
                self.logger.info("Appended \(recordCount, privacy: .public) usage log entries through id \(nextId - 1, privacy: .public)")
            } catch {
                self.logger.error("Failed to append usage log entries: \(error.localizedDescription, privacy: .public)")
            }
        }
    }

    private func ensureLogFileExists() throws {
        if !FileManager.default.fileExists(atPath: self.fileURL.path) {
            FileManager.default.createFile(atPath: self.fileURL.path, contents: nil)
            logger.debug("Created usage log file at \(self.fileURL.path, privacy: .public)")
        }
    }

    private func nextRecordId() -> Int64 {
        let current = defaults.object(forKey: Keys.nextRecordId) as? Int64 ?? 1
        return current
    }
}
