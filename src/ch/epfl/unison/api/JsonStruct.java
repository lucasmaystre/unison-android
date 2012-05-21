package ch.epfl.unison.api;


public abstract class JsonStruct {

    public static class Error extends JsonStruct {

        public Integer error;
        public String message;
    }

    public static class Success extends JsonStruct {

        public Boolean success;
    }

    public static class User extends JsonStruct {

        public Long uid;
        public String nickname;
        public String email;
        public String password;
        public Long rid;
        public Integer score;
        public Boolean predicted;  // or isPredicted?
    }

    public static class Track extends JsonStruct {

        public String artist;
        public String title;
        public String image;
        public Integer localId;
        public Integer rating;

        public Track() {}

        public Track(int localId, String artist, String title) {
            this.localId = localId;
            this.artist = artist;
            this.title = title;
        }
    }

    public static class Room extends JsonStruct {

        public Long rid;
        public String name;
        public Track track;
        public Float distance;
        public User master;
        public User[] users;
        public Integer nbUsers;
    }

    public static class RoomsList extends JsonStruct {

        public Room[] rooms;
    }

    public static class RatingsList extends JsonStruct {

        public Track[] ratings;
    }

    public static class Delta extends JsonStruct {

        public static final String TYPE_PUT = "PUT";
        public static final String TYPE_DELETE = "DELETE";

        public String type;
        public Track entry;

        public Delta() {}

        public Delta(String type, int localId, String artist, String title) {
            this.type = type;
            this.entry = new Track(localId, artist, title);
        }
    }
}
