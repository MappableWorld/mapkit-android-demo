package world.mappable.navikitdemo.domain.helpers

enum class Permission {
    LOCATION,
    NOTIFICATIONS,
}

interface PermissionManager {
    fun request(permissions: List<Permission>)
}
