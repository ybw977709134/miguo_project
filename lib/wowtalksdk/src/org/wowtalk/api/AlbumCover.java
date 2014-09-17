package org.wowtalk.api;

/**
 * Album cover is a photo displayed on user's moments page.
 */
public class AlbumCover {
    public String fileId;
    public String ext;
    public long timestamp;

    public AlbumCover() {
    }

    public AlbumCover(String fileId, String ext) {
        this.fileId = fileId;
        this.ext = ext;
    }
}
