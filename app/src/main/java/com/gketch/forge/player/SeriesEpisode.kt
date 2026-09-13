package com.gketch.forge.player

import com.gketch.forge.data.ForgeMediaItem

/**
 * Detect SxxExx / Episode-style filenames and pick the next episode in a folder.
 * Crash-isolated at call sites — pure parsing, no I/O.
 */
data class EpisodeMark(
    val season: Int?,
    val episode: Int,
    val sortKey: Long,
)

object SeriesEpisode {
    // S01E02, s1e2, S01.E02, S01_E02
    private val SXEX = Regex(
        """[Ss](\d{1,2})[.\-_ ]?[Ee](\d{1,3})""",
    )
    // 1x02 / 01x02
    private val NXNN = Regex(
        """(?:^|[^0-9])(\d{1,2})[xX](\d{1,3})(?:[^0-9]|$)""",
    )
    // Episode 2, Ep 02, EP.2, E02 (no season)
    private val EP_ONLY = Regex(
        """(?:^|[^A-Za-z])(?:[Ee]p(?:isode)?|[Ee])[.\-_ ]?(\d{1,3})(?:[^0-9]|$)""",
    )

    fun parse(name: String): EpisodeMark? {
        val base = name.substringBeforeLast('.').ifBlank { name }
        SXEX.find(base)?.let { m ->
            val season = m.groupValues[1].toIntOrNull() ?: return@let
            val ep = m.groupValues[2].toIntOrNull() ?: return@let
            return EpisodeMark(season, ep, season * 10_000L + ep)
        }
        NXNN.find(base)?.let { m ->
            val season = m.groupValues[1].toIntOrNull() ?: return@let
            val ep = m.groupValues[2].toIntOrNull() ?: return@let
            return EpisodeMark(season, ep, season * 10_000L + ep)
        }
        EP_ONLY.find(base)?.let { m ->
            val ep = m.groupValues[1].toIntOrNull() ?: return@let
            return EpisodeMark(null, ep, ep.toLong())
        }
        return null
    }

    fun looksLikeEpisode(name: String): Boolean = parse(name) != null

    /**
     * Next episode in [folderItems] after [current] by season/episode order.
     * Prefers same season + episode+1; otherwise next higher sortKey.
     */
    fun findNext(current: ForgeMediaItem, folderItems: List<ForgeMediaItem>): ForgeMediaItem? {
        val curMark = parse(current.title) ?: return null
        val candidates = folderItems
            .asSequence()
            .filter { it.uri.toString() != current.uri.toString() }
            .filter { it.kind == current.kind }
            .mapNotNull { item ->
                val mark = parse(item.title) ?: return@mapNotNull null
                // Same season scheme: both have season or both lack it
                if ((curMark.season == null) != (mark.season == null)) return@mapNotNull null
                if (curMark.season != null && mark.season != curMark.season) {
                    // Allow next season only when episode rolls (season+1, ep small)
                    if (mark.season != curMark.season + 1) return@mapNotNull null
                }
                item to mark
            }
            .filter { (_, mark) -> mark.sortKey > curMark.sortKey }
            .sortedBy { it.second.sortKey }
            .toList()
        return candidates.firstOrNull()?.first
    }
}
