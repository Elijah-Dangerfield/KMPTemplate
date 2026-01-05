import Foundation

enum AppGroup {
    static let identifier = "group.com.dangerfield.goodtimes"

    static func containerURL(fileManager: FileManager = .default) throws -> URL {
        guard let containerURL = fileManager.containerURL(forSecurityApplicationGroupIdentifier: identifier) else {
            throw UsageLogError.containerMissing
        }
        return containerURL
    }
}

enum UsageLogError: Error {
    case containerMissing
    case invalidPayload
    case ioFailure(Error)
}
