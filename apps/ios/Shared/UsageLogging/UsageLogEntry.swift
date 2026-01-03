import Foundation

struct UsageLogEntry: Codable {
    enum EntryType: String, Codable {
        case usageEvent
    }

    let id: Int64
    let createdAt: Date
    let type: EntryType
    let payload: UsageEventRecord
}

struct UsageLogBatch {
    let entries: [UsageLogEntry]
    let startOffset: UInt64
    let endOffset: UInt64
}
