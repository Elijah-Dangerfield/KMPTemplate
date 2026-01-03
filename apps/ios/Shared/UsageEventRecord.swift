import Foundation

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
