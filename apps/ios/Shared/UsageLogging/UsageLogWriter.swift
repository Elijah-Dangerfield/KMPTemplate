import Foundation
import OSLog

final class UsageLogWriter {
    private enum Keys {
        static let nextRecordId = "usageLog.nextRecordId"
    }

    private let queue = DispatchQueue(label: "com.kmptemplate.usage.log.writer", qos: .utility)
    private let fileURL: URL
    private let defaults: UserDefaults
    private let encoder = JSONEncoder()
    private let logger = Logger(subsystem: "com.kmptemplate.usage", category: "UsageLogWriter")

    init(fileManager: FileManager = .default, defaults: UserDefaults? = UserDefaults(suiteName: AppGroup.identifier)) throws {
        self.defaults = defaults ?? UserDefaults.standard
        let container = try AppGroup.containerURL(fileManager: fileManager)
        let logsFolder = container.appendingPathComponent("Logs", isDirectory: true)
        if !fileManager.fileExists(atPath: logsFolder.path) {
            try fileManager.createDirectory(at: logsFolder, withIntermediateDirectories: true)
        }
        self.fileURL = logsFolder.appendingPathComponent("usage-events.log", conformingTo: .text)
        encoder.dateEncodingStrategy = .iso8601
    }

    func append(records: [UsageEventRecord]) {
        guard !records.isEmpty else { return }
        queue.async { [weak self] in
            guard let self else { return }
            do {
                self.logger.debug("Preparing to append \(records.count, privacy: .public) records to usage log")
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
                self.logger.debug("Appended usage records through id \(nextId - 1, privacy: .public)")
            } catch {
                self.logger.error("Failed to append usage log records: \(error.localizedDescription, privacy: .public)")
            }
        }
    }

    private func ensureLogFileExists() throws {
        if !FileManager.default.fileExists(atPath: fileURL.path) {
            FileManager.default.createFile(atPath: fileURL.path, contents: nil)
        }
    }

    private func nextRecordId() -> Int64 {
        let current = defaults.object(forKey: Keys.nextRecordId) as? Int64 ?? 1
        return current
    }
}
