package com.HyperX.MI.Na.cmx
internal enum class PlaylistCategory(val label: String) {
    ALL("全部"), CREATED("自建"), COLLECTED("收藏");

    fun includes(playlist: Playlist, accountId: Long): Boolean = when (this) {
        ALL -> true
        CREATED -> if (playlist.source in setOf("netease", "qqmusic")) playlist.nativeOwned else playlist.editable(accountId)
        // Imported snapshots do not carry the original creator, so keep them in All.
        COLLECTED -> playlist.source in setOf("netease", "qqmusic") && !playlist.nativeOwned
    }
}
