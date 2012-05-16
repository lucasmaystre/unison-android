package ch.epfl.unison;


public class MusicItem implements Comparable<MusicItem> {

    public final int localId;
    public final String artist;
    public final String title;

    public MusicItem(int localId, String artist, String title) {
        this.localId = localId;
        this.artist = artist;
        this.title = title;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artist == null) ? 0 : artist.hashCode());
        result = prime * result + localId;
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MusicItem other = (MusicItem) obj;
        if (artist == null) {
            if (other.artist != null)
                return false;
        } else if (!artist.equals(other.artist))
            return false;
        if (localId != other.localId)
            return false;
        if (title == null) {
            if (other.title != null)
                return false;
        } else if (!title.equals(other.title))
            return false;
        return true;
    }

    public int compareTo(MusicItem another) {
        int artistComp = this.artist.compareTo(another.artist);
        if (artistComp != 0)
            return artistComp;

        int titleComp = this.title.compareTo(another.title);
        if (titleComp != 0)
            return titleComp;

        if (this.localId < another.localId) {
            return -1;
        } else if (this.localId > another.localId) {
            return 1;
        }

        return 0;
    }

}
