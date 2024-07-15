// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "NrfBluetoothMesh",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "NrfBluetoothMesh",
            targets: ["NrfMeshPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "NrfMeshPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/NrfMeshPlugin"),
        .testTarget(
            name: "NrfMeshPluginTests",
            dependencies: ["NrfMeshPlugin"],
            path: "ios/Tests/NrfMeshPluginTests")
    ]
)