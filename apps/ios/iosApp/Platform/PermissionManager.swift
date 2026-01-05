//
//  PermissionDataSource.swift
//  iosApp
//
//  Created by Elijah Dangerfield on 11/24/25.
//
import Combine
import ComposeApp
import FamilyControls
import Foundation
import UserNotifications
import AVFoundation
import Photos

class IOSPermissionManager: GoodtimesPermissionManager {

    private let authorizationCenter = AuthorizationCenter.shared
    private let logger = KLog.shared
    private var lastLoggedFamilyStatus: AuthorizationStatus?
    private var authorizationStatusCancellable: AnyCancellable?

    init() {
        authorizationStatusCancellable = authorizationCenter.$authorizationStatus
            .receive(on: DispatchQueue.main)
            .sink { [weak self] status in
                self?.handleAuthorizationStatusChange(newStatus: status)
            }
    }

    deinit {
        authorizationStatusCancellable?.cancel()
    }
    
    func __ensurePermission(permission: GoodtimesPermission) async throws -> GoodtimesPermissionResult {
        let currentStatus = checkPermissionStatus(permission: permission)
        
        if currentStatus == .granted {
            return .Granted.shared
        }
        
        return try await __requestPermission(permission: permission)
    }
    
    func __requestPermission(permission: GoodtimesPermission) async throws -> GoodtimesPermissionResult {
        switch onEnum(of: permission) {
        case .appUsageStats:
            return await requestFamilyControlsPermission()
        case .notifications:
            return await requestNotificationsPermission()
        case .camera:
            return await requestCameraPermission()
        case .photoLibrary:
            return await requestPhotoLibraryPermission()
        }
    }
    
    func checkPermissionStatus(permission: GoodtimesPermission) -> GoodtimesPermissionStatus {
        switch onEnum(of: permission) {
        case .appUsageStats:
            return checkFamilyControlsStatus()
        case .notifications:
            return checkNotificationsStatus()
        case .camera:
            return checkCameraStatus()
        case .photoLibrary:
            return checkPhotoLibraryStatus()
        }
    }
    
    // MARK: - Family Controls
    
    private func checkFamilyControlsStatus() -> GoodtimesPermissionStatus {
        let status = authorizationCenter.authorizationStatus
        logFamilyStatusIfNeeded(status)
        switch status {
        case .notDetermined:
            return .notDetermined
        case .approved:
            return .granted
        case .denied:
            return .denied
        @unknown default:
            return .notDetermined
        }
    }
    
    private func requestFamilyControlsPermission() async -> GoodtimesPermissionResult {
        do {
            let preStatus = authorizationCenter.authorizationStatus
            
            logger.withTag(tag: "PermissionManager").i {
                "FamilyControls authorization request starting. Current status: \(self.authorizationStatusDescription(preStatus))"
            }
            
            try await authorizationCenter.requestAuthorization(for: .individual)
            
            let resolvedStatus = await resolveFamilyControlsStatus()
            logger.withTag(tag: "PermissionManager").i { "FamilyControls authorization request completed. Resolved status: \(self.authorizationStatusDescription(resolvedStatus))" }
        
            lastLoggedFamilyStatus = resolvedStatus
            
            switch resolvedStatus {
            case .approved:
                return .Granted.shared
            case .denied:
                // If denied after request, user can't request again through the app
                return .Denied(canRequestAgain: false)
            case .notDetermined:
                // Shouldn't happen, but treat as denied with retry
                return .Denied(canRequestAgain: true)
            @unknown default:
                return .Denied(canRequestAgain: true)
            }
        } catch {
            let message = "FamilyControls authorization request failed: \(error.localizedDescription)"
            logger.withTag(tag: "PermissionManager", ).e { message }
        
            // Error during request - user can try again
            return .Denied(canRequestAgain: true)
        }
    }

