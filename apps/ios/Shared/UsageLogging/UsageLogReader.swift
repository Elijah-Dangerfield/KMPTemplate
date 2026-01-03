import Foundation
import OSLog

final class UsageLogReader {
    private enum Keys {
        static let lastProcessedOffset = "usageLog.lastProcessedOffset"
    }

    private let fileURL: URL
    private let defaults: UserDefaults
    private let decoder = JSONDecoder()
    private let logger = Logger(subsystem: "com.dangerfield.merizo.usage", category: "UsageLogReader")

    init(fileManager: FileManager = .default, defaults: UserDefaults? = UserDefaults(suiteName: AppGroup.identifier)) throws {
        self.defaults = defaults ?? UserDefaults.standard
        let container = try AppGroup.containerURL(fileManager: fileManager)
        let logsFolder = container.appendingPathComponent("Logs", isDirectory: true)
        self.fileURL = logsFolder.appendingPathComponent("usage-events.log", conformingTo: .text)
        decoder.dateDecodingStrategy = .iso8601
    }

    func nextBatch(maxBytes: Int = 256 * 1024) throws -> UsageLogBatch? {
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            logger.debug("Usage log file missing; nothing to read")
            return nil
        }
        let handle = try FileHandle(forReadingFrom: fileURL)
        let startOffset = storedOffset()
        try handle.seek(toOffset: startOffset)
        let data = try handle.readToEnd() ?? Data()
        try handle.close()
        if data.isEmpty {
            logger.debug("Usage log empty at offset \(startOffset, privacy: .public)")
            return nil
        }
        let decoded = try decodeEntries(from: data)
        guard !decoded.entries.isEmpty else {
            logger.debug("Decoded 0 entries from usage log chunk starting at \(startOffset, privacy: .public)")
            return nil
        }
        let endOffset = startOffset + decoded.consumedBytes
        logger.debug("Read \(decoded.entries.count, privacy: .public) entries [offset \(startOffset, privacy: .public) ..< \(endOffset, privacy: .public)]")
        return UsageLogBatch(entries: decoded.entries, startOffset: startOffset, endOffset: endOffset)
    }

    func acknowledge(batch: UsageLogBatch) throws {
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            persistOffset(0)
            logger.debug("Log file missing during ack; reset offset to 0")
            return
        }
        let fileSize = try FileManager.default.attributesOfItem(atPath: fileURL.path)[.size] as? NSNumber
        let totalBytes = UInt64(fileSize?.uint64Value ?? 0)
        guard batch.endOffset <= totalBytes else {
            persistOffset(0)
            logger.error("Batch end offset \(batch.endOffset, privacy: .public) exceeded file size \(totalBytes, privacy: .public); resetting offset")
            return
        }
        if batch.endOffset == totalBytes {
            try FileManager.default.removeItem(at: fileURL)
            persistOffset(0)
            logger.debug("Acknowledged entire file; removed usage log")
        } else {
            persistOffset(batch.endOffset)
            logger.debug("Acknowledged batch ending at offset \(batch.endOffset, privacy: .public)")
        }
    }

    private func decodeEntries(from data: Data) throws -> (entries: [UsageLogEntry], consumedBytes: UInt64) {
        guard let chunk = String(data: data, encoding: .utf8) else {
            throw UsageLogError.invalidPayload
        }
        var entries: [UsageLogEntry] = []
        var consumedBytes: UInt64 = 0
        for line in chunk.split(separator: "\n", omittingEmptySubsequences: true) {
            guard let lineData = line.appending("\n").data(using: .utf8) else { continue }
            if let entry = try? decoder.decode(UsageLogEntry.self, from: Data(line.utf8)) {
                entries.append(entry)
            }
            consumedBytes += UInt64(lineData.count)
        }
        return (entries, consumedBytes)
    }

    private func storedOffset() -> UInt64 {
        let storedValue = defaults.object(forKey: Keys.lastProcessedOffset) as? Double ?? 0
        return storedValue < 0 ? 0 : UInt64(storedValue)
    }

    private func persistOffset(_ offset: UInt64) {
        defaults.set(Double(offset), forKey: Keys.lastProcessedOffset)
    }
}
