package org.airsonic.player.service;

import org.airsonic.player.domain.Album;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.entity.StarredAlbum;
import org.airsonic.player.repository.AlbumRepository;
import org.airsonic.player.repository.OffsetBasedPageRequest;
import org.airsonic.player.repository.StarredAlbumRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final StarredAlbumRepository starredAlbumRepository;

    public AlbumService(AlbumRepository albumRepository, StarredAlbumRepository starredAlbumRepository) {
        this.albumRepository = albumRepository;
        this.starredAlbumRepository = starredAlbumRepository;
    }

    /**
     * Get album by id
     *
     * @param albumId album id to get
     * @return album or null if not found
     */
    public Album getAlbum(int albumId) {
        return albumRepository.findById(albumId).orElse(null);
    }

    /**
     * Get album by artist and name
     *
     * @param mediaFile media file to get album for
     * @return album or null if not found
     */
    public Album getAlbumByMediaFile(MediaFile mediaFile) {
        if (mediaFile == null || mediaFile.getAlbumArtist() == null || mediaFile.getAlbumName() == null) {
            return null;
        }
        return albumRepository.findByArtistAndName(mediaFile.getAlbumArtist(), mediaFile.getAlbumName()).orElse(null);
    }

    /**
     * Get album by artist name in music folders
     *
     * @param artist       artist name to get album for
     * @param musicFolders music folders to search in
     * @return albums or empty list if not found
     */
    public List<Album> getAlbumsByArtist(String artist, List<MusicFolder> musicFolders) {
        if (artist == null || CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return albumRepository.findByArtistAndFolderIdInAndPresentTrue(artist, MusicFolder.toIdList(musicFolders));
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param byArtist     Whether to sort by artist name
     * @param musicFolders Only return albums from these folders.
     * @param ignoreCase   Use case insensitive sorting
     * @return Albums in alphabetical order.
     */
    public List<Album> getAlphabeticalAlbums(boolean byArtist, boolean ignoreCase, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }

        Sort sortByArtist = ignoreCase ? Sort.by(Order.asc("artist").ignoreCase()) : Sort.by(Order.asc("artist"));
        Sort sortByAlbum = ignoreCase ? Sort.by(Order.asc("name").ignoreCase()) : Sort.by(Order.asc("name"));
        Sort sortById = Sort.by(Order.asc("id"));
        Sort sort = byArtist ? sortByArtist.and(sortByAlbum) : sortByAlbum;

        return albumRepository.findByFolderIdInAndPresentTrue(MusicFolder.toIdList(musicFolders),
                sort.and(sortById));
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param byArtist     Whether to sort by artist name
     * @param musicFolders Only return albums from these folders.
     * @param ignoreCase   Use case insensitive sorting
     * @return Albums in alphabetical order.
     */
    public List<Album> getAlphabeticalAlbums(int offset, int size, boolean byArtist, boolean ignoreCase,
            List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }

        Sort sortByArtist = ignoreCase ? Sort.by(Order.asc("artist").ignoreCase()) : Sort.by(Order.asc("artist"));
        Sort sortByAlbum = ignoreCase ? Sort.by(Order.asc("name").ignoreCase()) : Sort.by(Order.asc("name"));
        Sort sortById = Sort.by(Order.asc("id"));
        Sort sort = byArtist ? sortByArtist.and(sortByAlbum) : sortByAlbum;

        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(offset, size, sort.and(sortById));
        return albumRepository.findByFolderIdInAndPresentTrue(MusicFolder.toIdList(musicFolders), pageRequest);
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return The most frequently played albums.
     */
    public List<Album> getMostFrequentlyPlayedAlbums(int offset, int size, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(offset, size,
                Sort.by(Order.desc("playCount"), Order.asc("id")));
        return albumRepository.findByFolderIdInAndPlayCountGreaterThanAndPresentTrue(
                MusicFolder.toIdList(musicFolders), 0, pageRequest);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently played albums.
     */
    public List<Album> getMostResentlyPlayedAlbums(int offset, int size, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(offset, size,
                Sort.by(Order.desc("lastPlayed"), Order.asc("id")));
        return albumRepository.findByFolderIdInAndLastPlayedNotNullAndPresentTrue(
                MusicFolder.toIdList(musicFolders), pageRequest);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return The most recently added albums.
     */
    public List<Album> getRecentlyAddedAlbums(int offset, int size, List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(offset, size,
                Sort.by(Order.desc("created"), Order.desc("id")));
        return albumRepository.findByFolderIdInAndPresentTrue(MusicFolder.toIdList(musicFolders), pageRequest);
    }

    /**
     * Returns albums in a genre.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param genre        The genre name.
     * @param musicFolders Only return albums from these folders.
     * @return Albums in the genre.
     */
    public List<Album> getAlbumsByGenre(int offset, int count, String genre, List<MusicFolder> musicFolders) {
        if (genre == null || CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(offset, count, Sort.by(Order.asc("id")));

        return albumRepository.findByGenreAndFolderIdInAndPresentTrue(genre, MusicFolder.toIdList(musicFolders),
                pageRequest);
    }

    /**
     * Returns albums in a year range.
     *
     * @param offset       Number of albums to skip.
     * @param count        Maximum number of albums to return.
     * @param startYear    The start year.
     * @param endYear      The end year.
     * @param musicFolders Only return albums from these folders.
     * @return Albums in the year range.
     */
    public List<Album> getAlbumsByYear(int offset, int count, int startYear, int endYear,
            List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        Sort sort = (startYear <= endYear) ? Sort.by(Order.asc("year"), Order.asc("id"))
                : Sort.by(Order.desc("year"), Order.asc("id"));
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(offset, count, sort);

        return albumRepository.findByFolderIdInAndYearBetweenAndPresentTrue(MusicFolder.toIdList(musicFolders),
                startYear, endYear, pageRequest);
    }

    /**
     * Star or unstar album for user
     *
     * @param albumId  album id to star
     * @param username username to star album for
     * @param star     true to star, false to unstar
     * @return true if success, false if otherwise
     */
    public boolean starOrUnstar(int albumId, String username, boolean star) {
        if (!StringUtils.hasLength(username)) {
            return false;
        }
        return albumRepository.findById(albumId).map(album -> {
            if (star) {
                starredAlbumRepository.findByAlbumAndUsername(album, username).ifPresentOrElse(starredAlbum -> {
                    // already starred
                }, () -> {
                    // not starred yet
                        starredAlbumRepository.save(new StarredAlbum(album, username, Instant.now()));
                    });
            } else {
                starredAlbumRepository.deleteByAlbumAndUsername(album, username);
            }
            return true;
        }).orElse(false);
    }

    /**
     * Get date of album star
     *
     * @param albumId  album id to get star date for
     * @param username username to get star date for
     * @return date of album star or null if not starred
     */
    public Instant getAlbumStarredDate(Integer albumId, String username) {
        if (albumId == null || !StringUtils.hasLength(username)) {
            return null;
        }
        return albumRepository.findByIdAndStarredAlbumsUsername(albumId, username)
                .map(album -> album.getStarredAlbums().isEmpty() ? null : album.getStarredAlbums().get(0).getCreated())
                .orElse(null);
    }

    /**
     * Get starred albums for user
     *
     * @param username     username to get starred albums for
     * @param musicFolders music folders to search in
     * @return list of starred albums
     */
    public List<Album> getStarredAlbums(String username, List<MusicFolder> musicFolders) {
        if (!StringUtils.hasLength(username) || CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        return starredAlbumRepository
                .findByUsernameAndAlbumFolderIdInAndAlbumPresentTrue(username, MusicFolder.toIdList(musicFolders),
                        Sort.by(Order.desc("created"), Order.asc("albumId")))
                .stream().map(StarredAlbum::getAlbum).toList();
    }

    /**
     * Get starred albums for user
     *
     * @param offset       offset
     * @param size         size
     * @param username     username to get starred albums for
     * @param musicFolders music folders to search in
     * @return list of starred albums
     */
    public List<Album> getStarredAlbums(int offset, int size, String username, List<MusicFolder> musicFolders) {
        if (!StringUtils.hasLength(username) || CollectionUtils.isEmpty(musicFolders)) {
            return Collections.emptyList();
        }
        OffsetBasedPageRequest pageRequest = new OffsetBasedPageRequest(offset, size,
                Sort.by(Order.desc("created"), Order.asc("albumId")));
        return starredAlbumRepository
                .findByUsernameAndAlbumFolderIdInAndAlbumPresentTrue(username, MusicFolder.toIdList(musicFolders),
                        pageRequest)
                .stream().map(StarredAlbum::getAlbum).toList();
    }

    /**
     * delete all albums that are not present
     */
    public void expunge() {
        albumRepository.deleteAllByPresentFalse();
    }

    /**
     * Get ids of albums that are not present
     *
     * @return list of album ids
     */
    public List<Integer> getExpungeIds() {
        return albumRepository.findByPresentFalse().stream().map(Album::getId).toList();
    }

    /**
     * Get count of albums in music folders
     *
     * @param musicFolders music folders to search in
     * @return count of albums
     */
    public int getAlbumCount(List<MusicFolder> musicFolders) {
        if (CollectionUtils.isEmpty(musicFolders)) {
            return 0;
        }
        return albumRepository.countByFolderIdInAndPresentTrue(MusicFolder.toIdList(musicFolders));
    }

}