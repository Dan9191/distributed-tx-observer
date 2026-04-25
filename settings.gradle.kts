rootProject.name = "distributed-operation-observer"

include(
    "observer-service",
    "demo-services:order-service",
    "demo-services:payment-service",
    "demo-services:inventory-service",
    "demo-services:notification-service",
)
