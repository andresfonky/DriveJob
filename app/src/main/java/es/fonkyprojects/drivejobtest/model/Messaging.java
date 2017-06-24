package es.fonkyprojects.drivejob.model;

public class Messaging {

    private String username;
    private String userIdDestination;
    private String key;
    private int value;


    public Messaging() {
    }

    public Messaging(String username, String userId, String key, int value) {
        this.username = username;
        this.userIdDestination = userId;
        this.key = key;
        this.value = value;
    }

    public String getUsername() {
        return username;
    }

    public String getUserIdDestination() {
        return userIdDestination;
    }

    public String getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserIdDestination(String userIdDestination) {
        this.userIdDestination = userIdDestination;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(int value) {
        this.value = value;
    }
}