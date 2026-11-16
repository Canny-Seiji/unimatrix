package com.unimatrix.service;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
public final class ProfilePictureService {
 private static final Path DIRECTORY=Path.of("profilepics"); private ProfilePictureService(){}
 public static Path resolve(String username,String storedPath)throws IOException{Files.createDirectories(DIRECTORY);if("admin".equalsIgnoreCase(username)){for(String name:new String[]{"admin.jpg","admin.jpeg","admin.png"}){Path p=DIRECTORY.resolve(name);if(Files.exists(p))return p;}return null;}if(storedPath==null||storedPath.isBlank())return null;Path p=Path.of(storedPath);return Files.exists(p)?p:null;}
 public static Path storeUserPicture(String username,Path source)throws IOException{Files.createDirectories(DIRECTORY);String base=username.replaceAll("[^A-Za-z0-9_.-]","_");String name=source.getFileName().toString();int dot=name.lastIndexOf('.');String ext=dot<0?".jpg":name.substring(dot).toLowerCase();if(!ext.matches("\\.(png|jpg|jpeg|gif|bmp)"))ext=".jpg";Path target=DIRECTORY.resolve(base+ext);Files.copy(source,target,StandardCopyOption.REPLACE_EXISTING);return target;}
 public static ImageIcon loadIcon(String username,String storedPath,int size,int ignored)throws IOException{Path path=resolve(username,storedPath);if(path==null)return null;BufferedImage source=ImageIO.read(path.toFile());if(source==null)return null;int side=Math.min(source.getWidth(),source.getHeight());int x=(source.getWidth()-side)/2,y=(source.getHeight()-side)/2;BufferedImage square=source.getSubimage(x,y,side,side);BufferedImage scaled=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);Graphics2D g=scaled.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.drawImage(square,0,0,size,size,null);g.dispose();return new ImageIcon(scaled);}
}
