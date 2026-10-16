package com.unimatrix.dao;

import com.unimatrix.model.Room;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

    // Fetch all rooms from the database
    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String query = "SELECT id, room_name, building, capacity FROM rooms";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String roomName = rs.getString("room_name");
                String building = rs.getString("building");
                int capacity = rs.getInt("capacity");

                rooms.add(new Room(id, roomName, building, capacity));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching rooms from database:");
            e.printStackTrace();
        }

        return rooms;
    }

    // Add a new room to the database
    public boolean addRoom(Room room) {
        String query = "INSERT INTO rooms (room_name, building, capacity) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, room.getRoomName());
            pstmt.setString(2, room.getBuilding());
            pstmt.setInt(3, room.getCapacity());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error inserting room into database:");
            e.printStackTrace();
            return false;
        }
    }

    // For testing purposes, don't touch it
    /* 
    public static void main(String[] args) {
        RoomDAO dao = new RoomDAO();

        System.out.println("--- Testing Room Insertion ---");
        Room testRoom = new Room(0, "Lab 401", "IT Building", 45); // Sample room for testing
        boolean success = dao.addRoom(testRoom);
        
        if (success) {
            System.out.println("SUCCESS: Test room inserted into database!");
        } else {
            System.out.println("FAILURE: Could not insert room.");
        }

        System.out.println("\n--- Testing Fetching All Rooms ---");
        List<Room> rooms = dao.getAllRooms();
        for (Room room : rooms) {
            System.out.println("Found Room -> ID: " + room.getId() + 
                               ", Name: " + room.getRoomName() + 
                               ", Building: " + room.getBuilding() + 
                               ", Capacity: " + room.getCapacity());
        }
    }
    */
}