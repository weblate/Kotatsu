package org.koitharu.kotatsu.favourites.domain

import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toManga
import org.koitharu.kotatsu.core.db.entity.toMangaTags
import org.koitharu.kotatsu.favourites.data.FavouriteManga
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.list.domain.ListSortOrder
import org.koitharu.kotatsu.local.data.LocalMangaRepository
import org.koitharu.kotatsu.local.domain.LocalObserveMapper
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

@Reusable
class LocalFavoritesObserver @Inject constructor(
	localMangaRepository: LocalMangaRepository,
	private val db: MangaDatabase,
) : LocalObserveMapper<FavouriteManga, Manga>(localMangaRepository, limitStep = 10) {

	fun observeAll(
		order: ListSortOrder,
		filterOptions: Set<ListFilterOption>,
		limit: Int
	): Flow<List<Manga>> = observe(limit) { newLimit ->
		db.getFavouritesDao().observeAll(order, filterOptions, newLimit)
	}

	fun observeAll(
		categoryId: Long,
		order: ListSortOrder,
		filterOptions: Set<ListFilterOption>,
		limit: Int
	): Flow<List<Manga>> = observe(limit) { newLimit ->
		db.getFavouritesDao().observeAll(categoryId, order, filterOptions, newLimit)
	}

	override fun toManga(e: FavouriteManga) = e.manga.toManga(e.tags.toMangaTags())

	override fun toResult(e: FavouriteManga, manga: Manga) = manga
}