    private func resolveFamilyControlsStatus(
        pollIntervalNanoseconds: UInt64 = 200_000_000,
        timeoutSeconds: TimeInterval = 2
    ) async -> AuthorizationStatus {
        var status = authorizationCenter.authorizationStatus
        guard status == .notDetermined else { return status }

        let deadline = Date().addingTimeInterval(timeoutSeconds)
        while status == .notDetermined && Date() < deadline {
            try? await Task.sleep(nanoseconds: pollIntervalNanoseconds)
            status = authorizationCenter.authorizationStatus
        }

        return status
    }

    private func authorizationStatusDescription(_ status: AuthorizationStatus) -> String {
        switch status {
        case .notDetermined:
            return "notDetermined"
        case .approved:
            return "approved"
        case .denied:
            return "denied"
        @unknown default:
            return "unknown"
        }
    }

    private func logFamilyStatusIfNeeded(_ status: AuthorizationStatus) {
        guard lastLoggedFamilyStatus != status else { return }
        lastLoggedFamilyStatus = status
        logger.withTag(tag: "PermissionManager").i {
            "Reporting FamilyControls authorization status as \(self.authorizationStatusDescription(status))"
        }
    }

    private func handleAuthorizationStatusChange(newStatus status: AuthorizationStatus) {
        logger.withTag(tag: "PermissionManager").i {
            "AuthorizationCenter posted didChange notification; new status=\(self.authorizationStatusDescription(status))"
        }
      
        lastLoggedFamilyStatus = status
    }
    
    // MARK: - Notifications
    
    private func checkNotificationsStatus() -> GoodtimesPermissionStatus {
        var status: GoodtimesPermissionStatus = .notDetermined
        let semaphore = DispatchSemaphore(value: 0)
        
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .notDetermined:
                status = .notDetermined
            case .authorized, .provisional, .ephemeral:
                status = .granted
            case .denied:
                status = .denied
            @unknown default:
                status = .notDetermined
            }
            semaphore.signal()
        }
        
        semaphore.wait()
        return status
    }
    
    private func requestNotificationsPermission() async -> GoodtimesPermissionResult {
        do {
            let granted = try await UNUserNotificationCenter.current()
                .requestAuthorization(options: [.alert, .badge, .sound])
            
            if granted {
                return .Granted.shared
            } else {
                let settings = await UNUserNotificationCenter.current().notificationSettings()
                let canRequestAgain = settings.authorizationStatus == .notDetermined
                return .Denied(canRequestAgain: canRequestAgain)
            }
        } catch {
            return .Denied(canRequestAgain: true)
        }
    }
    
    // MARK: - Camera
    
    private func checkCameraStatus() -> GoodtimesPermissionStatus {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        switch status {
        case .notDetermined:
            return .notDetermined
        case .authorized:
            return .granted
        case .denied, .restricted:
            return .denied
        @unknown default:
            return .notDetermined
        }
    }
    
    private func requestCameraPermission() async -> GoodtimesPermissionResult {
        let granted = await AVCaptureDevice.requestAccess(for: .video)
        if granted {
            return .Granted.shared
        } else {
            let status = AVCaptureDevice.authorizationStatus(for: .video)
            let canRequestAgain = status == .notDetermined
            return .Denied(canRequestAgain: canRequestAgain)
        }
    }
    
    // MARK: - Photo Library
    
    private func checkPhotoLibraryStatus() -> GoodtimesPermissionStatus {
        let status = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        switch status {
        case .notDetermined:
            return .notDetermined
        case .authorized, .limited:
            return .granted
        case .denied, .restricted:
            return .denied
        @unknown default:
            return .notDetermined
        }
    }
    
    private func requestPhotoLibraryPermission() async -> GoodtimesPermissionResult {
        let status = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        switch status {
        case .authorized, .limited:
            return .Granted.shared
        case .denied, .restricted:
            return .Denied(canRequestAgain: false)
        case .notDetermined:
            return .Denied(canRequestAgain: true)
        @unknown default:
            return .Denied(canRequestAgain: true)
        }
    }
}

