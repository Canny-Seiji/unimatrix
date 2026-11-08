package com.unimatrix.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public final class AccountDAO {
    public record Account(int id,String username,String iconPath){ }
    private static final SecureRandom RANDOM=new SecureRandom();
    public static Account authenticate(String username,String password)throws SQLException{
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,username,icon_path,password_salt,password_hash FROM accounts WHERE username=?")){p.setString(1,username);try(ResultSet r=p.executeQuery()){if(!r.next()||!matches(password,r.getString(4),r.getString(5)))return null;return new Account(r.getInt(1),r.getString(2),r.getString(3));}}
    }
    public static Account find(String username)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,username,icon_path FROM accounts WHERE username=?")){p.setString(1,username);try(ResultSet r=p.executeQuery()){return r.next()?new Account(r.getInt(1),r.getString(2),r.getString(3)):null;}}}
    public static void deleteByUsername(String username)throws SQLException{if("admin".equalsIgnoreCase(username))throw new IllegalArgumentException("The hardcoded admin account cannot be deleted.");try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM accounts WHERE username=?")){p.setString(1,username);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Account no longer exists.");}}
    public static Account create(String username,String password)throws SQLException{
        if(username==null||!username.matches("[A-Za-z0-9_.-]{3,30}"))throw new IllegalArgumentException("Username must be 3-30 letters, numbers, dots, dashes, or underscores.");
        if(password==null||password.length()<6)throw new IllegalArgumentException("Password must contain at least 6 characters.");
        byte[] salt=new byte[16];RANDOM.nextBytes(salt);String encodedSalt=Base64.getEncoder().encodeToString(salt),hash=hash(password,salt);
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO accounts(username,password_salt,password_hash) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){p.setString(1,username);p.setString(2,encodedSalt);p.setString(3,hash);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();return new Account(r.getInt(1),username,null);}}
        catch(SQLIntegrityConstraintViolationException e){throw new IllegalArgumentException("That username is already in use.");}
    }
    public static void update(int id,String username,String password,String iconPath)throws SQLException{
        if(username==null||!username.matches("[A-Za-z0-9_.-]{3,30}"))throw new IllegalArgumentException("Username must be 3-30 letters, numbers, dots, dashes, or underscores.");
        try(Connection c=DatabaseConnection.getConnection()){
            if(password!=null&&!password.isBlank()){if(password.length()<6)throw new IllegalArgumentException("Password must contain at least 6 characters.");byte[] salt=new byte[16];RANDOM.nextBytes(salt);try(PreparedStatement p=c.prepareStatement("UPDATE accounts SET username=?,password_salt=?,password_hash=?,icon_path=? WHERE id=?")){p.setString(1,username);p.setString(2,Base64.getEncoder().encodeToString(salt));p.setString(3,hash(password,salt));p.setString(4,iconPath);p.setInt(5,id);p.executeUpdate();}}
            else try(PreparedStatement p=c.prepareStatement("UPDATE accounts SET username=?,icon_path=? WHERE id=?")){p.setString(1,username);p.setString(2,iconPath);p.setInt(3,id);p.executeUpdate();}
        }
    }
    public static void updateByUsername(String oldUsername,String username,String password,String iconPath)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT id FROM accounts WHERE username=?")){p.setString(1,oldUsername);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("The hardcoded admin account cannot be edited here.");update(r.getInt(1),username,password,iconPath);}}}
    private static boolean matches(String password,String salt,String expected){return MessageDigest.isEqual(hash(password,Base64.getDecoder().decode(salt)).getBytes(StandardCharsets.UTF_8),expected.getBytes(StandardCharsets.UTF_8));}
    private static String hash(String password,byte[] salt){try{var spec=new javax.crypto.spec.PBEKeySpec(password.toCharArray(),salt,120000,256);return Base64.getEncoder().encodeToString(javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());}catch(Exception e){throw new IllegalStateException(e);}}
}
