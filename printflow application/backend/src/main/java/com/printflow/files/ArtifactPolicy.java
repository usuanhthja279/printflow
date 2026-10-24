package com.printflow.files;
import java.util.*;
/** Admission policy only. DesignProofs requires a clean native scan and verified private storage before reads. */
public final class ArtifactPolicy {
 public static final long MAX_BYTES=20L*1024*1024;
 public enum State { QUARANTINED,CLEAN,REJECTED }
 private ArtifactPolicy(){}
 public static State inspect(String name,String mime,long size,byte[] prefix){
  if(name==null||name.length()>180||name.contains("/")||name.contains("\\")||name.contains("..")||name.chars().anyMatch(c->c<32)||size<1||size>MAX_BYTES||prefix==null)throw new IllegalArgumentException("Unsafe filename or size");
  String lower=name.toLowerCase(Locale.ROOT);boolean valid=false;
  if(lower.endsWith(".pdf")&&"application/pdf".equals(mime))valid=starts(prefix,new byte[]{37,80,68,70,45});
  if(lower.endsWith(".png")&&"image/png".equals(mime))valid=starts(prefix,new byte[]{(byte)137,80,78,71,13,10,26,10});
  if((lower.endsWith(".jpg")||lower.endsWith(".jpeg"))&&"image/jpeg".equals(mime))valid=starts(prefix,new byte[]{(byte)255,(byte)216,(byte)255});
  if(!valid)throw new IllegalArgumentException("Only matching PDF, PNG and JPEG types are admitted to quarantine");
  return State.QUARANTINED;
 }
 public static boolean downloadable(State state){return state==State.CLEAN;}
 private static boolean starts(byte[] input,byte[] expected){if(input.length<expected.length)return false;for(int i=0;i<expected.length;i++)if(input[i]!=expected[i])return false;return true;}
}
