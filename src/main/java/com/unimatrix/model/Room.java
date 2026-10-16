package com.unimatrix.model;

public class Room {
    private int id;
    private String roomName;
    private String building;
    private int capacity;

    public Room(int id, String roomName, String building, int capacity) {
        this.id = id;
        this.roomName = roomName;
        this.building = building;
        this.capacity = capacity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    @Override
    public String toString() {
        return roomName + " (" + building + " - Cap: " + capacity + ")";
    }
}