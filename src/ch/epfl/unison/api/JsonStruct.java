package ch.epfl.unison.api;


public abstract class JsonStruct {

    public static class Error extends JsonStruct {
        public Integer error;
        public String message;
    }

    public static class Success extends JsonStruct {
        public boolean success;
    }

    public static class User extends JsonStruct {
        public Long uid;
        public String nickname;
        public Long rid;
        public String email;
        public String password;
        public Integer score;
        public Boolean predicted;
    }

    public static class Track extends JsonStruct {
        public String artist;
        public String title;
        public Integer localId;
        public Integer rating;
    }

    public static class Room extends JsonStruct {
        public Long rid;
        public String name;
        public Track track;
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
        public String type;
        public Track entry;
    }
}
