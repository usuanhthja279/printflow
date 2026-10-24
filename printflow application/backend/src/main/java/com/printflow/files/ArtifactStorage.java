package com.printflow.files;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
@Component
public class ArtifactStorage {
 public record Stored(UUID id,long size,String hash){}
 private final Path root;private final Semaphore slots=new Semaphore(2);
 public ArtifactStorage(Environment env)throws IOException{
  if(Arrays.asList(env.getActiveProfiles()).contains("prod")&&env.getProperty("APP_ARTIFACT_ROOT","").isBlank())throw new IllegalArgumentException("Production requires explicit private artifact root");
  String configured=env.getProperty("APP_ARTIFACT_ROOT",".local/private-artifacts");if(configured.isBlank())throw new IllegalArgumentException("Artifact root must not be empty");
  root=Path.of(configured).toAbsolutePath().normalize();
  if(root.equals(Path.of("").toAbsolutePath().normalize())||root.getParent()==null)throw new IllegalArgumentException("Choose a dedicated private artifact directory");
  for(Path part:root)if(Set.of("public","static","www","wwwroot",".next").contains(part.toString().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Artifact root cannot be public");
  if(root.startsWith(Path.of("frontend").toAbsolutePath().normalize()))throw new IllegalArgumentException("Artifact root must be outside the frontend");
  Files.createDirectories(root);if(Files.isSymbolicLink(root)||!root.toRealPath().equals(root))throw new IllegalArgumentException("Artifact root must not contain links");privatePermissions(root,true);
 }
 public Stored write(InputStream input)throws IOException{
  if(!slots.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"File processing busy; retry shortly");
  UUID id=UUID.randomUUID();Path path=path(id);boolean success=false;
  try{
   MessageDigest hash=digest();long count=0;
   try(OutputStream out=Files.newOutputStream(path,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
    privatePermissions(path,false);byte[] buffer=new byte[16384];int n;
    while((n=input.read(buffer))!=-1){count+=n;if(count>ArtifactPolicy.MAX_BYTES)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"File exceeds 20 MiB");hash.update(buffer,0,n);out.write(buffer,0,n);}
   }
   if(count==0)throw new IllegalArgumentException("File is empty");success=true;return new Stored(id,count,HexFormat.of().formatHex(hash.digest()));
  }finally{if(!success)Files.deleteIfExists(path);slots.release();}
 }
 public Path path(UUID id){return root.resolve(id.toString()+".bin");}
 public byte[] prefix(UUID id)throws IOException{try(var in=Files.newInputStream(path(id),LinkOption.NOFOLLOW_LINKS)){return in.readNBytes(16);}}
 public void discard(UUID id)throws IOException{Files.deleteIfExists(path(id));}
 public InputStream verified(UUID id,long size,String expected)throws IOException{
  if(!slots.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"File processing busy; retry shortly");
  FileChannel channel=null;
  try{
   channel=FileChannel.open(path(id),StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS);
   if(channel.size()!=size)throw new IOException("Artifact integrity mismatch");MessageDigest hash=digest();var buffer=java.nio.ByteBuffer.allocate(16384);while(channel.read(buffer)>0){buffer.flip();hash.update(buffer);buffer.clear();}
   if(!MessageDigest.isEqual(hash.digest(),HexFormat.of().parseHex(expected)))throw new IOException("Artifact integrity mismatch");channel.position(0);final InputStream in=Channels.newInputStream(channel);
   return new FilterInputStream(in){boolean closed;@Override public void close()throws IOException{if(!closed){closed=true;try{super.close();}finally{slots.release();}}}};
  }catch(Exception e){if(channel!=null)channel.close();slots.release();if(e instanceof IOException io)throw io;throw e;}
 }
 private MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private static void privatePermissions(Path path,boolean directory)throws IOException{
  var posix=Files.getFileAttributeView(path,PosixFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);
  if(posix!=null){Files.setPosixFilePermissions(path,PosixFilePermissions.fromString(directory?"rwx------":"rw-------"));return;}
  var acl=Files.getFileAttributeView(path,AclFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);
  if(acl!=null){var entry=AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(Files.getOwner(path)).setPermissions(EnumSet.allOf(AclEntryPermission.class));if(directory)entry.setFlags(AclEntryFlag.FILE_INHERIT,AclEntryFlag.DIRECTORY_INHERIT);acl.setAcl(List.of(entry.build()));}
 }
}
