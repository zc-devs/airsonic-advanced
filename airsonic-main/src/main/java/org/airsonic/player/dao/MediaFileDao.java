/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2023 (C) Y.Tory, Yetangitu
 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.dao;

import com.google.common.collect.ImmutableMap;
import org.airsonic.player.domain.MediaFile;
import org.airsonic.player.domain.MusicFolder;
import org.airsonic.player.domain.RandomSearchCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Provides database services for media files.
 *
 * @author Sindre Mehus
 */
@Repository
public class MediaFileDao extends AbstractDao {
    private static final Logger LOG = LoggerFactory.getLogger(MediaFileDao.class);
    private static final String INSERT_COLUMNS = "path, folder_id, type, start_position, format, title, album, artist, album_artist, disc_number, " +
                                                "track_number, year, genre, bit_rate, variable_bit_rate, duration, file_size, width, height, " +
                                                "parent_path, index_path, play_count, last_played, comment, created, changed, last_scanned, " +
                                                "children_last_updated, present, version, mb_release_id, mb_recording_id";

    private static final String QUERY_COLUMNS = "id, " + INSERT_COLUMNS;

    public static final int VERSION = 4;

    private final MediaFileMapper rowMapper = new MediaFileMapper();

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void createOrUpdateMediaFile(MediaFile file, Consumer<MediaFile> preInsertionCallback) {
        LOG.trace("Creating/Updating new media file (id {}, path {}, fid {}, spos {}, dur {}, ip {}, type {})",
                    file.getId(), file.getPath(), file.getFolderId(), file.getStartPosition(), file.getDuration(), file.getIndexPath(), file.getMediaType().name());
        String sql = null;
        if (file.getId() != null) {
            sql = "" +
                    "update media_file set " +
                    "path=:path," +
                    "folder_id=:fid," +
                    "type=:type," +
                    "start_position=:spos," +
                    "format=:format," +
                    "title=:title," +
                    "album=:album," +
                    "artist=:artist," +
                    "album_artist=:albumartist," +
                    "disc_number=:dn," +
                    "track_number=:tn," +
                    "year=:year," +
                    "genre=:genre," +
                    "bit_rate=:br," +
                    "variable_bit_rate=:vbr," +
                    "duration=:dur," +
                    "file_size=:fs," +
                    "width=:w," +
                    "height=:h," +
                    "parent_path=:pp," +
                    "index_path=:ip," +
                    "play_count=:pc," +
                    "last_played=:lp," +
                    "comment=:comment," +
                    "changed=:changed," +
                    "last_scanned=:ls," +
                    "children_last_updated=:clu," +
                    "present=:pres," +
                    "version=:ver," +
                    "mb_release_id=:mbrelid," +
                    "mb_recording_id=:mbrecid " +
                    "where id=:id";
        } else {
            sql = "update media_file set " +
                    "type=:type," +
                    "format=:format," +
                    "title=:title," +
                    "album=:album," +
                    "artist=:artist," +
                    "album_artist=:albumartist," +
                    "disc_number=:dn," +
                    "track_number=:tn," +
                    "year=:year," +
                    "genre=:genre," +
                    "bit_rate=:br," +
                    "variable_bit_rate=:vbr," +
                    "duration=:dur," +
                    "file_size=:fs," +
                    "width=:w," +
                    "height=:h," +
                    "parent_path=:pp," +
                    "index_path=:ip," +
                    "play_count=:pc," +
                    "last_played=:lp," +
                    "comment=:comment," +
                    "changed=:changed," +
                    "last_scanned=:ls," +
                    "children_last_updated=:clu," +
                    "present=:pres," +
                    "version=:ver," +
                    "mb_release_id=:mbrelid," +
                    "mb_recording_id=:mbrecid " +
                    "where path=:path and folder_id=:fid and start_position=:spos";
        }

        Map<String, Object> args = new HashMap<>();
        args.put("id", file.getId());
        args.put("path", file.getPath());
        args.put("fid", file.getFolderId());
        args.put("type", file.getMediaType().name());
        args.put("spos", file.getStartPosition());
        args.put("format", file.getFormat());
        args.put("title", file.getTitle());
        args.put("album", file.getAlbumName());
        args.put("artist", file.getArtist());
        args.put("albumartist", file.getAlbumArtist());
        args.put("dn", file.getDiscNumber());
        args.put("tn", file.getTrackNumber());
        args.put("year", file.getYear());
        args.put("genre", file.getGenre());
        args.put("br", file.getBitRate());
        args.put("vbr", file.isVariableBitRate());
        args.put("dur", file.getDuration());
        args.put("fs", file.getFileSize());
        args.put("w", file.getWidth());
        args.put("h", file.getHeight());
        args.put("pp", file.getParentPath());
        args.put("ip", file.getIndexPath());
        args.put("pc", file.getPlayCount());
        args.put("lp", file.getLastPlayed());
        args.put("comment", file.getComment());
        args.put("changed", file.getChanged());
        args.put("ls", file.getLastScanned());
        args.put("clu", file.getChildrenLastUpdated());
        args.put("pres", file.isPresent());
        args.put("ver", VERSION);
        args.put("mbrelid", file.getMusicBrainzReleaseId());
        args.put("mbrecid", file.getMusicBrainzRecordingId());

        int n = namedUpdate(sql, args);

        if (n == 0) {

            preInsertionCallback.accept(file);

            update("insert into media_file (" + INSERT_COLUMNS + ") values (" + questionMarks(INSERT_COLUMNS) + ")",
                   file.getPath(), file.getFolderId(), file.getMediaType().name(), file.getStartPosition(), file.getFormat(), file.getTitle(),
                   file.getAlbumName(), file.getArtist(), file.getAlbumArtist(), file.getDiscNumber(), file.getTrackNumber(), file.getYear(),
                   file.getGenre(), file.getBitRate(), file.isVariableBitRate(), file.getDuration(), file.getFileSize(), file.getWidth(), file.getHeight(),
                   file.getParentPath(), file.getIndexPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(), file.getCreated(), file.getChanged(),
                   file.getLastScanned(), file.getChildrenLastUpdated(), file.isPresent(), VERSION, file.getMusicBrainzReleaseId(), file.getMusicBrainzRecordingId());
        }

        if (file.getId() == null) {
            try {
                Integer id = queryForInt("select id from media_file where path=? and folder_id=? and start_position=?",
                                    null, file.getPath(), file.getFolderId(), file.getStartPosition());
                file.setId(id);
            } catch (Exception e) {
                LOG.warn("failure getting id for mediaFile {}", file, e);
            }
        }
    }


    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria, final String username) {
        if (criteria.getMusicFolders().isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> args = new HashMap<>();
        args.put("folders", MusicFolder.toIdList(criteria.getMusicFolders()));
        args.put("username", username);
        args.put("fromYear", criteria.getFromYear());
        args.put("toYear", criteria.getToYear());
        args.put("genre", criteria.getGenre());
        args.put("minLastPlayed", criteria.getMinLastPlayedDate());
        args.put("maxLastPlayed", criteria.getMaxLastPlayedDate());
        args.put("minAlbumRating", criteria.getMinAlbumRating());
        args.put("maxAlbumRating", criteria.getMaxAlbumRating());
        args.put("minPlayCount", criteria.getMinPlayCount());
        args.put("maxPlayCount", criteria.getMaxPlayCount());
        args.put("starred", criteria.isShowStarredSongs());
        args.put("unstarred", criteria.isShowUnstarredSongs());
        args.put("format", criteria.getFormat());

        boolean joinAlbumRating = (criteria.getMinAlbumRating() != null || criteria.getMaxAlbumRating() != null);
        boolean joinStarred = (criteria.isShowStarredSongs() ^ criteria.isShowUnstarredSongs());

        String query = "select " + prefix(QUERY_COLUMNS, "media_file") + " from media_file ";

        if (joinStarred) {
            query += "left outer join starred_media_file on media_file.id = starred_media_file.media_file_id and starred_media_file.username = :username ";
        }

        if (joinAlbumRating) {
            query += "left outer join media_file media_album on media_album.type = 'ALBUM' and media_album.album = media_file.album and media_album.artist = media_file.artist ";
            query += "left outer join user_rating on user_rating.media_file_id = media_album.id and user_rating.username = :username ";
        }

        query += " where media_file.present and media_file.type = 'MUSIC'";
        query += " and media_file.index_path is null"; // exclude indexed files

        if (!criteria.getMusicFolders().isEmpty()) {
            query += " and media_file.folder_id in (:folders)";
        }

        if (criteria.getGenre() != null) {
            query += " and media_file.genre = :genre";
        }

        if (criteria.getFormat() != null) {
            query += " and media_file.format = :format";
        }

        if (criteria.getFromYear() != null) {
            query += " and media_file.year >= :fromYear";
        }

        if (criteria.getToYear() != null) {
            query += " and media_file.year <= :toYear";
        }

        if (criteria.getMinLastPlayedDate() != null) {
            query += " and media_file.last_played >= :minLastPlayed";
        }

        if (criteria.getMaxLastPlayedDate() != null) {
            if (criteria.getMinLastPlayedDate() == null) {
                query += " and (media_file.last_played is null or media_file.last_played <= :maxLastPlayed)";
            } else {
                query += " and media_file.last_played <= :maxLastPlayed";
            }
        }

        if (criteria.getMinAlbumRating() != null) {
            query += " and user_rating.rating >= :minAlbumRating";
        }

        if (criteria.getMaxAlbumRating() != null) {
            if (criteria.getMinAlbumRating() == null) {
                query += " and (user_rating.rating is null or user_rating.rating <= :maxAlbumRating)";
            } else {
                query += " and user_rating.rating <= :maxAlbumRating";
            }
        }

        if (criteria.getMinPlayCount() != null) {
            query += " and media_file.play_count >= :minPlayCount";
        }

        if (criteria.getMaxPlayCount() != null) {
            if (criteria.getMinPlayCount() == null) {
                query += " and (media_file.play_count is null or media_file.play_count <= :maxPlayCount)";
            } else {
                query += " and media_file.play_count <= :maxPlayCount";
            }
        }

        if (criteria.isShowStarredSongs() && !criteria.isShowUnstarredSongs()) {
            query += " and starred_media_file.id is not null";
        }

        if (criteria.isShowUnstarredSongs() && !criteria.isShowStarredSongs()) {
            query += " and starred_media_file.id is null";
        }

        query += " order by rand()";

        query += " limit " + criteria.getCount();

        return namedQuery(query, rowMapper, args);
    }

    public boolean markPresent(Map<Integer, Set<String>> paths, Instant lastScanned) {
        if (!paths.isEmpty()) {
            return paths.entrySet().parallelStream().map(e -> {
                int batches = (e.getValue().size() - 1) / 30000;
                List<String> pList = new ArrayList<>(e.getValue());

                return IntStream.rangeClosed(0, batches).parallel().map(b -> {
                    List<String> batch = pList.subList(b * 30000, Math.min(e.getValue().size(), b * 30000 + 30000));
                    return namedUpdate(
                            "update media_file set present=true, last_scanned = :lastScanned where path in (:paths) and folder_id=:folderId",
                            ImmutableMap.of("lastScanned", lastScanned, "paths", batch, "folderId", e.getKey()));
                }).sum() == e.getValue().size();
            }).reduce(true, (a, b) -> a && b);
        }

        return true;
    }

    public void markNonPresent(Instant lastScanned) {
        Instant childrenLastUpdated = Instant.ofEpochMilli(1);  // Used to force a children rescan if file is later resurrected.

        update("update media_file set present=false, children_last_updated=? where last_scanned < ? and present",
                childrenLastUpdated, lastScanned);
    }


    private static class MediaFileMapper implements RowMapper<MediaFile> {
        @Override
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MediaFile(
                    rs.getInt("id"),
                    rs.getString("path"),
                    rs.getInt("folder_id"),
                    MediaFile.MediaType.valueOf(rs.getString("type")),
                    rs.getDouble("start_position"),
                    rs.getString("format"),
                    rs.getString("title"),
                    rs.getString("album"),
                    rs.getString("artist"),
                    rs.getString("album_artist"),
                    rs.getInt("disc_number") == 0 ? null : rs.getInt("disc_number"),
                    rs.getInt("track_number") == 0 ? null : rs.getInt("track_number"),
                    rs.getInt("year") == 0 ? null : rs.getInt("year"),
                    rs.getString("genre"),
                    rs.getInt("bit_rate") == 0 ? null : rs.getInt("bit_rate"),
                    rs.getBoolean("variable_bit_rate"),
                    rs.getDouble("duration"),
                    rs.getLong("file_size") == 0 ? null : rs.getLong("file_size"),
                    rs.getInt("width") == 0 ? null : rs.getInt("width"),
                    rs.getInt("height") == 0 ? null : rs.getInt("height"),
                    rs.getString("parent_path"),
                    rs.getString("index_path"),
                    rs.getInt("play_count"),
                    Optional.ofNullable(rs.getTimestamp("last_played")).map(x -> x.toInstant()).orElse(null),
                    rs.getString("comment"),
                    Optional.ofNullable(rs.getTimestamp("created")).map(x -> x.toInstant()).orElse(null),
                    Optional.ofNullable(rs.getTimestamp("changed")).map(x -> x.toInstant()).orElse(null),
                    Optional.ofNullable(rs.getTimestamp("last_scanned")).map(x -> x.toInstant()).orElse(null),
                    Optional.ofNullable(rs.getTimestamp("children_last_updated")).map(x -> x.toInstant()).orElse(null),
                    rs.getBoolean("present"),
                    rs.getInt("version"),
                    rs.getString("mb_release_id"),
                    rs.getString("mb_recording_id"));
        }
    }

}
